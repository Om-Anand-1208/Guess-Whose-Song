package com.guesswhosesong.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.RoomSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import kotlinx.browser.document

private val WebColorScheme = darkColorScheme(
    primary = Color(0xFFB9A1FF),
    onPrimary = Color(0xFF24113F),
    secondary = Color(0xFFB8C7FF),
    onSecondary = Color(0xFF17213D),
    tertiary = Color(0xFF86EFAC),
    onTertiary = Color(0xFF063B20),
    background = Color(0xFF10131A),
    onBackground = Color(0xFFF4F5F7),
    surface = Color(0xFF1B202B),
    onSurface = Color(0xFFF4F5F7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF9AA3B2)
)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    (document.getElementById("loader-container") as? HTMLElement)?.remove()
    ComposeViewport(viewportContainerId = "ComposeTarget") { WebApp() }
}

@Composable
private fun WebApp() {
    val store = remember { WebGameStore() }
    val state by store.state.collectAsState()
    val authStatus by store.auth.status.collectAsState()
    val user by store.auth.user.collectAsState()
    DisposableEffect(Unit) { onDispose { store.close() } }

    MaterialTheme(colorScheme = WebColorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (state.page) {
                WebPage.JOIN -> JoinPage(store, state, authStatus, user)
                WebPage.LOBBY -> LobbyPage(store, state)
                WebPage.SUBMISSION -> SubmissionPage(store, state)
                WebPage.GAME -> GamePage(store, state)
                WebPage.RESULTS -> ResultsPage(store, state)
            }
        }
    }
}

@Composable
private fun PageFrame(title: String, state: WebUiState, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        state.notice?.let { Text(it, color = Color(0xFF86EFAC)) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        content()
    }
}

@Composable
private fun AuthActions(store: WebGameStore, status: AuthStatus, user: WebUser?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (status) {
                AuthStatus.LOADING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Signing you in as a guest…")
                }
                AuthStatus.ERROR -> Text("Guest authentication is unavailable. Check Firebase web configuration.")
                AuthStatus.READY -> Text(if (user?.isAnonymous == true) "Playing as a guest" else "Google account linked")
            }
            if (status == AuthStatus.READY && user?.isAnonymous == true) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = store::linkGoogle) { Text("Link Google") }
                    TextButton(onClick = store::recoverGoogle) { Text("Recover with Google") }
                }
            }
        }
    }
}

