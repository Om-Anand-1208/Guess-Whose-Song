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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.guesswhosesong.app.ui.components.AvatarCharacter
import com.guesswhosesong.app.ui.components.EmptyAvatarBadge
import com.guesswhosesong.app.ui.components.PostJoinHeader
import com.guesswhosesong.app.ui.components.avatarOption
import com.guesswhosesong.app.ui.theme.AppearanceSettingsSheet
import com.guesswhosesong.app.ui.theme.GwsPalette
import com.guesswhosesong.app.ui.theme.PostJoinPalette
import com.guesswhosesong.shared.dto.RoundRevealed
import com.guesswhosesong.shared.models.AvatarCatalog
import com.guesswhosesong.shared.models.AvatarCustomization
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
            modifier = Modifier.padding(start = 18.dp, end = 22.5.dp)
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
                    votingTimerSeconds = uiState.room?.settings?.votingTimerSeconds ?: 20,
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
            if (artist.isNotBlank()) Text(
                artist,
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
    Text(
        "Who submitted this song?",
        modifier = Modifier.fillMaxWidth().padding(start = 24.5.dp, end = 24.dp).padding(top = 10.dp, bottom = 10.dp),
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 18.7.sp,
            lineHeight = 23.sp,
            letterSpacing = 0.sp
        ),
        color = PostJoinPalette.Ink,
        fontWeight = FontWeight.Black
    )
}

@Composable
fun VotingSection(
    title: String,
    artist: String,
    albumArtUrl: String,
    roundIndex: Int,
    totalRounds: Int,
    votingTimerSeconds: Int = 30,
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

    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp, vertical = 4.dp)) {
            Text("Make your guess", style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, lineHeight = 37.sp), color = PostJoinPalette.Ink, fontWeight = FontWeight.Black)
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
            secondsLeft = secondsLeft,
            votingTimerSeconds = votingTimerSeconds
        )
        VoteChoicesHeading()
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
            val canConfirm = selectedCandidateId != null && secondsLeft > 0
            val confirmGradient = if (canConfirm) {
                listOf(Color(0xFF7054F4), Color(0xFF7D52FA), Color(0xFF714DF5))
            } else {
                listOf(Color(0xFF9A94B3), Color(0xFFA39AB7), Color(0xFF9A94B3))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(confirmGradient))
            ) {
                Button(
                    onClick = { selectedCandidateId?.let(onConfirmVote) },
                    enabled = canConfirm,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "Confirm vote",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VotingSteps() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 14.dp, bottom = 10.dp),
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
                modifier = Modifier.size(38.dp)
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
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
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
    secondsLeft: Int,
    votingTimerSeconds: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 6.5.dp, bottom = 5.5.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, PostJoinPalette.Outline.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(horizontal = 12.dp).padding(top = 18.dp, bottom = 19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                AlbumArtwork(albumArtUrl, modifier = Modifier.size(146.dp), contentDescription = "Song cover")
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title.ifBlank { "Song ${roundIndex + 1}" }, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 27.sp), color = PostJoinPalette.Ink, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(artist.ifBlank { "Unknown artist" }, style = MaterialTheme.typography.bodyMedium, color = PostJoinPalette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        Surface(color = Color(0xFFFFD34F), shape = RoundedCornerShape(50)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF151515), modifier = Modifier.size(18.dp))
                                Text("${secondsLeft}s left", fontWeight = FontWeight.Black, color = Color(0xFF151515), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Text("$votedCount/$totalCount voted", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = PostJoinPalette.Muted, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.weight(1f).height(6.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                    )
                    BoxWithConstraints(
                        Modifier.weight(1f).height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PostJoinPalette.Outline.copy(alpha = 0.38f))
                    ) {
                        val timerProgress = (
                            secondsLeft.toFloat() / votingTimerSeconds.coerceAtLeast(1)
                        ).coerceIn(0f, 1f)
                        Box(
                            Modifier.fillMaxWidth(timerProgress).fillMaxHeight()
                                .background(Color(0xFFFFD34F), RoundedCornerShape(50))
                        )
                    }
                }
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd).size(13.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
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
        modifier = modifier.padding(horizontal = 16.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(10.5.dp),
        contentPadding = PaddingValues(bottom = 6.dp)
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
            .heightIn(min = if (isSelected) 102.dp else 95.dp)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled && (!isSelf || allowSelfVote), onClick = onClick),
        color = background,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            VoteAvatarScene(
                avatarId = player.avatarId,
                customization = if (player.isComputer) {
                    AvatarCustomization.normalize(player.avatarCustomization, player.avatarId).copy(mouthId = "smile")
                } else player.avatarCustomization,
                modifier = Modifier.width(100.dp).height(if (isSelected) 76.dp else 68.dp)
            )
            Spacer(Modifier.width(19.5.dp))
            Column(Modifier.weight(1f)) {
                Text(player.displayName, style = MaterialTheme.typography.titleMedium, color = PostJoinPalette.Ink, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            .heightIn(min = if (isSelected) 102.dp else 95.dp)
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
        Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            VoteAvatarScene(
                avatarId = "cloud",
                customization = AvatarCustomization.defaultsFor("cloud").copy(mouthId = "smile"),
                modifier = Modifier.width(100.dp).height(if (isSelected) 76.dp else 68.dp)
            )
            Spacer(Modifier.width(19.5.dp))
            Column(Modifier.widthIn(max = 155.dp)) {
                Text("Nobody / Decoy", style = MaterialTheme.typography.titleMedium, color = PostJoinPalette.Ink, fontWeight = FontWeight.Black)
            }
            SelectionMark(isSelected)
        }
    }
}

