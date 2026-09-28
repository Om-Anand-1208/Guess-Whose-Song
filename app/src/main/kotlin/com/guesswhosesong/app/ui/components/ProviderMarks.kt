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
fun SpotifyMark() {
    Canvas(Modifier.size(27.dp)) {
        drawCircle(Color(0xFF1DB954))
        val stroke = size.minDimension * 0.075f
        listOf(
            Triple(0.27f, 0.39f, 0.74f),
            Triple(0.31f, 0.51f, 0.68f),
            Triple(0.36f, 0.63f, 0.58f)
        ).forEach { (left, y, right) ->
            val wave = Path().apply {
                moveTo(size.width * left, size.height * y)
                quadraticTo(size.width * 0.52f, size.height * (y - 0.10f), size.width * right, size.height * y)
            }
            drawPath(wave, Color(0xFF101010), style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    }
}
