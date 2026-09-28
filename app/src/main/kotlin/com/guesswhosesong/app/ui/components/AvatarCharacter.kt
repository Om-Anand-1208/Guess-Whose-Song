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
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

@Immutable
data class AvatarOption(
    val id: String,
    val name: String,
    val color: Color,
    val accent: Color,
    val colorName: String
)

val AvatarOptions = listOf(
    AvatarOption("sunny", "Sunny", Color(0xFFFFAE29), Color(0xFFEF8F18), "Orange"),
    AvatarOption("lime", "Lime", Color(0xFFB6F45B), Color(0xFF77C945), "Lime"),
    AvatarOption("violet", "Triangle", Color(0xFFA394F1), Color(0xFF7060C8), "Lavender"),
    AvatarOption("cloud", "Cloud", Color(0xFFAED5F4), Color(0xFF79ACD7), "Sky blue"),
    AvatarOption("star", "Star", Color(0xFFFFD43B), Color(0xFFD49E22), "Yellow"),
    AvatarOption("tangerine", "Honey", Color(0xFFEF9387), Color(0xFFCD675D), "Coral"),
    AvatarOption("berry", "Diamond", Color(0xFFF17FAC), Color(0xFFC94C78), "Pink"),
    AvatarOption("mint", "Heart", Color(0xFF73D9CA), Color(0xFF39AD9B), "Mint")
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
                    "${avatarOption(customization.shapeId).name} · ${avatarOption(customization.colorId).colorName}",
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
                    label = {
                        Text(
                            when (title) {
                                "Shape" -> avatarOption(option).name
                                "Color" -> avatarOption(option).colorName
                                else -> option.replaceFirstChar { it.uppercase() }
                            }
                        )
                    }
                )
            }
        }
    }
}

private val GwsAvatarInk = Color(0xFF17161A)

