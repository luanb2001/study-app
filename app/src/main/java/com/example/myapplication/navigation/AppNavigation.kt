package com.example.myapplication.navigation

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
import com.example.myapplication.feature.pomodoro.PomodoroSessionStore
import com.example.myapplication.feature.pomodoro.PomodoroSessionState
import com.example.myapplication.feature.profile.ProfileScreen
import com.example.myapplication.feature.study.StartStudyScreen
import com.example.myapplication.feature.study.RegisterStudiedStudyScreen
import com.example.myapplication.feature.study.ScheduleStudyScreen
import com.example.myapplication.feature.study.ScheduledStudy
import com.example.myapplication.feature.study.StudyEntry
import com.example.myapplication.feature.study.StudyRepository
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
    const val SUBJECT_DETAIL = "subject/{subject}"
    const val START_STUDY = "start_study"
    const val REGISTER_STUDY = "register_study"
    const val SCHEDULE_STUDY = "schedule_study"
    const val POMODORO = "pomodoro/{subject}?duration={duration}&breakDuration={breakDuration}&sessions={sessions}&isReview={isReview}&scheduledStudyId={scheduledStudyId}"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    studyRepository: StudyRepository,
    modifier: Modifier = Modifier
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
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(tween(250)) { it } + fadeIn(tween(180))
        },
        exitTransition = {
            slideOutHorizontally(tween(250)) { -it } + fadeOut(tween(180))
        },
        popEnterTransition = {
            slideInHorizontally(tween(250)) { -it } + fadeIn(tween(180))
        },
        popExitTransition = {
            slideOutHorizontally(tween(250)) { it } + fadeOut(tween(180))
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
                onStartPomodoro = { selectedSubject ->
                    navigateToPomodoro(
                        navController,
                        context,
                        "pomodoro/${Uri.encode(selectedSubject)}?duration=25&sessions=4&isReview=true"
                    )
                },
                onStartScheduledStudy = { study ->
                    navigateToPomodoro(
                        navController,
                        context,
                        "pomodoro/${Uri.encode(study.subject)}?duration=${study.studyMinutes}" +
                            "&breakDuration=${study.breakMinutes}" +
                            "&sessions=${study.sessionCount}&scheduledStudyId=${Uri.encode(study.id)}"
                    )
                },
                onContinuePomodoro = { session ->
                    navController.navigate(session.pomodoroRoute())
                }
            )
        }

        composable(Routes.CALENDAR) {
            CalendarScreen(
                studyRepository = studyRepository,
                onDeleteStudy = studyRepository::deleteStudy,
                onCancelScheduledStudy = studyRepository::cancelScheduledStudy,
                onScheduleStudy = {
                    navController.navigate(Routes.SCHEDULE_STUDY)
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(studyRepository = studyRepository)
        }

        composable(Routes.SUBJECTS) {
            SubjectsScreen(
                studyRepository = studyRepository,
                onOpenSubject = { subject ->
                    navController.navigate("subject/${Uri.encode(subject)}")
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
            arguments = listOf(navArgument("subject") { type = NavType.StringType })
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject").orEmpty()
            SubjectDetailScreen(
                subject = subject,
                studyRepository = studyRepository,
                onBack = { navController.popBackStack() },
                onStartStudy = {
                    navController.navigate(
                        "${Routes.START_STUDY}?subject=${Uri.encode(subject)}"
                    )
                },
                onRegisterStudy = {
                    navController.navigate(
                        "${Routes.REGISTER_STUDY}?subject=${Uri.encode(subject)}"
                    )
                },
                onDeleteStudy = studyRepository::deleteStudy,
                onScheduleStudy = {
                    navController.navigate(
                        "${Routes.SCHEDULE_STUDY}?subject=${Uri.encode(subject)}"
                    )
                },
                canStartStudy = activePomodoro == null
            )
        }

        composable(
            route = "${Routes.REGISTER_STUDY}?subject={subject}",
            arguments = listOf(
                navArgument("subject") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            RegisterStudiedStudyScreen(
                initialSubject = backStackEntry.arguments?.getString("subject").orEmpty(),
                onBack = { navController.popBackStack() },
                onRegisterStudy = { entry ->
                    studyRepository.recordStudy(entry)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "${Routes.SCHEDULE_STUDY}?subject={subject}",
            arguments = listOf(
                navArgument("subject") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            ScheduleStudyScreen(
                studyRepository = studyRepository,
                initialSubject = backStackEntry.arguments?.getString("subject").orEmpty(),
                onBack = { navController.popBackStack() },
                onSchedule = { study ->
                    studyRepository.schedule(study)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "${Routes.START_STUDY}?subject={subject}",
            arguments = listOf(
                navArgument("subject") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            StartStudyScreen(
                initialSubject = backStackEntry.arguments?.getString("subject").orEmpty(),
                onBack = { navController.popBackStack() },
                onStartStudy = { subject, durationMinutes, breakMinutes, sessions ->
                    navigateToPomodoro(
                        navController,
                        context,
                        "pomodoro/${Uri.encode(subject)}?duration=$durationMinutes&breakDuration=$breakMinutes&sessions=$sessions"
                    )
                },
                canStartStudy = activePomodoro == null
            )
        }

        composable(
            route = Routes.POMODORO,
            arguments = listOf(
                navArgument("subject") { type = NavType.StringType },
                navArgument("duration") {
                    type = NavType.IntType
                    defaultValue = 25
                },
                navArgument("breakDuration") {
                    type = NavType.IntType
                    defaultValue = 5
                },
                navArgument("sessions") {
                    type = NavType.IntType
                    defaultValue = 4
                },
                navArgument("isReview") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("scheduledStudyId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject").orEmpty()
            val isReview = backStackEntry.arguments?.getBoolean("isReview") ?: false
            val scheduledStudyId =
                backStackEntry.arguments?.getString("scheduledStudyId").orEmpty()
            PomodoroScreen(
                subject = subject,
                initialStudyMinutes = backStackEntry.arguments?.getInt("duration") ?: 25,
                initialBreakMinutes = backStackEntry.arguments?.getInt("breakDuration") ?: 5,
                initialSessionCount = backStackEntry.arguments?.getInt("sessions") ?: 4,
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

private fun PomodoroSessionState.pomodoroRoute(): String =
    "pomodoro/${Uri.encode(subject)}?duration=$studyMinutes" +
        "&breakDuration=$breakMinutes&sessions=$sessionCount" +
        "&isReview=$isReview&scheduledStudyId=${Uri.encode(scheduledStudyId)}"

private fun navigateToPomodoro(
    navController: NavHostController,
    context: android.content.Context,
    requestedRoute: String
) {
    val currentSession = PomodoroSessionStore.load(context)
    navController.navigate(currentSession?.pomodoroRoute() ?: requestedRoute)
}