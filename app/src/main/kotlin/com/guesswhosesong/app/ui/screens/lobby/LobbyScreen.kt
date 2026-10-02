package com.guesswhosesong.app.ui.screens.lobby

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.guesswhosesong.app.ui.components.avatarOption
import com.guesswhosesong.app.ui.components.PostJoinHeroCharacter
import com.guesswhosesong.app.ui.components.PostJoinHeroFloorShadow
import com.guesswhosesong.app.ui.components.PostJoinMusicNote
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
    val appearanceSettings = LocalAppearanceSettings.current
    val context = LocalContext.current
    val isDarkAppearance = PostJoinPalette.Background.luminance() < 0.5f
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
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        PostJoinHeader(
            roomCode = roomCode,
            onSettings = { showSettings = true },
            modifier = Modifier.padding(top = 10.5.dp),
            trailingControlsOffsetY = 4.dp,
            roomHorizontalPadding = 15.5.dp,
            roomVerticalPadding = 7.5.dp,
            roomOffsetY = (-2.75).dp
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp).offset(y = 3.dp)) {
                Text(
                    "Room lobby",
                    color = PostJoinPalette.Ink,
                    fontSize = 38.5.sp,
                    lineHeight = 45.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.35).sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
            LobbyHeroCharacters(Modifier.width(175.dp).height(123.dp).offset(y = 19.5.dp))
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
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Players",
                color = PostJoinPalette.Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.offset(x = 2.dp, y = 2.5.dp)
            )
            Spacer(Modifier.width(8.5.dp))
            Surface(
                modifier = Modifier.offset(x = if (isDarkAppearance) 6.dp else 0.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Text(
                    "${players.size} / $playerLimit",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 13.5.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Surface(
                modifier = Modifier.offset(x = (-0.5).dp).clickable(onClick = copyRoomCode),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (isDarkAppearance) 11.125.dp else 12.375.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                    Text("Invite friends", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        if (players.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
        players.forEachIndexed { index, player ->
            LobbyPlayerRow(
                player = player,
                isSelf = player.id == selfId,
                onRemove = if (isHost && player.id != selfId) ({ viewModel.kickPlayer(player.id) }) else null,
                modifier = if (index == 1) Modifier.offset(y = (-4).dp) else Modifier
            )
            androidx.compose.material3.HorizontalDivider(
                modifier = if (index == 1) Modifier.offset(y = (-4).dp) else Modifier,
                color = PostJoinPalette.Outline,
                thickness = 1.dp
            )
        }
        if (remainingSlots > 0 && players.isEmpty()) OpenSeatsCard(openSeats = remainingSlots)

        if (isHost && room?.settings?.gameMode == GameMode.MANUAL && players.none { it.isComputer } && remainingSlots > 0) {
            OutlinedButton(onClick = viewModel::addComputerPlayer, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Add Computer Player")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 23.dp, bottom = 10.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Game settings",
                color = PostJoinPalette.Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f).offset(x = 2.dp, y = if (isDarkAppearance) (-2.5).dp else 0.dp)
            )
            Surface(
                modifier = Modifier.offset(y = (-3).dp).clickable { showSettings = true },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.75.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                    Text("Edit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        GameSettingsSummary(
            mode = room?.settings?.gameMode ?: GameMode.MANUAL,
            rounds = roundCount,
            votingSeconds = room?.settings?.votingTimerSeconds ?: 20,
            playerLimit = playerLimit
        )

        when {
            room == null -> Surface(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                Text(if (uiState.isConnected) "Loading room…" else "Joining room…", modifier = Modifier.padding(15.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
            isHost -> {
                val canStart = connectedCount >= 2 && enoughRounds
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 0.5.dp).padding(top = 17.dp)
                        .height(if (isDarkAppearance) 66.dp else 64.dp)
                        .alpha(if (canStart) 1f else 0.50f)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF7657FF), Color(0xFF6F50F5))))
                        .clickable(enabled = canStart, onClick = viewModel::startGame),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.offset(x = (-10).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp).offset(x = (-3).dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start game", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp, lineHeight = 24.sp)
                    }
                }
                if (!enoughRounds || connectedCount < 2) Text(
                    if (!enoughRounds) "Choose at least ${players.size} rounds in settings before starting." else "Invite one more player to unlock song selection.",
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                    textAlign = TextAlign.Center,
                    color = PostJoinPalette.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            else -> Surface(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Waiting for ${players.firstOrNull { it.isHost }?.displayName ?: "the host"} to start", color = PostJoinPalette.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            onDisconnectGoogle = viewModel::disconnectGoogle,
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

}

@Composable
private fun LobbyHeroCharacters(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val floor = if (isDarkSurface) {
            Color(0xFF474A57).copy(alpha = 0.36f)
        } else {
            Color(0xFFFFD85A).copy(alpha = 0.27f)
        }
        PostJoinHeroFloorShadow(
            Modifier.align(Alignment.BottomStart).offset(x = 1.dp, y = (-5).dp).size(width = 120.dp, height = 27.dp),
            color = floor
        )
        PostJoinHeroFloorShadow(
            Modifier.align(Alignment.BottomEnd).offset(x = (-1).dp, y = (-4).dp).size(width = 128.dp, height = 27.dp),
            color = floor,
            alternate = true
        )
        PostJoinHeroCharacter(
            "lime",
            Modifier.align(Alignment.BottomStart).offset(x = (-6).dp, y = (-7).dp).size(103.dp),
            customization = AvatarCustomization.defaultsFor("lime").copy(eyesId = "dots", mouthId = "open"),
            tiltBodyOnly = true,
            richFinish = true,
            brightLimbs = isDarkSurface
        )
        PostJoinHeroCharacter(
            "sunny",
            Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = (-2).dp).size(100.dp),
            customization = AvatarCustomization.defaultsFor("sunny").copy(
                eyesId = "happy",
                mouthId = "open",
                accessoryId = "headphones"
            ),
            tiltBodyOnly = true,
            richFinish = true,
            brightLimbs = isDarkSurface
        )
        PostJoinMusicNote(
            Modifier.align(Alignment.TopCenter).offset(x = 2.dp, y = 1.dp).size(28.dp),
            color = Color(0xFFFFD64A),
            double = true
        )
        PostJoinMusicNote(
            Modifier.align(Alignment.TopEnd).offset(x = (-1).dp, y = 7.dp).size(27.dp),
            color = Color(0xFF8061FF)
        )
        LobbySoundAccent(
            Modifier.align(Alignment.TopStart).offset(x = 29.dp, y = 18.dp),
            color = Color(0xFF8061FF),
            rotation = -18f
        )
        LobbySoundAccent(
            Modifier.align(Alignment.TopStart).offset(x = 47.dp, y = 8.dp),
            color = Color(0xFF8061FF),
            rotation = 12f,
            height = 7.dp
        )
        LobbySoundAccent(
            Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 36.dp),
            color = Color(0xFFFFD64A),
            rotation = 52f,
            width = 3.dp,
            height = 10.dp
        )
        LobbySoundAccent(
            Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = 49.dp),
            color = Color(0xFFFFD64A),
            rotation = 68f,
            width = 3.dp,
            height = 7.dp
        )
    }
}

@Composable
private fun LobbySoundAccent(
    modifier: Modifier,
    color: Color,
    rotation: Float,
    width: androidx.compose.ui.unit.Dp = 4.dp,
    height: androidx.compose.ui.unit.Dp = 13.dp
) {
    Box(
        modifier.size(width = width, height = height)
            .rotate(rotation)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun LobbyPlayerRow(
    player: Player,
    isSelf: Boolean,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 2.5.dp, top = 15.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LobbyAvatarTile(player)
        Column(modifier = Modifier.weight(1f).padding(start = 17.5.dp, end = 5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isSelf) "${player.displayName} (you)" else player.displayName,
                    modifier = Modifier.weight(1f, fill = false),
                    color = PostJoinPalette.Ink,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (player.isHost) {
                    Spacer(Modifier.width(7.dp))
                    LobbyRoleBadge("HOST", Color(0xFFFFE49A), crown = true)
                }
                if (player.isComputer) {
                    Spacer(Modifier.width(7.dp))
                    LobbyRoleBadge("COMPUTER", Color(0xFFD6C9FF), leadingIcon = Icons.Default.SmartToy)
                }
            }
            if (player.isComputer) {
                Text(
                    "Picks songs and votes automatically.",
                    modifier = Modifier.padding(top = 4.dp),
                    color = PostJoinPalette.Muted,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            } else {
                Row(
                    modifier = Modifier.padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(9.dp).clip(CircleShape)
                            .background(if (player.connected) Color(0xFF79DF58) else Color(0xFFFF6A68))
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (player.connected) "Connected" else "Reconnecting…",
                        color = PostJoinPalette.Muted,
                        fontSize = 14.sp
                    )
                }
            }
        }
        if (onRemove != null) {
            TextButton(
                onClick = onRemove,
                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 6.dp)
            ) {
                Text("Remove", color = Color(0xFFFF6267), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun LobbyAvatarTile(player: Player) {
    val avatarColor = avatarOption(
        AvatarCustomization.normalize(player.avatarCustomization, player.avatarId).colorId
    ).color
    val tileColor = if (player.isComputer) {
        Color(0xFFFEEDCF)
    } else {
        Color(
            red = 0.70f + avatarColor.red * 0.30f,
            green = 0.70f + avatarColor.green * 0.30f,
            blue = 0.70f + avatarColor.blue * 0.30f
        )
    }
    Surface(
        modifier = Modifier.size(62.dp),
        color = tileColor,
        shape = RoundedCornerShape(18.dp)
    ) {
        PostJoinHeroCharacter(
            player.avatarId,
            modifier = Modifier.padding(6.dp),
            customization = player.avatarCustomization,
            heroPose = false
        )
    }
}

@Composable
private fun LobbyRoleBadge(label: String, color: Color, leadingIcon: ImageVector? = null, crown: Boolean = false) {
    val isComputerBadge = label == "COMPUTER"
    Surface(color = color, shape = CircleShape) {
        Row(
            modifier = Modifier.padding(
                horizontal = when {
                    isComputerBadge -> 10.5.dp
                    leadingIcon == null && !crown -> 9.dp
                    else -> 8.dp
                },
                vertical = 5.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (crown) LobbyCrownIcon(Modifier.size(14.dp))
            leadingIcon?.let {
                Icon(it, contentDescription = null, tint = Color(0xFF17161A), modifier = Modifier.size(if (isComputerBadge) 16.dp else 13.dp))
            }
            Text(
                label,
                color = Color(0xFF17161A),
                fontSize = if (isComputerBadge) 12.sp else 10.sp,
                lineHeight = if (isComputerBadge) 14.sp else 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LobbyCrownIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val ink = Color(0xFF17161A)
        val crown = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.31f)
            lineTo(size.width * 0.34f, size.height * 0.48f)
            lineTo(size.width * 0.49f, size.height * 0.12f)
            lineTo(size.width * 0.66f, size.height * 0.48f)
            lineTo(size.width * 0.89f, size.height * 0.29f)
            lineTo(size.width * 0.80f, size.height * 0.76f)
            lineTo(size.width * 0.20f, size.height * 0.76f)
            close()
        }
        drawPath(crown, ink)
        drawRoundRect(
            ink,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.79f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.04f)
        )
        listOf(0.12f to 0.25f, 0.49f to 0.06f, 0.89f to 0.23f).forEach { (x, y) ->
            drawCircle(ink, size.height * 0.075f, androidx.compose.ui.geometry.Offset(size.width * x, size.height * y))
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
    val isLightSurface = PostJoinPalette.Background.luminance() > 0.5f
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 1.dp, end = 0.5.dp)
            .offset(y = if (isLightSurface) 0.dp else (-1).dp),
        color = if (isLightSurface) Color(0xFFF9F6EF) else PostJoinPalette.Surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        border = if (isLightSurface) BorderStroke(0.5.dp, Color(0xFFF9F6EF)) else BorderStroke(1.dp, PostJoinPalette.Outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val values = listOf(
                Triple(Icons.Default.MusicNote, "Game mode", if (mode == GameMode.SPOTIFY_RECENT) "Recently played" else "Manual picks"),
                Triple(Icons.Default.Layers, "Total rounds", "$rounds"),
                Triple(Icons.Default.AccessTime, "Voting time", "${votingSeconds}s"),
                Triple(Icons.Default.Groups, "Player limit", "$playerLimit")
            )
            values.forEachIndexed { index, (icon, label, value) ->
                if (index > 0) {
                    Spacer(
                        Modifier.offset(x = 4.5.dp).width(1.dp).height(80.dp)
                            .background(PostJoinPalette.Outline)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = when (index) {
                        1 -> Color(0xFFFFB82E)
                        2 -> Color(0xFFFF7049)
                        else -> MaterialTheme.colorScheme.primary
                    }, modifier = Modifier.size(35.dp))
                    Text(label, color = PostJoinPalette.Muted, fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(value, color = PostJoinPalette.Ink, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    onDisconnectGoogle: () -> Unit,
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
                Text(if (isSpotifyConnected) "Disconnect" else "Connect Spotify")
            }
            OutlinedButton(
                onClick = if (isAccountLinked) onDisconnectGoogle else onLinkGoogle,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, PostJoinPalette.Ink.copy(alpha = 0.14f))
            ) {
                GoogleGMark()
                Spacer(Modifier.width(10.dp))
                Text(if (isAccountLinked) "Disconnect" else "Connect Google")
            }
            accountStatus?.takeIf { accountStatusIsError }?.let { message ->
                Text(
                    message,
                    color = if (accountStatusIsError) MaterialTheme.colorScheme.error else Color(0xFF32805A),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (isHost) {
                androidx.compose.material3.HorizontalDivider(color = PostJoinPalette.Ink.copy(alpha = 0.12f))
                Text("Room settings", style = MaterialTheme.typography.titleMedium, color = PostJoinPalette.Ink, fontWeight = FontWeight.Black)

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
                Text("Total rounds", style = MaterialTheme.typography.labelLarge, color = PostJoinPalette.Ink)
                RoundCountControl(
                    roundCount = settings.roundCount,
                    currentPlayerCount = currentPlayerCount,
                    onRoundCountChange = { settings = settings.copy(roundCount = it) }
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
