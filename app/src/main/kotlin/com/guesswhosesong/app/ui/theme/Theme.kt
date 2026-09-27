package com.guesswhosesong.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Warm paper + punchy character colors used throughout the game. */
object GwsPalette {
    val Ink = Color(0xFF17161A)
    val Paper = Color(0xFFFFFBF2)
    val Lavender = Color(0xFFB8B0FF)
    val LavenderDeep = Color(0xFF7668E8)
    val Tangerine = Color(0xFFFFAA3D)
    val Lime = Color(0xFFB8F45D)
    val Berry = Color(0xFFFF8EAD)
    val Sky = Color(0xFF97C8FF)
    val Mint = Color(0xFFB9F4D0)
    val Butter = Color(0xFFFFE49A)
}

private val LightColorScheme = lightColorScheme(
    primary = GwsPalette.LavenderDeep,
    onPrimary = Color.White,
    primaryContainer = GwsPalette.Lavender.copy(alpha = 0.45f),
    onPrimaryContainer = GwsPalette.Ink,
    secondary = GwsPalette.Tangerine,
    onSecondary = GwsPalette.Ink,
    secondaryContainer = GwsPalette.Butter,
    onSecondaryContainer = GwsPalette.Ink,
    tertiary = GwsPalette.Lime,
    onTertiary = GwsPalette.Ink,
    background = GwsPalette.Paper,
    onBackground = GwsPalette.Ink,
    surface = Color(0xFFFFFDF8),
    onSurface = GwsPalette.Ink,
    surfaceVariant = Color(0xFFF1EBDD),
    onSurfaceVariant = Color(0xFF5D5860),
    outline = Color(0xFF2D2930),
    outlineVariant = Color(0xFFCFC6B9),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC3BBFF),
    onPrimary = Color(0xFF29205F),
    primaryContainer = Color(0xFF51479B),
    onPrimaryContainer = Color(0xFFF0EDFF),
    secondary = Color(0xFFFFBA63),
    onSecondary = Color(0xFF3E2000),
    secondaryContainer = Color(0xFF664000),
    onSecondaryContainer = Color(0xFFFFDDB4),
    tertiary = Color(0xFFC9FF79),
    onTertiary = Color(0xFF1D3400),
    background = Color(0xFF211F29),
    onBackground = Color(0xFFF7F1E8),
    surface = Color(0xFF2B2933),
    onSurface = Color(0xFFF7F1E8),
    surfaceVariant = Color(0xFF45414D),
    onSurfaceVariant = Color(0xFFD0C8D1),
    outline = Color(0xFFE4DCE6),
    outlineVariant = Color(0xFF605A67),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val GwsTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

private val GwsShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

@Composable
fun GuessWhoseSongTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = GwsTypography,
        shapes = GwsShapes,
        content = content
    )
}
