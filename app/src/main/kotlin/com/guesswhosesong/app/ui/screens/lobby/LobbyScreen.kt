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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
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
import com.guesswhosesong.app.ui.theme.GwsPalette
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.RoundCountRules

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GwsPalette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Room: ${room?.joinCode ?: joinCode}",
                modifier = Modifier.weight(1f),
                color = GwsPalette.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            ConnectionPill(connected = uiState.isConnected)
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = GwsPalette.Ink
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        val roomCode = room?.joinCode ?: joinCode
        RoomCodeCard(
            code = roomCode,
            connectedCount = connectedCount,
            playerLimit = playerLimit,
            onCopyCode = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Room code", roomCode.uppercase()))
                Toast.makeText(context, "Room code copied", Toast.LENGTH_SHORT).show()
            }
        )

        uiState.error?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = Color(0xFFFFE2DF),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    error,
                    color = Color(0xFF9D2922),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Players (${players.size}/$playerLimit)", color = GwsPalette.Ink, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$connectedCount connected",
                    color = GwsPalette.Ink.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (players.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.08f))
                    ) {
                        Text(
                            if (uiState.isConnected) "You’re in. Waiting for friends to join." else "Connecting to your room…",
                            modifier = Modifier.padding(16.dp),
                            color = GwsPalette.Ink.copy(alpha = 0.68f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            items(players, key = { it.id }) { player ->
                PlayerListItem(
                    player = player,
                    isSelf = player.id == selfId,
                    onKick = if (isHost && player.id != selfId) ({ viewModel.kickPlayer(player.id) }) else null
                )
            }
            if (remainingSlots > 0) {
                item {
                    OpenSeatsCard(openSeats = remainingSlots)
                }
            }
        }

        when {
            room == null -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GwsPalette.Lavender.copy(alpha = 0.32f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (uiState.isConnected) "Loading room…" else "Joining room…",
                        modifier = Modifier.padding(15.dp),
                        textAlign = TextAlign.Center,
                        color = GwsPalette.Ink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            isHost -> {
                Button(
                    onClick = viewModel::startGame,
                    enabled = connectedCount >= 2 && enoughRounds,
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GwsPalette.Tangerine,
                        contentColor = GwsPalette.Ink,
                        disabledContainerColor = GwsPalette.Tangerine.copy(alpha = 0.5f),
                        disabledContentColor = GwsPalette.Ink.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Start Game", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                when {
                    !enoughRounds -> Text(
                        "Choose at least ${players.size} rounds in settings before starting.",
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        textAlign = TextAlign.Center,
                        color = GwsPalette.Ink.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    connectedCount < 2 -> Text(
                        "Invite one more player to unlock song selection.",
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        textAlign = TextAlign.Center,
                        color = GwsPalette.Ink.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            else -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GwsPalette.Lavender.copy(alpha = 0.34f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "You’re all set",
                            color = GwsPalette.Ink,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Waiting for ${players.firstOrNull { it.isHost }?.displayName ?: "the host"} to start",
                            color = GwsPalette.Ink.copy(alpha = 0.66f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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
            onDismiss = { showSettings = false },
            onSave = { name, avatar, settings ->
                if (viewModel.updatePlayerProfile(name, avatar)) {
                    if (isHost) viewModel.updateSettings(settings)
                    showSettings = false
                }
            }
        )
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
                color = GwsPalette.Ink,
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
        color = GwsPalette.Lavender.copy(alpha = 0.34f),
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, GwsPalette.LavenderDeep.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ROOM CODE",
                    color = GwsPalette.Ink.copy(alpha = 0.62f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    code.uppercase(),
                    color = GwsPalette.Ink,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.2.sp
                )
                Text(
                    "Share this code with friends",
                    color = GwsPalette.Ink.copy(alpha = 0.66f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Surface(color = Color.White.copy(alpha = 0.72f), shape = RoundedCornerShape(13.dp)) {
                IconButton(onClick = onCopyCode, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy room code",
                        tint = GwsPalette.Ink
                    )
                }
            }
            Spacer(Modifier.width(7.dp))
            Surface(color = Color.White.copy(alpha = 0.72f), shape = RoundedCornerShape(14.dp)) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$connectedCount/$playerLimit",
                        color = GwsPalette.Ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "PLAYERS",
                        color = GwsPalette.Ink.copy(alpha = 0.58f),
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
            color = GwsPalette.Ink,
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
            .background(GwsPalette.Lavender.copy(alpha = 0.28f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(CircleShape)
                .background(GwsPalette.LavenderDeep)
        )
    }
}

@Composable
private fun OpenSeatsCard(openSeats: Int) {
    val swatches = AvatarOptions.take(4)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.54f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
            Text(
                "Room for ${openSeats.coerceAtMost(4)} more",
                color = GwsPalette.Ink.copy(alpha = 0.72f),
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
                    color = GwsPalette.Ink.copy(alpha = 0.56f),
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
        color = if (connected) GwsPalette.Lime.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.78f),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(
            1.dp,
            if (connected) spotifyGreen.copy(alpha = 0.55f) else GwsPalette.Ink.copy(alpha = 0.12f)
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
                    color = GwsPalette.Ink,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    if (connected) "Your song picks are ready" else "Optional · get song suggestions",
                    color = GwsPalette.Ink.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!connected) {
                TextButton(onClick = onConnect) {
                    Text("Connect", color = GwsPalette.LavenderDeep, fontWeight = FontWeight.ExtraBold)
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
        color = if (isSelf) GwsPalette.Lavender.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.86f),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(
            if (isSelf) 1.5.dp else 1.dp,
            if (isSelf) GwsPalette.LavenderDeep.copy(alpha = 0.56f) else GwsPalette.Ink.copy(alpha = 0.08f)
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
                        color = GwsPalette.Ink,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isSelf) {
                        Spacer(Modifier.width(6.dp))
                        Text("YOU", color = GwsPalette.LavenderDeep, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    if (player.isHost) {
                        Spacer(Modifier.width(6.dp))
                        Text("HOST", color = Color(0xFFB46A0B), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(
                    if (player.connected) "In the room" else "Reconnecting…",
                    color = if (player.connected) GwsPalette.Ink.copy(alpha = 0.58f) else Color(0xFF9D2922),
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = GwsPalette.Paper) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, color = GwsPalette.Ink)
            Text(
                "Update your player and connections.",
                style = MaterialTheme.typography.bodyMedium,
                color = GwsPalette.Ink.copy(alpha = 0.66f),
                modifier = Modifier.padding(bottom = 2.dp)
            )

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
                border = BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.14f))
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
                androidx.compose.material3.HorizontalDivider(color = GwsPalette.Ink.copy(alpha = 0.12f))
                Text("Room settings", style = MaterialTheme.typography.titleMedium, color = GwsPalette.Ink, fontWeight = FontWeight.Black)
                Text("Make the round feel like your group.", style = MaterialTheme.typography.bodySmall, color = GwsPalette.Ink.copy(alpha = 0.66f))

                Text("Total rounds", style = MaterialTheme.typography.labelLarge, color = GwsPalette.Ink)
                RoundCountControl(
                    roundCount = settings.roundCount,
                    currentPlayerCount = currentPlayerCount,
                    onRoundCountChange = { settings = settings.copy(roundCount = it) }
                )
                Text(
                    "Choose from ${RoundCountRules.minimumForPlayerCount(currentPlayerCount)} to ${RoundCountRules.MAX_ROUNDS}. Everyone contributes at least one song.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GwsPalette.Ink.copy(alpha = 0.66f)
                )

                Text("Voting time", style = MaterialTheme.typography.labelLarge, color = GwsPalette.Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 20, 30).forEach { secs ->
                        FilterChip(
                            selected = settings.votingTimerSeconds == secs,
                            onClick = { settings = settings.copy(votingTimerSeconds = secs) },
                            label = { Text("${secs}s") }
                        )
                    }
                }

                Text("Player limit: ${settings.playerLimit}", style = MaterialTheme.typography.labelLarge, color = GwsPalette.Ink)
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
                colors = ButtonDefaults.buttonColors(containerColor = GwsPalette.Tangerine, contentColor = GwsPalette.Ink)
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
            color = GwsPalette.Ink,
            fontWeight = FontWeight.Bold
        )
        OutlinedButton(
            onClick = { onRoundCountChange((roundCount + 1).coerceAtMost(RoundCountRules.MAX_ROUNDS)) },
            enabled = roundCount < RoundCountRules.MAX_ROUNDS,
            modifier = Modifier.semantics { contentDescription = "Increase rounds" }
        ) { Text("+") }
    }
}
