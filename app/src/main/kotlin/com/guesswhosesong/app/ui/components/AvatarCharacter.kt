package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guesswhosesong.shared.models.AvatarCatalog
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Immutable
data class AvatarOption(
    val id: String,
    val name: String,
    val color: Color,
    val accent: Color
)

val AvatarOptions = listOf(
    AvatarOption("sunny", "Sunny", Color(0xFFFFB43E), Color(0xFFFFD56D)),
    AvatarOption("lime", "Lime", Color(0xFFB8F45D), Color(0xFF75C94A)),
    AvatarOption("violet", "Violet", Color(0xFF8F7CF7), Color(0xFF6553C9)),
    AvatarOption("tangerine", "Tangy", Color(0xFFFF8B3D), Color(0xFFFFCF5C)),
    AvatarOption("cloud", "Cloudy", Color(0xFF9CCBFF), Color(0xFF679DEB)),
    AvatarOption("star", "Stella", Color(0xFFFFDF72), Color(0xFFE6A72E)),
    AvatarOption("berry", "Berry", Color(0xFFFF91B3), Color(0xFFD95178)),
    AvatarOption("mint", "Minty", Color(0xFF8CE7C1), Color(0xFF42B88B))
)

fun avatarOption(id: String): AvatarOption =
    AvatarOptions.firstOrNull { it.id == id } ?: AvatarOptions.first()

@Composable
fun AvatarCharacter(
    avatarId: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val option = avatarOption(AvatarCatalog.normalize(avatarId))
    Canvas(modifier = modifier.aspectRatio(1f)) {
        drawAvatar(option)
    }
}

@Composable
fun AvatarBadge(
    avatarId: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size / 2.7f),
        color = avatarOption(avatarId).color.copy(alpha = 0.22f),
        tonalElevation = 0.dp
    ) {
        AvatarCharacter(avatarId = avatarId, modifier = Modifier.padding(size / 10f))
    }
}

