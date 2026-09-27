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
import coil.compose.AsyncImage
import com.guesswhosesong.app.ui.components.EmptyAvatarBadge
import com.guesswhosesong.app.ui.components.GuessWhoseSongWordmark
import com.guesswhosesong.app.ui.theme.GwsPalette
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
            .background(GwsPalette.Paper)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
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
                GuessWhoseSongWordmark(Modifier.padding(start = 20.dp, top = 5.dp, bottom = 2.dp))
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
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GuessWhoseSongWordmark(modifier = Modifier.weight(1f))
                Text(
                    "ROUND ${roundIndex + 1} OF ${if (totalRounds > 0) totalRounds else "?"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.9.sp,
                    color = GwsPalette.Ink
                )
            }
            Spacer(Modifier.height(10.dp))
            RoundProgressDots(roundIndex = roundIndex, totalRounds = totalRounds)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(GwsPalette.LavenderDeep, GwsPalette.Lavender, GwsPalette.Tangerine)
                    )
                )
        ) {
            if (albumArtUrl.isNotBlank()) {
                AsyncImage(
                    model = albumArtUrl,
                    contentDescription = "Album art",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AbstractCoverArtwork()
            }
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
                color = GwsPalette.Ink.copy(alpha = 0.63f),
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
                color = GwsPalette.LavenderDeep,
                trackColor = GwsPalette.Lavender.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(currentStr, style = MaterialTheme.typography.labelSmall, color = GwsPalette.Ink.copy(alpha = 0.6f))
                Text("${remainingSeconds}s remaining", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GwsPalette.LavenderDeep)
                Text(totalStr, style = MaterialTheme.typography.labelSmall, color = GwsPalette.Ink.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Listen closely. Voting starts when this preview ends.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = GwsPalette.Ink.copy(alpha = 0.67f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RoundProgressDots(roundIndex: Int, totalRounds: Int) {
    val dotCount = totalRounds.coerceIn(1, 12)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(dotCount) { index ->
            Surface(
                modifier = Modifier.size(if (index == roundIndex) 10.dp else 8.dp),
                shape = CircleShape,
                color = if (index == roundIndex) GwsPalette.LavenderDeep else GwsPalette.Ink.copy(alpha = 0.12f)
            ) { }
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
                .background(GwsPalette.LavenderDeep.copy(alpha = 0.78f))
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
    val transition = rememberInfiniteTransition(label = "audio_wave")
    val heights = listOf(0.34f, 0.68f, 0.47f, 0.95f, 0.55f, 0.78f, 0.39f, 0.82f, 0.52f, 0.7f, 0.34f, 0.6f, 0.9f, 0.44f, 0.72f, 0.36f, 0.8f)
    Row(
        modifier = modifier.height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, staticHeight ->
            val movingHeight by transition.animateFloat(
                initialValue = staticHeight * 0.55f,
                targetValue = staticHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(340 + (index % 5) * 80, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_$index"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(if (isPlaying) movingHeight else 0.16f)
                    .clip(RoundedCornerShape(50))
                    .background(GwsPalette.LavenderDeep.copy(alpha = if (isPlaying) 0.88f else 0.35f))
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
        color = Color.White,
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.08f)),
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
                    .background(Brush.linearGradient(listOf(GwsPalette.LavenderDeep, GwsPalette.Tangerine)))
            ) {
                if (albumArtUrl.isNotBlank()) {
                    AsyncImage(model = albumArtUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else AbstractCoverArtwork()
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title.ifBlank { "Song ${roundIndex + 1}" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist.ifBlank { "Now playing" }, style = MaterialTheme.typography.bodySmall, color = GwsPalette.Ink.copy(alpha = 0.62f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(7.dp))
            Surface(color = GwsPalette.Lavender.copy(alpha = 0.36f), shape = RoundedCornerShape(50)) {
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
    ) {
        Text("Who submitted it?", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Choose a player", style = MaterialTheme.typography.bodySmall, color = GwsPalette.Ink.copy(alpha = 0.63f))
            Text("$votedCount/$totalCount voted · ${secondsLeft}s", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GwsPalette.LavenderDeep)
        }
        LinearProgressIndicator(
            progress = { if (totalCount > 0) votedCount.toFloat() / totalCount else 0f },
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(5.dp).clip(RoundedCornerShape(50)),
            color = GwsPalette.LavenderDeep,
            trackColor = GwsPalette.Lavender.copy(alpha = 0.25f)
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
    var selectedCandidateId by remember(players, votedPlayerId) { mutableStateOf(votedPlayerId) }
    val hasVoted = votedPlayerId != null
    val targetPlayerId = if (hasVoted) votedPlayerId else selectedCandidateId
    val targetDisplayName = when (targetPlayerId) {
        GameConstants.DECOY_ID -> GameConstants.DECOY_NAME
        else -> players.find { it.id == targetPlayerId }?.displayName
    }

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 9.dp)) {
        VotingHeader(votedCount = votedCount, totalCount = totalCount, deadlineMs = deadlineMs)
        if (isSelfSong) {
            Surface(
                color = GwsPalette.Butter.copy(alpha = 0.52f),
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
            selfPlayerId = selfPlayerId,
            selectedPlayerId = targetPlayerId,
            enabled = !hasVoted,
            onSelect = { candidateId -> if (!hasVoted) selectedCandidateId = candidateId },
            modifier = Modifier.weight(1f)
        )
        if (hasVoted) {
            Surface(
                color = GwsPalette.Lime.copy(alpha = 0.42f),
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
                enabled = selectedCandidateId != null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GwsPalette.Tangerine, contentColor = GwsPalette.Ink)
            ) {
                Text(
                    targetDisplayName?.let { "Vote for $it" } ?: "Choose a player to vote",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium
                )
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
        modifier = modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
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
fun PlayerVoteCard(player: Player, isSelf: Boolean, isSelected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) GwsPalette.LavenderDeep else GwsPalette.Ink.copy(alpha = 0.09f)
    val background = when {
        isSelected -> GwsPalette.Lavender.copy(alpha = 0.3f)
        isSelf -> GwsPalette.Butter.copy(alpha = 0.18f)
        else -> Color.White
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled && !isSelf, onClick = onClick),
        color = background,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            EmptyAvatarBadge(avatarId = player.avatarId, size = 43.dp, selected = isSelected)
            Spacer(Modifier.height(6.dp))
            Text(player.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 5.dp))
            Text(
                when {
                    isSelf -> "You"
                    isSelected -> "Selected"
                    else -> " "
                },
                style = MaterialTheme.typography.labelSmall,
                color = GwsPalette.LavenderDeep,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DecoyVoteCard(isSelected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) GwsPalette.LavenderDeep else GwsPalette.Ink.copy(alpha = 0.09f),
                RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (isSelected) GwsPalette.Lavender.copy(alpha = 0.3f) else Color.White,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = if (isSelected) GwsPalette.Lavender.copy(alpha = 0.46f) else GwsPalette.Butter.copy(alpha = 0.68f),
                shape = CircleShape
            ) { }
            Spacer(Modifier.height(7.dp))
            Text("Nobody / Decoy", style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center)
            Text(if (isSelected) "Selected" else "", style = MaterialTheme.typography.labelSmall, color = GwsPalette.LavenderDeep, fontWeight = FontWeight.Bold)
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
            item(key = "game-wordmark") {
                GuessWhoseSongWordmark()
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("SUBMITTED BY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp, color = GwsPalette.LavenderDeep)
                    Spacer(Modifier.height(3.dp))
                    Text(revealData.submitterName, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(7.dp))
                    EmptyAvatarBadge(
                        avatarId = players.find { it.id == revealData.songEntry.submitterId }?.avatarId ?: "",
                        size = 62.dp
                    )
                    if (isDecoy) {
                        Text("Nobody in the room submitted this song.", style = MaterialTheme.typography.bodySmall, color = GwsPalette.Ink.copy(alpha = 0.65f), textAlign = TextAlign.Center)
                    } else if (submitterDelta != null) {
                        Text("Nobody guessed it · +${submitterDelta.delta} point", style = MaterialTheme.typography.bodySmall, color = GwsPalette.LavenderDeep, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
            item {
                Surface(
                    color = GwsPalette.Lavender.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(GwsPalette.LavenderDeep, GwsPalette.Tangerine)))
                        ) {
                            val image = revealData.songEntry.albumArtUrl
                            if (image.isNotBlank()) AsyncImage(model = image, contentDescription = null, modifier = Modifier.fillMaxSize())
                            else AbstractCoverArtwork()
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(revealData.songEntry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(revealData.songEntry.artist, style = MaterialTheme.typography.bodySmall, color = GwsPalette.Ink.copy(alpha = 0.63f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    color = if (result.correct) GwsPalette.Lime.copy(alpha = 0.28f) else Color.White,
                    shape = RoundedCornerShape(15.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.07f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EmptyAvatarBadge(avatarId = player?.avatarId ?: "", size = 38.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(result.voterName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "Voted ${result.guessedPlayerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwsPalette.Ink.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            if (result.correct) "Correct" else "Not quite",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (result.correct) Color(0xFF39752A) else GwsPalette.Ink.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        if (delta != null && delta.delta != 0) {
                            Spacer(Modifier.width(8.dp))
                            Text("+${delta.delta}", color = GwsPalette.LavenderDeep, fontWeight = FontWeight.Black)
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
                    color = GwsPalette.Ink,
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
            .background(GwsPalette.Paper)
            .border(width = 1.dp, color = GwsPalette.Ink.copy(alpha = 0.08f))
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
            Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelMedium, color = GwsPalette.LavenderDeep, fontWeight = FontWeight.Bold)
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
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = GwsPalette.LavenderDeep)
                }
            }
        }
    }
}
