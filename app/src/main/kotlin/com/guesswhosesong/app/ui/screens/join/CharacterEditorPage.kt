package com.guesswhosesong.app.ui.screens.join

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guesswhosesong.app.ui.components.AvatarCharacter
import com.guesswhosesong.app.ui.components.AvatarOptions
import com.guesswhosesong.app.ui.components.GuessWhoseSongWordmark
import com.guesswhosesong.app.ui.components.avatarOption
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.AvatarCustomizationCatalog

private enum class CharacterEditorTab(val title: String) {
    SHAPE("Shape"),
    COLOR("Color"),
    EYES("Eyes"),
    MOUTH("Mouth"),
    ACCESSORIES("Accessories")
}

@Composable
internal fun CharacterEditorPage(
    customization: AvatarCustomization,
    statusMessage: String?,
    statusIsError: Boolean,
    onCustomizationChange: (AvatarCustomization) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(CharacterEditorTab.SHAPE) }
    var statusVisible by remember(statusMessage) { mutableStateOf(true) }
    val colorScheme = MaterialTheme.colorScheme
    val darkTheme = colorScheme.background.luminance() < 0.5f

    Column(
        modifier = Modifier.fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = if (darkTheme) 18.dp else 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GuessWhoseSongWordmark(
                modifier = Modifier.weight(1f),
                fontSize = 25.5.sp,
                lineHeight = 29.5.sp
            )
            androidx.compose.material3.TextButton(onClick = onCancel) {
                androidx.compose.material3.Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
                Text("Back", color = colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (statusVisible) statusMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                color = if (statusIsError) colorScheme.errorContainer else colorScheme.primaryContainer.copy(alpha = 0.62f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.14f))
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!statusIsError && !darkTheme) {
                        Surface(modifier = Modifier.size(34.dp), color = Color.White, shape = RoundedCornerShape(50.dp)) {
                            Box(contentAlignment = Alignment.Center) { CharacterEditorGoogleMark() }
                        }
                    } else {
                        Text(
                            if (statusIsError) "!" else "✓",
                            modifier = Modifier
                                .size(27.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (statusIsError) colorScheme.error else Color(0xFF62D960)),
                            color = Color(0xFF15221A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        message,
                        modifier = Modifier.weight(1f),
                        color = if (statusIsError) colorScheme.onErrorContainer else colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!statusIsError && !darkTheme) {
                        IconButton(
                            onClick = { statusVisible = false },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(colorScheme.primaryContainer.copy(alpha = 0.72f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss status", tint = colorScheme.onSurfaceVariant)
                        }
                    } else if (!statusIsError) {
                        CharacterStatusConfetti(Modifier.size(width = 34.dp, height = 30.dp))
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = if (darkTheme) colorScheme.surface.copy(alpha = 0.48f) else Color.Transparent,
            shape = RoundedCornerShape(32.dp),
            border = if (darkTheme) BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.20f)) else null
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
                    start = if (darkTheme) 14.dp else 10.dp,
                    top = if (darkTheme) 17.dp else 10.dp,
                    end = if (darkTheme) 14.dp else 10.dp,
                    bottom = if (darkTheme) 14.dp else 10.dp
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
                Text(
                    "Character editor",
                    modifier = Modifier.padding(horizontal = if (darkTheme) 0.dp else 7.dp),
                    style = MaterialTheme.typography.headlineLarge.let { headline ->
                        headline.copy(
                            fontSize = headline.fontSize * 1.15f,
                            lineHeight = headline.lineHeight * 1.15f
                        )
                    },
                    color = colorScheme.onBackground
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().height(if (darkTheme) 161.dp else 179.dp),
                    color = if (darkTheme) Color(0xFF222730) else Color(0xFFFFF8E7),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CharacterPreviewDecor()
                        AvatarCharacter(
                            avatarId = customization.shapeId,
                            modifier = Modifier
                                .size(if (darkTheme) 153.dp else 180.dp)
                                .offset(y = if (darkTheme) 0.dp else (-5.5).dp)
                                .graphicsLayer {
                                    if (darkTheme) {
                                        // The dark reference shows a taller character higher in
                                        // the preview, with a slightly wider face.
                                        scaleX = 1.09f
                                        scaleY = 1.14f
                                        translationY = -16.dp.toPx()
                                    } else {
                                        scaleX = 0.93f
                                    }
                                },
                            customization = customization,
                            brightLimbs = darkTheme,
                            darkArms = darkTheme
                        )
                    }
                }

                Spacer(Modifier.height(if (darkTheme) 14.dp else 15.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CharacterEditorTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        Column(
                            modifier = Modifier.weight(1f)
                                .graphicsLayer {
                                    if (darkTheme) translationX = -2.dp.toPx()
                                }
                                .selectable(
                                    selected = selected,
                                    role = Role.Tab,
                                    onClick = { selectedTab = tab }
                                )
                                .semantics { contentDescription = "${tab.title} options" },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (darkTheme) {
                                Surface(
                                    modifier = Modifier.size(62.dp),
                                    color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(15.dp),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CharacterEditorTabGlyph(tab, selected, Modifier.size(width = 34.dp, height = 30.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    tab.title,
                                    color = if (selected) Color.White else colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(77.dp),
                                    color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(17.dp),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CharacterEditorTabGlyph(tab, selected, Modifier.size(width = 34.dp, height = 30.dp))
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            tab.title,
                                            color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(if (darkTheme) 9.5.dp else 7.dp)) {
                    choicesFor(selectedTab).chunked(4).forEach { rowOptions ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowOptions.forEach { option ->
                                val selected = selectedValue(selectedTab, customization) == option
                                val updated = updatedCustomization(selectedTab, customization, option)
                                val preview = if (selectedTab == CharacterEditorTab.SHAPE && !selected) {
                                    AvatarCustomization.defaultsFor(option)
                                } else {
                                    updated
                                }
                                Surface(
                                    modifier = Modifier.weight(1f).aspectRatio(1f)
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = { onCustomizationChange(updated) }
                                        )
                                        .semantics { contentDescription = "${choiceLabel(selectedTab, option)} ${selectedTab.title.lowercase()}" },
                                    color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.68f),
                                    shape = RoundedCornerShape(21.dp),
                                    border = BorderStroke(
                                        if (selected) 2.5.dp else 1.dp,
                                        if (selected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        AvatarCharacter(
                                            avatarId = preview.shapeId,
                                            modifier = Modifier.fillMaxSize().padding(7.dp),
                                            customization = preview,
                                            brightLimbs = darkTheme
                                        )
                                    }
                                }
                            }
                            repeat(4 - rowOptions.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (darkTheme) 0.dp else 10.dp)
                .padding(
                    top = if (darkTheme) 9.dp else 12.dp,
                    bottom = if (darkTheme) 11.dp else 13.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(if (darkTheme) 10.dp else 8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(if (darkTheme) 54.dp else 57.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.5.dp, colorScheme.outlineVariant)
            ) { Text("Cancel", color = colorScheme.onSurface, fontWeight = FontWeight.Bold) }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1.2f).height(if (darkTheme) 54.dp else 57.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color(0xFF765CF5), Color(0xFF8A6CFF))
                        ),
                        RoundedCornerShape(50)
                    ),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) { Text("Save character", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CharacterPreviewDecor() {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Canvas(Modifier.fillMaxSize()) {
        val ground = if (dark) Color(0xFF303540) else Color(0xFFFFE9AD)
        drawOval(ground.copy(alpha = 0.42f), topLeft = Offset(size.width * 0.19f, size.height * 0.79f), size = Size(size.width * 0.62f, size.height * 0.17f))
        drawOval(ground.copy(alpha = 0.28f), topLeft = Offset(size.width * 0.30f, size.height * 0.81f), size = Size(size.width * 0.40f, size.height * 0.11f))
        if (!dark) {
            val cloud = Color(0xFFFFEFC8).copy(alpha = 0.78f)
            fun cloudAt(x: Float, y: Float, width: Float, height: Float) {
                drawRoundRect(
                    cloud,
                    topLeft = Offset(x, y + height * 0.36f),
                    size = Size(width, height * 0.48f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * 0.24f)
                )
                drawCircle(cloud, height * 0.35f, Offset(x + width * 0.24f, y + height * 0.43f))
                drawCircle(cloud, height * 0.48f, Offset(x + width * 0.5f, y + height * 0.33f))
                drawCircle(cloud, height * 0.34f, Offset(x + width * 0.75f, y + height * 0.43f))
            }
            cloudAt(size.width * 0.69f, size.height * 0.05f, size.width * 0.12f, size.height * 0.24f)
            cloudAt(-size.width * 0.025f, size.height * 0.70f, size.width * 0.18f, size.height * 0.3f)

            val yellow = Color(0xFFFFB928)
            val violet = Color(0xFF8065F5)
            drawCircle(yellow, size.width * 0.012f, Offset(size.width * 0.16f, size.height * 0.48f))
            drawLine(yellow, Offset(size.width * 0.18f, size.height * 0.47f), Offset(size.width * 0.18f, size.height * 0.27f), size.width * 0.013f, StrokeCap.Round)
            drawLine(yellow, Offset(size.width * 0.18f, size.height * 0.27f), Offset(size.width * 0.23f, size.height * 0.31f), size.width * 0.013f, StrokeCap.Round)
            drawCircle(yellow, size.width * 0.018f, Offset(size.width * 0.17f, size.height * 0.49f))
            drawCircle(violet, size.width * 0.012f, Offset(size.width * 0.83f, size.height * 0.25f))
            drawLine(violet, Offset(size.width * 0.84f, size.height * 0.24f), Offset(size.width * 0.84f, size.height * 0.08f), size.width * 0.013f, StrokeCap.Round)
            drawLine(violet, Offset(size.width * 0.84f, size.height * 0.08f), Offset(size.width * 0.89f, size.height * 0.12f), size.width * 0.013f, StrokeCap.Round)
            drawCircle(violet, size.width * 0.018f, Offset(size.width * 0.83f, size.height * 0.25f))
        } else {
            val yellow = Color(0xFFFFC62E)
            val violet = Color(0xFF8A6CFF)
            drawLine(yellow, Offset(size.width * 0.26f, size.height * 0.31f), Offset(size.width * 0.30f, size.height * 0.42f), size.width * 0.018f, StrokeCap.Round)
            drawLine(yellow, Offset(size.width * 0.25f, size.height * 0.46f), Offset(size.width * 0.29f, size.height * 0.47f), size.width * 0.018f, StrokeCap.Round)
            drawLine(violet, Offset(size.width * 0.74f, size.height * 0.30f), Offset(size.width * 0.79f, size.height * 0.20f), size.width * 0.018f, StrokeCap.Round)
            drawLine(violet, Offset(size.width * 0.78f, size.height * 0.43f), Offset(size.width * 0.83f, size.height * 0.40f), size.width * 0.018f, StrokeCap.Round)
        }
        val colors = listOf(Color(0xFFFFC62E), Color(0xFF8A6CFF))
        val points = listOf(
            Triple(0.29f, 0.28f, 0), Triple(0.25f, 0.43f, 0),
            Triple(0.72f, 0.26f, 1), Triple(0.77f, 0.40f, 1)
        )
        points.forEachIndexed { index, (x, y, colorIndex) ->
            val start = Offset(size.width * x, size.height * y)
            val length = if (index % 2 == 0) size.width * 0.025f else size.width * 0.017f
            drawLine(colors[colorIndex], start, Offset(start.x + length, start.y - length * 0.72f), size.width * 0.012f, StrokeCap.Round)
        }
    }
}

@Composable
private fun CharacterEditorTabGlyph(tab: CharacterEditorTab, selected: Boolean, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val dark = colorScheme.background.luminance() < 0.5f
    val ink = when {
        selected && dark -> Color.White
        selected -> colorScheme.primary
        else -> colorScheme.onSurface
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = size.minDimension * 0.075f
        when (tab) {
            CharacterEditorTab.SHAPE -> {
                val body = Path().apply {
                    moveTo(w * 0.34f, h * 0.22f)
                    lineTo(w * 0.66f, h * 0.22f)
                    lineTo(w * 0.76f, h * 0.34f)
                    lineTo(w * 0.76f, h * 0.63f)
                    lineTo(w * 0.65f, h * 0.73f)
                    lineTo(w * 0.35f, h * 0.73f)
                    lineTo(w * 0.24f, h * 0.63f)
                    lineTo(w * 0.24f, h * 0.34f)
                    close()
                }
                if (selected && !dark) {
                    val fill = if (dark) Color(0xFFB5A6FF) else colorScheme.primary
                    val faceInk = if (dark) Color(0xFF28233D) else Color.White
                    val edge = if (dark) Color(0xFFE5DEFF) else colorScheme.primary
                    val filledStroke = stroke * 1.35f
                    drawLine(edge, Offset(w * 0.24f, h * 0.45f), Offset(w * 0.10f, h * 0.37f), filledStroke, StrokeCap.Round)
                    drawLine(edge, Offset(w * 0.76f, h * 0.45f), Offset(w * 0.90f, h * 0.37f), filledStroke, StrokeCap.Round)
                    drawLine(edge, Offset(w * 0.42f, h * 0.73f), Offset(w * 0.38f, h * 0.93f), filledStroke, StrokeCap.Round)
                    drawLine(edge, Offset(w * 0.58f, h * 0.73f), Offset(w * 0.62f, h * 0.93f), filledStroke, StrokeCap.Round)
                    drawPath(body, fill)
                    drawPath(body, edge, style = Stroke(width = stroke * 0.75f, cap = StrokeCap.Round))
                    drawCircle(faceInk, stroke * 0.58f, Offset(w * 0.43f, h * 0.45f))
                    drawCircle(faceInk, stroke * 0.58f, Offset(w * 0.57f, h * 0.45f))
                    drawArc(
                        color = faceInk,
                        startAngle = 18f,
                        sweepAngle = 144f,
                        useCenter = false,
                        topLeft = Offset(w * 0.46f, h * 0.53f),
                        size = Size(w * 0.08f, h * 0.10f),
                        style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round)
                    )
                } else {
                    drawPath(body, ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    drawCircle(ink, stroke * 0.48f, Offset(w * 0.43f, h * 0.45f))
                    drawCircle(ink, stroke * 0.48f, Offset(w * 0.57f, h * 0.45f))
                    drawLine(ink, Offset(w * 0.24f, h * 0.45f), Offset(w * 0.10f, h * 0.37f), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(w * 0.76f, h * 0.45f), Offset(w * 0.90f, h * 0.37f), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(w * 0.42f, h * 0.73f), Offset(w * 0.38f, h * 0.93f), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(w * 0.58f, h * 0.73f), Offset(w * 0.62f, h * 0.93f), stroke, StrokeCap.Round)
                    if (selected) {
                        drawOval(ink, Offset(w * 0.465f, h * 0.53f), Size(w * 0.07f, h * 0.10f))
                    }
                }
            }
            CharacterEditorTab.COLOR -> {
                val radius = size.minDimension * 0.23f
                drawCircle(Color(0xFFFFB72F), radius, Offset(w * 0.37f, h * 0.42f))
                drawCircle(Color(0xFF53D78D), radius, Offset(w * 0.63f, h * 0.42f))
                drawCircle(Color(0xFF8B69FF), radius, Offset(w * 0.37f, h * 0.66f))
                drawCircle(Color(0xFF42C4E8), radius, Offset(w * 0.63f, h * 0.66f))
            }
            CharacterEditorTab.EYES -> {
                drawCircle(ink, size.minDimension * 0.10f, Offset(w * 0.34f, h * 0.5f))
                drawCircle(ink, size.minDimension * 0.10f, Offset(w * 0.66f, h * 0.5f))
            }
            CharacterEditorTab.MOUTH -> {
                drawArc(
                    color = ink,
                    startAngle = 18f,
                    sweepAngle = 144f,
                    useCenter = false,
                    topLeft = Offset(w * 0.24f, h * 0.15f),
                    size = Size(w * 0.52f, h * 0.68f),
                    style = Stroke(width = stroke * 1.5f, cap = StrokeCap.Round)
                )
            }
            CharacterEditorTab.ACCESSORIES -> {
                drawArc(
                    color = ink,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.24f, h * 0.02f),
                    size = Size(w * 0.52f, h * 0.88f),
                    style = Stroke(width = stroke * 1.7f, cap = StrokeCap.Round)
                )
                drawRoundRect(ink, Offset(w * 0.13f, h * 0.43f), Size(w * 0.17f, h * 0.34f), androidx.compose.ui.geometry.CornerRadius(stroke))
                drawRoundRect(ink, Offset(w * 0.70f, h * 0.43f), Size(w * 0.17f, h * 0.34f), androidx.compose.ui.geometry.CornerRadius(stroke))
            }
        }
    }
}

@Composable
private fun CharacterEditorGoogleMark() {
    Canvas(Modifier.size(24.dp)) {
        val strokeWidth = 4.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        drawArc(Color(0xFF4285F4), -43f, 86f, false, topLeft, arcSize, style = style)
        drawArc(Color(0xFF34A853), 47f, 86f, false, topLeft, arcSize, style = style)
        drawArc(Color(0xFFFBBC05), 137f, 86f, false, topLeft, arcSize, style = style)
        drawArc(Color(0xFFEA4335), 227f, 86f, false, topLeft, arcSize, style = style)
        drawLine(Color(0xFF4285F4), Offset(size.width / 2f, size.height / 2f), Offset(size.width - strokeWidth / 2f, size.height / 2f), strokeWidth, StrokeCap.Butt)
    }
}

@Composable
private fun CharacterStatusConfetti(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawLine(Color(0xFFFFC62E), Offset(size.width * 0.18f, size.height * 0.63f), Offset(size.width * 0.07f, size.height * 0.40f), size.width * 0.13f, StrokeCap.Round)
        drawLine(Color(0xFFFF8B3D), Offset(size.width * 0.58f, size.height * 0.78f), Offset(size.width * 0.48f, size.height * 0.60f), size.width * 0.13f, StrokeCap.Round)
    }
}

private fun choicesFor(tab: CharacterEditorTab): List<String> = when (tab) {
    CharacterEditorTab.SHAPE -> AvatarCustomizationCatalog.shapeIds
    CharacterEditorTab.COLOR -> AvatarCustomizationCatalog.colorIds
    CharacterEditorTab.EYES -> AvatarCustomizationCatalog.eyesIds
    CharacterEditorTab.MOUTH -> AvatarCustomizationCatalog.mouthIds
    CharacterEditorTab.ACCESSORIES -> AvatarCustomizationCatalog.accessoryIds
}

private fun selectedValue(tab: CharacterEditorTab, customization: AvatarCustomization): String = when (tab) {
    CharacterEditorTab.SHAPE -> customization.shapeId
    CharacterEditorTab.COLOR -> customization.colorId
    CharacterEditorTab.EYES -> customization.eyesId
    CharacterEditorTab.MOUTH -> customization.mouthId
    CharacterEditorTab.ACCESSORIES -> customization.accessoryId
}

private fun updatedCustomization(
    tab: CharacterEditorTab,
    customization: AvatarCustomization,
    value: String
): AvatarCustomization = when (tab) {
    CharacterEditorTab.SHAPE -> customization.copy(shapeId = value)
    CharacterEditorTab.COLOR -> customization.copy(colorId = value)
    CharacterEditorTab.EYES -> customization.copy(eyesId = value)
    CharacterEditorTab.MOUTH -> customization.copy(mouthId = value)
    CharacterEditorTab.ACCESSORIES -> customization.copy(accessoryId = value)
}

private fun choiceLabel(tab: CharacterEditorTab, value: String): String = when (tab) {
    CharacterEditorTab.SHAPE -> avatarOption(value).name
    CharacterEditorTab.COLOR -> avatarOption(value).colorName
    CharacterEditorTab.EYES, CharacterEditorTab.MOUTH, CharacterEditorTab.ACCESSORIES ->
        value.replace('-', ' ').replaceFirstChar { it.uppercase() }
}
