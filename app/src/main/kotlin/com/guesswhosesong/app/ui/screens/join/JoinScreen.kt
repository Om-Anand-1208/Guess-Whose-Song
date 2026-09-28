package com.guesswhosesong.app.ui.screens.join

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.R
import com.guesswhosesong.app.ui.components.AvatarPicker
import com.guesswhosesong.app.ui.theme.GwsPalette
import com.guesswhosesong.shared.models.AvatarCustomization

private val GwsDisplayFont = FontFamily(Font(R.font.comfortaa_bold, FontWeight.Bold))

@Composable
fun JoinScreen(
    viewModel: JoinViewModel = hiltViewModel(),
    onNavigateToLobby: (joinCode: String, displayName: String, avatarCustomization: AvatarCustomization) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var showingSavePlayer by rememberSaveable { mutableStateOf(false) }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> result.data?.let(viewModel::linkGoogle) }
    val recoveryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> result.data?.let(viewModel::recoverWithGoogle) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JoinEvent.NavigateToLobby -> onNavigateToLobby(event.joinCode, event.displayName, event.avatarCustomization)
            }
        }
    }

    BackHandler(enabled = showingSavePlayer) { showingSavePlayer = false }

    if (showingSavePlayer) {
        SavePlayerPage(
            isAccountLinked = uiState.isAccountLinked,
            statusMessage = uiState.accountStatus,
            statusIsError = uiState.accountStatusIsError,
            onLinkGoogle = {
                val activity = context as? Activity
                if (activity != null) googleLauncher.launch(viewModel.googleSignInIntent(activity))
            },
            onRecoverGoogle = {
                val activity = context as? Activity
                if (activity != null) recoveryLauncher.launch(viewModel.googleSignInIntent(activity))
            },
            onBackToGame = { showingSavePlayer = false },
            onKeepPlayingAsGuest = { showingSavePlayer = false }
        )
    } else {
        WelcomePage(
            displayName = uiState.displayName,
            joinCode = uiState.joinCode,
            avatarCustomization = uiState.avatarCustomization,
            isLoading = uiState.isLoading,
            isSpotifyConnected = uiState.isSpotifyConnected,
            error = uiState.error,
            onDisplayNameChange = viewModel::onDisplayNameChanged,
            onJoinCodeChange = viewModel::onJoinCodeChanged,
            onAvatarSelected = viewModel::onAvatarSelected,
            onAvatarCustomizationChanged = viewModel::onAvatarCustomizationChanged,
            onCreateRoom = {
                keyboard?.hide()
                viewModel.createRoom()
            },
            onJoinRoom = {
                keyboard?.hide()
                viewModel.joinRoom()
            },
            onConnectSpotify = viewModel::connectSpotify,
            onDisconnectSpotify = viewModel::disconnectSpotify,
            onSavePlayer = { showingSavePlayer = true }
        )
    }
}

