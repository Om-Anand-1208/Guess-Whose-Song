package com.guesswhosesong.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2980B9),
    secondary = Color(0xFF6DD5FA),
    tertiary = Color(0xFFE8F4F8),
    background = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF212529),
    onSurface = Color(0xFF212529),
    error = Color(0xFFDC3545)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6DD5FA),
    secondary = Color(0xFF2980B9),
    tertiary = Color(0xFF1A1A2E),
    background = Color(0xFF0F0F23),
    surface = Color(0xFF1A1A2E),
    onPrimary = Color(0xFF0F0F23),
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFFF6B6B)
)

@Composable
fun GuessWhoseSongTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
