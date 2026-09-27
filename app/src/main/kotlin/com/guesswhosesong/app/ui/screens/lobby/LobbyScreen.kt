package com.guesswhosesong.app.ui.screens.lobby

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
import com.guesswhosesong.shared.models.RoomSettings

@Composable
fun LobbyScreen(
    joinCode: String,
    displayName: String,
    avatarId: String,
    viewModel: LobbyViewModel = hiltViewModel(),
    onNavigateToSubmission: () -> Unit,
    onKicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(joinCode) {
        viewModel.connect(joinCode, displayName, avatarId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LobbyEvent.NavigateToSubmission -> onNavigateToSubmission()
                LobbyEvent.NavigateToGame -> onNavigateToSubmission()
                LobbyEvent.Kicked -> onKicked()
            }
        }
    }

    val room = uiState.room
    val selfId = uiState.selfPlayerId
    val self = room?.players?.find { it.id == selfId }
    val isHost = self?.isHost == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("THE HANGOUT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary)
                Text("Room ${room?.joinCode ?: joinCode}", style = MaterialTheme.typography.headlineSmall)
            }
            Surface(
                color = GwsPalette.Butter,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "${room?.players?.size ?: 0}/${room?.settings?.playerLimit ?: 10} players",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            if (isHost) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Game settings")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GwsPalette.Lavender.copy(alpha = 0.42f),
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarBadge(self?.avatarId ?: avatarId, size = 54.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("You’re in!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${self?.displayName ?: displayName} · choose your song when the host starts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("♫", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (!uiState.isConnected) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        uiState.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Players", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                Text("make some noise", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(room?.players ?: emptyList(), key = { it.id }) { player ->
                PlayerListItem(
                    player = player,
                    isSelf = player.id == selfId,
                    isHost = isHost,
                    onKick = if (isHost && player.id != selfId) ({ viewModel.kickPlayer(player.id) }) else null
                )
            }
        }

        SpotifyLobbyCard(
            connected = uiState.isSpotifyConnected,
            onConnect = viewModel::connectSpotify
        )
        Spacer(Modifier.height(8.dp))

        if (isHost) {
            Button(
                onClick = viewModel::startGame,
                enabled = (room.players.count { it.connected }) >= 2,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Start the mystery  →", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Waiting for ${room?.players?.firstOrNull { it.isHost }?.displayName ?: "the host"} to start…", modifier = Modifier.padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showSettings && room != null) {
        HostSettingsSheet(
            currentSettings = room.settings,
            onSettingsUpdated = { settings ->
                viewModel.updateSettings(settings)
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun SpotifyLobbyCard(connected: Boolean, onConnect: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (connected) Color(0xFF1DB954).copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (connected) Color(0xFF1DB954) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("♫", fontSize = 22.sp, color = Color(0xFF159447))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (connected) "Spotify connected" else "Connect Spotify", fontWeight = FontWeight.Bold, color = if (connected) Color(0xFF159447) else MaterialTheme.colorScheme.onSurface)
                Text(if (connected) "Top tracks ready" else "Optional · make song picks faster", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!connected) TextButton(onClick = onConnect) { Text("Connect") }
            else Text("Ready", color = Color(0xFF159447), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PlayerListItem(
    player: Player,
    isSelf: Boolean,
    isHost: Boolean,
    onKick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelf) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = if (isSelf) 2.dp else 0.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarBadge(player.avatarId, size = 48.dp)
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(player.displayName, fontWeight = if (isSelf) FontWeight.ExtraBold else FontWeight.Bold)
                    if (isSelf) Text("  you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (player.isHost) Text("  ✦ host", style = MaterialTheme.typography.labelSmall, color = GwsPalette.Tangerine, fontWeight = FontWeight.Bold)
                }
                Text(
                    if (player.connected) "in the room" else "reconnecting…",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (player.connected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
            if (onKick != null) TextButton(onClick = onKick) { Text("Kick", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HostSettingsSheet(
    currentSettings: RoomSettings,
    onSettingsUpdated: (RoomSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text("Tune the room", style = MaterialTheme.typography.headlineSmall)
            Text("Make the round feel like your group.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

            Text("Round length", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                com.guesswhosesong.shared.models.RoundLengthPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = settings.roundLengthPreset == preset,
                        onClick = { settings = settings.copy(roundLengthPreset = preset) },
                        label = { Text(preset.name.lowercase().replaceFirstChar { it.uppercaseChar() }) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Voting time", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf(10, 15, 20, 30).forEach { secs ->
                    FilterChip(
                        selected = settings.votingTimerSeconds == secs,
                        onClick = { settings = settings.copy(votingTimerSeconds = secs) },
                        label = { Text("${secs}s") }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Player limit: ${settings.playerLimit}", style = MaterialTheme.typography.labelLarge)
            Slider(value = settings.playerLimit.toFloat(), onValueChange = { settings = settings.copy(playerLimit = it.toInt()) }, valueRange = 2f..20f, steps = 17)

            Spacer(Modifier.height(16.dp))
            Button(onClick = { onSettingsUpdated(settings) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Save settings") }
            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
    }
}