@Composable
private fun WelcomePage(
    displayName: String,
    joinCode: String,
    avatarCustomization: AvatarCustomization,
    isLoading: Boolean,
    isSpotifyConnected: Boolean,
    error: String?,
    onDisplayNameChange: (String) -> Unit,
    onJoinCodeChange: (String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onAvatarCustomizationChanged: (AvatarCustomization) -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onConnectSpotify: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    onSavePlayer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GwsPalette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            JoinLogoConfetti(left = true)
            JoinDisplayText(
                "Guess\nWhose Song",
                fontSize = 35.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.9).sp,
                strokeWidth = 2.dp,
                textAlign = TextAlign.Center
            )
            JoinLogoConfetti(left = false)
        }
        Text(
            "Pick a character. Bring your best songs.",
            color = GwsPalette.Ink.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        )
        Image(
            painter = painterResource(R.drawable.welcome_hero),
            contentDescription = "Friends playing a music guessing game",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(166.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        AvatarPicker(
            customization = avatarCustomization,
            onPresetSelected = onAvatarSelected,
            onCustomizationChanged = onAvatarCustomizationChanged,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = { Text("Your name") },
            placeholder = { Text("e.g. Maya") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(9.dp))
        Button(
            onClick = onCreateRoom,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF983D),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Create a room", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = GwsPalette.Ink.copy(alpha = 0.18f))
            Text(
                "  OR JOIN A ROOM  ",
                color = GwsPalette.Ink.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = GwsPalette.Ink.copy(alpha = 0.18f))
        }

        OutlinedTextField(
            value = joinCode,
            onValueChange = onJoinCodeChange,
            label = { Text("Room code") },
            placeholder = { Text("ABCD12") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = { onJoinRoom() }),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onJoinRoom,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GwsPalette.Lavender.copy(alpha = 0.74f),
                contentColor = GwsPalette.Ink
            ),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Join a room", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }

        error?.let { message ->
            Text(
                message,
                color = if (message.contains("linked", ignoreCase = true) || message.contains("recovered", ignoreCase = true)) Color(0xFF32805A) else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        SpotifyConnection(
            connected = isSpotifyConnected,
            onConnect = onConnectSpotify,
            onDisconnect = onDisconnectSpotify,
            modifier = Modifier.padding(top = 12.dp)
        )
        TextButton(onClick = onSavePlayer, modifier = Modifier.padding(top = 1.dp, bottom = 5.dp)) {
            Text("Save your player", color = GwsPalette.LavenderDeep, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SavePlayerPage(
    isAccountLinked: Boolean,
    statusMessage: String?,
    statusIsError: Boolean,
    onLinkGoogle: () -> Unit,
    onRecoverGoogle: () -> Unit,
    onBackToGame: () -> Unit,
    onKeepPlayingAsGuest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GwsPalette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBackToGame) {
                Text("← Back to welcome", color = GwsPalette.LavenderDeep, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 3.dp)) {
            val compact = maxWidth < 350.dp
            if (compact) {
                JoinDisplayText(
                    "Save your player",
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.6).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    JoinLogoConfetti(left = true)
                    JoinDisplayText(
                        "Save your player",
                        fontSize = 29.sp,
                        lineHeight = 34.sp,
                        letterSpacing = (-0.6).sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    JoinLogoConfetti(left = false)
                }
            }
        }
        Text(
            "Link Google to pick up where you left off.",
            color = GwsPalette.Ink.copy(alpha = 0.68f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 13.dp)
        )
        Image(
            painter = painterResource(R.drawable.save_player_hero),
            contentDescription = "A saved player profile ready to return to the game",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(218.dp)
                .clip(RoundedCornerShape(26.dp))
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isAccountLinked) GwsPalette.Lime.copy(alpha = 0.35f) else Color.White,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onLinkGoogle,
                    enabled = !isAccountLinked,
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GwsPalette.Tangerine,
                        contentColor = GwsPalette.Ink,
                        disabledContainerColor = GwsPalette.Lime.copy(alpha = 0.55f),
                        disabledContentColor = GwsPalette.Ink
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("G", color = Color(0xFF4285F4), fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.size(8.dp))
                        Text(if (isAccountLinked) "Google account linked" else "Link with Google", fontWeight = FontWeight.Black)
                    }
                }
                TextButton(onClick = onRecoverGoogle, modifier = Modifier.padding(top = 3.dp)) {
                    Text("Already linked? Recover your player", color = GwsPalette.LavenderDeep, fontWeight = FontWeight.Bold)
                }
            }
        }

        statusMessage?.let { message ->
            Text(
                message,
                color = if (statusIsError) MaterialTheme.colorScheme.error else Color(0xFF32805A),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }

        OutlinedButton(
            onClick = onKeepPlayingAsGuest,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.72f),
                contentColor = GwsPalette.LavenderDeep
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 13.dp).height(52.dp)
        ) {
            Text("Keep playing as guest", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun JoinDisplayText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit,
    textAlign: TextAlign = TextAlign.Start,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.1.dp,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
    val style = TextStyle(
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontFamily = GwsDisplayFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = letterSpacing
    )
    Box(modifier = modifier) {
        Text(text, modifier = Modifier.clearAndSetSemantics { }, color = GwsPalette.Ink, style = style.copy(drawStyle = Stroke(width = strokeWidthPx)), textAlign = textAlign, maxLines = maxLines)
        Text(text, color = GwsPalette.Ink, style = style, textAlign = textAlign, maxLines = maxLines)
    }
}

@Composable
private fun JoinLogoConfetti(left: Boolean) {
    Canvas(Modifier.size(width = 19.dp, height = 58.dp)) {
        val stroke = 4.dp.toPx()
        if (left) {
            drawLine(Color(0xFFFFAA3D), Offset(size.width * 0.55f, size.height * 0.15f), Offset(size.width * 0.9f, size.height * 0.36f), stroke, StrokeCap.Round)
            drawLine(Color(0xFFFF72A7), Offset(size.width * 0.08f, size.height * 0.55f), Offset(size.width * 0.46f, size.height * 0.62f), stroke, StrokeCap.Round)
        } else {
            drawLine(Color(0xFF9DEBC1), Offset(size.width * 0.12f, size.height * 0.19f), Offset(size.width * 0.48f, size.height * 0.36f), stroke, StrokeCap.Round)
            drawLine(Color(0xFFFFAA3D), Offset(size.width * 0.57f, size.height * 0.1f), Offset(size.width * 0.92f, size.height * 0.34f), stroke, StrokeCap.Round)
            drawLine(Color(0xFF8F7CF7), Offset(size.width * 0.58f, size.height * 0.65f), Offset(size.width * 0.89f, size.height * 0.58f), stroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun SpotifyConnection(
    connected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = if (connected) onDisconnect else onConnect,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GwsPalette.Mint,
            contentColor = GwsPalette.Ink
        ),
        modifier = modifier.fillMaxWidth().height(66.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (connected) "Spotify connected" else "Spotify",
                    fontWeight = FontWeight.Black,
                    color = GwsPalette.Ink
                )
                Text(
                    if (connected) "Top songs are ready to use" else "Optional · get song suggestions",
                    style = MaterialTheme.typography.bodySmall,
                    color = GwsPalette.Ink.copy(alpha = 0.7f)
                )
            }
            Text(if (connected) "Disconnect" else "Connect", color = Color(0xFF126B39), fontWeight = FontWeight.Black)
        }
    }
}
