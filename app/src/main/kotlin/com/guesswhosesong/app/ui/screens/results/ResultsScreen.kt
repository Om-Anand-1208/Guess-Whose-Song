package com.guesswhosesong.app.ui.screens.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.ui.components.EmptyAvatarBadge
import com.guesswhosesong.app.ui.components.GuessWhoseSongWordmark
import com.guesswhosesong.app.ui.theme.GwsPalette
import com.guesswhosesong.shared.models.Player

@Composable
fun ResultsScreen(
    viewModel: ResultsViewModel = hiltViewModel(),
    onNavigateToSubmission: () -> Unit,
    onNavigateToJoin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ResultsEvent.NavigateToSubmission -> onNavigateToSubmission()
                ResultsEvent.NavigateToJoin -> onNavigateToJoin()
            }
        }
    }

    val winner = uiState.players.firstOrNull()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GwsPalette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GuessWhoseSongWordmark(Modifier.fillMaxWidth().padding(bottom = 8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GwsPalette.Lavender.copy(alpha = 0.36f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, GwsPalette.LavenderDeep.copy(alpha = 0.16f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 17.dp, vertical = 15.dp)) {
                Text(
                    "ROUND COMPLETE",
                    color = GwsPalette.LavenderDeep,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    "That’s a wrap!",
                    modifier = Modifier.padding(top = 3.dp),
                    color = GwsPalette.Ink,
                    fontSize = 27.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.7).sp
                )
                Text(
                    "The room’s music detectives ranked up.",
                    color = GwsPalette.Ink.copy(alpha = 0.66f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (winner != null) {
            WinnerCard(winner = winner)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 17.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text("Final scores", color = GwsPalette.Ink, style = MaterialTheme.typography.titleLarge)
                Text(
                    "A little music, a lot of bragging rights",
                    color = GwsPalette.Ink.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Surface(color = GwsPalette.Lime.copy(alpha = 0.38f), shape = CircleShape) {
                Text(
                    "${uiState.players.size} PLAYERS",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    color = GwsPalette.Ink,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.players.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(17.dp),
                        border = BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.08f))
                    ) {
                        Text(
                            "Final standings will appear here.",
                            modifier = Modifier.padding(16.dp),
                            color = GwsPalette.Ink.copy(alpha = 0.64f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            itemsIndexed(uiState.players, key = { _, player -> player.id }) { index, player ->
                ScoreboardItem(rank = index + 1, player = player, isSelf = player.id == uiState.selfPlayerId)
            }
        }

        Spacer(Modifier.height(11.dp))
        if (uiState.isHost) {
            Button(
                onClick = viewModel::playAgain,
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GwsPalette.Tangerine,
                    contentColor = GwsPalette.Ink
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Play another round", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::endRoom,
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GwsPalette.Lavender.copy(alpha = 0.5f),
                    contentColor = GwsPalette.Ink
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("End game", fontWeight = FontWeight.ExtraBold)
            }
        } else {
            Surface(
                color = GwsPalette.Lavender.copy(alpha = 0.34f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("That was a good round", color = GwsPalette.Ink, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Waiting for the host to continue…",
                        color = GwsPalette.Ink.copy(alpha = 0.64f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun WinnerCard(winner: Player) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 11.dp),
        color = GwsPalette.Butter.copy(alpha = 0.78f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFFE5B84C).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmptyAvatarBadge(winner.avatarId, size = 56.dp, selected = true)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(color = Color.White.copy(alpha = 0.68f), shape = CircleShape) {
                    Text(
                        "ROOM CHAMPION",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = GwsPalette.Ink.copy(alpha = 0.72f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    winner.displayName,
                    modifier = Modifier.padding(top = 4.dp),
                    color = GwsPalette.Ink,
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Top listener", color = GwsPalette.Ink.copy(alpha = 0.64f), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${winner.score}",
                    color = GwsPalette.Ink,
                    fontSize = 23.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "POINTS",
                    color = GwsPalette.Ink.copy(alpha = 0.62f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

@Composable
fun ScoreboardItem(rank: Int, player: Player, isSelf: Boolean) {
    val baseColor = when (rank) {
        1 -> GwsPalette.Butter.copy(alpha = 0.66f)
        2 -> GwsPalette.Lavender.copy(alpha = 0.29f)
        3 -> GwsPalette.Lime.copy(alpha = 0.31f)
        else -> Color.White.copy(alpha = 0.84f)
    }
    val rankColor = when (rank) {
        1 -> GwsPalette.Tangerine
        2 -> GwsPalette.Lavender
        3 -> GwsPalette.Lime
        else -> GwsPalette.Paper
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelf) GwsPalette.Lavender.copy(alpha = 0.4f) else baseColor,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(
            if (isSelf) 1.5.dp else 1.dp,
            if (isSelf) GwsPalette.LavenderDeep.copy(alpha = 0.54f) else GwsPalette.Ink.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = rankColor.copy(alpha = 0.78f), shape = CircleShape) {
                Text(
                    "$rank",
                    modifier = Modifier.size(31.dp).padding(top = 6.dp),
                    color = GwsPalette.Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.width(10.dp))
            EmptyAvatarBadge(player.avatarId, size = 42.dp, selected = isSelf)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        player.displayName,
                        color = GwsPalette.Ink,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isSelf) {
                        Spacer(Modifier.width(5.dp))
                        Text("YOU", color = GwsPalette.LavenderDeep, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(
                    when (rank) {
                        1 -> "Room champion"
                        2 -> "Runner-up"
                        else -> "Great ears"
                    },
                    color = GwsPalette.Ink.copy(alpha = 0.58f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${player.score}",
                    color = GwsPalette.Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
                Text(
                    "PTS",
                    color = GwsPalette.Ink.copy(alpha = 0.55f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
