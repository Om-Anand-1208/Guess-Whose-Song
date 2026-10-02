package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.guesswhosesong.shared.models.AvatarCatalog
import com.guesswhosesong.shared.models.AvatarCustomization

/** Renders a post-join mascot, with an optional raised-arm hero pose. */
@Composable
fun PostJoinHeroCharacter(
    avatarId: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    customization: AvatarCustomization? = null,
    heroPose: Boolean = true,
    tiltBodyOnly: Boolean = false,
    richFinish: Boolean = false,
    brightLimbs: Boolean = false
) {
    val appearance = AvatarCustomization.normalize(customization, AvatarCatalog.normalize(avatarId))

    val rotation = if (heroPose) when (appearance.shapeId) {
        "lime" -> -4f
        "sunny" -> 2f
        "violet" -> -1f
        "tangerine" -> 1.5f
        "berry" -> -2f
        else -> 0f
    } else 0f
    Box(modifier = modifier) {
        AvatarCharacter(
            avatarId = avatarId,
            modifier = Modifier.fillMaxSize().graphicsLayer(rotationZ = if (tiltBodyOnly) 0f else rotation),
            contentDescription = contentDescription,
            customization = appearance,
            heroPose = heroPose,
            brightLimbs = brightLimbs,
            bodyRotation = if (tiltBodyOnly) rotation else 0f,
            richFinish = richFinish
        )
    }
}

/** Draws the small eighth-note glyphs shared by the post-join hero scenes. */
@Composable
fun PostJoinMusicNote(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color,
    double: Boolean = false
) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stemWidth = (w * 0.075f).coerceAtLeast(1.4f)
        val headW = w * 0.30f
        val headH = h * 0.20f
        val ink = color

        fun head(centerX: Float, centerY: Float) {
            val headRect = androidx.compose.ui.geometry.Rect(
                centerX - headW / 2f,
                centerY - headH / 2f,
                centerX + headW / 2f,
                centerY + headH / 2f
            )
            drawContext.transform.rotate(-24f, headRect.center)
            drawOval(ink, topLeft = headRect.topLeft, size = headRect.size)
            drawContext.transform.rotate(24f, headRect.center)
        }

        if (double) {
            val leftX = w * 0.26f
            val rightX = w * 0.67f
            val leftY = h * 0.78f
            val rightY = h * 0.68f
            head(leftX, leftY)
            head(rightX, rightY)
            drawLine(ink, androidx.compose.ui.geometry.Offset(leftX + headW * 0.42f, leftY - headH * 0.24f), androidx.compose.ui.geometry.Offset(leftX + headW * 0.42f, h * 0.20f), stemWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(ink, androidx.compose.ui.geometry.Offset(rightX + headW * 0.42f, rightY - headH * 0.24f), androidx.compose.ui.geometry.Offset(rightX + headW * 0.42f, h * 0.12f), stemWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            val beam = androidx.compose.ui.graphics.Path().apply {
                moveTo(leftX + headW * 0.42f, h * 0.20f)
                cubicTo(w * 0.49f, h * 0.10f, w * 0.58f, h * 0.08f, rightX + headW * 0.42f, h * 0.12f)
                lineTo(rightX + headW * 0.42f, h * 0.24f)
                cubicTo(w * 0.57f, h * 0.21f, w * 0.48f, h * 0.23f, leftX + headW * 0.42f, h * 0.32f)
                close()
            }
            drawPath(beam, ink)
        } else {
            val centerX = w * 0.37f
            val centerY = h * 0.78f
            head(centerX, centerY)
            val stemX = centerX + headW * 0.42f
            drawLine(ink, androidx.compose.ui.geometry.Offset(stemX, centerY - headH * 0.22f), androidx.compose.ui.geometry.Offset(stemX, h * 0.12f), stemWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            val flag = androidx.compose.ui.graphics.Path().apply {
                moveTo(stemX, h * 0.12f)
                cubicTo(w * 0.83f, h * 0.15f, w * 0.82f, h * 0.27f, w * 0.62f, h * 0.36f)
                cubicTo(w * 0.75f, h * 0.25f, w * 0.73f, h * 0.22f, stemX, h * 0.23f)
                close()
            }
            drawPath(flag, ink)
        }
    }
}

/** Irregular ground pools under the mascots in the post-join hero scenes. */
@Composable
fun PostJoinHeroFloorShadow(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color,
    alternate: Boolean = false
) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            if (!alternate) {
                moveTo(w * 0.03f, h * 0.58f)
                cubicTo(w * 0.04f, h * 0.34f, w * 0.19f, h * 0.20f, w * 0.35f, h * 0.34f)
                cubicTo(w * 0.50f, h * 0.21f, w * 0.65f, h * 0.27f, w * 0.72f, h * 0.43f)
                cubicTo(w * 0.87f, h * 0.28f, w * 0.99f, h * 0.41f, w * 0.97f, h * 0.60f)
                cubicTo(w * 0.94f, h * 0.84f, w * 0.76f, h * 0.91f, w * 0.61f, h * 0.78f)
                cubicTo(w * 0.46f, h * 0.92f, w * 0.31f, h * 0.84f, w * 0.22f, h * 0.76f)
                cubicTo(w * 0.09f, h * 0.83f, w * 0.00f, h * 0.73f, w * 0.03f, h * 0.58f)
            } else {
                moveTo(w * 0.02f, h * 0.60f)
                cubicTo(w * 0.06f, h * 0.40f, w * 0.18f, h * 0.32f, w * 0.34f, h * 0.43f)
                cubicTo(w * 0.47f, h * 0.28f, w * 0.63f, h * 0.34f, w * 0.69f, h * 0.49f)
                cubicTo(w * 0.84f, h * 0.36f, w * 0.98f, h * 0.46f, w * 0.98f, h * 0.63f)
                cubicTo(w * 0.96f, h * 0.83f, w * 0.78f, h * 0.88f, w * 0.62f, h * 0.79f)
                cubicTo(w * 0.47f, h * 0.89f, w * 0.28f, h * 0.82f, w * 0.19f, h * 0.73f)
                cubicTo(w * 0.08f, h * 0.81f, w * 0.00f, h * 0.72f, w * 0.02f, h * 0.60f)
            }
            close()
        }
        drawPath(path, color)
    }
}