@Composable
private fun JoinPage(store: WebGameStore, state: WebUiState, status: AuthStatus, user: WebUser?) {
    PageFrame("Guess Whose Song", state) {
        Text("Join a room with friends. No registration is required.")
        AuthActions(store, status, user)
        OutlinedTextField(
            value = state.displayName,
            onValueChange = store::setDisplayName,
            label = { Text("Display name") },
            supportingText = { Text("1–24 characters") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.joinCode,
            onValueChange = store::setJoinCode,
            label = { Text("Room code") },
            supportingText = { Text("Six characters") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = store::joinRoom, enabled = status == AuthStatus.READY && !state.isBusy) { Text("Join room") }
            OutlinedButton(onClick = store::createRoom, enabled = status == AuthStatus.READY && !state.isBusy) { Text("Create room") }
        }
        Text("The room code and display name stay out of the URL.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LobbyPage(store: WebGameStore, state: WebUiState) {
    val room = state.room
    val isHost = room?.hostId == state.selfPlayerId
    PageFrame("Room: ${room?.joinCode ?: state.joinCode}", state) {
        if (room == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                Text("Connecting to room…")
            }
            Text("The room code is ready. Waiting for the lobby connection…", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = store::leaveRoom) { Text("Cancel") }
            SpotifyLobbyControl(store, state)
        } else {
            Text("Players (${room.players.size}/${room.settings.playerLimit})", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                room.players.forEach { player ->
                    PlayerRow(
                        player = player,
                        isSelf = player.id == state.selfPlayerId,
                        canKick = isHost && player.id != state.selfPlayerId,
                        onKick = { store.kick(player.id) }
                    )
                }
            }

            SpotifyLobbyControl(store, state)

            if (isHost) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Game Settings", fontWeight = FontWeight.Bold)
                        Text("Player limit: ${room.settings.playerLimit}")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 4, 6, 10, 20).forEach { limit ->
                                TextButton(onClick = { store.updateSettings(room.settings.copy(playerLimit = limit)) }) {
                                    Text(limit.toString())
                                }
                            }
                        }
                        Text("Voting time: ${room.settings.votingTimerSeconds}s")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(10, 15, 20, 30).forEach { seconds ->
                                TextButton(onClick = { store.updateSettings(room.settings.copy(votingTimerSeconds = seconds)) }) {
                                    Text("${seconds}s")
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = store::startGame,
                    enabled = room.players.size >= 2,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Start Game", style = MaterialTheme.typography.titleMedium) }
            } else {
                Text("Waiting for host to start…", modifier = Modifier.fillMaxWidth())
            }

            ChatPanel(store, state)
            OutlinedButton(onClick = store::leaveRoom, modifier = Modifier.fillMaxWidth()) { Text("Leave room") }
        }
    }
}

@Composable
private fun SpotifyLobbyControl(store: WebGameStore, state: WebUiState) {
    if (!state.spotifyConnected) {
        Button(
            onClick = store::connectSpotify,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎧 Connect Spotify", color = Color.White)
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✓ Spotify Connected", color = Color(0xFF4ADE80), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("Top tracks ready", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PlayerRow(player: Player, isSelf: Boolean, canKick: Boolean, onKick: () -> Unit) {
    val label = buildString {
        append(player.displayName)
        if (isSelf) append(" (you)")
        if (player.isHost) append(" 👑")
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Normal)
                Text(
                    if (player.connected) "Connected" else "Reconnecting…",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (player.connected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
            }
            if (canKick) TextButton(onClick = onKick) { Text("Kick", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun SpotifyControls(store: WebGameStore, state: WebUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Spotify", fontWeight = FontWeight.Bold)
                Text(if (state.spotifyConnected) "Connected" else "Not connected")
            }
            if (state.spotifyConnected) {
                TextButton(onClick = store::disconnectSpotify) { Text("Disconnect") }
            } else {
                Button(onClick = store::connectSpotify) { Text("Connect") }
            }
            TextButton(onClick = store::refreshSpotify) { Text("Refresh") }
        }
    }
}

@Composable
private fun SubmissionPage(store: WebGameStore, state: WebUiState) {
    PageFrame("Choose your songs", state) {
        Text("Pick up to ${state.room?.settings?.roundLengthPreset?.songsPerPlayer ?: 1} songs before the timer ends.")
        Text(deadlineLabel(state.deadlineEpochMillis), style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = state.searchQuery, onValueChange = store::setSearchQuery, label = { Text("Search songs") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = store::search, enabled = state.searchQuery.isNotBlank()) { Text("Search") }
        }
        state.searchResults.forEach { track ->
            OutlinedButton(onClick = { store.selectTrack(track) }, modifier = Modifier.fillMaxWidth()) {
                Text("${track.title} · ${track.artist}")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { store.loadSpotifySuggestions(); store.refreshSpotify() }) { Text("Load Spotify picks") }
        }
        state.spotifySuggestions.forEach { suggestion ->
            TextButton(onClick = { store.selectSpotifySuggestion(suggestion) }) { Text("Add ${suggestion.title} · ${suggestion.artist}") }
        }
        Divider()
        Text("Selected songs", fontWeight = FontWeight.Bold)
        if (state.pendingSongs.isEmpty()) Text("No songs selected yet.")
        state.pendingSongs.forEach { song ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${song.title} · ${song.artist}", modifier = Modifier.weight(1f))
                TextButton(onClick = { store.removeSong(song.songId) }) { Text("Remove") }
            }
        }
        Button(onClick = store::lockSongs, enabled = state.pendingSongs.isNotEmpty()) { Text("Lock songs") }
    }
}

@Composable
private fun GamePage(store: WebGameStore, state: WebUiState) {
    PageFrame("Game", state) {
        state.preview?.let { preview ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Round ${preview.roundIndex + 1} of ${preview.totalRounds}", fontWeight = FontWeight.Bold)
                    Text(preview.title, style = MaterialTheme.typography.titleLarge)
                    Text(preview.artist)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = store::playCurrentPreview, enabled = preview.previewUrl.isNotBlank()) { Text("Play preview") }
                        OutlinedButton(onClick = store::stopCurrentPreview) { Text("Stop") }
                    }
                }
            }
        }
        state.voting?.let { voting ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Who submitted this song?", fontWeight = FontWeight.Bold)
                    Text(deadlineLabel(voting.votingDeadlineEpochMillis))
                    voting.players.filter { it.id != state.selfPlayerId }.forEach { player ->
                        Button(onClick = { store.castVote(player.id) }, modifier = Modifier.fillMaxWidth()) { Text(player.displayName) }
                    }
                }
            }
        }
        state.reveal?.let { reveal ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Reveal", fontWeight = FontWeight.Bold)
                    Text("${reveal.songEntry.title} · ${reveal.songEntry.artist}")
                    Text("Submitted by ${reveal.submitterName}")
                }
            }
        }
        ChatPanel(store, state)
    }
}

@Composable
private fun ResultsPage(store: WebGameStore, state: WebUiState) {
    val room = state.room
    val isHost = room?.hostId == state.selfPlayerId
    PageFrame("Results", state) {
        state.results?.players?.forEachIndexed { index, player ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}. ${player.displayName}", modifier = Modifier.weight(1f))
                Text("${player.score} points")
            }
        }
        if (isHost) {
            Button(onClick = store::playAgain) { Text("Play again") }
            OutlinedButton(onClick = store::endRoom) { Text("End room") }
        }
        OutlinedButton(onClick = store::leaveRoom) { Text("Leave") }
    }
}

@Composable
private fun ChatPanel(store: WebGameStore, state: WebUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Chat", fontWeight = FontWeight.Bold)
            state.chat.takeLast(12).forEach { message -> Text("${message.senderName}: ${message.text}") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.chatDraft, onValueChange = store::setChatDraft, label = { Text("Message") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = store::sendChat, enabled = state.chatDraft.isNotBlank()) { Text("Send") }
            }
        }
    }
}

private fun deadlineLabel(deadline: Long): String {
    if (deadline <= 0L) return ""
    val seconds = ((deadline - currentEpochMillis()) / 1_000L).coerceAtLeast(0L)
    return "$seconds seconds remaining"
}
