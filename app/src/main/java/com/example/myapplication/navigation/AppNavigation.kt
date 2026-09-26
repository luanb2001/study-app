package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.HomeScreen
import com.example.myapplication.feature.study.RegisterStudyScreen

object Routes {
    const val HOME = "home"
    const val REGISTER_STUDY = "register_study"
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
                }
            )
        }

        composable(Routes.REGISTER_STUDY) {
            RegisterStudyScreen()
        }
    }
}