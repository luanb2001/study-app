package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.feature.calendar.CalendarScreen
import com.example.myapplication.feature.home.HomeScreen
import com.example.myapplication.feature.pomodoro.PomodoroScreen
import com.example.myapplication.feature.profile.ProfileScreen
import com.example.myapplication.feature.study.RegisterStudyScreen
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.feature.subjects.SubjectDetailScreen
import com.example.myapplication.feature.subjects.SubjectsScreen
import com.example.myapplication.R
import android.net.Uri

object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val SUBJECTS = "subjects"
    const val PROFILE = "profile"
    const val SUBJECT_DETAIL = "subject/{subject}"
    const val REGISTER_STUDY = "register_study"
    const val POMODORO = "pomodoro"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onRegisterStudy = {
                    navController.navigate(Routes.REGISTER_STUDY)
                },
                onStartPomodoro = { selectedSubject ->
                    navController.navigate("${Routes.POMODORO}/$selectedSubject")
                }
            )
        }

        composable(Routes.CALENDAR) {
            CalendarScreen()
        }

        composable(Routes.PROFILE) {
            ProfileScreen()
        }

        composable(Routes.SUBJECTS) {
            SubjectsScreen(
                onOpenSubject = { subject ->
                    navController.navigate("subject/${Uri.encode(subject)}")
                },
                onRegisterStudy = {
                    navController.navigate(Routes.REGISTER_STUDY)
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
                onRegisterStudy = {
                    navController.navigate(
                        "${Routes.REGISTER_STUDY}?subject=${Uri.encode(subject)}"
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
            RegisterStudyScreen(
                initialSubject = backStackEntry.arguments?.getString("subject").orEmpty(),
                onBack = { navController.popBackStack() },
                onRegisterStudy = { entry ->
                    StudyRepository.add(entry)
                    navController.popBackStack()
                }
            )
        }

        composable("${Routes.POMODORO}/{subject}") { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject")
                ?: stringResource(R.string.pomodoro_study)
            PomodoroScreen(
                subject = subject,
                onBack = { navController.popBackStack() }
            )
        }
    }
}