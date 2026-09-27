package com.guesswhosesong.app.ui.screens.results

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.ui.components.AvatarBadge
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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GwsPalette.Lavender.copy(alpha = 0.52f),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✦", fontSize = 34.sp, color = GwsPalette.Tangerine)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("That’s a wrap!", style = MaterialTheme.typography.headlineSmall)
                    Text("The room’s music detectives ranked up.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("🏆", fontSize = 34.sp)
            }
        }

        if (winner != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = GwsPalette.Butter,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AvatarBadge(winner.avatarId, size = 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Top listener", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(winner.displayName, style = MaterialTheme.typography.titleLarge)
                    }
                    Text("${winner.score}\npts", textAlign = androidx.compose.ui.text.style.TextAlign.End, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Final scores", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text("best guessers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(uiState.players, key = { _, player -> player.id }) { index, player ->
                ScoreboardItem(rank = index + 1, player = player, isSelf = player.id == uiState.selfPlayerId)
            }
        }

        Spacer(Modifier.height(14.dp))
        if (uiState.isHost) {
            Button(
                onClick = viewModel::playAgain,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("Play another round  →", fontSize = 16.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = viewModel::endRoom, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("End game") }
        } else {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Waiting for the host to continue…", modifier = Modifier.padding(15.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ScoreboardItem(rank: Int, player: Player, isSelf: Boolean) {
    val accent = when (rank) {
        1 -> GwsPalette.Butter
        2 -> GwsPalette.Lavender.copy(alpha = 0.46f)
        3 -> GwsPalette.Mint.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelf) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else accent,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = if (isSelf) 2.dp else 0.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (rank <= 3) listOf("🥇", "🥈", "🥉")[rank - 1] else "#$rank", fontSize = 22.sp, modifier = Modifier.width(42.dp))
            AvatarBadge(player.avatarId, size = 46.dp)
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(player.displayName + if (isSelf) "  you" else "", fontWeight = FontWeight.ExtraBold)
                Text(if (rank == 1) "room champion" else "great ears", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${player.score} pts", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
}
