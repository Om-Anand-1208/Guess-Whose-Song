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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.AvatarCustomizationCatalog
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
    contentDescription: String? = null,
    customization: AvatarCustomization? = null
) {
    val appearance = AvatarCustomization.normalize(customization, AvatarCatalog.normalize(avatarId))
    val option = avatarOption(appearance.colorId).copy(id = appearance.shapeId)
    val accessibleDescription = contentDescription
    Canvas(modifier = modifier.aspectRatio(1f).semantics {
        accessibleDescription?.let { this.contentDescription = it }
    }) {
        drawAvatar(option, appearance)
    }
}

@Composable
fun AvatarBadge(
    avatarId: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    customization: AvatarCustomization? = null
) {
    val appearance = AvatarCustomization.normalize(customization, AvatarCatalog.normalize(avatarId))
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size / 2.7f),
        color = avatarOption(appearance.colorId).color.copy(alpha = 0.22f),
        tonalElevation = 0.dp
    ) {
        AvatarCharacter(
            avatarId = appearance.shapeId,
            modifier = Modifier.padding(size / 10f),
            customization = appearance
        )
    }
}

@Composable
fun AvatarPicker(
    customization: AvatarCustomization,
    onPresetSelected: (String) -> Unit,
    onCustomizationChanged: (AvatarCustomization) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var draftCustomization by remember(customization) { mutableStateOf(customization) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Choose your character", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Pick a preset or make it yours",
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
                    val preset = AvatarCustomization.defaultsFor(option.id)
                    val isSelected = customization.shapeId == option.id
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f), CircleShape)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) option.accent else GwsAvatarInk.copy(alpha = 0.13f),
                                shape = CircleShape
                            )
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onPresetSelected(option.id) }
                            )
                            .semantics { contentDescription = "${option.name} avatar preset" },
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarCharacter(option.id, modifier = Modifier.size(37.dp), customization = preset)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBadge(
                avatarId = customization.shapeId,
                size = 48.dp,
                customization = customization
            )
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text("Your avatar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${avatarOption(customization.shapeId).name} · ${avatarOption(customization.colorId).name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = {
                draftCustomization = customization
                showEditor = true
            }) {
                Text("Customize")
            }
        }
    }

    if (showEditor) {
        AvatarCustomizationSheet(
            customization = draftCustomization,
            onCustomizationChanged = { draftCustomization = it },
            onDismiss = {
                draftCustomization = customization
                showEditor = false
            },
            onCancel = {
                draftCustomization = customization
                showEditor = false
            },
            onSave = {
                onCustomizationChanged(draftCustomization)
                showEditor = false
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AvatarCustomizationSheet(
    customization: AvatarCustomization,
    onCustomizationChanged: (AvatarCustomization) -> Unit,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarCharacter(
                avatarId = customization.shapeId,
                modifier = Modifier.size(100.dp),
                customization = customization
            )
            Text("Customize your avatar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            AvatarChoiceGroup(
                title = "Shape",
                options = AvatarCustomizationCatalog.shapeIds,
                selected = customization.shapeId,
                onSelected = { onCustomizationChanged(customization.copy(shapeId = it)) }
            )
            AvatarChoiceGroup(
                title = "Color",
                options = AvatarCustomizationCatalog.colorIds,
                selected = customization.colorId,
                onSelected = { onCustomizationChanged(customization.copy(colorId = it)) }
            )
            AvatarChoiceGroup(
                title = "Eyes",
                options = AvatarCustomizationCatalog.eyesIds,
                selected = customization.eyesId,
                onSelected = { onCustomizationChanged(customization.copy(eyesId = it)) }
            )
            AvatarChoiceGroup(
                title = "Mouth",
                options = AvatarCustomizationCatalog.mouthIds,
                selected = customization.mouthId,
                onSelected = { onCustomizationChanged(customization.copy(mouthId = it)) }
            )
            AvatarChoiceGroup(
                title = "Accessory",
                options = AvatarCustomizationCatalog.accessoryIds,
                selected = customization.accessoryId,
                onSelected = { onCustomizationChanged(customization.copy(accessoryId = it)) }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    onCustomizationChanged(AvatarCustomization.defaultsFor(customization.shapeId))
                }) {
                    Text("Reset")
                }
                TextButton(onClick = {
                    onCustomizationChanged(
                        AvatarCustomization(
                            shapeId = AvatarCustomizationCatalog.shapeIds.random(),
                            colorId = AvatarCustomizationCatalog.colorIds.random(),
                            eyesId = AvatarCustomizationCatalog.eyesIds.random(),
                            mouthId = AvatarCustomizationCatalog.mouthIds.random(),
                            accessoryId = AvatarCustomizationCatalog.accessoryIds.random()
                        )
                    )
                }) {
                    Text("Randomize")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun AvatarChoiceGroup(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 13.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    label = { Text(option.replaceFirstChar { it.uppercase() }) }
                )
            }
        }
    }
}

