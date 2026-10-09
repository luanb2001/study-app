package com.example.myapplication

import android.app.PictureInPictureParams
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
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
    private var pomodoroRunning = false
    private var pipMode by mutableStateOf(false)

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
                    isInPictureInPictureMode = pipMode,
                    onPomodoroRunningChange = ::updatePomodoroRunning,
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (pomodoroRunning && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enterPictureInPictureMode(pictureInPictureParams(autoEnter = false))
        }

    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipMode = isInPictureInPictureMode
    }

    private fun updatePomodoroRunning(isRunning: Boolean) {
        pomodoroRunning = isRunning

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(pictureInPictureParams(autoEnter = isRunning))
        }

    }

    private fun pictureInPictureParams(autoEnter: Boolean): PictureInPictureParams =
        PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .apply {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(autoEnter)
                }

            }
            .build()
}
