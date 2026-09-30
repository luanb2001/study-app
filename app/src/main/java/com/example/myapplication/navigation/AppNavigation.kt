package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
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

object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val SUBJECTS = "subjects"
    const val PROFILE = "profile"
    const val SUBJECT_DETAIL = "subject/{subject}"
    const val START_STUDY = "start_study"
    const val REGISTER_STUDY = "register_study"
    const val SCHEDULE_STUDY = "schedule_study"
    const val POMODORO = "pomodoro/{subject}?duration={duration}&breakDuration={breakDuration}&autoStart={autoStart}&sessions={sessions}"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
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
                onStartStudy = {
                    navController.navigate(Routes.START_STUDY)
                },
                onScheduleStudy = {
                    navController.navigate(Routes.SCHEDULE_STUDY)
                },
                onStartPomodoro = { selectedSubject ->
                    navController.navigate(
                        "pomodoro/${Uri.encode(selectedSubject)}?duration=25&autoStart=false&sessions=4"
                    )
                }
            )
        }

        composable(Routes.CALENDAR) {
            CalendarScreen(
                onScheduleStudy = {
                    navController.navigate(Routes.SCHEDULE_STUDY)
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen()
        }

        composable(Routes.SUBJECTS) {
            SubjectsScreen(
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
                }
            )
        }

        composable(
            route = Routes.SUBJECT_DETAIL,
            arguments = listOf(navArgument("subject") { type = NavType.StringType })
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject").orEmpty()
            SubjectDetailScreen(
                subject = subject,
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
                onScheduleStudy = {
                    navController.navigate(
                        "${Routes.SCHEDULE_STUDY}?subject=${Uri.encode(subject)}"
                    )
                }
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
                    StudyRepository.add(entry)
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
                initialSubject = backStackEntry.arguments?.getString("subject").orEmpty(),
                onBack = { navController.popBackStack() },
                onSchedule = { study ->
                    StudyRepository.schedule(study)
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
                    navController.navigate(
                        "pomodoro/${Uri.encode(subject)}?duration=$durationMinutes&breakDuration=$breakMinutes&autoStart=true&sessions=$sessions"
                    )
                }
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
                navArgument("autoStart") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("sessions") {
                    type = NavType.IntType
                    defaultValue = 4
                }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject").orEmpty()
            PomodoroScreen(
                subject = subject,
                initialStudyMinutes = backStackEntry.arguments?.getInt("duration") ?: 25,
                initialBreakMinutes = backStackEntry.arguments?.getInt("breakDuration") ?: 5,
                initialSessionCount = backStackEntry.arguments?.getInt("sessions") ?: 4,
                startImmediately = backStackEntry.arguments?.getBoolean("autoStart") ?: false,
                onCompleted = { durationMinutes, sessions, summary ->
                    StudyRepository.add(
                        StudyEntry(
                            date = LocalDate.now(),
                            subject = subject,
                            description = summary,
                            durationMinutes = durationMinutes,
                            sessionCount = sessions
                        )
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}