package com.guesswhosesong.app.ui.screens.join

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
    var showingSettings by rememberSaveable { mutableStateOf(false) }

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

    LaunchedEffect(showingSettings) {
        if (showingSettings) viewModel.refreshSavedProfile()
    }

    BackHandler(enabled = showingSettings) { showingSettings = false }

    if (showingSettings) {
        SavePlayerPage(
            displayName = uiState.displayName,
            avatarCustomization = uiState.avatarCustomization,
            isSpotifyConnected = uiState.isSpotifyConnected,
            isAccountLinked = uiState.isAccountLinked,
            statusMessage = uiState.accountStatus,
            statusIsError = uiState.accountStatusIsError,
            error = uiState.error,
            onLinkGoogle = {
                val activity = context as? Activity
                if (activity != null) googleLauncher.launch(viewModel.googleSignInIntent(activity))
            },
            onRecoverGoogle = {
                val activity = context as? Activity
                if (activity != null) recoveryLauncher.launch(viewModel.googleSignInIntent(activity))
            },
            onConnectSpotify = viewModel::connectSpotify,
            onDisconnectSpotify = viewModel::disconnectSpotify,
            onSaveProfile = { name, avatar ->
                if (viewModel.savePlayerProfile(name, avatar)) showingSettings = false
            },
            onBackToGame = { showingSettings = false }
        )
    } else {
        WelcomePage(
            joinCode = uiState.joinCode,
            isLoading = uiState.isLoading,
            error = uiState.error,
            onJoinCodeChange = viewModel::onJoinCodeChanged,
            onCreateRoom = {
                keyboard?.hide()
                viewModel.createRoom()
            },
            onJoinRoom = {
                keyboard?.hide()
                viewModel.joinRoom()
            },
            onSettings = { showingSettings = true }
        )
    }
}

@Composable
internal fun WelcomePage(
    joinCode: String,
    isLoading: Boolean,
    error: String?,
    onJoinCodeChange: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onSettings: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(GwsPalette.Paper, GwsPalette.Paper, Color(0xFFD1F07F))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        WelcomeSceneryBackground(Modifier.fillMaxSize())
        val compactHeader = maxWidth < 360.dp
        val wideLayout = maxWidth >= 760.dp
        val sceneMinHeight = (maxHeight - 92.dp).coerceIn(460.dp, 700.dp)
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 3.dp)) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                if (!compactHeader) JoinLogoConfetti(left = true)
                JoinDisplayText(
                    "Guess Whose Song",
                    fontSize = if (compactHeader) 22.sp else 24.sp,
                    lineHeight = if (compactHeader) 27.sp else 30.sp,
                    letterSpacing = (-0.7).sp,
                    strokeWidth = 2.dp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                if (!compactHeader) JoinLogoConfetti(left = false)
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier.align(Alignment.CenterEnd).size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GwsPalette.Lavender.copy(alpha = 0.32f))
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = GwsPalette.Ink, modifier = Modifier.size(22.dp))
            }
        }
        Text(
            "Create a room or join your friends.",
            color = GwsPalette.Ink.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 7.dp)
        )
        if (wideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 1280.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.welcome_hero),
                    contentDescription = "Friends playing a music guessing game",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1.2f).aspectRatio(1.5f)
                )
                WelcomeRoomActions(
                    joinCode, isLoading, error, onJoinCodeChange, onCreateRoom, onJoinRoom,
                    modifier = Modifier.weight(0.8f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = sceneMinHeight)
            ) {
                Image(
                    painter = painterResource(R.drawable.welcome_hero),
                    contentDescription = "Friends playing a music guessing game",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.5f)
                )
                WelcomeRoomActions(
                    joinCode, isLoading, error, onJoinCodeChange, onCreateRoom, onJoinRoom,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }

        }
    }
}

