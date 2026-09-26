package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val extraSmall: Dp = 4.dp,   // Espaçamentos micro (entre ícone e texto curto)
    val small: Dp = 8.dp,        // Espaçamento interno de componentes pequenos
    val medium: Dp = 12.dp,      // Espaçamento de cards menores ou grupos
    val large: Dp = 16.dp,       // Padding padrão das telas e cartões
    val extraLarge: Dp = 24.dp,  // Separação entre seções principais
    val huge: Dp = 32.dp         // Margens amplas para destacar o cronômetro/foco
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current