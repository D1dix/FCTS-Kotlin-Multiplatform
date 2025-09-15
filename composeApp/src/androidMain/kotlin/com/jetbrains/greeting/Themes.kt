package com.jetbrains.greeting

import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF66D2CE),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFCF6679),
    surfaceVariant = Color(0xFF2D2D2D),
    primaryContainer = Color(0xFF3700B3),
    secondaryContainer = Color(0xFF018786)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF66D2CE),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = Color(0xFFB00020),
    surfaceVariant = Color(0xFFF5F5F5),
    primaryContainer = Color(0xFFE8DEF8),
    secondaryContainer = Color(0xFFCCFBF1)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Detecta el modo oscuro en el sistema
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}




    val SelectedColor = Color(0xFF66D2CE)
    val ImageBoxBackground = Color(0xFFEAEAEA)
    val Accent1 = Color(0xFF2DAA9E)
    val Accent2 = Color(0xFFE3D2C3)
