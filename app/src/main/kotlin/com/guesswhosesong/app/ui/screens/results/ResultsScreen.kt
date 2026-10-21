package com.guesswhosesong.app.ui.screens.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.ui.components.PlayerInitialBadge
import com.guesswhosesong.app.ui.theme.PostJoinPalette
import com.guesswhosesong.shared.models.Player

@Composable
fun ResultsScreen(
    viewModel: ResultsViewModel = hiltViewModel(),
    onNavigateToLobby: (LobbyDestination) -> Unit,
    onNavigateToSubmission: () -> Unit,
    onNavigateToJoin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.lobbyDestination) {
        uiState.lobbyDestination?.let(onNavigateToLobby)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ResultsEvent.NavigateToSubmission -> onNavigateToSubmission()
                ResultsEvent.NavigateToJoin -> onNavigateToJoin()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Final scores",
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.players.isEmpty()) {
                    item {
                        Text(
                            "Final standings will appear here.",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            color = PostJoinPalette.Muted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                itemsIndexed(uiState.players, key = { _, player -> player.id }) { _, player ->
                    val rank = uiState.players.indexOfFirst { it.score == player.score } + 1
                    ScoreboardItem(rank = rank, player = player, isSelf = player.id == uiState.selfPlayerId)
                }
            }
            Spacer(Modifier.height(16.dp))
            ResultsActions(
                isHost = uiState.isHost,
                onPlayAgain = viewModel::playAgain,
                onEndGame = viewModel::endRoom
            )
        }
    }
}

@Composable
fun ResultsActions(isHost: Boolean, onPlayAgain: () -> Unit, onEndGame: () -> Unit) {
    if (isHost) {
        Button(
            onClick = onPlayAgain,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Play Again", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onEndGame, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("End Game")
        }
    } else {
        Text(
            "Waiting for host to continue…",
            color = PostJoinPalette.Muted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ScoreboardItem(rank: Int, player: Player, isSelf: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelf) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                ordinalRank(rank),
                modifier = Modifier.width(42.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            PlayerInitialBadge(player.displayName, player.avatarCustomization, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(player.displayName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isSelf) {
                        Spacer(Modifier.width(5.dp))
                        Text("(you)", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${player.score}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("pts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

private fun ordinalRank(rank: Int): String {
    val suffix = if (rank % 100 in 11..13) "th" else when (rank % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$rank$suffix"
}
