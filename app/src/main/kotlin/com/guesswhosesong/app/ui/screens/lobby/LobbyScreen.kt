package com.guesswhosesong.app.ui.screens.lobby

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.RoomSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    joinCode: String,
    selfPlayerId: String,
    displayName: String,
    viewModel: LobbyViewModel = hiltViewModel(),
    onNavigateToSubmission: () -> Unit,
    onKicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(joinCode) {
        viewModel.connect(joinCode, displayName)
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
    val selfId = uiState.selfPlayerId.ifBlank { selfPlayerId }
    val isHost = room?.players?.find { it.id == selfId }?.isHost ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Room: $joinCode") },
                actions = {
                    if (isHost) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!uiState.isConnected) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = "Players (${room?.players?.size ?: 0}/${room?.settings?.playerLimit ?: 10})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(room?.players ?: emptyList()) { player ->
                    PlayerListItem(
                        player = player,
                        isSelf = player.id == selfId,
                        isHost = isHost,
                        onKick = if (isHost && player.id != selfId) {
                            { viewModel.kickPlayer(player.id) }
                        } else null
                    )
                }
            }

            if (isHost) {
                Button(
                    onClick = viewModel::startGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = 8.dp),
                    enabled = (room?.players?.size ?: 0) >= 2
                ) {
                    Text("Start Game", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text(
                    text = "Waiting for host to start...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (showSettings && room != null) {
        HostSettingsSheet(
            currentSettings = room.settings,
            players = room.players,
            selfPlayerId = selfId,
            onSettingsUpdated = { settings ->
                viewModel.updateSettings(settings)
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
fun PlayerListItem(
    player: Player,
    isSelf: Boolean,
    isHost: Boolean,
    onKick: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.displayName,
                        fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelf) Text(
                        " (you)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (player.isHost) Text(" \uD83D\uDC51", style = MaterialTheme.typography.bodySmall)
                    if (!player.connected) Text(
                        " \u26A1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (onKick != null) {
                TextButton(onClick = onKick) {
                    Text("Kick", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostSettingsSheet(
    currentSettings: RoomSettings,
    players: List<Player>,
    selfPlayerId: String,
    onSettingsUpdated: (RoomSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Game Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text("Round length", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.guesswhosesong.shared.models.RoundLengthPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = settings.roundLengthPreset == preset,
                        onClick = { settings = settings.copy(roundLengthPreset = preset) },
                        label = { Text(preset.name.lowercase().replaceFirstChar { it.uppercaseChar() }) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Voting time", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 15, 20, 30).forEach { secs ->
                    FilterChip(
                        selected = settings.votingTimerSeconds == secs,
                        onClick = { settings = settings.copy(votingTimerSeconds = secs) },
                        label = { Text("${secs}s") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Player limit: ${settings.playerLimit}", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = settings.playerLimit.toFloat(),
                onValueChange = { settings = settings.copy(playerLimit = it.toInt()) },
                valueRange = 2f..20f,
                steps = 17
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onSettingsUpdated(settings) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Settings") }

            Spacer(Modifier.height(16.dp))
        }
    }
}
