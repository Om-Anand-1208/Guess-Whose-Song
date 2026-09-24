package com.guesswhosesong.app.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.guesswhosesong.shared.dto.RoundRevealed
import com.guesswhosesong.shared.models.ChatMessage
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.RoundPhase

@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onNavigateToResults: () -> Unit,
    onNavigateToSubmission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var chatInput by remember { mutableStateOf("") }
    var chatExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                GameEvent.NavigateToResults -> onNavigateToResults()
                GameEvent.NavigateToSubmission -> onNavigateToSubmission()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.totalRounds > 0) {
            Text(
                text = "Round ${uiState.roundIndex + 1} of ${uiState.totalRounds}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (uiState.roundPhase == RoundPhase.PLAYING_PREVIEW ||
            uiState.roundPhase == RoundPhase.VOTING) {
            MediaPlayerModule(
                title = uiState.title,
                artist = uiState.artist,
                albumArtUrl = uiState.albumArtUrl,
                previewUrl = uiState.previewUrl,
                isPlaying = uiState.roundPhase == RoundPhase.PLAYING_PREVIEW
            )
        }

        when (uiState.roundPhase) {
            RoundPhase.PLAYING_PREVIEW -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "\uD83C\uDFB5 Listen carefully...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            RoundPhase.VOTING -> {
                VotingSection(
                    players = uiState.players,
                    selfPlayerId = uiState.selfPlayerId,
                    votedPlayerId = uiState.votedPlayerId,
                    votedCount = uiState.votedCount,
                    totalCount = uiState.totalVoters,
                    deadlineMs = uiState.votingDeadlineEpochMs,
                    onConfirmVote = viewModel::castVote,
                    modifier = Modifier.weight(1f)
                )
            }
            RoundPhase.REVEALING -> {
                RevealPanel(
                    revealData = uiState.revealData,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        ChatDock(
            messages = uiState.chatMessages,
            expanded = chatExpanded,
            onExpandToggle = { chatExpanded = !chatExpanded },
            input = chatInput,
            onInputChange = { chatInput = it },
            onSend = {
                if (chatInput.isNotBlank()) {
                    viewModel.sendChat(chatInput.trim())
                    chatInput = ""
                }
            }
        )
    }
}

@Composable
fun MediaPlayerModule(
    title: String,
    artist: String,
    albumArtUrl: String,
    previewUrl: String,
    isPlaying: Boolean
) {
    val context = LocalContext.current
    val exoPlayer = remember(previewUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(previewUrl))
            prepare()
            playWhenReady = true
        }
    }

    var playbackProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            val duration = exoPlayer.duration.takeIf { it > 0 } ?: 1L
            playbackProgress = exoPlayer.currentPosition.toFloat() / duration
            kotlinx.coroutines.delay(500)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && isPlaying) {
                exoPlayer.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { playbackProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun VotingHeader(votedCount: Int, totalCount: Int, deadlineMs: Long) {
    var secondsLeft by remember { mutableIntStateOf(0) }
    LaunchedEffect(deadlineMs) {
        while (true) {
            val remaining = ((deadlineMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            secondsLeft = remaining
            if (remaining == 0) break
            kotlinx.coroutines.delay(500)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Who submitted it?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "$votedCount/$totalCount voted \u00B7 ${secondsLeft}s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun VotingSection(
    players: List<Player>,
    selfPlayerId: String,
    votedPlayerId: String?,
    votedCount: Int,
    totalCount: Int,
    deadlineMs: Long,
    onConfirmVote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCandidateId by remember(players, votedPlayerId) {
        mutableStateOf(votedPlayerId)
    }

    val hasVoted = votedPlayerId != null
    val targetPlayerId = if (hasVoted) votedPlayerId else selectedCandidateId
    val targetPlayer = players.find { it.id == targetPlayerId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        VotingHeader(
            votedCount = votedCount,
            totalCount = totalCount,
            deadlineMs = deadlineMs
        )

        Spacer(Modifier.height(8.dp))

        VoteGrid(
            players = players,
            selfPlayerId = selfPlayerId,
            selectedPlayerId = targetPlayerId,
            enabled = !hasVoted,
            onSelect = { candidateId ->
                if (!hasVoted) {
                    selectedCandidateId = candidateId
                }
            },
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.height(10.dp))

        // Confirm vote button or confirmed status banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasVoted) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "✓ Vote submitted for ${targetPlayer?.displayName ?: "player"}!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "($votedCount/$totalCount ready)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        selectedCandidateId?.let { onConfirmVote(it) }
                    },
                    enabled = selectedCandidateId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (targetPlayer != null) {
                        Text(
                            "Vote for ${targetPlayer.displayName} 🗳️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "Tap a player above to select",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoteGrid(
    players: List<Player>,
    selfPlayerId: String,
    selectedPlayerId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(players) { player ->
            PlayerVoteCard(
                player = player,
                isSelf = player.id == selfPlayerId,
                isSelected = player.id == selectedPlayerId,
                enabled = enabled,
                onClick = { onSelect(player.id) }
            )
        }
    }
}

@Composable
fun PlayerVoteCard(
    player: Player,
    isSelf: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        !enabled && !isSelected -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                } else {
                    Text(player.displayName.take(2).uppercase(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = player.displayName + if (isSelf) " (you)" else "",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun RevealPanel(revealData: RoundRevealed?, modifier: Modifier = Modifier) {
    if (revealData == null) return
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "\uD83C\uDFA4 Submitted by: ${revealData.submitterName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text("Who guessed what:", style = MaterialTheme.typography.titleSmall)
        }
        items(revealData.voteResults) { result ->
            val icon = if (result.correct) "\u2705" else "\u274C"
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Text("$icon ", fontSize = 18.sp)
                    Column {
                        Text(result.voterName, fontWeight = FontWeight.Bold)
                        Text(
                            "\u2192 ${result.guessedPlayerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result.correct) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (result.correct) {
                        val delta = revealData.scoreDeltas.find { it.playerId == result.voterId }
                        if (delta != null) Text(
                            "+${delta.delta}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDock(
    messages: List<ChatMessage>,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandToggle)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("\uD83D\uDCAC Chat", style = MaterialTheme.typography.labelMedium)
            Text(if (expanded) "\u25BC" else "\u25B2", style = MaterialTheme.typography.labelMedium)
        }

        if (expanded) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { msg ->
                    Text(
                        text = "${msg.senderName}: ${msg.text}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("Say something...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSend) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                }
            }
        }
    }
}
