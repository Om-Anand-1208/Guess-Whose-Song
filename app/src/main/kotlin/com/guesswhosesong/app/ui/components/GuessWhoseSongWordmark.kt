package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guesswhosesong.app.R

/** The single app wordmark used by entry, player, and game screens. */
@Composable
fun GuessWhoseSongWordmark(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 23.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 25.sp
) {
    val scale = (fontSize.value / 23f).coerceIn(0.72f, 1.45f)
    val family = FontFamily(Font(R.font.comfortaa_bold, FontWeight.Bold))
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Guess Whose Song"
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandConfetti(left = true, scale = scale)
        Spacer(Modifier.width(4.dp * scale))
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy((-3).dp * scale)) {
            Text(
                "Guess",
                fontFamily = family,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                lineHeight = lineHeight,
                letterSpacing = (-0.8).sp * scale,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Whose Song",
                fontFamily = family,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                lineHeight = lineHeight,
                letterSpacing = (-0.8).sp * scale,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.width(3.dp * scale))
        BrandConfetti(left = false, scale = scale)
    }
}

@Composable
private fun BrandConfetti(left: Boolean, scale: Float) {
    Canvas(Modifier.size(width = 19.dp * scale, height = 58.dp * scale)) {
        val stroke = 4.dp.toPx() * scale
        if (left) {
            drawLine(Color(0xFFFFAA3D), Offset(size.width * 0.55f, size.height * 0.15f), Offset(size.width * 0.9f, size.height * 0.36f), stroke, StrokeCap.Round)
            drawLine(Color(0xFFFF72A7), Offset(size.width * 0.08f, size.height * 0.55f), Offset(size.width * 0.46f, size.height * 0.62f), stroke, StrokeCap.Round)
        } else {
            drawLine(Color(0xFF9DEBC1), Offset(size.width * 0.12f, size.height * 0.19f), Offset(size.width * 0.48f, size.height * 0.36f), stroke, StrokeCap.Round)
            drawLine(Color(0xFFFFAA3D), Offset(size.width * 0.57f, size.height * 0.1f), Offset(size.width * 0.92f, size.height * 0.34f), stroke, StrokeCap.Round)
            drawLine(Color(0xFF8F7CF7), Offset(size.width * 0.58f, size.height * 0.65f), Offset(size.width * 0.89f, size.height * 0.58f), stroke, StrokeCap.Round)
        }
    }
}
