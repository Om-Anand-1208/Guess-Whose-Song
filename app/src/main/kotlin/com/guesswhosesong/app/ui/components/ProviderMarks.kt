package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun GoogleGMark() {
    Canvas(Modifier.size(22.dp)) {
        val strokeWidth = 3.5.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val ringStroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        drawArc(Color(0xFF4285F4), -43f, 86f, false, topLeft, arcSize, style = ringStroke)
        drawArc(Color(0xFF34A853), 47f, 86f, false, topLeft, arcSize, style = ringStroke)
        drawArc(Color(0xFFFBBC05), 137f, 86f, false, topLeft, arcSize, style = ringStroke)
        drawArc(Color(0xFFEA4335), 227f, 86f, false, topLeft, arcSize, style = ringStroke)
        drawLine(
            Color(0xFF4285F4),
            Offset(size.width / 2f, size.height / 2f),
            Offset(size.width - strokeWidth / 2f, size.height / 2f),
            strokeWidth,
            StrokeCap.Butt
        )
    }
}

@Composable
fun SpotifyMark(modifier: Modifier = Modifier.size(27.dp)) {
    Canvas(modifier) {
        val diameter = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val green = Color(0xFF1ED760)
        val ink = Color(0xFF101010)
        drawCircle(green, radius = diameter / 2f, center = center)

        val lineWidth = diameter * 0.075f
        val waves = listOf(
            listOf(0.19f, 0.34f, 0.39f, 0.23f, 0.65f, 0.25f, 0.81f, 0.36f),
            listOf(0.22f, 0.47f, 0.40f, 0.39f, 0.61f, 0.41f, 0.78f, 0.49f),
            listOf(0.28f, 0.59f, 0.43f, 0.53f, 0.58f, 0.55f, 0.71f, 0.61f)
        )
        waves.forEach { points ->
            val wave = Path().apply {
                moveTo(diameter * points[0], diameter * points[1])
                cubicTo(
                    diameter * points[2], diameter * points[3],
                    diameter * points[4], diameter * points[5],
                    diameter * points[6], diameter * points[7]
                )
            }
            drawPath(wave, ink, style = Stroke(width = lineWidth, cap = StrokeCap.Round))
        }
    }
}