private val GwsAvatarInk = Color(0xFF17161A)

private fun DrawScope.drawAvatar(option: AvatarOption, appearance: AvatarCustomization) {
    val s = min(size.width, size.height)
    val ink = Color(0xFF17161A)
    val stroke = (s * 0.045f).coerceAtLeast(1.5f)
    val center = Offset(size.width / 2f, size.height / 2f)
    val lineStyle = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

    var faceX = center.x
    var faceY = center.y
    var faceScale = 0.8f

    fun face(x: Float, y: Float, scale: Float = 1f) {
        faceX = x
        faceY = y
        faceScale = scale
        val eyeY = y - s * 0.02f * scale
        val leftEyeX = x - s * 0.12f * scale
        val rightEyeX = x + s * 0.12f * scale
        val eyeRadius = stroke * 0.72f * scale
        when (appearance.eyesId) {
            "happy" -> listOf(leftEyeX, rightEyeX).forEach { eyeX ->
                drawArc(
                    color = ink,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(eyeX - s * 0.055f * scale, eyeY - s * 0.035f * scale),
                    size = androidx.compose.ui.geometry.Size(s * 0.11f * scale, s * 0.08f * scale),
                    style = Stroke(stroke * 0.9f, cap = StrokeCap.Round)
                )
            }
            "sleepy" -> listOf(leftEyeX, rightEyeX).forEach { eyeX ->
                drawLine(ink, Offset(eyeX - s * 0.045f * scale, eyeY), Offset(eyeX + s * 0.045f * scale, eyeY), strokeWidth = stroke * 1.1f, cap = StrokeCap.Round)
            }
            "wink" -> {
                drawCircle(ink, eyeRadius, Offset(leftEyeX, eyeY))
                drawArc(
                    color = ink,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(rightEyeX - s * 0.055f * scale, eyeY - s * 0.035f * scale),
                    size = androidx.compose.ui.geometry.Size(s * 0.11f * scale, s * 0.08f * scale),
                    style = Stroke(stroke * 0.9f, cap = StrokeCap.Round)
                )
            }
            "sunglasses" -> {
                val lensWidth = s * 0.12f * scale
                val lensHeight = s * 0.075f * scale
                drawRoundRect(ink, Offset(leftEyeX - lensWidth / 2f, eyeY - lensHeight / 2f), androidx.compose.ui.geometry.Size(lensWidth, lensHeight), androidx.compose.ui.geometry.CornerRadius(stroke))
                drawRoundRect(ink, Offset(rightEyeX - lensWidth / 2f, eyeY - lensHeight / 2f), androidx.compose.ui.geometry.Size(lensWidth, lensHeight), androidx.compose.ui.geometry.CornerRadius(stroke))
                drawLine(ink, Offset(leftEyeX + lensWidth / 2f, eyeY), Offset(rightEyeX - lensWidth / 2f, eyeY), strokeWidth = stroke * 0.72f, cap = StrokeCap.Round)
            }
            else -> {
                drawCircle(ink, eyeRadius, Offset(leftEyeX, eyeY))
                drawCircle(ink, eyeRadius, Offset(rightEyeX, eyeY))
            }
        }

        val mouthTop = y + s * 0.12f * scale
        when (appearance.mouthId) {
            "open" -> drawOval(
                ink,
                topLeft = Offset(x - s * 0.065f * scale, mouthTop - s * 0.005f * scale),
                size = androidx.compose.ui.geometry.Size(s * 0.13f * scale, s * 0.13f * scale)
            )
            "tongue" -> {
                drawOval(
                    ink,
                    topLeft = Offset(x - s * 0.07f * scale, mouthTop - s * 0.01f * scale),
                    size = androidx.compose.ui.geometry.Size(s * 0.14f * scale, s * 0.13f * scale)
                )
                drawOval(
                    Color(0xFFE85B76),
                    topLeft = Offset(x - s * 0.035f * scale, mouthTop + s * 0.065f * scale),
                    size = androidx.compose.ui.geometry.Size(s * 0.07f * scale, s * 0.055f * scale)
                )
            }
            else -> {
                val mouth = Path().apply {
                    moveTo(x - s * 0.12f * scale, mouthTop)
                    cubicTo(
                        x - s * 0.04f * scale, y + s * (if (appearance.mouthId == "grin") 0.25f else 0.21f) * scale,
                        x + s * 0.04f * scale, y + s * (if (appearance.mouthId == "grin") 0.25f else 0.21f) * scale,
                        x + s * 0.12f * scale, mouthTop
                    )
                }
                drawPath(mouth, ink, style = lineStyle)
                if (appearance.mouthId == "grin") {
                    drawRoundRect(
                        Color.White,
                        topLeft = Offset(x - s * 0.055f * scale, mouthTop + s * 0.014f * scale),
                        size = androidx.compose.ui.geometry.Size(s * 0.11f * scale, s * 0.038f * scale),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.01f * scale)
                    )
                }
            }
        }
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
            face(c.x, c.y, scale = 0.85f)
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
            face(center.x, s * 0.56f, scale = 0.82f)
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

    when (appearance.accessoryId) {
        "headphones" -> {
            val band = Path().apply {
                moveTo(faceX - s * 0.23f * faceScale, faceY - s * 0.08f * faceScale)
                cubicTo(
                    faceX - s * 0.24f * faceScale, faceY - s * 0.35f * faceScale,
                    faceX + s * 0.24f * faceScale, faceY - s * 0.35f * faceScale,
                    faceX + s * 0.23f * faceScale, faceY - s * 0.08f * faceScale
                )
            }
            drawPath(band, option.accent, style = Stroke(stroke * 1.6f, cap = StrokeCap.Round))
            drawRoundRect(option.accent, Offset(faceX - s * 0.27f * faceScale, faceY - s * 0.09f * faceScale), androidx.compose.ui.geometry.Size(s * 0.075f, s * 0.15f), androidx.compose.ui.geometry.CornerRadius(stroke))
            drawRoundRect(option.accent, Offset(faceX + s * 0.195f * faceScale, faceY - s * 0.09f * faceScale), androidx.compose.ui.geometry.Size(s * 0.075f, s * 0.15f), androidx.compose.ui.geometry.CornerRadius(stroke))
        }
        "glasses" -> {
            val eyeY = faceY - s * 0.02f * faceScale
            val lensWidth = s * 0.13f * faceScale
            val lensHeight = s * 0.095f * faceScale
            val lensStyle = Stroke(stroke * 0.85f)
            drawRoundRect(ink, Offset(faceX - s * 0.12f * faceScale - lensWidth / 2f, eyeY - lensHeight / 2f), androidx.compose.ui.geometry.Size(lensWidth, lensHeight), androidx.compose.ui.geometry.CornerRadius(stroke * 2), style = lensStyle)
            drawRoundRect(ink, Offset(faceX + s * 0.12f * faceScale - lensWidth / 2f, eyeY - lensHeight / 2f), androidx.compose.ui.geometry.Size(lensWidth, lensHeight), androidx.compose.ui.geometry.CornerRadius(stroke * 2), style = lensStyle)
            drawLine(ink, Offset(faceX - s * 0.055f * faceScale, eyeY), Offset(faceX + s * 0.055f * faceScale, eyeY), strokeWidth = stroke * 0.75f, cap = StrokeCap.Round)
        }
        "cap" -> {
            val cap = Path().apply {
                moveTo(center.x - s * 0.25f, s * 0.30f)
                cubicTo(center.x - s * 0.22f, s * 0.12f, center.x + s * 0.2f, s * 0.12f, center.x + s * 0.25f, s * 0.30f)
                lineTo(center.x + s * 0.31f, s * 0.34f)
                cubicTo(center.x + s * 0.20f, s * 0.41f, center.x - s * 0.15f, s * 0.40f, center.x - s * 0.25f, s * 0.30f)
                close()
            }
            drawPath(cap, option.accent)
            drawPath(cap, ink, style = Stroke(stroke * 0.7f, join = StrokeJoin.Round))
            drawLine(ink, Offset(center.x - s * 0.22f, s * 0.30f), Offset(center.x + s * 0.22f, s * 0.30f), strokeWidth = stroke * 0.65f, cap = StrokeCap.Round)
        }
        "bow" -> {
            val bowY = center.y - s * 0.27f
            val leftBow = Path().apply {
                moveTo(center.x, bowY)
                lineTo(center.x - s * 0.15f, bowY - s * 0.11f)
                lineTo(center.x - s * 0.17f, bowY + s * 0.08f)
                close()
            }
            val rightBow = Path().apply {
                moveTo(center.x, bowY)
                lineTo(center.x + s * 0.15f, bowY - s * 0.11f)
                lineTo(center.x + s * 0.17f, bowY + s * 0.08f)
                close()
            }
            drawPath(leftBow, option.accent)
            drawPath(rightBow, option.accent)
            drawCircle(ink, s * 0.025f, Offset(center.x, bowY))
        }
        "flower" -> {
            val flowerCenter = Offset(center.x + s * 0.23f, center.y - s * 0.24f)
            repeat(5) { index ->
                val angle = (-90f + index * 72f) * (Math.PI / 180f).toFloat()
                drawCircle(option.accent, s * 0.055f, Offset(flowerCenter.x + cos(angle) * s * 0.055f, flowerCenter.y + sin(angle) * s * 0.055f))
            }
            drawCircle(Color(0xFFFFD45E), s * 0.035f, flowerCenter)
        }
    }
}
