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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.guesswhosesong.app.ui.theme.GwsPalette
import com.guesswhosesong.shared.models.Player

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = GwsPalette.Paper,
        topBar = {
            TopAppBar(
                title = { Text("Final Scores 🏆") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GwsPalette.Paper)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.players.isEmpty()) {
                    item {
                        Text(
                            "Final standings will appear here.",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            color = GwsPalette.Ink.copy(alpha = 0.66f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                itemsIndexed(uiState.players, key = { _, player -> player.id }) { index, player ->
                    ScoreboardItem(rank = index + 1, player = player, isSelf = player.id == uiState.selfPlayerId)
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
            colors = ButtonDefaults.buttonColors(containerColor = GwsPalette.Tangerine, contentColor = GwsPalette.Ink),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Play Again 🎵", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onEndGame, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("End Game")
        }
    } else {
        Text(
            "Waiting for host to continue…",
            color = GwsPalette.Ink.copy(alpha = 0.65f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ScoreboardItem(rank: Int, player: Player, isSelf: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelf) GwsPalette.Lavender.copy(alpha = 0.3f) else Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (rank <= 3) listOf("🥇", "🥈", "🥉")[rank - 1] else "#$rank",
                modifier = Modifier.width(40.dp),
                fontSize = 20.sp
            )
            EmptyAvatarBadge(player.avatarId, size = 38.dp, selected = isSelf, customization = player.avatarCustomization)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(player.displayName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isSelf) {
                        Spacer(Modifier.width(5.dp))
                        Text("(you)", color = GwsPalette.LavenderDeep, fontSize = 10.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${player.score}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("pts", color = GwsPalette.Ink.copy(alpha = 0.6f), fontSize = 10.sp)
            }
        }
    }
}
