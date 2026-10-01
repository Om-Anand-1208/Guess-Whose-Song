package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guesswhosesong.app.R

@Composable
fun PostJoinHeader(
    roomCode: String,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BrandWordmark(Modifier.weight(1f))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(17.dp)
        ) {
            Column(Modifier.padding(horizontal = 11.dp, vertical = 6.dp)) {
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
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
            IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Appearance and settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BrandWordmark(modifier: Modifier = Modifier) {
    val family = FontFamily(Font(R.font.comfortaa_bold))
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ConfettiStrokes(Modifier.size(width = 18.dp, height = 38.dp))
        Spacer(Modifier.width(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy((-3).dp)) {
            Text(
                "Guess",
                fontFamily = family,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.8).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Whose Song",
                fontFamily = family,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.8).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.width(4.dp))
        ConfettiStrokes(Modifier.size(width = 18.dp, height = 38.dp), mirrored = true)
    }
}

@Composable
private fun ConfettiStrokes(modifier: Modifier = Modifier, mirrored: Boolean = false) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = if (mirrored) Alignment.Start else Alignment.End
    ) {
        Box(Modifier.size(5.dp, 9.dp).rotate(if (mirrored) 34f else -34f).background(Color(0xFFFF737D), CircleShape))
        Box(Modifier.size(7.dp, 18.dp).rotate(if (mirrored) -32f else 32f).background(Color(0xFFFFC941), CircleShape))
        Box(Modifier.size(5.dp, 9.dp).rotate(if (mirrored) -34f else 34f).background(Color(0xFF93E96B), CircleShape))
    }
}
