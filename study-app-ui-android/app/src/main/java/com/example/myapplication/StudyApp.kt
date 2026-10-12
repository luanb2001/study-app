package com.example.myapplication

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.core.data.APP_PREFERENCES_NAME
import com.example.myapplication.core.data.AppThemeMode
import com.example.myapplication.feature.account.data.local.AccountSessionStore
import com.example.myapplication.feature.account.data.local.DeviceIdentityStore
import com.example.myapplication.feature.account.data.local.TrialPeriodStore
import com.example.myapplication.feature.account.data.remote.StudyApiClient
import com.example.myapplication.feature.account.ui.AccountScreen
import com.example.myapplication.feature.onboarding.ui.OnboardingScreen
import com.example.myapplication.feature.pomodoro.data.local.PomodoroSessionStore
import com.example.myapplication.feature.pomodoro.data.service.PomodoroNotificationService
import com.example.myapplication.feature.pomodoro.ui.PomodoroPictureInPictureScreen
import com.example.myapplication.feature.study.data.local.LocalStudyRepository
import com.example.myapplication.feature.study.data.local.StudyDatabase
import com.example.myapplication.feature.study.data.mock.MockFlashcardGenerator
import com.example.myapplication.feature.study.data.reminder.ReviewReminderScheduler
import com.example.myapplication.feature.study.data.sync.PendingSyncConflict
import com.example.myapplication.feature.study.data.sync.StudySyncWorker
import com.example.myapplication.feature.study.domain.repository.StudyRepository
import com.example.myapplication.feature.study.ui.SyncConflictDialog
import com.example.myapplication.navigation.AppNavigation
import com.example.myapplication.navigation.Routes
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class BottomNavigationDestination(
    val route: String,
    val label: Int,
    val icon: ImageVector
)

private val bottomNavigationDestinations = listOf(
    BottomNavigationDestination(Routes.HOME, R.string.nav_home, Icons.Outlined.Home),
    BottomNavigationDestination(
        Routes.CALENDAR,
        R.string.nav_calendar,
        Icons.Outlined.CalendarMonth
    ),
    BottomNavigationDestination(
        Routes.SUBJECTS,
        R.string.nav_subjects,
        Icons.AutoMirrored.Outlined.MenuBook
    ),
    BottomNavigationDestination(Routes.PROFILE, R.string.nav_profile, Icons.Outlined.Person)
)

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