@Composable
fun AvatarPicker(
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Choose your color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Tap a circle to pick your player color",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(10.dp))
        AvatarOptions.chunked(4).forEachIndexed { rowIndex, rowOptions ->
            if (rowIndex > 0) Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowOptions.forEach { option ->
                    val isSelected = option.id == AvatarCatalog.normalize(selectedId)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(option.color, CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) option.accent else Color.White.copy(alpha = 0.9f),
                                shape = CircleShape
                            )
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelected(option.id) }
                            )
                            .semantics { contentDescription = "${option.name} player color" }
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawAvatar(option: AvatarOption) {
    val s = min(size.width, size.height)
    val ink = Color(0xFF17161A)
    val stroke = (s * 0.045f).coerceAtLeast(1.5f)
    val center = Offset(size.width / 2f, size.height / 2f)
    val lineStyle = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun face(x: Float, y: Float, scale: Float = 1f, happy: Boolean = true) {
        drawCircle(ink, radius = stroke * 0.72f * scale, center = Offset(x - s * 0.12f * scale, y - s * 0.02f * scale))
        drawCircle(ink, radius = stroke * 0.72f * scale, center = Offset(x + s * 0.12f * scale, y - s * 0.02f * scale))
        val mouth = Path().apply {
            moveTo(x - s * 0.12f * scale, y + s * 0.12f * scale)
            cubicTo(
                x - s * 0.04f * scale, y + s * (if (happy) 0.21f else 0.06f) * scale,
                x + s * 0.04f * scale, y + s * (if (happy) 0.21f else 0.06f) * scale,
                x + s * 0.12f * scale, y + s * 0.12f * scale
            )
        }
        drawPath(mouth, ink, style = lineStyle)
    }

    fun limbs(x: Float, y: Float, bodyWidth: Float, bodyBottom: Float) {
        drawLine(ink, Offset(x - bodyWidth / 2f, y + s * 0.02f), Offset(x - bodyWidth * 0.8f, y + s * 0.15f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(ink, Offset(x + bodyWidth / 2f, y + s * 0.02f), Offset(x + bodyWidth * 0.8f, y - s * 0.05f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(ink, Offset(x - bodyWidth * 0.2f, bodyBottom), Offset(x - bodyWidth * 0.28f, bodyBottom + s * 0.18f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(ink, Offset(x + bodyWidth * 0.2f, bodyBottom), Offset(x + bodyWidth * 0.35f, bodyBottom + s * 0.17f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(ink, Offset(x - bodyWidth * 0.42f, bodyBottom + s * 0.18f), Offset(x - bodyWidth * 0.18f, bodyBottom + s * 0.18f), strokeWidth = stroke * 1.4f, cap = StrokeCap.Round)
        drawLine(ink, Offset(x + bodyWidth * 0.2f, bodyBottom + s * 0.17f), Offset(x + bodyWidth * 0.48f, bodyBottom + s * 0.17f), strokeWidth = stroke * 1.4f, cap = StrokeCap.Round)
    }

    when (option.id) {
        "sunny", "tangerine" -> {
            val r = s * if (option.id == "sunny") 0.31f else 0.28f
            val c = Offset(center.x, center.y - s * 0.02f)
            if (option.id == "sunny") {
                repeat(10) { index ->
                    val angle = (index * 36f - 90f) * (Math.PI / 180f).toFloat()
                    val inner = Offset(c.x + cos(angle) * r * 1.28f, c.y + sin(angle) * r * 1.28f)
                    val outer = Offset(c.x + cos(angle) * r * 1.58f, c.y + sin(angle) * r * 1.58f)
                    drawLine(option.accent, inner, outer, strokeWidth = stroke * 1.25f, cap = StrokeCap.Round)
                }
            }
            drawCircle(option.color, r, c)
            drawCircle(ink, stroke * 0.72f, Offset(c.x - r * 0.28f, c.y - r * 0.08f))
            drawCircle(ink, stroke * 0.72f, Offset(c.x + r * 0.28f, c.y - r * 0.08f))
            drawArc(
                color = ink,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(c.x - r * 0.30f, c.y + r * 0.05f),
                size = androidx.compose.ui.geometry.Size(r * 0.60f, r * 0.38f),
                style = Stroke(stroke * 0.8f, cap = StrokeCap.Round)
            )
            limbs(c.x, c.y + r * 0.6f, r * 1.25f, c.y + r)
            if (option.id == "tangerine") {
                drawLine(ink, Offset(c.x - r * 0.12f, c.y - r * 1.02f), Offset(c.x + r * 0.12f, c.y - r * 1.28f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawCircle(option.accent, r * 0.18f, Offset(c.x + r * 0.2f, c.y - r * 1.31f))
            }
        }
        "lime", "mint" -> {
            val w = s * 0.50f
            val h = s * 0.46f
            val top = center.y - h * 0.46f
            val left = center.x - w / 2f
            drawRoundRect(option.color, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.10f))
            drawRoundRect(ink, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.10f), style = lineStyle)
            drawLine(option.accent, Offset(left + s * 0.05f, top + s * 0.06f), Offset(left + w - s * 0.05f, top + s * 0.06f), strokeWidth = stroke * 1.5f, cap = StrokeCap.Round)
            face(center.x, top + h * 0.54f, scale = 0.8f)
            limbs(center.x, top + h * 0.54f, w, top + h)
        }
        "violet" -> {
            val path = Path().apply {
                moveTo(center.x, s * 0.16f)
                lineTo(s * 0.82f, s * 0.77f)
                lineTo(s * 0.18f, s * 0.77f)
                close()
            }
            drawPath(path, option.color)
            drawPath(path, ink, style = lineStyle)
            drawLine(ink, Offset(s * 0.28f, s * 0.53f), Offset(s * 0.72f, s * 0.53f), strokeWidth = stroke * 1.25f, cap = StrokeCap.Round)
            drawCircle(ink, stroke * 0.7f, Offset(s * 0.38f, s * 0.57f))
            drawCircle(ink, stroke * 0.7f, Offset(s * 0.62f, s * 0.57f))
            drawLine(ink, Offset(s * 0.4f, s * 0.78f), Offset(s * 0.34f, s * 0.94f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(ink, Offset(s * 0.6f, s * 0.78f), Offset(s * 0.68f, s * 0.94f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(ink, Offset(s * 0.24f, s * 0.61f), Offset(s * 0.08f, s * 0.49f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(ink, Offset(s * 0.76f, s * 0.61f), Offset(s * 0.91f, s * 0.67f), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        "cloud" -> {
            val cloud = Path().apply {
                moveTo(s * 0.18f, s * 0.60f)
                cubicTo(s * 0.16f, s * 0.43f, s * 0.31f, s * 0.37f, s * 0.41f, s * 0.45f)
                cubicTo(s * 0.46f, s * 0.23f, s * 0.76f, s * 0.21f, s * 0.79f, s * 0.47f)
                cubicTo(s * 0.95f, s * 0.47f, s * 0.96f, s * 0.69f, s * 0.79f, s * 0.72f)
                lineTo(s * 0.28f, s * 0.72f)
                cubicTo(s * 0.18f, s * 0.71f, s * 0.14f, s * 0.66f, s * 0.18f, s * 0.60f)
                close()
            }
            drawPath(cloud, option.color)
            drawPath(cloud, ink, style = lineStyle)
            face(center.x, s * 0.58f, scale = 0.78f)
            drawLine(ink, Offset(s * 0.36f, s * 0.75f), Offset(s * 0.31f, s * 0.92f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(ink, Offset(s * 0.64f, s * 0.75f), Offset(s * 0.71f, s * 0.90f), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        "star" -> {
            val star = Path()
            repeat(10) { index ->
                val angle = (-90f + index * 36f) * (Math.PI / 180f).toFloat()
                val radius = if (index % 2 == 0) s * 0.38f else s * 0.17f
                val point = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
                if (index == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
            }
            star.close()
            drawPath(star, option.color)
            drawPath(star, ink, style = lineStyle)
            face(center.x, center.y + s * 0.01f, scale = 0.72f)
            drawLine(ink, Offset(s * 0.28f, s * 0.67f), Offset(s * 0.12f, s * 0.77f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(ink, Offset(s * 0.72f, s * 0.67f), Offset(s * 0.88f, s * 0.57f), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        "berry" -> {
            drawCircle(option.color, s * 0.30f, Offset(center.x, center.y + s * 0.04f))
            drawCircle(option.accent, s * 0.30f, Offset(center.x, center.y + s * 0.04f), style = lineStyle)
            drawCircle(option.accent, s * 0.025f, Offset(center.x - s * 0.12f, center.y - s * 0.05f))
            drawCircle(option.accent, s * 0.025f, Offset(center.x + s * 0.02f, center.y + s * 0.12f))
            drawCircle(option.accent, s * 0.025f, Offset(center.x + s * 0.14f, center.y - s * 0.10f))
            drawLine(ink, Offset(center.x, center.y - s * 0.24f), Offset(center.x + s * 0.05f, center.y - s * 0.38f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(ink, Offset(center.x + s * 0.05f, center.y - s * 0.36f), Offset(center.x + s * 0.20f, center.y - s * 0.40f), strokeWidth = stroke, cap = StrokeCap.Round)
            face(center.x, center.y + s * 0.02f, scale = 0.75f)
            limbs(center.x, center.y + s * 0.31f, s * 0.70f, center.y + s * 0.34f)
        }
        else -> {
            drawCircle(option.color, s * 0.31f, center)
            drawCircle(ink, s * 0.31f, center, style = lineStyle)
            face(center.x, center.y, scale = 0.8f)
        }
    }
}
