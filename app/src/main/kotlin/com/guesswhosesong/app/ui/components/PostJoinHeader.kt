package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PostJoinHeader(
    roomCode: String,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    trailingControlsOffsetY: Dp = 0.dp,
    roomOffsetY: Dp = 0.dp,
    roomHorizontalPadding: Dp = 11.dp,
    roomVerticalPadding: Dp = 6.dp
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        val compact = maxWidth < 390.dp
        val wordmarkSize = if (compact) 18.sp else 22.sp
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                GuessWhoseSongWordmark(
                    modifier = Modifier.weight(1f).widthIn(max = 320.dp).offset(y = if (compact) 0.dp else (-1).dp),
                    fontSize = wordmarkSize,
                    lineHeight = wordmarkSize * 1.12f
                )
                if (!compact) RoomCodeBadge(roomCode, roomHorizontalPadding, roomVerticalPadding, roomOffsetY)
                AppearanceToggleButton(modifier = Modifier.offset(y = trailingControlsOffsetY))
                SettingsHeaderButton(onClick = onSettings, modifier = Modifier.offset(y = trailingControlsOffsetY))
            }
            if (compact) {
                Box(Modifier.fillMaxWidth()) {
                    RoomCodeBadge(
                        roomCode,
                        roomHorizontalPadding,
                        roomVerticalPadding,
                        roomOffsetY,
                        Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomCodeBadge(
    roomCode: String,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    offsetY: Dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.offset(y = offsetY),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(17.dp)
    ) {
        Column(Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)) {
            Text(
                "ROOM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                letterSpacing = 1.sp,
                lineHeight = 13.sp
            )
            Text(
                roomCode.ifBlank { "------" }.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp,
                lineHeight = 21.sp
            )
        }
    }
}