@Composable
private fun WelcomeRoomActions(
    joinCode: String,
    isLoading: Boolean,
    error: String?,
    onJoinCodeChange: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onCreateRoom,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF983D),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC96B24))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Create a room", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onJoinRoom,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GwsPalette.Lavender.copy(alpha = 0.74f),
                contentColor = GwsPalette.Ink
            ),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7769D9))
        ) {
            Text("Join a room", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }

        error?.let { message ->
            Text(
                message,
                color = if (message.contains("linked", ignoreCase = true) || message.contains("recovered", ignoreCase = true)) Color(0xFF32805A) else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun WelcomeSceneryBackground(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val grassStart = h * 0.48f
        drawRect(GwsPalette.Paper)

        fun cloud(centerX: Float, centerY: Float, width: Float, height: Float) {
            val color = Color(0xFFD9D2FF)
            drawRoundRect(
                color = color,
                topLeft = Offset(centerX - width * 0.48f, centerY - height * 0.02f),
                size = Size(width * 0.96f, height * 0.48f),
                cornerRadius = CornerRadius(height * 0.2f)
            )
            drawCircle(color, height * 0.35f, Offset(centerX - width * 0.25f, centerY - height * 0.12f))
            drawCircle(color, height * 0.48f, Offset(centerX, centerY - height * 0.24f))
            drawCircle(color, height * 0.34f, Offset(centerX + width * 0.27f, centerY - height * 0.1f))
        }

        cloud(w * 0.08f, h * 0.29f, w * 0.32f, h * 0.045f)
        cloud(w * 0.94f, h * 0.34f, w * 0.27f, h * 0.04f)
        drawCircle(Color(0xFFFFC433), w.coerceAtMost(h) * 0.047f, Offset(w * 0.84f, h * 0.31f))

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFD1F07F), Color(0xFFB8E86F)),
                startY = grassStart,
                endY = h
            ),
            topLeft = Offset(0f, grassStart),
            size = Size(w, h - grassStart)
        )
        val hill = Path().apply {
            moveTo(0f, h * 0.57f)
            cubicTo(w * 0.24f, h * 0.52f, w * 0.39f, h * 0.63f, w * 0.56f, h * 0.57f)
            cubicTo(w * 0.78f, h * 0.5f, w * 0.9f, h * 0.62f, w, h * 0.55f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = hill, color = Color(0xFFDDF49B))

        val tufts = listOf(
            0.08f to 0.84f, 0.29f to 0.93f, 0.53f to 0.86f, 0.76f to 0.96f, 0.94f to 0.83f,
            0.16f to 0.99f, 0.41f to 0.91f, 0.68f to 0.98f, 0.88f to 0.89f
        )
        val tuftStroke = (w * 0.007f).coerceIn(1.5f, 3.5f)
        tufts.forEach { (x, y) ->
            val base = Offset(w * x, h * y)
            val height = (w * 0.025f).coerceIn(7f, 15f)
            val color = Color(0xFF5C9E43)
            drawLine(color, base, Offset(base.x - height * 0.42f, base.y - height), tuftStroke, StrokeCap.Round)
            drawLine(color, base, Offset(base.x, base.y - height * 1.25f), tuftStroke, StrokeCap.Round)
            drawLine(color, base, Offset(base.x + height * 0.45f, base.y - height * 0.92f), tuftStroke, StrokeCap.Round)
        }
        drawCircle(Color(0xFF89C86B), w * 0.12f, Offset(0f, h * 1.02f))
        drawCircle(Color(0xFF89C86B), w * 0.13f, Offset(w, h * 1.02f))
    }
}

