package com.guesswhosesong.app.ui.screens.game

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.guesswhosesong.app.ui.components.AvatarBadge
import com.guesswhosesong.shared.dto.RoundRevealed
import com.guesswhosesong.shared.models.ChatMessage
import com.guesswhosesong.shared.models.GameConstants
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

    val context = LocalContext.current
    val isPlayingPreview = uiState.roundPhase == RoundPhase.PLAYING_PREVIEW

    val exoPlayer = remember(uiState.previewUrl) {
        if (uiState.previewUrl.isNotBlank()) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uiState.previewUrl))
                prepare()
                playWhenReady = true
            }
        } else null
    }

    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(30_000L) }

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            val dur = player.duration.takeIf { it > 0 } ?: 30_000L
            durationMs = dur
            currentPositionMs = player.currentPosition
            playbackProgress = (player.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
            kotlinx.coroutines.delay(250)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                exoPlayer?.pause()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && isPlayingPreview) {
                exoPlayer?.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.release()
        }
    }

    LaunchedEffect(isPlayingPreview) {
        if (!isPlayingPreview) exoPlayer?.pause() else exoPlayer?.play()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        when (uiState.roundPhase) {
            RoundPhase.PLAYING_PREVIEW -> {
                HeroPreviewScreen(
                    title = uiState.title,
                    artist = uiState.artist,
                    albumArtUrl = uiState.albumArtUrl,
                    roundIndex = uiState.roundIndex,
                    totalRounds = uiState.totalRounds,
                    playbackProgress = playbackProgress,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    isPlaying = isPlayingPreview,
                    modifier = Modifier.weight(1f)
                )
            }
            RoundPhase.VOTING -> {
                Column(modifier = Modifier.weight(1f)) {
                    CompactTrackBar(
                        title = uiState.title,
                        artist = uiState.artist,
                        albumArtUrl = uiState.albumArtUrl,
                        roundIndex = uiState.roundIndex,
                        totalRounds = uiState.totalRounds
                    )
                    VotingSection(
                        players = uiState.players,
                        selfPlayerId = uiState.selfPlayerId,
                        votedPlayerId = uiState.votedPlayerId,
                        votedCount = uiState.votedCount,
                        totalCount = uiState.totalVoters,
                        deadlineMs = uiState.votingDeadlineEpochMs,
                        isSelfSong = uiState.isSelfSong,
                        onConfirmVote = viewModel::castVote,
                        modifier = Modifier.weight(1f)
                    )
                }
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
fun HeroPreviewScreen(
    title: String,
    artist: String,
    albumArtUrl: String,
    roundIndex: Int,
    totalRounds: Int,
    playbackProgress: Float,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_and_waves")

    // Subtle breathing pulse for album art when music plays
    val albumScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.03f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "album_scale"
    )

    // Pulsing alpha for the "PREVIEW" live dot
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot"
    )

    val currentSeconds = (currentPositionMs / 1000).coerceAtLeast(0)
    val totalSeconds = (durationMs / 1000).coerceAtLeast(1)
    val remainingSeconds = ((durationMs - currentPositionMs) / 1000).coerceAtLeast(0)
    val currentStr = "0:${(currentSeconds % 60).toString().padStart(2, '0')}"
    val totalStr = "0:${(totalSeconds % 60).toString().padStart(2, '0')}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Top Status Badges ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "ROUND ${roundIndex + 1} OF ${if (totalRounds > 0) totalRounds else '?'}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = liveDotAlpha)
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (title.isBlank()) "GET READY" else if (isPlaying) "PREVIEW" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Hero Album Art with Vinyl / Ambient Glow Effect ──
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(albumScale)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary)
            )

            Card(
                modifier = Modifier
                    .size(230.dp)
                    .scale(albumScale),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                if (albumArtUrl.isNotBlank()) {
                    AsyncImage(
                        model = albumArtUrl,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎵",
                            fontSize = 64.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Track Title, Artist, & Soundwave Visualizer ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (title.isNotBlank()) title else "Get Ready! 🎵",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (artist.isNotBlank()) artist else "Round ${roundIndex + 1} starting soon...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            // Animated Visualizer Bars
            AudioVisualizerBars(isPlaying = isPlaying)
        }

        Spacer(Modifier.height(16.dp))

        // ── Progress Bar & Timer ──
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { playbackProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${remainingSeconds}s remaining",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = totalStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Teaser Hint Card ──
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎧", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Listen closely! Voting starts when this preview ends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AudioVisualizerBars(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")
    val b1 by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val b2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(360, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val b3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(510, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b3"
    )
    val b4 by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(440, easing = FastOutLinearInEasing), RepeatMode.Reverse),
        label = "b4"
    )
    val b5 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(390, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "b5"
    )

    val bars = listOf(b1, b2, b3, b4, b5)
    Row(
        modifier = modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { value ->
            val scale = (if (isPlaying) value else 0.2f).coerceIn(0.15f, 1.0f)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight(scale)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun CompactTrackBar(
    title: String,
    artist: String,
    albumArtUrl: String,
    roundIndex: Int,
    totalRounds: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(44.dp)
            ) {
                if (albumArtUrl.isNotBlank()) {
                    AsyncImage(
                        model = albumArtUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎵", fontSize = 16.sp)
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { "Song ${roundIndex + 1}" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist.ifBlank { "Playing track..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Round ${roundIndex + 1}/${if (totalRounds > 0) totalRounds else '?'}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    isSelfSong: Boolean = false,
    onConfirmVote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCandidateId by remember(players, votedPlayerId) {
        mutableStateOf(votedPlayerId)
    }

    val hasVoted = votedPlayerId != null
    val targetPlayerId = if (hasVoted) votedPlayerId else selectedCandidateId
    val targetDisplayName = when (targetPlayerId) {
        GameConstants.DECOY_ID -> GameConstants.DECOY_NAME
        else -> players.find { it.id == targetPlayerId }?.displayName
    }

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

        if (isSelfSong) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🤫", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "This is your song! You earn 1 point if NO ONE guesses it's yours!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

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
                            text = "✓ Vote submitted for ${targetDisplayName ?: "player"}!",
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
                    if (targetDisplayName != null) {
                        Text(
                            "Vote for $targetDisplayName 🗳️",
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
        item {
            DecoyVoteCard(
                isSelected = selectedPlayerId == GameConstants.DECOY_ID,
                enabled = enabled,
                onClick = { onSelect(GameConstants.DECOY_ID) }
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
    val canClick = enabled && !isSelf
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isSelf -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = canClick, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                AvatarBadge(player.avatarId, size = 52.dp)
                if (isSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(7.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isSelf) "${player.displayName}\n(you)" else player.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
                color = if (isSelf) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DecoyVoteCard(
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
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
                        else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                } else {
                    Text("❓", fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Nobody / Decoy",
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
    val isDecoy = revealData.songEntry.submitterId == GameConstants.DECOY_ID
    val submitterDelta = if (!isDecoy) {
        val otherGuessesForSubmitter = revealData.voteResults
            .filter { it.voterId != revealData.songEntry.submitterId }
            .count { it.guessedPlayerId == revealData.songEntry.submitterId }
        if (otherGuessesForSubmitter == 0) {
            revealData.scoreDeltas.find { it.playerId == revealData.songEntry.submitterId }
        } else null
    } else null

    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "\uD83C\uDFA4 Submitted by: ${revealData.submitterName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (isDecoy) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🤖", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Mystery Decoy Track!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Nobody in the room submitted this song!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        } else if (submitterDelta != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎭", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "${revealData.submitterName} stumped everyone!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Nobody guessed them — bonus +1 point awarded!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Who guessed what:", style = MaterialTheme.typography.titleSmall)
        }

        items(revealData.voteResults) { result ->
            val icon = if (result.correct) "\u2705" else "\u274C"
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
