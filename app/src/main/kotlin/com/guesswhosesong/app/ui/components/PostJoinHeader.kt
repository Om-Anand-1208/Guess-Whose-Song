package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PostJoinHeader(
    roomCode: String,
    leadingTitle: String? = null,
    leadingTitleFontSize: TextUnit = 38.5.sp,
    leadingTitleLineHeight: TextUnit = 45.sp,
    modifier: Modifier = Modifier,
    roomOffsetY: Dp = 0.dp,
    roomHorizontalPadding: Dp = 11.dp,
    roomVerticalPadding: Dp = 6.dp
) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (leadingTitle.isNullOrBlank()) {
            RoomCodeBadge(
                roomCode,
                roomHorizontalPadding,
                roomVerticalPadding,
                roomOffsetY,
                Modifier.align(Alignment.CenterEnd)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    leadingTitle,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = leadingTitleFontSize,
                    lineHeight = leadingTitleLineHeight,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.35).sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                RoomCodeBadge(roomCode, roomHorizontalPadding, roomVerticalPadding, roomOffsetY)
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
