package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.guesswhosesong.app.ui.theme.GwsPalette

/** Small text-only brand marker used on the post-join game screens. */
@Composable
fun GuessWhoseSongWordmark(modifier: Modifier = Modifier) {
    Row(modifier = modifier.height(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(GwsPalette.LavenderDeep, GwsPalette.Tangerine)))
        )
        Spacer(Modifier.width(7.dp))
        Text(
            "GUESS WHOSE SONG",
            color = GwsPalette.Ink,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}
