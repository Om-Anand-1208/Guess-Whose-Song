package com.guesswhosesong.app.ui.screens.lobby

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.ui.components.AvatarOptions
import com.guesswhosesong.app.ui.components.AvatarPicker
import com.guesswhosesong.app.ui.components.EmptyAvatarBadge
import com.guesswhosesong.app.ui.components.GoogleGMark
import com.guesswhosesong.app.ui.components.SpotifyMark
import com.guesswhosesong.app.ui.components.PostJoinHeader
import com.guesswhosesong.app.ui.components.AvatarCharacter
import com.guesswhosesong.app.ui.theme.GwsPalette
import com.guesswhosesong.app.ui.theme.PostJoinPalette
import com.guesswhosesong.app.ui.theme.LocalAppearanceSettings
import com.guesswhosesong.app.ui.theme.AppearanceModePicker
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.GameMode
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.RoundCountRules

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    joinCode: String,
    displayName: String,
    avatarCustomization: AvatarCustomization,
    viewModel: LobbyViewModel = hiltViewModel(),
    onNavigateToSubmission: () -> Unit,
    onKicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var chatDraft by remember { mutableStateOf("") }
    val appearanceSettings = LocalAppearanceSettings.current
    val context = LocalContext.current
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> result.data?.let(viewModel::linkGoogle) }

    LaunchedEffect(joinCode) {
        viewModel.connect(joinCode, displayName, avatarCustomization)
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
    val players = room?.players.orEmpty()
    val selfId = uiState.selfPlayerId
    val self = players.find { it.id == selfId }
    val isHost = self?.isHost == true
    val playerLimit = room?.settings?.playerLimit ?: 10
    val roundCount = room?.settings?.roundCount ?: RoundCountRules.DEFAULT_ROUNDS
    val enoughRounds = RoundCountRules.isValid(roundCount, players.size)
    val connectedCount = players.count { it.connected }
    val remainingSlots = (playerLimit - players.size).coerceAtLeast(0)

    val roomCode = room?.joinCode ?: joinCode
    val copyRoomCode = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Room code", roomCode.uppercase()))
        Toast.makeText(context, "Room code copied", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(PostJoinPalette.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        PostJoinHeader(roomCode = roomCode, onSettings = { showSettings = true })

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("THE HANGOUT", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp)
                Text("Room lobby", style = MaterialTheme.typography.displaySmall, color = PostJoinPalette.Ink)
                Text(
                    "Get your people in the room, then let the music do the talking.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PostJoinPalette.Muted
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarCharacter("lime", Modifier.size(48.dp), customization = AvatarCustomization.defaultsFor("lime").copy(eyesId = "happy", mouthId = "open"))
                AvatarCharacter("sunny", Modifier.size(52.dp), customization = AvatarCustomization.defaultsFor("sunny").copy(eyesId = "sleepy", mouthId = "open", accessoryId = "headphones"))
            }
        }

        uiState.error?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                color = Color(0xFFFFE2DF),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(error, color = Color(0xFF9D2922), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Players", style = MaterialTheme.typography.headlineSmall, color = PostJoinPalette.Ink)
            Spacer(Modifier.width(10.dp))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Text("${players.size} / $playerLimit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = copyRoomCode) { Text("Invite friends", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }

        if (players.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
                color = PostJoinPalette.Surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, PostJoinPalette.Outline)
            ) {
                Text(
                    if (uiState.isConnected) "You’re in. Waiting for friends to join." else "Connecting to your room…",
                    modifier = Modifier.padding(16.dp),
                    color = PostJoinPalette.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        players.forEach { player ->
            PlayerListItem(
                player = player,
                isSelf = player.id == selfId,
                onKick = if (isHost && player.id != selfId) ({ viewModel.kickPlayer(player.id) }) else null
            )
            Spacer(Modifier.height(8.dp))
        }
        if (remainingSlots > 0 && players.isEmpty()) OpenSeatsCard(openSeats = remainingSlots)

        if (isHost && room?.settings?.gameMode == GameMode.MANUAL && players.none { it.isComputer } && remainingSlots > 0) {
            OutlinedButton(onClick = viewModel::addComputerPlayer, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Add Computer Player")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Game settings", style = MaterialTheme.typography.headlineSmall, color = PostJoinPalette.Ink, modifier = Modifier.weight(1f))
            TextButton(onClick = { showSettings = true }) { Text("Edit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }
        GameSettingsSummary(
            mode = room?.settings?.gameMode ?: GameMode.MANUAL,
            rounds = roundCount,
            votingSeconds = room?.settings?.votingTimerSeconds ?: 20,
            playerLimit = playerLimit
        )

        when {
            room == null -> Surface(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                Text(if (uiState.isConnected) "Loading room…" else "Joining room…", modifier = Modifier.padding(15.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
            isHost -> {
                val canStart = connectedCount >= 2 && enoughRounds
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(64.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF7760F5), Color(0xFF714FF1))))
                        .clickable(enabled = canStart, onClick = viewModel::startGame),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(29.dp))
                    Text("Start game", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                }
                if (!enoughRounds || connectedCount < 2) Text(
                    if (!enoughRounds) "Choose at least ${players.size} rounds in settings before starting." else "Invite one more player to unlock song selection.",
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                    textAlign = TextAlign.Center,
                    color = PostJoinPalette.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            else -> Surface(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You’re all set", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold)
                    Text("Waiting for ${players.firstOrNull { it.isHost }?.displayName ?: "the host"} to start", color = PostJoinPalette.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(20.dp)).clickable { showChat = true }
                .padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) { Text("◉", fontSize = 26.sp, color = MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Room chat", fontWeight = FontWeight.Bold, color = PostJoinPalette.Ink)
                Text("Say hi to the room!", color = PostJoinPalette.Muted, style = MaterialTheme.typography.bodyMedium)
            }
            Text("›", fontSize = 32.sp, color = PostJoinPalette.Muted)
        }
    }

    if (showSettings && room != null) {
        LobbySettingsSheet(
            displayName = self?.displayName ?: displayName,
            avatarCustomization = self?.avatarCustomization ?: avatarCustomization,
            isSpotifyConnected = uiState.isSpotifyConnected,
            isAccountLinked = uiState.isAccountLinked,
            accountStatus = uiState.accountStatus,
            accountStatusIsError = uiState.accountStatusIsError,
            roomSettings = room.settings,
            currentPlayerCount = players.size,
            isHost = isHost,
            profileError = uiState.error,
            onConnectSpotify = viewModel::connectSpotify,
            onDisconnectSpotify = viewModel::disconnectSpotify,
            onLinkGoogle = {
                val activity = context as? Activity
                if (activity != null) googleLauncher.launch(viewModel.googleSignInIntent(activity))
            },
            hasComputerPlayer = players.any { it.isComputer },
            onDismiss = { showSettings = false },
            onSave = { name, avatar, settings ->
                if (viewModel.updatePlayerProfile(name, avatar)) {
                    if (isHost) viewModel.updateSettings(settings)
                    showSettings = false
                }
            }
        )
    }

    if (showChat) {
        ModalBottomSheet(
            onDismissRequest = { showChat = false },
            containerColor = PostJoinPalette.Surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Room chat", style = MaterialTheme.typography.headlineSmall, color = PostJoinPalette.Ink)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(uiState.chatMessages.takeLast(30)) { message ->
                        Column {
                            Text(message.senderName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(message.text, color = PostJoinPalette.Ink)
                        }
                    }
                    if (uiState.chatMessages.isEmpty()) item { Text("Say hi to the room!", color = PostJoinPalette.Muted) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = chatDraft,
                        onValueChange = { chatDraft = it.take(280) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.sendChat(chatDraft); chatDraft = "" },
                        enabled = chatDraft.isNotBlank()
                    ) { Text("Send") }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun GameSettingsSummary(
    mode: GameMode,
    rounds: Int,
    votingSeconds: Int,
    playerLimit: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PostJoinPalette.Surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, PostJoinPalette.Outline)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val values = listOf(
                Triple("♫", "Game mode", if (mode == GameMode.SPOTIFY_RECENT) "Recently played" else "Manual picks"),
                Triple("▱", "Total rounds", "$rounds"),
                Triple("◷", "Voting time", "${votingSeconds}s"),
                Triple("♟", "Player limit", "$playerLimit")
            )
            values.forEachIndexed { index, (icon, label, value) ->
                if (index > 0) Spacer(Modifier.width(1.dp).height(72.dp).background(PostJoinPalette.Outline))
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(icon, color = when (index) {
                        1 -> Color(0xFFFFB82E)
                        2 -> Color(0xFFFF7049)
                        else -> MaterialTheme.colorScheme.primary
                    }, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(label, color = PostJoinPalette.Muted, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(value, color = PostJoinPalette.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ConnectionPill(connected: Boolean) {
    val dotColor = if (connected) Color(0xFF54A956) else GwsPalette.Tangerine
    Surface(
        color = if (connected) GwsPalette.Lime.copy(alpha = 0.38f) else GwsPalette.Butter.copy(alpha = 0.65f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(6.dp))
            Text(
                if (connected) "LIVE" else "JOINING",
                color = PostJoinPalette.Ink,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
private fun RoomCodeCard(
    code: String,
    connectedCount: Int,
    playerLimit: Int,
    onCopyCode: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PostJoinPalette.Selected.copy(alpha = 0.34f),
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ROOM CODE",
                    color = PostJoinPalette.Ink.copy(alpha = 0.62f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    code.uppercase(),
                    color = PostJoinPalette.Ink,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.2.sp
                )
                Text(
                    "Share this code with friends",
                    color = PostJoinPalette.Ink.copy(alpha = 0.66f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Surface(color = PostJoinPalette.SurfaceVariant.copy(alpha = 0.72f), shape = RoundedCornerShape(13.dp)) {
                IconButton(onClick = onCopyCode, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy room code",
                        tint = PostJoinPalette.Ink
                    )
                }
            }
            Spacer(Modifier.width(7.dp))
            Surface(color = PostJoinPalette.SurfaceVariant.copy(alpha = 0.72f), shape = RoundedCornerShape(14.dp)) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$connectedCount/$playerLimit",
                        color = PostJoinPalette.Ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "PLAYERS",
                        color = PostJoinPalette.Ink.copy(alpha = 0.58f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CapacityPill(occupied: Int, capacity: Int) {
    Surface(color = GwsPalette.Butter.copy(alpha = 0.74f), shape = CircleShape) {
        Text(
            if (occupied >= capacity) "ROOM FULL" else "${(capacity - occupied).coerceAtLeast(0)} SPOTS LEFT",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = PostJoinPalette.Ink,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun CapacityBar(occupied: Int, capacity: Int) {
    val fraction = if (capacity <= 0) 0f else (occupied.toFloat() / capacity).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(PostJoinPalette.Selected.copy(alpha = 0.28f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun OpenSeatsCard(openSeats: Int) {
    val swatches = AvatarOptions.take(4)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PostJoinPalette.Surface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, PostJoinPalette.Ink.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
            Text(
                "Room for ${openSeats.coerceAtMost(4)} more",
                color = PostJoinPalette.Ink.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                swatches.forEach { option ->
                    EmptyAvatarBadge(option.id, size = 38.dp)
                }
                Text(
                    "Invite a friend to fill a spot",
                    modifier = Modifier.weight(1f),
                    color = PostJoinPalette.Ink.copy(alpha = 0.56f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun SpotifyLobbyCard(connected: Boolean, onConnect: () -> Unit) {
    val spotifyGreen = Color(0xFF188849)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (connected) GwsPalette.Lime.copy(alpha = 0.3f) else PostJoinPalette.Surface,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(
            1.dp,
            if (connected) spotifyGreen.copy(alpha = 0.55f) else PostJoinPalette.Ink.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = spotifyGreen.copy(alpha = 0.12f), shape = CircleShape) {
                Text(
                    "SPOTIFY",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    color = spotifyGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (connected) "Spotify connected" else "Connect Spotify",
                    color = PostJoinPalette.Ink,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    if (connected) "Your song picks are ready" else "Optional · get song suggestions",
                    color = PostJoinPalette.Ink.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!connected) {
                TextButton(onClick = onConnect) {
                    Text("Connect", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Text("READY", color = spotifyGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun PlayerListItem(
    player: Player,
    isSelf: Boolean,
    onKick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelf) PostJoinPalette.Selected.copy(alpha = 0.55f) else PostJoinPalette.Surface,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(
            if (isSelf) 1.5.dp else 1.dp,
            if (isSelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.56f) else PostJoinPalette.Ink.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmptyAvatarBadge(
                player.avatarId,
                size = 43.dp,
                selected = isSelf,
                customization = player.avatarCustomization
            )
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        player.displayName,
                        color = PostJoinPalette.Ink,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isSelf) {
                        Spacer(Modifier.width(6.dp))
                        Text("YOU", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    if (player.isHost) {
                        Spacer(Modifier.width(6.dp))
                        Text("HOST", color = Color(0xFFB46A0B), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    if (player.isComputer) {
                        Spacer(Modifier.width(6.dp))
                        Text("🤖 COMPUTER", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(
                    if (player.isComputer) "Picks songs and votes automatically" else if (player.connected) "In the room" else "Reconnecting…",
                    color = if (player.connected) PostJoinPalette.Ink.copy(alpha = 0.58f) else Color(0xFF9D2922),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (onKick != null) {
                TextButton(onClick = onKick, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)) {
                    Text("Kick", color = Color(0xFF9D2922), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LobbySettingsSheet(
    displayName: String,
    avatarCustomization: AvatarCustomization,
    isSpotifyConnected: Boolean,
    isAccountLinked: Boolean,
    accountStatus: String?,
    accountStatusIsError: Boolean,
    roomSettings: RoomSettings,
    currentPlayerCount: Int,
    hasComputerPlayer: Boolean,
    isHost: Boolean,
    profileError: String?,
    onConnectSpotify: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    onLinkGoogle: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, AvatarCustomization, RoomSettings) -> Unit
) {
    var draftName by remember(displayName) { mutableStateOf(displayName) }
    var draftAvatar by remember(displayName) { mutableStateOf(avatarCustomization) }
    var settings by remember(roomSettings) { mutableStateOf(roomSettings) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PostJoinPalette.Background) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, color = PostJoinPalette.Ink)
            Text(
                "Update your player and connections.",
                style = MaterialTheme.typography.bodyMedium,
                color = PostJoinPalette.Ink.copy(alpha = 0.66f),
                modifier = Modifier.padding(bottom = 2.dp)
            )

            val appearanceSettings = LocalAppearanceSettings.current
            AppearanceModePicker(
                mode = appearanceSettings.mode,
                onModeChange = appearanceSettings.setMode
            )
            androidx.compose.material3.HorizontalDivider(color = PostJoinPalette.Outline)

            OutlinedTextField(
                value = draftName,
                onValueChange = { draftName = it.take(24) },
                label = { Text("Display name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            AvatarPicker(
                customization = draftAvatar,
                onPresetSelected = { draftAvatar = AvatarCustomization.defaultsFor(it) },
                onCustomizationChanged = { draftAvatar = it }
            )

            OutlinedButton(
                onClick = if (isSpotifyConnected) onDisconnectSpotify else onConnectSpotify,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, Color(0xFF1DB954))
            ) {
                SpotifyMark()
                Spacer(Modifier.width(10.dp))
                Text(if (isSpotifyConnected) "Spotify connected · Disconnect" else "Connect Spotify")
            }
            OutlinedButton(
                onClick = onLinkGoogle,
                enabled = !isAccountLinked,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, PostJoinPalette.Ink.copy(alpha = 0.14f))
            ) {
                GoogleGMark()
                Spacer(Modifier.width(10.dp))
                Text(if (isAccountLinked) "Google account linked" else "Link Google account")
            }
            accountStatus?.let { message ->
                Text(
                    message,
                    color = if (accountStatusIsError) MaterialTheme.colorScheme.error else Color(0xFF32805A),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (isHost) {
                androidx.compose.material3.HorizontalDivider(color = PostJoinPalette.Ink.copy(alpha = 0.12f))
                Text("Room settings", style = MaterialTheme.typography.titleMedium, color = PostJoinPalette.Ink, fontWeight = FontWeight.Black)
                Text("Make the round feel like your group.", style = MaterialTheme.typography.bodySmall, color = PostJoinPalette.Ink.copy(alpha = 0.66f))

                Text("Game mode", style = MaterialTheme.typography.labelLarge, color = PostJoinPalette.Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.gameMode == GameMode.MANUAL,
                        onClick = { settings = settings.copy(gameMode = GameMode.MANUAL) },
                        label = { Text("Manual picks") }
                    )
                    FilterChip(
                        selected = settings.gameMode == GameMode.SPOTIFY_RECENT,
                        onClick = { settings = settings.copy(gameMode = GameMode.SPOTIFY_RECENT) },
                        enabled = !hasComputerPlayer,
                        label = { Text("Recently Played") }
                    )
                }
                Text(
                    if (hasComputerPlayer) {
                        "Remove the computer player before switching to Recently Played."
                    } else if (settings.gameMode == GameMode.SPOTIFY_RECENT) {
                        "Use unique playable tracks from players’ recent Spotify history."
                    } else {
                        "Everyone chooses songs before the game starts."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = PostJoinPalette.Ink.copy(alpha = 0.66f)
                )

                Text("Total rounds", style = MaterialTheme.typography.labelLarge, color = PostJoinPalette.Ink)
                RoundCountControl(
                    roundCount = settings.roundCount,
                    currentPlayerCount = currentPlayerCount,
                    onRoundCountChange = { settings = settings.copy(roundCount = it) }
                )
                Text(
                    if (settings.gameMode == GameMode.SPOTIFY_RECENT) {
                        "Choose from ${RoundCountRules.minimumForPlayerCount(currentPlayerCount)} to ${RoundCountRules.MAX_ROUNDS} rounds."
                    } else {
                        "Choose from ${RoundCountRules.minimumForPlayerCount(currentPlayerCount)} to ${RoundCountRules.MAX_ROUNDS}. Everyone contributes at least one song."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = PostJoinPalette.Ink.copy(alpha = 0.66f)
                )

                Text("Voting time", style = MaterialTheme.typography.labelLarge, color = PostJoinPalette.Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 20, 30).forEach { secs ->
                        FilterChip(
                            selected = settings.votingTimerSeconds == secs,
                            onClick = { settings = settings.copy(votingTimerSeconds = secs) },
                            label = { Text("${secs}s") }
                        )
                    }
                }

                Text("Player limit: ${settings.playerLimit}", style = MaterialTheme.typography.labelLarge, color = PostJoinPalette.Ink)
                Slider(
                    value = settings.playerLimit.toFloat(),
                    onValueChange = { settings = settings.copy(playerLimit = it.toInt()) },
                    valueRange = 2f..20f,
                    steps = 17
                )
            }

            profileError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = { onSave(draftName, draftAvatar, settings) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GwsPalette.Tangerine, contentColor = PostJoinPalette.Ink)
            ) {
                Text("Save changes", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun RoundCountControl(
    roundCount: Int,
    currentPlayerCount: Int,
    onRoundCountChange: (Int) -> Unit
) {
    val minimumRounds = RoundCountRules.minimumForPlayerCount(currentPlayerCount)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { onRoundCountChange((roundCount - 1).coerceAtLeast(minimumRounds)) },
            enabled = roundCount > minimumRounds,
            modifier = Modifier.semantics { contentDescription = "Decrease rounds" }
        ) { Text("−") }
        Text(
            "$roundCount rounds",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = PostJoinPalette.Ink,
            fontWeight = FontWeight.Bold
        )
        OutlinedButton(
            onClick = { onRoundCountChange((roundCount + 1).coerceAtMost(RoundCountRules.MAX_ROUNDS)) },
            enabled = roundCount < RoundCountRules.MAX_ROUNDS,
            modifier = Modifier.semantics { contentDescription = "Increase rounds" }
        ) { Text("+") }
    }
}
