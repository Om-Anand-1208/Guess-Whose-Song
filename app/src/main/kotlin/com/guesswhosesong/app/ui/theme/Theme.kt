package com.guesswhosesong.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
fun GuessWhoseSongTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
