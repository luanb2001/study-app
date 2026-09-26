package com.example.myapplication.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MutedBlue,
    onPrimary = OffWhite,
    secondary = SageGreen,
    onSecondary = OffWhite,
    tertiary = SoftTerracotta,
    onTertiary = OffWhite,

    background = OffWhite,               // OffWhite -> fundo geral
    onBackground = Charcoal,
    surface = SoftGrey,                  // SoftGrey -> cards/superfícies secundárias
    onSurface = Charcoal,
    surfaceVariant = OffWhite,
    onSurfaceVariant = SubtleGrey,
    outline = BorderGrey                 // BorderGrey -> bordas/divisores
)

private val DarkColorScheme = darkColorScheme(
    primary = MutedBlueLight,
    onPrimary = DarkBackground,
    secondary = SageGreenLight,
    onSecondary = DarkBackground,
    tertiary = SoftTerracottaLight,
    onTertiary = DarkBackground,

    background = DarkBackground,
    onBackground = OnDarkText,
    surface = DarkSurface,
    onSurface = OnDarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSubtext,
    outline = DarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Travado em false para garantir a identidade visual única do aplicativo
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}