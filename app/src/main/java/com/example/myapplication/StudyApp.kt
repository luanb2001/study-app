package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.feature.onboarding.OnboardingScreen
import com.example.myapplication.feature.pomodoro.PomodoroPictureInPictureScreen
import com.example.myapplication.feature.study.LocalStudyRepository
import com.example.myapplication.feature.study.MockFlashcardGenerator
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.feature.study.reminder.ReviewReminderScheduler
import com.example.myapplication.navigation.AppNavigation
import com.example.myapplication.navigation.Routes
import com.example.myapplication.ui.theme.MyApplicationTheme

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

@Composable
fun StudyApp(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    isInPictureInPictureMode: Boolean = false,
    onPomodoroRunningChange: (Boolean) -> Unit = {},
    onThemeModeChange: (AppThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val studyRepository = remember(context) {
        LocalStudyRepository(context.applicationContext)
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

    LaunchedEffect(context) {
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
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavigation(
                navController = navController,
                studyRepository = studyRepository,
                flashcardGenerator = MockFlashcardGenerator,
                themeMode = themeMode,
                onPomodoroRunningChange = onPomodoroRunningChange,
                onThemeModeChange = onThemeModeChange,
                modifier = Modifier.fillMaxSize()
            )

            if (isInPictureInPictureMode) {
                PomodoroPictureInPictureScreen(modifier = Modifier.fillMaxSize())
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
