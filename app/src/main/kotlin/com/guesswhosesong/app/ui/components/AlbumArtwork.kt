package com.guesswhosesong.app.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

@Composable
fun AlbumArtwork(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Album cover",
    contentScale: ContentScale = ContentScale.Crop
) {
    var failed by remember(url) { mutableStateOf(url.isBlank()) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF34295F), Color(0xFF6F58D9), Color(0xFFFFB13B))), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!failed) {
            AsyncImage(
                model = url.trim(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> failed = false
                        is AsyncImagePainter.State.Error -> {
                            failed = true
                            Log.w("AlbumArtwork", "Album cover request failed for ${url.take(160)}", state.result.throwable)
                        }
                        else -> Unit
                    }
                }
            )
        }
        if (failed) Text("♫", color = Color.White.copy(alpha = 0.92f), fontSize = 32.sp, fontWeight = FontWeight.Black)
    }
}