private fun DrawScope.drawAvatar(option: AvatarOption, appearance: AvatarCustomization) {
    val s = min(size.width, size.height)
    val x = (size.width - s) / 2f
    val y = (size.height - s) / 2f
    val ink = GwsAvatarInk
    val stroke = (s * 0.034f).coerceAtLeast(1.1f)
    val outline = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(nx: Float, ny: Float) = Offset(x + s * nx, y + s * ny)
    val body = Path()

    fun drawBody(path: Path) {
        drawPath(path, option.color)
        drawPath(path, ink, style = outline)
    }

    fun drawLegs(leftX: Float, rightX: Float, topY: Float) {
        val leftLeg = Path().apply {
            moveTo(p(leftX, topY).x, p(leftX, topY).y)
            cubicTo(p(leftX - 0.01f, topY + 0.07f).x, p(leftX - 0.01f, topY + 0.07f).y,
                p(leftX - 0.02f, 0.91f).x, p(leftX - 0.02f, 0.91f).y,
                p(leftX - 0.02f, 0.93f).x, p(leftX - 0.02f, 0.93f).y)
        }
        val rightLeg = Path().apply {
            moveTo(p(rightX, topY).x, p(rightX, topY).y)
            cubicTo(p(rightX + 0.01f, topY + 0.07f).x, p(rightX + 0.01f, topY + 0.07f).y,
                p(rightX + 0.02f, 0.91f).x, p(rightX + 0.02f, 0.91f).y,
                p(rightX + 0.02f, 0.93f).x, p(rightX + 0.02f, 0.93f).y)
        }
        drawPath(leftLeg, ink, style = Stroke(stroke * 0.9f, cap = StrokeCap.Round))
        drawPath(rightLeg, ink, style = Stroke(stroke * 0.9f, cap = StrokeCap.Round))
        drawOval(ink, topLeft = p(leftX - 0.055f, 0.92f), size = androidx.compose.ui.geometry.Size(s * 0.09f, s * 0.035f))
        drawOval(ink, topLeft = p(rightX - 0.035f, 0.92f), size = androidx.compose.ui.geometry.Size(s * 0.09f, s * 0.035f))
    }

    fun drawArms(leftX: Float, rightX: Float, armY: Float, leftHandY: Float, rightHandY: Float) {
        val leftHand = p(leftX - 0.12f, leftHandY)
        val rightHand = p(rightX + 0.12f, rightHandY)
        val leftArm = Path().apply {
            moveTo(p(leftX, armY).x, p(leftX, armY).y)
            cubicTo(p(leftX - 0.06f, armY).x, p(leftX - 0.06f, armY).y,
                p(leftX - 0.13f, leftHandY + 0.04f).x, p(leftX - 0.13f, leftHandY + 0.04f).y,
                leftHand.x, leftHand.y)
        }
        val rightArm = Path().apply {
            moveTo(p(rightX, armY).x, p(rightX, armY).y)
            cubicTo(p(rightX + 0.06f, armY).x, p(rightX + 0.06f, armY).y,
                p(rightX + 0.13f, rightHandY + 0.04f).x, p(rightX + 0.13f, rightHandY + 0.04f).y,
                rightHand.x, rightHand.y)
        }
        drawPath(leftArm, ink, style = Stroke(stroke * 0.85f, cap = StrokeCap.Round))
        drawPath(rightArm, ink, style = Stroke(stroke * 0.85f, cap = StrokeCap.Round))
        listOf(leftHand, rightHand).forEach { hand ->
            drawCircle(Color(0xFFFFFBF3), s * 0.023f, hand)
            drawCircle(ink, s * 0.023f, hand, style = Stroke(stroke * 0.55f))
        }
    }

    when (appearance.shapeId) {
        "lime" -> { drawArms(0.22f, 0.78f, 0.55f, 0.56f, 0.56f); drawLegs(0.42f, 0.58f, 0.73f) }
        "violet" -> { drawArms(0.25f, 0.75f, 0.58f, 0.65f, 0.36f); drawLegs(0.42f, 0.58f, 0.75f) }
        "tangerine" -> { drawArms(0.24f, 0.76f, 0.56f, 0.59f, 0.59f); drawLegs(0.42f, 0.58f, 0.75f) }
        "berry" -> { drawArms(0.29f, 0.71f, 0.53f, 0.61f, 0.40f); drawLegs(0.44f, 0.56f, 0.79f) }
        "cloud" -> drawLegs(0.42f, 0.58f, 0.70f)
        "star" -> drawLegs(0.43f, 0.57f, 0.76f)
        "mint" -> drawLegs(0.43f, 0.57f, 0.77f)
        else -> drawLegs(0.43f, 0.57f, 0.75f)
    }

    when (appearance.shapeId) {
        "sunny" -> {
            drawCircle(option.color, s * 0.30f, p(0.5f, 0.48f))
            drawCircle(ink, s * 0.30f, p(0.5f, 0.48f), style = outline)
        }
        "lime" -> {
            val rect = androidx.compose.ui.geometry.Size(s * 0.56f, s * 0.52f)
            drawRoundRect(option.color, p(0.22f, 0.23f), rect, androidx.compose.ui.geometry.CornerRadius(s * 0.055f))
            drawRoundRect(ink, p(0.22f, 0.23f), rect, androidx.compose.ui.geometry.CornerRadius(s * 0.055f), style = outline)
            drawLine(option.accent, p(0.28f, 0.29f), p(0.72f, 0.29f), strokeWidth = stroke * 1.35f, cap = StrokeCap.Round)
        }
        "violet" -> {
            body.reset()
            body.moveTo(p(0.50f, 0.17f).x, p(0.50f, 0.17f).y)
            body.lineTo(p(0.83f, 0.77f).x, p(0.83f, 0.77f).y)
            body.lineTo(p(0.17f, 0.77f).x, p(0.17f, 0.77f).y)
            body.close()
            drawBody(body)
        }
        "cloud" -> {
            body.reset()
            body.moveTo(p(0.14f, 0.62f).x, p(0.14f, 0.62f).y)
            body.cubicTo(p(0.10f, 0.53f).x, p(0.10f, 0.53f).y, p(0.18f, 0.43f).x, p(0.18f, 0.43f).y, p(0.32f, 0.46f).x, p(0.32f, 0.46f).y)
            body.cubicTo(p(0.34f, 0.29f).x, p(0.34f, 0.29f).y, p(0.51f, 0.25f).x, p(0.51f, 0.25f).y, p(0.59f, 0.41f).x, p(0.59f, 0.41f).y)
            body.cubicTo(p(0.73f, 0.34f).x, p(0.73f, 0.34f).y, p(0.87f, 0.44f).x, p(0.87f, 0.44f).y, p(0.84f, 0.56f).x, p(0.84f, 0.56f).y)
            body.cubicTo(p(0.91f, 0.64f).x, p(0.91f, 0.64f).y, p(0.83f, 0.73f).x, p(0.83f, 0.73f).y, p(0.70f, 0.72f).x, p(0.70f, 0.72f).y)
            body.lineTo(p(0.28f, 0.72f).x, p(0.28f, 0.72f).y)
            body.cubicTo(p(0.15f, 0.74f).x, p(0.15f, 0.74f).y, p(0.10f, 0.68f).x, p(0.10f, 0.68f).y, p(0.14f, 0.62f).x, p(0.14f, 0.62f).y)
            body.close()
            drawBody(body)
        }
        "star" -> {
            body.reset()
            repeat(10) { index ->
                val angle = (-PI / 2.0 + index * PI / 5.0).toFloat()
                val radius = if (index % 2 == 0) 0.39f else 0.18f
                val point = p(0.5f + cos(angle) * radius, 0.51f + sin(angle) * radius)
                if (index == 0) body.moveTo(point.x, point.y) else body.lineTo(point.x, point.y)
            }
            body.close()
            drawBody(body)
        }
        "tangerine" -> {
            body.reset()
            listOf(0.35f to 0.29f, 0.65f to 0.29f, 0.78f to 0.42f, 0.78f to 0.61f,
                0.65f to 0.75f, 0.35f to 0.75f, 0.22f to 0.61f, 0.22f to 0.42f).forEachIndexed { index, point ->
                val at = p(point.first, point.second)
                if (index == 0) body.moveTo(at.x, at.y) else body.lineTo(at.x, at.y)
            }
            body.close()
            drawBody(body)
        }
        "berry" -> {
            body.reset()
            body.moveTo(p(0.50f, 0.16f).x, p(0.50f, 0.16f).y)
            body.cubicTo(p(0.59f, 0.27f).x, p(0.59f, 0.27f).y, p(0.73f, 0.42f).x, p(0.73f, 0.42f).y, p(0.80f, 0.50f).x, p(0.80f, 0.50f).y)
            body.cubicTo(p(0.71f, 0.60f).x, p(0.71f, 0.60f).y, p(0.58f, 0.76f).x, p(0.58f, 0.76f).y, p(0.50f, 0.82f).x, p(0.50f, 0.82f).y)
            body.cubicTo(p(0.42f, 0.76f).x, p(0.42f, 0.76f).y, p(0.29f, 0.60f).x, p(0.29f, 0.60f).y, p(0.20f, 0.50f).x, p(0.20f, 0.50f).y)
            body.cubicTo(p(0.27f, 0.42f).x, p(0.27f, 0.42f).y, p(0.41f, 0.27f).x, p(0.41f, 0.27f).y, p(0.50f, 0.16f).x, p(0.50f, 0.16f).y)
            body.close()
            drawBody(body)
        }
        else -> {
            body.reset()
            body.moveTo(p(0.50f, 0.78f).x, p(0.50f, 0.78f).y)
            body.cubicTo(p(0.43f, 0.72f).x, p(0.43f, 0.72f).y, p(0.19f, 0.54f).x, p(0.19f, 0.54f).y, p(0.19f, 0.40f).x, p(0.19f, 0.40f).y)
            body.cubicTo(p(0.19f, 0.24f).x, p(0.19f, 0.24f).y, p(0.36f, 0.22f).x, p(0.36f, 0.22f).y, p(0.50f, 0.36f).x, p(0.50f, 0.36f).y)
            body.cubicTo(p(0.64f, 0.22f).x, p(0.64f, 0.22f).y, p(0.81f, 0.24f).x, p(0.81f, 0.24f).y, p(0.81f, 0.40f).x, p(0.81f, 0.40f).y)
            body.cubicTo(p(0.81f, 0.54f).x, p(0.81f, 0.54f).y, p(0.57f, 0.72f).x, p(0.57f, 0.72f).y, p(0.50f, 0.78f).x, p(0.50f, 0.78f).y)
            body.close()
            drawBody(body)
        }
    }

    val faceY = when (appearance.shapeId) {
        "cloud" -> 0.54f
        "star" -> 0.51f
        "tangerine" -> 0.53f
        "berry" -> 0.49f
        "mint" -> 0.48f
        else -> 0.49f
    }
    val faceScale = 0.82f
    val eyeY = faceY - 0.045f * faceScale
    val leftEyeX = 0.5f - 0.115f * faceScale
    val rightEyeX = 0.5f + 0.115f * faceScale
    val eyeRadius = s * 0.021f * faceScale

    if (appearance.accessoryId == "headphones") {
        val band = Path().apply {
            moveTo(p(0.23f, 0.46f).x, p(0.23f, 0.46f).y)
            cubicTo(p(0.20f, 0.11f).x, p(0.20f, 0.11f).y, p(0.80f, 0.11f).x, p(0.80f, 0.11f).y, p(0.77f, 0.46f).x, p(0.77f, 0.46f).y)
        }
        drawPath(band, ink, style = Stroke(stroke * 1.7f, cap = StrokeCap.Round))
        drawRoundRect(ink, p(0.14f, 0.40f), androidx.compose.ui.geometry.Size(s * 0.12f, s * 0.19f), androidx.compose.ui.geometry.CornerRadius(s * 0.045f))
        drawRoundRect(Color(0xFF466EE6), p(0.17f, 0.43f), androidx.compose.ui.geometry.Size(s * 0.055f, s * 0.12f), androidx.compose.ui.geometry.CornerRadius(s * 0.025f))
        drawRoundRect(ink, p(0.74f, 0.40f), androidx.compose.ui.geometry.Size(s * 0.12f, s * 0.19f), androidx.compose.ui.geometry.CornerRadius(s * 0.045f))
        drawRoundRect(Color(0xFF466EE6), p(0.77f, 0.43f), androidx.compose.ui.geometry.Size(s * 0.055f, s * 0.12f), androidx.compose.ui.geometry.CornerRadius(s * 0.025f))
    }

    if (appearance.accessoryId == "cap") {
        val cap = Path().apply {
            moveTo(p(0.29f, 0.33f).x, p(0.29f, 0.33f).y)
            cubicTo(p(0.30f, 0.16f).x, p(0.30f, 0.16f).y, p(0.67f, 0.15f).x, p(0.67f, 0.15f).y, p(0.72f, 0.31f).x, p(0.72f, 0.31f).y)
            lineTo(p(0.82f, 0.35f).x, p(0.82f, 0.35f).y)
            cubicTo(p(0.72f, 0.41f).x, p(0.72f, 0.41f).y, p(0.39f, 0.41f).x, p(0.39f, 0.41f).y, p(0.27f, 0.35f).x, p(0.27f, 0.35f).y)
            close()
        }
        drawPath(cap, Color(0xFF3969E8))
        drawPath(cap, ink, style = Stroke(stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    when (appearance.eyesId) {
        "happy" -> listOf(leftEyeX, rightEyeX).forEach { eyeX ->
            val eye = Path().apply {
                moveTo(p(eyeX - 0.055f, eyeY).x, p(eyeX - 0.055f, eyeY).y)
                cubicTo(p(eyeX - 0.025f, eyeY + 0.045f).x, p(eyeX - 0.025f, eyeY + 0.045f).y,
                    p(eyeX + 0.025f, eyeY + 0.045f).x, p(eyeX + 0.025f, eyeY + 0.045f).y,
                    p(eyeX + 0.055f, eyeY).x, p(eyeX + 0.055f, eyeY).y)
            }
            drawPath(eye, ink, style = Stroke(stroke * 1.2f, cap = StrokeCap.Round))
        }
        "sleepy" -> listOf(leftEyeX, rightEyeX).forEach { eyeX ->
            drawLine(ink, p(eyeX - 0.045f, eyeY), p(eyeX + 0.045f, eyeY + 0.012f), strokeWidth = stroke * 1.15f, cap = StrokeCap.Round)
        }
        "wink" -> {
            drawCircle(ink, eyeRadius, p(leftEyeX, eyeY))
            val wink = Path().apply {
                moveTo(p(rightEyeX - 0.055f, eyeY).x, p(rightEyeX - 0.055f, eyeY).y)
                cubicTo(p(rightEyeX - 0.02f, eyeY + 0.045f).x, p(rightEyeX - 0.02f, eyeY + 0.045f).y,
                    p(rightEyeX + 0.02f, eyeY + 0.045f).x, p(rightEyeX + 0.02f, eyeY + 0.045f).y,
                    p(rightEyeX + 0.055f, eyeY).x, p(rightEyeX + 0.055f, eyeY).y)
            }
            drawPath(wink, ink, style = Stroke(stroke * 1.2f, cap = StrokeCap.Round))
        }
        "sunglasses" -> {
            listOf(leftEyeX, rightEyeX).forEach { eyeX ->
                drawRoundRect(ink, p(eyeX - 0.075f, eyeY - 0.045f), androidx.compose.ui.geometry.Size(s * 0.15f, s * 0.09f), androidx.compose.ui.geometry.CornerRadius(s * 0.025f))
            }
            drawLine(ink, p(leftEyeX + 0.07f, eyeY), p(rightEyeX - 0.07f, eyeY), strokeWidth = stroke)
        }
        else -> {
            drawCircle(ink, eyeRadius, p(leftEyeX, eyeY))
            drawCircle(ink, eyeRadius, p(rightEyeX, eyeY))
        }
    }

    val mouthY = faceY + 0.075f
    when (appearance.mouthId) {
        "smile" -> {
            val mouth = Path().apply {
                moveTo(p(0.5f - 0.09f * faceScale, mouthY).x, p(0.5f - 0.09f * faceScale, mouthY).y)
                cubicTo(p(0.5f - 0.04f * faceScale, mouthY + 0.075f).x, p(0.5f - 0.04f * faceScale, mouthY + 0.075f).y,
                    p(0.5f + 0.04f * faceScale, mouthY + 0.075f).x, p(0.5f + 0.04f * faceScale, mouthY + 0.075f).y,
                    p(0.5f + 0.09f * faceScale, mouthY).x, p(0.5f + 0.09f * faceScale, mouthY).y)
            }
            drawPath(mouth, ink, style = Stroke(stroke * 1.25f, cap = StrokeCap.Round))
        }
        else -> {
            val mouthWidth = if (appearance.mouthId == "grin") 0.14f else 0.11f
            val mouthHeight = if (appearance.mouthId == "grin") 0.12f else 0.10f
            drawOval(ink, topLeft = p(0.5f - mouthWidth / 2f, mouthY - 0.025f), size = androidx.compose.ui.geometry.Size(s * mouthWidth, s * mouthHeight))
            if (appearance.mouthId == "tongue") {
                drawOval(Color(0xFFEF7187), topLeft = p(0.5f - 0.035f, mouthY + 0.03f), size = androidx.compose.ui.geometry.Size(s * 0.07f, s * 0.045f))
            }
        }
    }

    when (appearance.accessoryId) {
        "glasses" -> {
            listOf(leftEyeX, rightEyeX).forEach { eyeX ->
                drawRoundRect(ink, p(eyeX - 0.075f, eyeY - 0.045f), androidx.compose.ui.geometry.Size(s * 0.15f, s * 0.09f), androidx.compose.ui.geometry.CornerRadius(s * 0.025f), style = Stroke(stroke))
            }
            drawLine(ink, p(leftEyeX + 0.07f, eyeY), p(rightEyeX - 0.07f, eyeY), strokeWidth = stroke)
        }
        "bow" -> {
            val bow = Path().apply {
                moveTo(p(0.50f, 0.24f).x, p(0.50f, 0.24f).y)
                cubicTo(p(0.39f, 0.13f).x, p(0.39f, 0.13f).y, p(0.31f, 0.18f).x, p(0.31f, 0.18f).y, p(0.42f, 0.31f).x, p(0.42f, 0.31f).y)
                cubicTo(p(0.45f, 0.34f).x, p(0.45f, 0.34f).y, p(0.48f, 0.29f).x, p(0.48f, 0.29f).y, p(0.50f, 0.24f).x, p(0.50f, 0.24f).y)
                cubicTo(p(0.61f, 0.13f).x, p(0.61f, 0.13f).y, p(0.69f, 0.18f).x, p(0.69f, 0.18f).y, p(0.58f, 0.31f).x, p(0.58f, 0.31f).y)
                close()
            }
            drawPath(bow, option.accent)
            drawPath(bow, ink, style = Stroke(stroke * 0.75f))
            drawCircle(ink, s * 0.025f, p(0.50f, 0.27f))
        }
        "flower" -> {
            repeat(5) { index ->
                val angle = (-PI / 2.0 + index * 2.0 * PI / 5.0).toFloat()
                drawCircle(Color(0xFFFFF3F6), s * 0.045f, p(0.73f + cos(angle) * 0.045f, 0.27f + sin(angle) * 0.045f))
            }
            drawCircle(option.accent, s * 0.027f, p(0.73f, 0.27f))
        }
    }
}
