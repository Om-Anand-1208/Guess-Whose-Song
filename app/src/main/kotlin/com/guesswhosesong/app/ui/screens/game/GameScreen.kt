package com.guesswhosesong.app.ui.screens.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player as Media3Player
import androidx.media3.exoplayer.ExoPlayer
import com.guesswhosesong.app.ui.components.AlbumArtwork
import com.guesswhosesong.app.ui.components.EmptyAvatarBadge
import com.guesswhosesong.app.ui.components.PostJoinHeader
import com.guesswhosesong.app.ui.theme.AppearanceSettingsSheet
import com.guesswhosesong.app.ui.theme.GwsPalette
import com.guesswhosesong.app.ui.theme.PostJoinPalette
import com.guesswhosesong.shared.dto.RoundRevealed
import com.guesswhosesong.shared.models.ChatMessage
import com.guesswhosesong.shared.models.GameConstants
import com.guesswhosesong.shared.models.GameMode
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
    var showAppearanceSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                GameEvent.NavigateToResults -> onNavigateToResults()
                GameEvent.NavigateToSubmission -> onNavigateToSubmission()
            }
        }
    }

    val context = LocalContext.current
    val isPreviewPhase = uiState.roundPhase == RoundPhase.PLAYING_PREVIEW
    val exoPlayer = remember(uiState.previewUrl, uiState.roundIndex) {
        if (uiState.previewUrl.isNotBlank()) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uiState.previewUrl))
                prepare()
                playWhenReady = true
            }
        } else null
    }
    var isActuallyPlaying by remember(exoPlayer) { mutableStateOf(exoPlayer?.isPlaying == true) }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer
        if (player == null) {
            isActuallyPlaying = false
            onDispose { }
        } else {
            val listener = object : Media3Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isActuallyPlaying = isPlaying
                }
            }
            player.addListener(listener)
            isActuallyPlaying = player.isPlaying
            onDispose { player.removeListener(listener) }
        }
    }

    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(30_000L) }

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            val duration = player.duration.takeIf { it > 0 } ?: 30_000L
            durationMs = duration
            currentPositionMs = player.currentPosition
            playbackProgress = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            kotlinx.coroutines.delay(250)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer, isPreviewPhase) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                exoPlayer?.pause()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && isPreviewPhase) {
                exoPlayer?.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    LaunchedEffect(isPreviewPhase) {
        if (!isPreviewPhase) exoPlayer?.pause() else exoPlayer?.play()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostJoinPalette.Background)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        PostJoinHeader(
            roomCode = uiState.room?.joinCode.orEmpty(),
            onSettings = { showAppearanceSettings = true },
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        when (uiState.roundPhase) {
            RoundPhase.PLAYING_PREVIEW -> HeroPreviewScreen(
                title = uiState.title,
                artist = uiState.artist,
                albumArtUrl = uiState.albumArtUrl,
                roundIndex = uiState.roundIndex,
                totalRounds = uiState.totalRounds,
                playbackProgress = playbackProgress,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isPlaying = isActuallyPlaying,
                modifier = Modifier.weight(1f)
            )
            RoundPhase.VOTING -> Column(modifier = Modifier.weight(1f)) {
                VotingSection(
                    title = uiState.title,
                    artist = uiState.artist,
                    albumArtUrl = uiState.albumArtUrl,
                    roundIndex = uiState.roundIndex,
                    totalRounds = uiState.totalRounds,
                    players = uiState.players,
                    eligibleOwnerIds = uiState.eligibleOwnerIds,
                    selfPlayerId = uiState.selfPlayerId,
                    votedPlayerId = uiState.votedPlayerId,
                    votedCount = uiState.votedCount,
                    totalCount = uiState.totalVoters,
                    deadlineMs = uiState.votingDeadlineEpochMs,
                    isSelfSong = uiState.isSelfSong && uiState.room?.settings?.gameMode != GameMode.SPOTIFY_RECENT,
                    allowSelfVote = uiState.room?.settings?.gameMode == GameMode.SPOTIFY_RECENT,
                    onConfirmVote = viewModel::castVote,
                    modifier = Modifier.weight(1f)
                )
            }
            RoundPhase.REVEALING -> RevealPanel(
                revealData = uiState.revealData,
                players = uiState.players,
                currentRoundIndex = uiState.roundIndex,
                totalRounds = uiState.totalRounds,
                modifier = Modifier.weight(1f)
            )
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
    if (showAppearanceSettings) {
        AppearanceSettingsSheet(onDismiss = { showAppearanceSettings = false })
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
    val displayedPositionMs = currentPositionMs.coerceIn(0L, durationMs)
    val currentSeconds = (displayedPositionMs / 1000).coerceAtLeast(0)
    val totalSeconds = (durationMs / 1000).coerceAtLeast(1)
    val remainingSeconds = ((durationMs - displayedPositionMs) / 1000).coerceAtLeast(0)
    val currentStr = "0:${(currentSeconds % 60).toString().padStart(2, '0')}"
    val totalStr = "0:${(totalSeconds % 60).toString().padStart(2, '0')}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
                Text(
                    "ROUND ${roundIndex + 1} OF ${if (totalRounds > 0) totalRounds else "?"}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) GwsPalette.Mint else PostJoinPalette.Ink.copy(alpha = 0.28f))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (title.isBlank()) "GET READY" else if (isPlaying) "PREVIEW" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, PostJoinPalette.Selected, GwsPalette.Tangerine)
                    )
                )
        ) {
            AlbumArtwork(
                albumArtUrl,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Album art",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                title.ifBlank { "Get ready" },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                artist.ifBlank { "Listen closely" },
                style = MaterialTheme.typography.titleMedium,
                color = PostJoinPalette.Ink.copy(alpha = 0.63f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            AudioVisualizerBars(isPlaying = isPlaying)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { playbackProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = PostJoinPalette.Selected.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(currentStr, style = MaterialTheme.typography.labelSmall, color = PostJoinPalette.Ink.copy(alpha = 0.6f))
                Text("${remainingSeconds}s remaining", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(totalStr, style = MaterialTheme.typography.labelSmall, color = PostJoinPalette.Ink.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Listen closely. Voting starts when this preview ends.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AbstractCoverArtwork() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(GwsPalette.Tangerine, GwsPalette.Berry)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 20.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.78f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 48.dp)
                .clip(RoundedCornerShape(50))
                .background(GwsPalette.Tangerine.copy(alpha = 0.9f))
        )
    }
}

@Composable
fun AudioVisualizerBars(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "audio_bars")
    val heights = listOf(0.25f, 0.8f, 0.3f, 0.9f, 0.4f)
    Row(
        modifier = modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEachIndexed { index, staticHeight ->
            val movingHeight by transition.animateFloat(
                initialValue = staticHeight,
                targetValue = when (index) {
                    0 -> 1f
                    1 -> 0.2f
                    2 -> 0.95f
                    3 -> 0.35f
                    else -> 0.85f
                },
                animationSpec = infiniteRepeatable(
                    animation = tween(360 + index * 35, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            val height = (if (isPlaying) movingHeight else 0.2f).coerceIn(0.15f, 1f)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isPlaying) 1f else 0.45f))
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
        color = PostJoinPalette.Surface,
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PostJoinPalette.Ink.copy(alpha = 0.08f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, GwsPalette.Tangerine)))
            ) {
                AlbumArtwork(albumArtUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title.ifBlank { "Song ${roundIndex + 1}" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist.ifBlank { "Now playing" }, style = MaterialTheme.typography.bodySmall, color = PostJoinPalette.Ink.copy(alpha = 0.62f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(7.dp))
            Surface(color = PostJoinPalette.Selected.copy(alpha = 0.36f), shape = RoundedCornerShape(50)) {
                Text(
                    "${roundIndex + 1}/${if (totalRounds > 0) totalRounds else "?"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun VoteChoicesHeading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 19.dp, vertical = 5.dp)
    ) {
        Text("Who submitted this song?", style = MaterialTheme.typography.titleLarge, color = PostJoinPalette.Ink, fontWeight = FontWeight.Black)
        Text("Choose one card, then confirm your vote.", style = MaterialTheme.typography.bodySmall, color = PostJoinPalette.Muted)
    }
}

@Composable
fun VotingSection(
    title: String,
    artist: String,
    albumArtUrl: String,
    roundIndex: Int,
    totalRounds: Int,
    players: List<Player>,
    eligibleOwnerIds: List<String> = emptyList(),
    selfPlayerId: String,
    votedPlayerId: String?,
    votedCount: Int,
    totalCount: Int,
    deadlineMs: Long,
    isSelfSong: Boolean = false,
    allowSelfVote: Boolean = false,
    onConfirmVote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCandidateId by remember(players, votedPlayerId) { mutableStateOf(votedPlayerId) }
    val hasVoted = votedPlayerId != null
    val targetPlayerId = if (hasVoted) votedPlayerId else selectedCandidateId
    val targetDisplayName = when (targetPlayerId) {
        GameConstants.DECOY_ID -> GameConstants.DECOY_NAME
        else -> players.find { it.id == targetPlayerId }?.displayName
    }
    var secondsLeft by remember(deadlineMs) { mutableIntStateOf(0) }
    LaunchedEffect(deadlineMs) {
        while (true) {
            val remaining = ((deadlineMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            secondsLeft = remaining
            if (remaining == 0) break
            kotlinx.coroutines.delay(500)
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 9.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) {
            Text("VOTE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.6.sp)
            Text("Make your guess", style = MaterialTheme.typography.headlineLarge, color = PostJoinPalette.Ink, fontWeight = FontWeight.Black)
            Text("Who picked this one? Trust your music memory.", style = MaterialTheme.typography.bodyMedium, color = PostJoinPalette.Muted)
        }
        VotingSteps()
        VotingSongCard(
            title = title,
            artist = artist,
            albumArtUrl = albumArtUrl,
            roundIndex = roundIndex,
            totalRounds = totalRounds,
            votedCount = votedCount,
            totalCount = totalCount,
            secondsLeft = secondsLeft
        )
        VoteChoicesHeading()
        if (isSelfSong) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp)
            ) {
                Text(
                    "This is your song. You earn 1 point if nobody guesses it’s yours.",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        VoteGrid(
            players = players,
            eligibleOwnerIds = eligibleOwnerIds,
            filterEligibleOwners = allowSelfVote,
            selfPlayerId = selfPlayerId,
            selectedPlayerId = targetPlayerId,
            enabled = !hasVoted && secondsLeft > 0,
            allowSelfVote = allowSelfVote,
            showDecoy = !allowSelfVote,
            onSelect = { candidateId -> if (!hasVoted) selectedCandidateId = candidateId },
            modifier = Modifier.weight(1f)
        )
        if (hasVoted) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            ) {
                Text(
                    "Vote locked for ${targetDisplayName ?: "player"} · $votedCount/$totalCount ready",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = { selectedCandidateId?.let(onConfirmVote) },
                enabled = selectedCandidateId != null && secondsLeft > 0,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(58.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7557F4), contentColor = Color.White)
            ) {
                Text(
                    "▶  Confirm vote",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun VotingSteps() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        listOf("Listen", "Vote", "Reveal").forEachIndexed { index, label ->
            if (index > 0) {
                Box(Modifier.weight(1f).height(1.dp).background(PostJoinPalette.Outline))
            }
            val active = index == 1
            Surface(
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(37.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("0${index + 1}", color = if (active) Color.White else PostJoinPalette.Muted, fontWeight = FontWeight.Black)
                }
            }
            Text(
                label,
                modifier = Modifier.padding(start = 7.dp, end = if (index == 2) 0.dp else 11.dp),
                color = if (active) MaterialTheme.colorScheme.primary else PostJoinPalette.Muted,
                fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun VotingSongCard(
    title: String,
    artist: String,
    albumArtUrl: String,
    roundIndex: Int,
    totalRounds: Int,
    votedCount: Int,
    totalCount: Int,
    secondsLeft: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, PostJoinPalette.Outline.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                AlbumArtwork(albumArtUrl, modifier = Modifier.size(118.dp), contentDescription = "Song cover")
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("NOW GUESSING · ${roundIndex + 1}/${if (totalRounds > 0) totalRounds else "?"}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                    Text(title.ifBlank { "Song ${roundIndex + 1}" }, style = MaterialTheme.typography.titleLarge, color = PostJoinPalette.Ink, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(artist.ifBlank { "Unknown artist" }, style = MaterialTheme.typography.bodyMedium, color = PostJoinPalette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                            Text("◷  ${secondsLeft}s left", modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
                        }
                        Text("$votedCount/$totalCount voted", style = MaterialTheme.typography.labelMedium, color = PostJoinPalette.Muted, fontWeight = FontWeight.Bold)
                    }
                }
            }
            LinearProgressIndicator(
                progress = { if (totalCount > 0) (votedCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(5.dp).clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = PostJoinPalette.Outline.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun VoteGrid(
    players: List<Player>,
    eligibleOwnerIds: List<String> = emptyList(),
    filterEligibleOwners: Boolean = false,
    selfPlayerId: String,
    selectedPlayerId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    allowSelfVote: Boolean = false,
    showDecoy: Boolean = true,
    modifier: Modifier = Modifier
) {
    val choices = if (filterEligibleOwners) players.filter { it.id in eligibleOwnerIds }
    else if (eligibleOwnerIds.isEmpty()) players else players.filter { it.id in eligibleOwnerIds }
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(choices, key = { it.id }) { player ->
            PlayerVoteCard(
                player = player,
                isSelf = player.id == selfPlayerId,
                isSelected = player.id == selectedPlayerId,
                enabled = enabled,
                allowSelfVote = allowSelfVote,
                onClick = { onSelect(player.id) }
            )
        }
        if (showDecoy) item(key = GameConstants.DECOY_ID) {
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
    allowSelfVote: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else PostJoinPalette.Ink.copy(alpha = 0.09f)
    val background = when {
        isSelected -> PostJoinPalette.Selected.copy(alpha = 0.3f)
        isSelf -> GwsPalette.Butter.copy(alpha = 0.18f)
        else -> PostJoinPalette.Surface
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled && (!isSelf || allowSelfVote), onClick = onClick),
        color = background,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            EmptyAvatarBadge(
                avatarId = player.avatarId,
                size = 72.dp,
                selected = isSelected,
                customization = player.avatarCustomization
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(player.displayName, style = MaterialTheme.typography.titleMedium, color = PostJoinPalette.Ink, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (isSelf) "Your song · choose someone else" else if (isSelected) "Selected" else "Could it be them?", style = MaterialTheme.typography.bodyMedium, color = PostJoinPalette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SelectionMark(isSelected)
        }
    }
}

@Composable
fun DecoyVoteCard(isSelected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else PostJoinPalette.Ink.copy(alpha = 0.09f),
                RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (isSelected) PostJoinPalette.Selected.copy(alpha = 0.3f) else PostJoinPalette.Surface,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            EmptyAvatarBadge(avatarId = "cloud", size = 72.dp, selected = isSelected)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Nobody / Decoy", style = MaterialTheme.typography.titleMedium, color = PostJoinPalette.Ink, fontWeight = FontWeight.Black)
                Text("The song belongs to nobody here.", style = MaterialTheme.typography.bodyMedium, color = PostJoinPalette.Muted)
            }
            SelectionMark(isSelected)
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    Surface(
        modifier = Modifier.size(31.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = CircleShape,
        border = if (selected) null else BorderStroke(1.5.dp, PostJoinPalette.Muted.copy(alpha = 0.65f))
    ) {
        if (selected) Box(contentAlignment = Alignment.Center) {
            Text("✓", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun RevealPanel(
    revealData: RoundRevealed?,
    players: List<Player>,
    currentRoundIndex: Int,
    totalRounds: Int,
    modifier: Modifier = Modifier
) {
    if (revealData == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Reveal incoming", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val isDecoy = revealData.songEntry.submitterId == GameConstants.DECOY_ID
    val submitterDelta = if (!isDecoy) {
        val correctGuesses = revealData.voteResults.count {
            it.voterId != revealData.songEntry.submitterId && it.guessedPlayerId == revealData.songEntry.submitterId
        }
        if (correctGuesses == 0) revealData.scoreDeltas.find { it.playerId == revealData.songEntry.submitterId } else null
    } else null
    val isFinalRound = totalRounds > 0 && currentRoundIndex >= totalRounds - 1
    var revealSecondsLeft by remember(revealData) { mutableIntStateOf(5) }
    LaunchedEffect(revealData) {
        revealSecondsLeft = 5
        repeat(5) {
            kotlinx.coroutines.delay(1_000)
            revealSecondsLeft = (revealSecondsLeft - 1).coerceAtLeast(0)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 19.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item(key = "round-number") {
                Text(
                    "Round ${currentRoundIndex + 1} of ${totalRounds.coerceAtLeast(1)}",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = PostJoinPalette.Ink.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center
                )
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("SUBMITTED BY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(3.dp))
                    Text(revealData.submitterName, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(7.dp))
                    EmptyAvatarBadge(
                        avatarId = players.find { it.id == revealData.songEntry.submitterId }?.avatarId ?: "",
                        size = 62.dp,
                        customization = players.find { it.id == revealData.songEntry.submitterId }?.avatarCustomization
                    )
                    if (isDecoy) {
                        Text("Nobody in the room submitted this song.", style = MaterialTheme.typography.bodySmall, color = PostJoinPalette.Ink.copy(alpha = 0.65f), textAlign = TextAlign.Center)
                    } else if (submitterDelta != null) {
                        Text("Nobody guessed it · +${submitterDelta.delta} point", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
            item {
                Surface(
                    color = PostJoinPalette.Selected.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        AlbumArtwork(revealData.songEntry.albumArtUrl, contentDescription = null, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(revealData.songEntry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(revealData.songEntry.artist, style = MaterialTheme.typography.bodySmall, color = PostJoinPalette.Ink.copy(alpha = 0.63f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            item {
                Text("How everyone voted", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 2.dp))
            }
            items(revealData.voteResults) { result ->
                val player = players.find { it.id == result.voterId }
                val delta = revealData.scoreDeltas.find { it.playerId == result.voterId }
                Surface(
                    color = if (result.correct) GwsPalette.Lime.copy(alpha = 0.28f) else PostJoinPalette.Surface,
                    shape = RoundedCornerShape(15.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PostJoinPalette.Ink.copy(alpha = 0.07f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EmptyAvatarBadge(
                            avatarId = player?.avatarId ?: "",
                            size = 38.dp,
                            customization = player?.avatarCustomization
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(result.voterName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "Voted ${result.guessedPlayerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PostJoinPalette.Ink.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            if (result.correct) "Correct" else "Not quite",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (result.correct) Color(0xFF39752A) else PostJoinPalette.Ink.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        if (delta != null && delta.delta != 0) {
                            Spacer(Modifier.width(8.dp))
                            Text("+${delta.delta}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 19.dp, vertical = 10.dp),
            color = GwsPalette.Tangerine,
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                Text(
                    "${if (isFinalRound) "Results" else "Next round"} in ${revealSecondsLeft}s",
                    color = PostJoinPalette.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
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
            .background(PostJoinPalette.Background)
            .border(width = 1.dp, color = PostJoinPalette.Ink.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandToggle)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ROOM CHAT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
            Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        if (expanded) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    Text(
                        text = "${message.senderName}: ${message.text}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("Say something…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(7.dp))
                IconButton(onClick = onSend) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
