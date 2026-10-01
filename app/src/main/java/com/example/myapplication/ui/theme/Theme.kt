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
    primaryContainer = MutedBlueContainer,
    onPrimaryContainer = OnMutedBlueContainer,
    secondary = SageGreen,
    onSecondary = OffWhite,
    secondaryContainer = SageGreenContainer,
    onSecondaryContainer = OnSageGreenContainer,
    tertiary = SoftTerracotta,
    onTertiary = OffWhite,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,

    background = OffWhite,
    onBackground = Charcoal,
    surface = SoftGrey,
    onSurface = Charcoal,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = SubtleGrey,
    outline = BorderGrey,
    outlineVariant = BorderGrey,
    surfaceTint = MutedBlue,
    inversePrimary = MutedBlueLight,
    inverseSurface = Charcoal,
    inverseOnSurface = OffWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = MutedBlueLight,
    onPrimary = DarkBackground,
    primaryContainer = DarkBlueContainer,
    onPrimaryContainer = OnDarkBlueContainer,
    secondary = SageGreenLight,
    onSecondary = DarkBackground,
    secondaryContainer = DarkGreenContainer,
    onSecondaryContainer = OnDarkGreenContainer,
    tertiary = SoftTerracottaLight,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkTerracottaContainer,
    onTertiaryContainer = OnDarkTerracottaContainer,

    background = DarkBackground,
    onBackground = OnDarkText,
    surface = DarkSurface,
    onSurface = OnDarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSubtext,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    surfaceTint = MutedBlueLight,
    inversePrimary = MutedBlue,
    inverseSurface = OnDarkText,
    inverseOnSurface = DarkBackground
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