@Composable
fun StudyApp(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    isInPictureInPictureMode: Boolean = false,
    onPomodoroRunningChange: (Boolean) -> Unit = {},
    onThemeModeChange: (AppThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionStore = remember(context) { AccountSessionStore(context) }
    val installationId = remember(context) { DeviceIdentityStore(context).installationId() }
    val trialPeriod = remember(context) { TrialPeriodStore(context) }
    val studyDatabase = remember(context) { StudyDatabase(context) }
    var accountSession by remember(context) { mutableStateOf(sessionStore.load()) }
    var showAccountScreen by remember { mutableStateOf(false) }
    var trialExpired by remember {
        mutableStateOf(accountSession == null && trialPeriod.isExpired())
    }
    var trialDaysRemaining by remember {
        mutableIntStateOf(trialPeriod.remainingDays())
    }

    LaunchedEffect(installationId) {
        trialPeriod.ensureStarted()
        trialPeriod.scheduleExpiry(context, installationId)

        if (trialExpired) {
            studyDatabase.clearTrialData(installationId)
            PomodoroNotificationService.reset(context)
        }

    }

    LaunchedEffect(installationId, accountSession) {

        if (accountSession == null) {
            var remainingMillis: Long

            do {
                remainingMillis =
                    (trialPeriod.deadlineMillis() - System.currentTimeMillis()).coerceAtLeast(0L)
                trialDaysRemaining = trialPeriod.remainingDays()

                if (remainingMillis > 0L) {
                    kotlinx.coroutines.delay(minOf(remainingMillis, MILLIS_PER_DAY))
                }
            } while (remainingMillis > 0L)

            if (!trialExpired) {
                studyDatabase.clearTrialData(installationId)
                PomodoroNotificationService.reset(context)
                trialExpired = true
            }

        }

    }

    if (accountSession == null && (trialExpired || showAccountScreen)) {
        AccountScreen(
            installationId = installationId,
            onAuthenticated = { session ->
                showAccountScreen = false
                accountSession = session
            },
            onCancel = if (trialExpired) null else {
                {
                    showAccountScreen = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        val dataScopeId = accountSession?.accountId ?: installationId
        val studyRepository = remember(context, dataScopeId) {
            LocalStudyRepository(context.applicationContext)
        }
        var syncConflicts by remember(dataScopeId) {
            mutableStateOf(
                accountSession?.let { studyDatabase.pendingConflicts(dataScopeId) }.orEmpty()
            )
        }
        var selectedConflict by remember(dataScopeId) {
            mutableStateOf<PendingSyncConflict?>(null)
        }
        var syncError by remember(dataScopeId) {
            mutableStateOf(
                accountSession?.let {
                    context.getSharedPreferences(
                        StudySyncWorker.SYNC_STATUS_PREFERENCES,
                        Context.MODE_PRIVATE
                    ).getString("${StudySyncWorker.ERROR_KEY}.$dataScopeId", null)
                }
            )
        }
        var hasCompletedOnboarding by remember {
            mutableStateOf(
                context.getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .getBoolean("has_completed_onboarding", false)
            )
        }
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

        LaunchedEffect(context, dataScopeId) {
            withContext(Dispatchers.IO) {

                if (accountSession == null) {
                    studyDatabase.assignLegacyTrialData(installationId)
                } else {
                    studyDatabase.claimDeviceData(installationId, dataScopeId)
                    PomodoroSessionStore.claimDeviceSession(context, installationId, dataScopeId)
                }

            }

            studyRepository.refreshFromDatabase()

            if (accountSession != null) {
                syncConflicts = studyDatabase.pendingConflicts(dataScopeId)
                StudySyncWorker.enqueue(context, dataScopeId)
            }

            ReviewReminderScheduler.scheduleDaily(context)
        }

        LaunchedEffect(hasCompletedOnboarding) {

            if (
                hasCompletedOnboarding &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

        }

        DisposableEffect(studyRepository) {
            val syncReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    studyRepository.refreshFromDatabase()
                    syncError = context.getSharedPreferences(
                        StudySyncWorker.SYNC_STATUS_PREFERENCES,
                        Context.MODE_PRIVATE
                    ).getString(
                        "${StudySyncWorker.ERROR_KEY}.$dataScopeId",
                        null
                    )
                }
            }

            ContextCompat.registerReceiver(
                context,
                syncReceiver,
                IntentFilter(StudySyncWorker.SYNC_COMPLETED_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            onDispose { context.unregisterReceiver(syncReceiver) }
        }

        val navController = rememberNavController()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {

                if (!isInPictureInPictureMode) {
                    BottomNavigation(navController)
                }

            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                syncError?.let { message ->

                    val visibleMessage = when (message) {
                        StudySyncWorker.AUTH_REQUIRED_STATUS ->
                            stringResource(R.string.sync_error_auth_required)
                        StudySyncWorker.CONFLICT_STATUS ->
                            stringResource(R.string.sync_error_conflict)
                        else -> message
                    }

                    Text(
                        text = stringResource(R.string.sync_error_label, visibleMessage),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable(enabled = syncConflicts.isNotEmpty()) {
                                selectedConflict = syncConflicts.firstOrNull()
                            }
                            .padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AppNavigation(
                        navController = navController,
                        studyRepository = studyRepository,
                        flashcardGenerator = MockFlashcardGenerator,
                        themeMode = themeMode,
                        onPomodoroRunningChange = onPomodoroRunningChange,
                        onThemeModeChange = onThemeModeChange,
                        accountEmail = accountSession?.email,
                        trialDaysRemaining = trialDaysRemaining.takeIf { accountSession == null },
                        onCreateAccount = { showAccountScreen = true },
                        onSignOut = {
                            val refreshToken = sessionStore.load()?.refreshToken.orEmpty()
                            PomodoroNotificationService.stop(context)
                            sessionStore.clear()
                            accountSession = null

                            if (refreshToken.isNotBlank()) {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            StudyApiClient().logout(refreshToken)
                                        }
                                    } catch (exception: IOException) {
                                        Log.w("StudyApp", "Could not revoke signed-out session.", exception)
                                    }
                                }
                            }

                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isInPictureInPictureMode) {
                        PomodoroPictureInPictureScreen(modifier = Modifier.fillMaxSize())
                    }

                }
            }
        }

        if (!hasCompletedOnboarding) {
            OnboardingScreen(
                onFinish = {
                    context.getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("has_completed_onboarding", true)
                        .apply()
                    hasCompletedOnboarding = true
                }
            )
        }

        selectedConflict?.let { conflict ->
            SyncConflictDialog(
                conflict = conflict,
                onKeepServer = {
                    studyDatabase.resolveConflict(dataScopeId, conflict, keepServer = true)
                    studyRepository.refreshFromDatabase()
                    syncConflicts = studyDatabase.pendingConflicts(dataScopeId)
                    selectedConflict = syncConflicts.firstOrNull()
                    StudySyncWorker.enqueue(context, dataScopeId)
                },
                onKeepLocal = {
                    studyDatabase.resolveConflict(dataScopeId, conflict, keepServer = false)
                    syncConflicts = studyDatabase.pendingConflicts(dataScopeId)
                    selectedConflict = syncConflicts.firstOrNull()
                    StudySyncWorker.enqueue(context, dataScopeId)
                }
            )
        }

    }

}

@Composable
fun BottomNavigation(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {

        bottomNavigationDestinations.forEach { destination ->
            val isSelected = currentRoute == destination.route ||
                (destination.route == Routes.SUBJECTS &&
                    currentRoute == Routes.SUBJECT_DETAIL)
            NavigationBarItem(
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.label)
                    )
                },
                label = { Text(stringResource(destination.label)) }
            )
        }

    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MyApplicationTheme {
        StudyApp()
    }
}