@Composable
internal fun SavePlayerPage(
    displayName: String,
    avatarCustomization: AvatarCustomization,
    isSpotifyConnected: Boolean,
    isAccountLinked: Boolean,
    statusMessage: String?,
    statusIsError: Boolean,
    error: String?,
    onLinkGoogle: () -> Unit,
    onRecoverGoogle: () -> Unit,
    onConnectSpotify: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    onSaveProfile: (String, AvatarCustomization) -> Unit,
    onBackToGame: () -> Unit,
) {
    var draftName by remember(displayName) { mutableStateOf(displayName) }
    var draftAvatar by remember(displayName) { mutableStateOf(avatarCustomization) }

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
                Text("← Back", color = GwsPalette.LavenderDeep, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 3.dp)) {
            val compact = maxWidth < 350.dp
            if (compact) {
                JoinDisplayText(
                    "Settings",
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
                        "Settings",
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
            "Make your player yours, then connect the services you use.",
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
                .height(148.dp)
                .clip(RoundedCornerShape(26.dp))
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Your player", style = MaterialTheme.typography.titleMedium, color = GwsPalette.Ink, fontWeight = FontWeight.Black)
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it.take(24) },
                    label = { Text("Display name") },
                    placeholder = { Text("dashingbuilder") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                AvatarPicker(
                    customization = draftAvatar,
                    onPresetSelected = { shapeId -> draftAvatar = AvatarCustomization.defaultsFor(shapeId) },
                    onCustomizationChanged = { draftAvatar = it },
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        SpotifyConnection(
            connected = isSpotifyConnected,
            onConnect = onConnectSpotify,
            onDisconnect = onDisconnectSpotify,
            modifier = Modifier.padding(top = 2.dp)
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
                        containerColor = Color.White,
                        contentColor = GwsPalette.Ink,
                        disabledContainerColor = GwsPalette.Lime.copy(alpha = 0.55f),
                        disabledContentColor = GwsPalette.Ink
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.12f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GoogleGMark()
                        Spacer(Modifier.size(10.dp))
                        Text(if (isAccountLinked) "Google account linked" else "Link Google account", fontWeight = FontWeight.Black)
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

        error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }

        Button(
            onClick = { onSaveProfile(draftName, draftAvatar) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GwsPalette.Tangerine, contentColor = GwsPalette.Ink),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp)
        ) {
            Text("Save settings", fontWeight = FontWeight.ExtraBold)
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
private fun GoogleGMark() {
    Canvas(Modifier.size(22.dp)) {
        val strokeWidth = 3.5.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val ringStroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        drawArc(Color(0xFF4285F4), -43f, 86f, false, topLeft, arcSize, style = ringStroke)
        drawArc(Color(0xFF34A853), 47f, 86f, false, topLeft, arcSize, style = ringStroke)
        drawArc(Color(0xFFFBBC05), 137f, 86f, false, topLeft, arcSize, style = ringStroke)
        drawArc(Color(0xFFEA4335), 227f, 86f, false, topLeft, arcSize, style = ringStroke)
        drawLine(
            Color(0xFF4285F4),
            Offset(size.width / 2f, size.height / 2f),
            Offset(size.width - strokeWidth / 2f, size.height / 2f),
            strokeWidth,
            StrokeCap.Butt
        )
    }
}

@Composable
private fun SpotifyMark() {
    Canvas(Modifier.size(27.dp)) {
        drawCircle(Color(0xFF1DB954))
        val lineColor = Color(0xFF101010)
        val stroke = size.minDimension * 0.075f
        listOf(
            Triple(0.27f, 0.39f, 0.74f),
            Triple(0.31f, 0.51f, 0.68f),
            Triple(0.36f, 0.63f, 0.58f)
        ).forEach { (left, y, right) ->
            val wave = Path().apply {
                moveTo(size.width * left, size.height * y)
                quadraticTo(size.width * 0.52f, size.height * (y - 0.10f), size.width * right, size.height * y)
            }
            drawPath(wave, lineColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
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
            containerColor = Color.White,
            contentColor = GwsPalette.Ink
        ),
        modifier = modifier.fillMaxWidth().height(66.dp).border(1.dp, Color(0xFF1DB954), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpotifyMark()
            Spacer(Modifier.width(11.dp))
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
