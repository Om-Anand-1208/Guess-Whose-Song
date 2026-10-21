package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guesswhosesong.shared.models.AvatarCatalog
import com.guesswhosesong.shared.models.AvatarCustomization

/** Colored avatar surface used for player identity across the game. */
@Composable
fun EmptyAvatarBadge(
    avatarId: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    selected: Boolean = false,
    customization: AvatarCustomization? = null
) {
    val appearance = AvatarCustomization.normalize(customization, AvatarCatalog.normalize(avatarId))
    val option = avatarOption(appearance.colorId)
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size / 2.7f),
        color = option.color.copy(alpha = 0.24f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) option.accent else option.color.copy(alpha = 0.72f)
        ),
        tonalElevation = 0.dp
    ) {
        AvatarCharacter(
            avatarId = appearance.shapeId,
            modifier = Modifier.padding(size / 10f),
            customization = appearance
        )
    }
}

/** Initial and saved random color used for editable player identity. */
@Composable
fun PlayerInitialBadge(
    displayName: String,
    customization: AvatarCustomization?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val appearance = AvatarCustomization.normalize(customization, AvatarCatalog.DEFAULT_ID)
    val color = avatarOption(appearance.colorId).color
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = color,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.14f)),
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                displayName.trim().firstOrNull()?.uppercase() ?: "?",
                color = Color(0xFF17161A),
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}