/** Scenic, vote-only framing for avatars, matching the illustrated choice cards. */
@Composable
private fun VoteAvatarScene(
    avatarId: String,
    modifier: Modifier = Modifier,
    customization: AvatarCustomization? = null
) {
    val appearance = AvatarCustomization.normalize(customization, AvatarCatalog.normalize(avatarId))
    val option = avatarOption(appearance.colorId)
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun point(x: Float, y: Float) = Offset(w * x, h * y)
            fun drawBackdrop(path: Path, color: Color) = drawPath(path, color)

            when (appearance.shapeId) {
                "lime" -> {
                    val blob = Path().apply {
                        moveTo(point(0.12f, 0.76f).x, point(0.12f, 0.76f).y)
                        cubicTo(point(0.01f, 0.67f).x, point(0.01f, 0.67f).y, point(0.08f, 0.43f).x, point(0.08f, 0.43f).y, point(0.23f, 0.41f).x, point(0.23f, 0.41f).y)
                        cubicTo(point(0.25f, 0.23f).x, point(0.25f, 0.23f).y, point(0.45f, 0.25f).x, point(0.45f, 0.25f).y, point(0.51f, 0.37f).x, point(0.51f, 0.37f).y)
                        cubicTo(point(0.64f, 0.25f).x, point(0.64f, 0.25f).y, point(0.86f, 0.33f).x, point(0.86f, 0.33f).y, point(0.82f, 0.51f).x, point(0.82f, 0.51f).y)
                        cubicTo(point(0.99f, 0.63f).x, point(0.99f, 0.63f).y, point(0.85f, 0.78f).x, point(0.85f, 0.78f).y, point(0.68f, 0.80f).x, point(0.68f, 0.80f).y)
                        lineTo(point(0.28f, 0.82f).x, point(0.28f, 0.82f).y)
                        cubicTo(point(0.19f, 0.83f).x, point(0.19f, 0.83f).y, point(0.12f, 0.81f).x, point(0.12f, 0.81f).y, point(0.12f, 0.76f).x, point(0.12f, 0.76f).y)
                        close()
                    }
                    drawBackdrop(blob, Color(0xFF799344).copy(alpha = if (darkTheme) 0.48f else 0.24f))
                    drawOval(
                        color = Color(0xFF78934B).copy(alpha = if (darkTheme) 0.66f else 0.28f),
                        topLeft = point(0.08f, 0.84f),
                        size = Size(w * 0.84f, h * 0.13f)
                    )
                }
                "sunny" -> {
                    // The sunny character sits on a pale halo and a warm yellow ground stroke.
                    drawOval(
                        color = if (darkTheme) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.80f),
                        topLeft = point(0.08f, 0.25f),
                        size = Size(w * 0.84f, h * 0.62f)
                    )
                    drawOval(
                        color = Color(0xFFFFD34F).copy(alpha = if (darkTheme) 0.55f else 0.72f),
                        topLeft = point(0.08f, 0.84f),
                        size = Size(w * 0.84f, h * 0.13f)
                    )
                    drawVoteMusicNote(point(0.17f, 0.30f), Color(0xFF8B69FF), h * 0.22f)
                    drawVoteMusicNote(point(0.89f, 0.29f), Color(0xFFFFC52E), h * 0.23f)
                    listOf(
                        point(0.07f, 0.54f) to point(0.16f, 0.58f),
                        point(0.09f, 0.64f) to point(0.18f, 0.64f)
                    ).forEach { (start, end) ->
                        drawLine(Color(0xFFFFC52E), start, end, strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
                "cloud" -> {
                    val blob = Path().apply {
                        moveTo(point(0.13f, 0.76f).x, point(0.13f, 0.76f).y)
                        cubicTo(point(0.04f, 0.68f).x, point(0.04f, 0.68f).y, point(0.09f, 0.51f).x, point(0.09f, 0.51f).y, point(0.22f, 0.48f).x, point(0.22f, 0.48f).y)
                        cubicTo(point(0.22f, 0.32f).x, point(0.22f, 0.32f).y, point(0.40f, 0.28f).x, point(0.40f, 0.28f).y, point(0.50f, 0.42f).x, point(0.50f, 0.42f).y)
                        cubicTo(point(0.61f, 0.26f).x, point(0.61f, 0.26f).y, point(0.81f, 0.34f).x, point(0.81f, 0.34f).y, point(0.81f, 0.49f).x, point(0.81f, 0.49f).y)
                        cubicTo(point(0.96f, 0.49f).x, point(0.96f, 0.49f).y, point(0.95f, 0.70f).x, point(0.95f, 0.70f).y, point(0.84f, 0.77f).x, point(0.84f, 0.77f).y)
                        lineTo(point(0.13f, 0.76f).x, point(0.13f, 0.76f).y)
                        close()
                    }
                    drawBackdrop(blob, option.color.copy(alpha = if (darkTheme) 0.20f else 0.28f))
                    drawOval(
                        color = Color(0xFF8CB8E5).copy(alpha = if (darkTheme) 0.64f else 0.36f),
                        topLeft = point(0.08f, 0.84f),
                        size = Size(w * 0.84f, h * 0.13f)
                    )
                    val rayColor = Color(0xFF8B69FF)
                    val rayWidth = 2.4.dp.toPx()
                    listOf(
                        Offset(0.34f, 0.24f) to Offset(0.30f, 0.13f),
                        Offset(0.45f, 0.20f) to Offset(0.45f, 0.07f),
                        Offset(0.56f, 0.20f) to Offset(0.59f, 0.08f),
                        Offset(0.68f, 0.23f) to Offset(0.73f, 0.13f)
                    ).forEach { (from, to) ->
                        drawLine(rayColor, point(from.x, from.y), point(to.x, to.y), strokeWidth = rayWidth, cap = StrokeCap.Round)
                    }
                }
                else -> {
                    drawOval(
                        color = option.color.copy(alpha = if (darkTheme) 0.20f else 0.16f),
                        topLeft = point(0.04f, 0.28f),
                        size = Size(w * 0.92f, h * 0.62f)
                    )
                    drawOval(
                        color = option.accent.copy(alpha = if (darkTheme) 0.55f else 0.28f),
                        topLeft = point(0.08f, 0.84f),
                        size = Size(w * 0.84f, h * 0.13f)
                    )
                }
            }
        }

        AvatarCharacter(
            avatarId = appearance.shapeId,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = 0.90f
                scaleY = 0.84f
            },
            customization = appearance,
            heroPose = true,
            bodyRotation = when (appearance.shapeId) {
                "lime" -> -4f
                "sunny" -> 2f
                "violet" -> -1f
                "tangerine" -> 1.5f
                "berry" -> -2f
                else -> 0f
            }
        )

        when (appearance.shapeId) {
            "lime" -> Text(
                "?",
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = (-2).dp).rotate(8f),
                color = Color(0xFF8B69FF),
                fontSize = 26.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVoteMusicNote(
    center: Offset,
    color: Color,
    unit: Float
) {
    val ink = Color(0xFF17161A)
    val head = Offset(center.x - unit * 0.36f, center.y + unit * 0.29f)
    val headSize = Size(unit * 0.43f, unit * 0.29f)
    val stemStart = Offset(head.x + headSize.width * 0.78f, head.y + headSize.height * 0.12f)
    val stemEnd = Offset(stemStart.x, center.y - unit * 0.52f)
    val flag = Path().apply {
        moveTo(stemEnd.x, stemEnd.y)
        cubicTo(
            stemEnd.x + unit * 0.40f, stemEnd.y + unit * 0.03f,
            stemEnd.x + unit * 0.48f, stemEnd.y + unit * 0.24f,
            stemEnd.x + unit * 0.40f, stemEnd.y + unit * 0.42f
        )
    }
    val outline = 1.2.dp.toPx()
    val noteStroke = 2.1.dp.toPx()

    drawLine(ink, stemStart, stemEnd, strokeWidth = noteStroke + outline, cap = StrokeCap.Round)
    drawPath(flag, ink, style = Stroke(noteStroke + outline, cap = StrokeCap.Round))
    drawOval(ink, topLeft = head, size = headSize)

    drawLine(color, stemStart, stemEnd, strokeWidth = noteStroke, cap = StrokeCap.Round)
    drawPath(flag, color, style = Stroke(noteStroke, cap = StrokeCap.Round))
    drawOval(
        color,
        topLeft = Offset(head.x + outline / 2f, head.y + outline / 2f),
        size = Size(headSize.width - outline, headSize.height - outline)
    )
}

@Composable
private fun SelectionMark(selected: Boolean) {
    Surface(
        modifier = Modifier.size(if (selected) 31.dp else 24.dp),
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
