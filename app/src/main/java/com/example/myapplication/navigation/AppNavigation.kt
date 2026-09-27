package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.feature.home.HomeScreen
import com.example.myapplication.feature.pomodoro.PomodoroScreen
import com.example.myapplication.feature.study.RegisterStudyScreen
import com.example.myapplication.R

object Routes {
    const val HOME = "home"
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

        composable(Routes.REGISTER_STUDY) {
            RegisterStudyScreen()
        }

        composable("${Routes.POMODORO}/{subject}") { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject")
                ?: stringResource(R.string.pomodoro_study)
            PomodoroScreen(subject = subject)
        }
    }
}