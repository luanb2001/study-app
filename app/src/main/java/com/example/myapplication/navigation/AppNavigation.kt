package com.example.myapplication.navigation

import com.example.myapplication.AppThemeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.feature.calendar.CalendarScreen
import com.example.myapplication.feature.home.HomeScreen
import com.example.myapplication.feature.pomodoro.PomodoroScreen
import com.example.myapplication.feature.pomodoro.PomodoroPhase
import com.example.myapplication.feature.pomodoro.PomodoroSessionStore
import com.example.myapplication.feature.pomodoro.PomodoroSessionState
import com.example.myapplication.feature.profile.ProfileScreen
import com.example.myapplication.feature.study.StartStudyScreen
import com.example.myapplication.feature.study.RegisterStudiedStudyScreen
import com.example.myapplication.feature.study.ScheduleStudyScreen
import com.example.myapplication.feature.study.StudyEntry
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.feature.study.FlashcardGenerator
import com.example.myapplication.feature.study.MockFlashcardGenerator
import com.example.myapplication.feature.study.ReviewModeScreen
import com.example.myapplication.feature.study.FlashcardsScreen
import com.example.myapplication.feature.subjects.SubjectDetailScreen
import com.example.myapplication.feature.subjects.SubjectsScreen
import android.net.Uri
import java.time.LocalDate
import kotlinx.coroutines.delay

