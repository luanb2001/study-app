package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.Context
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myapplication.AppThemeMode
import com.example.myapplication.APP_PREFERENCES_NAME
import com.example.myapplication.THEME_MODE_PREFERENCE_KEY
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences = remember {
                getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(
                    AppThemeMode.fromPreference(
                        preferences.getString(THEME_MODE_PREFERENCE_KEY, null)
                    )
                )
            }
            val systemDarkTheme = isSystemInDarkTheme()
            MyApplicationTheme(
                darkTheme = themeMode.isDark(systemDarkTheme)
            ) {
                StudyApp(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        preferences.edit()
                            .putString(THEME_MODE_PREFERENCE_KEY, mode.preferenceValue)
                            .apply()
                        themeMode = mode
                    }
                )
            }
        }
    }
}
