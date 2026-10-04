package com.example.myapplication

const val APP_PREFERENCES_NAME = "study_app_preferences"
const val THEME_MODE_PREFERENCE_KEY = "theme_mode"

enum class AppThemeMode(val preferenceValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun isDark(systemThemeIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemThemeIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromPreference(value: String?): AppThemeMode =
            entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}