object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val SUBJECTS = "subjects"
    const val PROFILE = "profile"
    const val START_STUDY = "start_study"
    const val REGISTER_STUDY = "register_study"
    const val SCHEDULE_STUDY = "schedule_study"
    const val SUBJECT_ARGUMENT = "subject"
    const val DURATION_ARGUMENT = "duration"
    const val BREAK_DURATION_ARGUMENT = "breakDuration"
    const val SESSIONS_ARGUMENT = "sessions"
    const val IS_REVIEW_ARGUMENT = "isReview"
    const val SCHEDULED_STUDY_ID_ARGUMENT = "scheduledStudyId"
    const val SUBJECT_DETAIL_PREFIX = "subject/"
    const val REVIEW_MODE_PREFIX = "review_mode/"
    const val FLASHCARDS_PREFIX = "flashcards/"
    const val POMODORO_PREFIX = "pomodoro/"
    const val SUBJECT_DETAIL = "$SUBJECT_DETAIL_PREFIX{$SUBJECT_ARGUMENT}"
    const val REVIEW_MODE = "$REVIEW_MODE_PREFIX{$SUBJECT_ARGUMENT}"
    const val FLASHCARDS = "$FLASHCARDS_PREFIX{$SUBJECT_ARGUMENT}"
    const val REGISTER_STUDY_WITH_SUBJECT = "$REGISTER_STUDY?$SUBJECT_ARGUMENT={$SUBJECT_ARGUMENT}"
    const val SCHEDULE_STUDY_WITH_SUBJECT = "$SCHEDULE_STUDY?$SUBJECT_ARGUMENT={$SUBJECT_ARGUMENT}"
    const val START_STUDY_WITH_SUBJECT = "$START_STUDY?$SUBJECT_ARGUMENT={$SUBJECT_ARGUMENT}"
    const val POMODORO =
        "$POMODORO_PREFIX{$SUBJECT_ARGUMENT}?$DURATION_ARGUMENT={$DURATION_ARGUMENT}" +
            "&$BREAK_DURATION_ARGUMENT={$BREAK_DURATION_ARGUMENT}" +
            "&$SESSIONS_ARGUMENT={$SESSIONS_ARGUMENT}" +
            "&$IS_REVIEW_ARGUMENT={$IS_REVIEW_ARGUMENT}" +
            "&$SCHEDULED_STUDY_ID_ARGUMENT={$SCHEDULED_STUDY_ID_ARGUMENT}"

    fun subjectDetail(subject: String): String =
        "$SUBJECT_DETAIL_PREFIX${Uri.encode(subject)}"

    fun reviewMode(subject: String): String =
        "$REVIEW_MODE_PREFIX${Uri.encode(subject)}"

    fun flashcards(subject: String): String =
        "$FLASHCARDS_PREFIX${Uri.encode(subject)}"

    fun registerStudy(subject: String): String =
        "$REGISTER_STUDY?$SUBJECT_ARGUMENT=${Uri.encode(subject)}"

    fun scheduleStudy(subject: String): String =
        "$SCHEDULE_STUDY?$SUBJECT_ARGUMENT=${Uri.encode(subject)}"

    fun startStudy(subject: String): String =
        "$START_STUDY?$SUBJECT_ARGUMENT=${Uri.encode(subject)}"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    studyRepository: StudyRepository,
    themeMode: AppThemeMode,
    onPomodoroRunningChange: (Boolean) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    flashcardGenerator: FlashcardGenerator = MockFlashcardGenerator
) {
    val context = LocalContext.current
    var activePomodoro by remember(context) {
        mutableStateOf(PomodoroSessionStore.load(context))
    }
    LaunchedEffect(context) {
        while (true) {
            activePomodoro = PomodoroSessionStore.load(context)
            delay(500L)
        }
    }
    val isPomodoroRunning = activePomodoro?.let { session ->
        session.isRunning && session.phase != PomodoroPhase.COMPLETED
    } == true
    LaunchedEffect(isPomodoroRunning) {
        onPomodoroRunningChange(isPomodoroRunning)
    }
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = {
            val direction = navigationDirection(
                initialState.destination.route,
                targetState.destination.route
            )
            slideInHorizontally(tween(250)) { direction * it } + fadeIn(tween(180))
        },
        exitTransition = {
            val direction = navigationDirection(
                initialState.destination.route,
                targetState.destination.route
            )
            slideOutHorizontally(tween(250)) { -direction * it } + fadeOut(tween(180))
        },
        popEnterTransition = {
            val direction = navigationDirection(
                initialState.destination.route,
                targetState.destination.route
            )
            slideInHorizontally(tween(250)) { direction * it } + fadeIn(tween(180))
        },
        popExitTransition = {
            val direction = navigationDirection(
                initialState.destination.route,
                targetState.destination.route
            )
            slideOutHorizontally(tween(250)) { -direction * it } + fadeOut(tween(180))
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                studyRepository = studyRepository,
                onStartStudy = {
                    navController.navigate(Routes.START_STUDY)
                },
                onScheduleStudy = {
                    navController.navigate(Routes.SCHEDULE_STUDY)
                },
                onStartReview = { selectedSubject ->
                    navController.navigate(Routes.reviewMode(selectedSubject))
                },
                onStartScheduledStudy = { study ->
                    navigateToPomodoro(
                        navController,
                        context,
                        newPomodoroRoute(
                            subject = study.subject,
                            studyMinutes = study.studyMinutes,
                            breakMinutes = study.breakMinutes,
                            sessionCount = study.sessionCount,
                            scheduledStudyId = study.id
                        )
                    )
                },
                onContinuePomodoro = { session ->
                    navController.navigate(session.pomodoroRoute())
                },
                onSeeProgress = {
                    navController.navigate(Routes.PROFILE) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.CALENDAR) {
            CalendarScreen(
                studyRepository = studyRepository,
                onDeleteStudy = studyRepository::deleteStudy,
                onCancelScheduledStudy = studyRepository::cancelScheduledStudy,
                onRescheduleReview = studyRepository::rescheduleReview,
                onScheduleStudy = {
                    navController.navigate(Routes.SCHEDULE_STUDY)
                }
            )
        }

        composable(
            route = Routes.REVIEW_MODE,
            arguments = listOf(navArgument(Routes.SUBJECT_ARGUMENT) {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val subject = backStackEntry.arguments
                ?.getString(Routes.SUBJECT_ARGUMENT)
                .orEmpty()
            ReviewModeScreen(
                subject = subject,
                onBack = { navController.popBackStack() },
                onStartFlashcards = {
                    navController.navigate(Routes.flashcards(subject))
                },
                onStartPomodoro = {
                    navigateToPomodoro(
                        navController,
                        context,
                        newPomodoroRoute(
                            subject = subject,
                            studyMinutes = 25,
                            sessionCount = 4,
                            isReview = true
                        )
                    )
                }
            )
        }

        composable(
            route = Routes.FLASHCARDS,
            arguments = listOf(navArgument(Routes.SUBJECT_ARGUMENT) {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val subject = backStackEntry.arguments
                ?.getString(Routes.SUBJECT_ARGUMENT)
                .orEmpty()
            FlashcardsScreen(
                subject = subject,
                studies = studyRepository.forSubject(subject),
                flashcardGenerator = flashcardGenerator,
                onBack = { navController.popBackStack() },
                onComplete = { difficulty ->
                    studyRepository.completeFlashcardReview(subject, difficulty)
                },
                onFinish = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                studyRepository = studyRepository,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange
            )
        }

        composable(Routes.SUBJECTS) {
            SubjectsScreen(
                studyRepository = studyRepository,
                onOpenSubject = { subject ->
                    navController.navigate(Routes.subjectDetail(subject))
                },
                onStartStudy = {
                    navController.navigate(Routes.START_STUDY)
                },
                onRegisterStudy = {
                    navController.navigate(Routes.REGISTER_STUDY)
                },
                onScheduleStudy = {
                    navController.navigate(Routes.SCHEDULE_STUDY)
                },
                canStartStudy = activePomodoro == null
            )
        }

        composable(
            route = Routes.SUBJECT_DETAIL,
            arguments = listOf(navArgument(Routes.SUBJECT_ARGUMENT) {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val subject = backStackEntry.arguments
                ?.getString(Routes.SUBJECT_ARGUMENT)
                .orEmpty()
            SubjectDetailScreen(
                subject = subject,
                studyRepository = studyRepository,
                onBack = { navController.popBackStack() },
                onStartStudy = {
                    navController.navigate(Routes.startStudy(subject))
                },
                onRegisterStudy = {
                    navController.navigate(Routes.registerStudy(subject))
                },
                onDeleteStudy = studyRepository::deleteStudy,
                onScheduleStudy = {
                    navController.navigate(Routes.scheduleStudy(subject))
                },
                canStartStudy = activePomodoro == null
            )
        }

        composable(
            route = Routes.REGISTER_STUDY_WITH_SUBJECT,
            arguments = listOf(
                navArgument(Routes.SUBJECT_ARGUMENT) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            RegisterStudiedStudyScreen(
                initialSubject = backStackEntry.arguments
                    ?.getString(Routes.SUBJECT_ARGUMENT)
                    .orEmpty(),
                onBack = { navController.popBackStack() },
                onRegisterStudy = { entry ->
                    studyRepository.recordStudy(entry)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.SCHEDULE_STUDY_WITH_SUBJECT,
            arguments = listOf(
                navArgument(Routes.SUBJECT_ARGUMENT) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            ScheduleStudyScreen(
                studyRepository = studyRepository,
                initialSubject = backStackEntry.arguments
                    ?.getString(Routes.SUBJECT_ARGUMENT)
                    .orEmpty(),
                onBack = { navController.popBackStack() },
                onSchedule = { study ->
                    studyRepository.schedule(study)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.START_STUDY_WITH_SUBJECT,
            arguments = listOf(
                navArgument(Routes.SUBJECT_ARGUMENT) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            StartStudyScreen(
                initialSubject = backStackEntry.arguments
                    ?.getString(Routes.SUBJECT_ARGUMENT)
                    .orEmpty(),
                onBack = { navController.popBackStack() },
                onStartStudy = { subject, durationMinutes, breakMinutes, sessions ->
                    navigateToPomodoro(
                        navController,
                        context,
                        newPomodoroRoute(
                            subject = subject,
                            studyMinutes = durationMinutes,
                            breakMinutes = breakMinutes,
                            sessionCount = sessions
                        )
                    )
                },
                canStartStudy = activePomodoro == null
            )
        }

        composable(
            route = Routes.POMODORO,
            arguments = listOf(
                navArgument(Routes.SUBJECT_ARGUMENT) { type = NavType.StringType },
                navArgument(Routes.DURATION_ARGUMENT) {
                    type = NavType.IntType
                    defaultValue = 25
                },
                navArgument(Routes.BREAK_DURATION_ARGUMENT) {
                    type = NavType.IntType
                    defaultValue = 5
                },
                navArgument(Routes.SESSIONS_ARGUMENT) {
                    type = NavType.IntType
                    defaultValue = 4
                },
                navArgument(Routes.IS_REVIEW_ARGUMENT) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(Routes.SCHEDULED_STUDY_ID_ARGUMENT) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments
                ?.getString(Routes.SUBJECT_ARGUMENT)
                .orEmpty()
            val isReview = backStackEntry.arguments
                ?.getBoolean(Routes.IS_REVIEW_ARGUMENT)
                ?: false
            val scheduledStudyId =
                backStackEntry.arguments
                    ?.getString(Routes.SCHEDULED_STUDY_ID_ARGUMENT)
                    .orEmpty()
            PomodoroScreen(
                subject = subject,
                initialStudyMinutes = backStackEntry.arguments
                    ?.getInt(Routes.DURATION_ARGUMENT)
                    ?: 25,
                initialBreakMinutes = backStackEntry.arguments
                    ?.getInt(Routes.BREAK_DURATION_ARGUMENT)
                    ?: 5,
                initialSessionCount = backStackEntry.arguments
                    ?.getInt(Routes.SESSIONS_ARGUMENT)
                    ?: 4,
                isReview = isReview,
                scheduledStudyId = scheduledStudyId,
                onCompleted = { durationMinutes, sessions, summary ->
                    studyRepository.recordStudy(
                        StudyEntry(
                            date = LocalDate.now(),
                            subject = subject,
                            description = summary,
                            durationMinutes = durationMinutes,
                            sessionCount = sessions
                        ),
                        isReview = isReview
                    )

                    if (scheduledStudyId.isNotEmpty()) {
                        studyRepository.scheduled()
                            .firstOrNull { it.id == scheduledStudyId }
                            ?.let(studyRepository::cancelScheduledStudy)
                    }

                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }

}

private fun navigationDirection(initialRoute: String?, targetRoute: String?): Int {
    val initialPosition = routePosition(initialRoute)
    val targetPosition = routePosition(targetRoute)
    return if (targetPosition >= initialPosition) 1 else -1
}

private fun routePosition(route: String?): Int = when {
    route == Routes.HOME -> 0
    route == Routes.CALENDAR -> 1
    route == Routes.SUBJECTS ||
        route?.startsWith(Routes.SUBJECT_DETAIL_PREFIX) == true -> 2
    route == Routes.PROFILE -> 3
    else -> 0
}

private fun PomodoroSessionState.pomodoroRoute(): String =
    "${Routes.POMODORO_PREFIX}${Uri.encode(subject)}?" +
        "${Routes.DURATION_ARGUMENT}=$studyMinutes" +
        "&${Routes.BREAK_DURATION_ARGUMENT}=$breakMinutes" +
        "&${Routes.SESSIONS_ARGUMENT}=$sessionCount" +
        "&${Routes.IS_REVIEW_ARGUMENT}=$isReview" +
        "&${Routes.SCHEDULED_STUDY_ID_ARGUMENT}=${Uri.encode(scheduledStudyId)}"

private fun newPomodoroRoute(
    subject: String,
    studyMinutes: Int,
    sessionCount: Int,
    breakMinutes: Int? = null,
    isReview: Boolean? = null,
    scheduledStudyId: String? = null
): String = buildString {
    append("${Routes.POMODORO_PREFIX}${Uri.encode(subject)}?")
    append("${Routes.DURATION_ARGUMENT}=$studyMinutes")
    breakMinutes?.let {
        append("&${Routes.BREAK_DURATION_ARGUMENT}=$it")
    }
    append("&${Routes.SESSIONS_ARGUMENT}=$sessionCount")
    isReview?.let { append("&${Routes.IS_REVIEW_ARGUMENT}=$it") }
    scheduledStudyId
        ?.takeIf { it.isNotBlank() }
        ?.let {
            append("&${Routes.SCHEDULED_STUDY_ID_ARGUMENT}=${Uri.encode(it)}")
        }
}

private fun navigateToPomodoro(
    navController: NavHostController,
    context: android.content.Context,
    requestedRoute: String
) {
    val currentSession = PomodoroSessionStore.load(context)
    navController.navigate(currentSession?.pomodoroRoute() ?: requestedRoute)
}