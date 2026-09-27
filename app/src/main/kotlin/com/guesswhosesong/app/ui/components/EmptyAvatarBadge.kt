package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Neutral reserved avatar surface for screens awaiting the next character-art direction. */
@Composable
fun EmptyAvatarBadge(
    avatarId: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    selected: Boolean = false
) {
    val option = avatarOption(avatarId)
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size / 2.7f),
        color = option.color.copy(alpha = 0.24f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) option.accent else option.color.copy(alpha = 0.72f)
        ),
        tonalElevation = 0.dp
    ) { }
}
