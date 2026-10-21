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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.R
import com.guesswhosesong.app.ui.components.AppearanceToggleButton
import com.guesswhosesong.app.ui.components.GuessWhoseSongWordmark
import com.guesswhosesong.app.ui.components.PlayerInitialBadge
import com.guesswhosesong.app.ui.components.SpotifyMark
import com.guesswhosesong.app.ui.components.SettingsHeaderButton
import com.guesswhosesong.shared.models.AvatarCustomization
import kotlinx.coroutines.delay

private val GwsDisplayFont = FontFamily(Font(R.font.comfortaa_bold, FontWeight.Bold))

@Composable
fun JoinScreen(
    viewModel: JoinViewModel = hiltViewModel(),
    incomingInviteCode: String? = null,
    onInviteHandled: () -> Unit = {},
    onNavigateToLobby: (joinCode: String, displayName: String, avatarCustomization: AvatarCustomization) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var showingPlayerSettings by rememberSaveable { mutableStateOf(false) }
    var profileDraftName by remember { mutableStateOf(uiState.displayName) }
    var profileDraftAvatar by remember { mutableStateOf(uiState.avatarCustomization) }
    var profileNameSaved by rememberSaveable { mutableStateOf(false) }
    var profileNameError by rememberSaveable { mutableStateOf<String?>(null) }
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> result.data?.let(viewModel::linkGoogle) }

    fun saveProfileDraft(): Boolean {
        val cleanName = profileDraftName.trim()
        if (cleanName.isBlank() || cleanName.length > 24 || cleanName.any(Char::isISOControl)) {
            profileNameSaved = false
            profileNameError = "Enter a name with 1 to 24 characters"
            return false
        }
        if (cleanName == viewModel.uiState.value.displayName.trim()) {
            profileNameError = null
            return true
        }
        val saved = viewModel.savePlayerProfile(cleanName, profileDraftAvatar)
        profileNameSaved = saved
        if (saved) profileDraftAvatar = viewModel.uiState.value.avatarCustomization
        profileNameError = if (saved) null else "Enter a name with 1 to 24 characters"
        return saved
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JoinEvent.NavigateToLobby -> onNavigateToLobby(event.joinCode, event.displayName, event.avatarCustomization)
                JoinEvent.OpenPlayerSettings -> {
                    viewModel.refreshSavedProfile()
                    profileDraftName = viewModel.uiState.value.displayName
                    profileDraftAvatar = viewModel.uiState.value.avatarCustomization
                    profileNameSaved = false
                    profileNameError = null
                    showingPlayerSettings = true
                }
            }
        }
    }

    LaunchedEffect(incomingInviteCode) {
        if (incomingInviteCode != null) {
            viewModel.onJoinCodeChanged(incomingInviteCode)
            viewModel.joinRoom()
            onInviteHandled()
        }
    }

    BackHandler(enabled = showingPlayerSettings) {
        saveProfileDraft()
        showingPlayerSettings = false
    }

    LaunchedEffect(showingPlayerSettings, profileDraftName) {
        if (!showingPlayerSettings) return@LaunchedEffect
        val cleanName = profileDraftName.trim()
        if (cleanName.isBlank() || cleanName.length > 24 || cleanName.any(Char::isISOControl)) {
            profileNameSaved = false
            profileNameError = "Enter a name with 1 to 24 characters"
            return@LaunchedEffect
        }
        if (cleanName == viewModel.uiState.value.displayName.trim()) {
            profileNameError = null
            return@LaunchedEffect
        }
        delay(500)
        saveProfileDraft()
    }

    WelcomePage(
        joinCode = uiState.joinCode,
        isLoading = uiState.isLoading || uiState.isServerStarting,
        error = uiState.error,
        onJoinCodeChange = {
            viewModel.onJoinCodeChanged(it)
        },
        onCreateRoom = {
            keyboard?.hide()
            viewModel.createRoom()
        },
        onJoinRoom = {
            keyboard?.hide()
            viewModel.joinRoom()
        },
        onSettings = {
            viewModel.openPlayerSettings()
        }
    )

    if (uiState.isServerStarting) {
        ServerStartingDialog()
    }

    if (showingPlayerSettings) {
        SavePlayerPage(
            draftName = profileDraftName,
            draftAvatar = profileDraftAvatar,
            nameSaved = profileNameSaved,
            nameError = profileNameError,
            onNameChange = { profileDraftName = it; profileNameSaved = false; profileNameError = null },
            isSpotifyConnected = uiState.isSpotifyConnected,
            isAccountLinked = uiState.isAccountLinked,
            statusMessage = uiState.accountStatus,
            statusIsError = uiState.accountStatusIsError,
            onLinkGoogle = {
                val activity = context as? Activity
                if (activity != null) googleLauncher.launch(viewModel.googleSignInIntent(activity))
            },
            onDisconnectGoogle = viewModel::disconnectGoogle,
            onConnectSpotify = viewModel::connectSpotify,
            onDisconnectSpotify = viewModel::disconnectSpotify,
            onRefreshSpotify = viewModel::refreshSpotifyStatus,
            onBackToGame = {
                saveProfileDraft()
                showingPlayerSettings = false
            }
        )
    }
}

@Composable
private fun ServerStartingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(42.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
                Text(
                    "Getting the game ready",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "This can take up to a minute after the game has been idle.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
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
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .statusBarsPadding().navigationBarsPadding().imePadding()) {
        val availableWidth = maxWidth
        val taglineFontSize = if (availableWidth < 400.dp) 24.sp else 25.sp
        val taglineLineHeight = if (availableWidth < 400.dp) 28.75.sp else 30.sp
        val actionPanelReserve = when {
            maxHeight < 620.dp -> 230.dp
            darkTheme -> 280.dp
            else -> 260.dp
        }
        WelcomeSceneryBackground(Modifier.fillMaxSize())
        Column(
            modifier = Modifier.widthIn(max = 620.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(bottom = actionPanelReserve)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 12.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GuessWhoseSongWordmark(
                    modifier = Modifier.weight(1f),
                    fontSize = 25.5.sp,
                    lineHeight = 29.5.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    AppearanceToggleButton()
                    SettingsHeaderButton(onClick = onSettings)
                }
            }
        if (darkTheme) {
            val darkHeadlineLineHeight = if (availableWidth < 400.dp) 31.dp else 33.dp
            val darkHeadlineHeight = 34.dp + darkHeadlineLineHeight * 2 + 10.dp
            // Keep the action panel at its calibrated round-8 y-position while
            // the taller art crop reaches its full 900px source bounds.
            val darkArtworkSlotHeight = availableWidth.coerceAtMost(620.dp) * (850f / 852f) - 10.dp
            Box(
                modifier = Modifier.fillMaxWidth().height(darkHeadlineHeight + darkArtworkSlotHeight)
            ) {
                Image(
                    painter = painterResource(R.drawable.welcome_hero_dark_scene),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.align(Alignment.TopCenter)
                        .offset(y = darkHeadlineHeight - 31.dp)
                        .fillMaxWidth()
                        .aspectRatio(852f / 900f)
                )
                WelcomeTagline(
                    fontSize = taglineFontSize,
                    lineHeight = taglineLineHeight,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .padding(top = 35.dp, bottom = 10.dp)
                )
            }
        } else {
            WelcomeTagline(
                fontSize = taglineFontSize,
                lineHeight = taglineLineHeight,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 35.dp, bottom = 10.dp)
            )
            Image(
                painter = painterResource(R.drawable.welcome_hero_light_scene),
                contentDescription = "Friends playing a music guessing game",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().offset(y = 2.5.dp).aspectRatio(852f / 768f)
                    .graphicsLayer { scaleY = 1.016f }
            )
        }
        }
        WelcomeRoomActions(
            joinCode = joinCode,
            isLoading = isLoading,
            error = error,
            onJoinCodeChange = onJoinCodeChange,
            onCreateRoom = onCreateRoom,
            onJoinRoom = onJoinRoom,
            darkTheme = darkTheme,
            modifier = Modifier.widthIn(max = 620.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = if (darkTheme) 16.dp else 15.dp)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun WelcomeTagline(
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    JoinDisplayText(
        text = "Your friends picked the songs.\nCan you guess who?",
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = (-1.42).sp,
        strokeWidth = 0.55.dp,
        modifier = modifier
    )
}

@Composable
private fun WelcomeRoomActions(
    joinCode: String,
    isLoading: Boolean,
    error: String?,
    onJoinCodeChange: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), shape = RoundedCornerShape(28.dp)) {
      BoxWithConstraints {
      val compactHeight = maxHeight < 620.dp
      Column(
          modifier = Modifier.padding(
              start = if (darkTheme) 15.dp else 15.dp,
              top = if (compactHeight) 10.dp else if (darkTheme) 23.dp else 16.dp,
              end = if (darkTheme) 15.dp else 15.dp,
              bottom = if (compactHeight) 10.dp else if (darkTheme) 18.dp else 13.dp
          ),
          verticalArrangement = Arrangement.spacedBy(if (compactHeight) 6.dp else if (darkTheme) 11.dp else 9.dp)
      ) {
        Button(
            onClick = onCreateRoom,
            enabled = !isLoading,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(if (compactHeight) 48.dp else 54.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFF8733), Color(0xFFFFA041))),
                    RoundedCornerShape(50)
                )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Create room",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = if (compactHeight) 18.sp else 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.CenterEnd).size(20.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = if (darkTheme) 0.dp else 5.5.dp)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "OR JOIN A ROOM",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }

        val roomCodeFill = if (darkTheme) Color(0xFF232932) else Color(0xFFF6F3EC)
        val roomCodeText = if (darkTheme) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF65636C)
        val roomCodeHint = if (darkTheme) roomCodeText else Color(0xFF96939D)
        val roomCodeBorder = if (darkTheme) Color(0xFF414652) else Color(0xFFE8E5DF)
        OutlinedTextField(
            value = joinCode,
            onValueChange = onJoinCodeChange,
            label = {
                Text(
                    "Room code",
                    modifier = Modifier.offset(x = if (darkTheme) 0.dp else 5.dp),
                    fontSize = if (compactHeight) 16.sp else if (darkTheme) 16.sp else 18.sp,
                    color = roomCodeHint
                )
            },
            placeholder = {
                Text(
                    "ABCD12",
                    modifier = Modifier.offset(x = if (darkTheme) 0.dp else 5.dp),
                    fontSize = if (compactHeight) 16.sp else if (darkTheme) 16.sp else 18.sp,
                    color = roomCodeHint
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = { onJoinRoom() }),
            shape = RoundedCornerShape(50.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = roomCodeText,
                unfocusedTextColor = roomCodeText,
                focusedContainerColor = roomCodeFill,
                unfocusedContainerColor = roomCodeFill,
                focusedBorderColor = roomCodeBorder,
                unfocusedBorderColor = roomCodeBorder,
                focusedLabelColor = roomCodeHint,
                unfocusedLabelColor = roomCodeHint,
                focusedPlaceholderColor = roomCodeHint,
                unfocusedPlaceholderColor = roomCodeHint
            ),
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = if (darkTheme) 1.5.dp else 0.dp)
                .height(if (compactHeight) 56.dp else if (darkTheme) 63.5.dp else 62.dp)
                .offset(y = if (darkTheme) (-7).dp else (-8).dp),
        )
        Button(
            onClick = onJoinRoom,
            enabled = !isLoading,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(if (compactHeight) 48.dp else 57.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF7055F5), Color(0xFF8065FF))),
                    RoundedCornerShape(50)
                )
        ) {
            Row(
                modifier = Modifier.offset(x = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Join room", fontSize = if (compactHeight) 18.sp else 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(21.dp))
            }
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
    }
}

@Composable
private fun WelcomeSceneryBackground(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    val dark = background.luminance() < 0.5f
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (dark) {
            drawRect(background)
            return@Canvas
        }
        val grassStart = h * 0.50f
        drawRect(background)

        fun cloud(centerX: Float, centerY: Float, width: Float, height: Float) {
            val color = if (dark) Color(0xFF30284F).copy(alpha = 0.55f) else Color(0xFFD9D2FF)
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

        cloud(w * 0.08f, h * 0.34f, w * 0.32f, h * 0.045f)
        cloud(w * 0.94f, h * 0.39f, w * 0.27f, h * 0.04f)
        drawCircle(if (dark) Color(0xFFFFD56A) else Color(0xFFFFC433), w.coerceAtMost(h) * 0.047f, Offset(w * 0.84f, h * 0.36f))

        drawRect(
            brush = Brush.verticalGradient(
                colors = if (dark) listOf(Color(0xFF152C29), Color(0xFF102321)) else listOf(Color(0xFFD1F07F), Color(0xFFB8E86F)),
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
        drawPath(path = hill, color = if (dark) Color(0xFF19312F) else Color(0xFFDDF49B))

        val tufts = listOf(
            0.08f to 0.84f, 0.29f to 0.93f, 0.53f to 0.86f, 0.76f to 0.96f, 0.94f to 0.83f,
            0.16f to 0.99f, 0.41f to 0.91f, 0.68f to 0.98f, 0.88f to 0.89f
        )
        val tuftStroke = (w * 0.007f).coerceIn(1.5f, 3.5f)
        tufts.forEach { (x, y) ->
            val base = Offset(w * x, h * y)
            val height = (w * 0.025f).coerceIn(7f, 15f)
            val color = if (dark) Color(0xFF3C7659) else Color(0xFF5C9E43)
            drawLine(color, base, Offset(base.x - height * 0.42f, base.y - height), tuftStroke, StrokeCap.Round)
            drawLine(color, base, Offset(base.x, base.y - height * 1.25f), tuftStroke, StrokeCap.Round)
            drawLine(color, base, Offset(base.x + height * 0.45f, base.y - height * 0.92f), tuftStroke, StrokeCap.Round)
        }
        drawCircle(if (dark) Color(0xFF17322B) else Color(0xFF89C86B), w * 0.12f, Offset(0f, h * 1.02f))
        drawCircle(if (dark) Color(0xFF17322B) else Color(0xFF89C86B), w * 0.13f, Offset(w, h * 1.02f))
    }
}

@Composable
internal fun SavePlayerPage(
    draftName: String,
    draftAvatar: AvatarCustomization,
    nameSaved: Boolean,
    nameError: String?,
    onNameChange: (String) -> Unit,
    isSpotifyConnected: Boolean,
    isAccountLinked: Boolean,
    statusMessage: String?,
    statusIsError: Boolean,
    onLinkGoogle: () -> Unit,
    onDisconnectGoogle: () -> Unit,
    onConnectSpotify: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    onRefreshSpotify: () -> Unit,
    onBackToGame: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val darkTheme = colorScheme.background.luminance() < 0.5f
    Dialog(
        onDismissRequest = onBackToGame,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val compactScreen = maxHeight < 760.dp
            val popupMaxHeight = (maxHeight * 0.86f).coerceAtMost(680.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .heightIn(max = popupMaxHeight)
                    .padding(horizontal = 18.dp),
                color = colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = popupMaxHeight)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(if (compactScreen) 14.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compactScreen) 12.dp else 16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Player settings",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        TextButton(onClick = onBackToGame) { Text("Done") }
                    }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.surface.copy(alpha = if (darkTheme) 0.9f else 0.84f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.32f))
                ) {
                    Column(modifier = Modifier.padding(if (compactScreen) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlayerInitialBadge(
                                displayName = draftName,
                                customization = draftAvatar,
                                size = if (compactScreen) 48.dp else 60.dp
                            )
                            OutlinedTextField(
                                value = draftName,
                                onValueChange = { onNameChange(it.take(24)) },
                                placeholder = { Text("Display name") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    imeAction = ImeAction.Done
                                ),
                                trailingIcon = if (nameSaved) {
                                    { Icon(Icons.Default.Check, contentDescription = "Name saved", tint = Color(0xFF35C76F)) }
                                } else null,
                                modifier = Modifier.weight(1f).widthIn(min = 0.dp)
                            )
                        }
                        nameError?.let {
                            Text(it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                    Column(verticalArrangement = Arrangement.spacedBy(if (compactScreen) 14.dp else 16.dp)) {
                        GoogleConnectionCard(
                            connected = isAccountLinked,
                            onManage = if (isAccountLinked) onDisconnectGoogle else onLinkGoogle,
                            compact = compactScreen
                        )
                        SpotifyProfileCard(
                            connected = isSpotifyConnected,
                            onManage = if (isSpotifyConnected) onDisconnectSpotify else onConnectSpotify,
                            onRefresh = onRefreshSpotify,
                            compact = compactScreen
                        )
                    }
                statusMessage?.takeIf { statusIsError }?.let { message ->
                    Surface(color = colorScheme.errorContainer, shape = RoundedCornerShape(18.dp)) {
                        Text(
                            message,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            color = colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

}

@Composable
private fun GoogleConnectionCard(
    connected: Boolean,
    onManage: () -> Unit,
    compact: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val darkTheme = colorScheme.background.luminance() < 0.5f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (darkTheme) colorScheme.surface else colorScheme.surface.copy(alpha = 0.84f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = if (darkTheme) 0.34f else 0.18f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(if (compact) 42.dp else 54.dp), color = Color.White, shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) { GoogleGMark(if (compact) 23.dp else 28.dp) }
            }
            Text(
                "Google account",
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            androidx.compose.material3.TextButton(
                onClick = onManage,
                modifier = Modifier.height(if (compact) 38.dp else 42.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colorScheme.primaryContainer.copy(alpha = if (darkTheme) 0.55f else 0.42f),
                    contentColor = colorScheme.primary
                )
            ) {
                Text(if (connected) "Disconnect" else "Connect", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpotifyProfileCard(
    connected: Boolean,
    onManage: () -> Unit,
    onRefresh: () -> Unit,
    compact: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val darkTheme = colorScheme.background.luminance() < 0.5f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (darkTheme) colorScheme.surface else colorScheme.surface.copy(alpha = 0.84f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = if (darkTheme) 0.34f else 0.18f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(if (compact) 42.dp else 54.dp), color = Color.White, shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) { SpotifyMark() }
            }
            Text(
                "Spotify",
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            androidx.compose.material3.TextButton(
                onClick = onManage,
                modifier = Modifier.height(if (compact) 38.dp else 42.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colorScheme.primaryContainer.copy(alpha = if (darkTheme) 0.55f else 0.42f),
                    contentColor = colorScheme.primary
                )
            ) {
                Text(if (connected) "Disconnect" else "Connect", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(42.dp)
                    .border(1.dp, colorScheme.outlineVariant, CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(Icons.Default.Sync, contentDescription = "Sync Spotify status", tint = colorScheme.primary)
            }
        }
    }
}

@Composable
internal fun JoinDisplayText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit,
    textAlign: TextAlign = TextAlign.Start,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.1.dp,
    maxLines: Int = Int.MAX_VALUE,
    textOffsetX: androidx.compose.ui.unit.Dp = 0.dp,
    textOffsetY: androidx.compose.ui.unit.Dp = 0.dp,
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
    val textColor = MaterialTheme.colorScheme.onBackground
    Box(modifier = modifier) {
        Text(text, modifier = Modifier.clearAndSetSemantics { }.offset(x = textOffsetX, y = textOffsetY), color = textColor, style = style.copy(drawStyle = Stroke(width = strokeWidthPx)), textAlign = textAlign, maxLines = maxLines)
        Text(text, modifier = Modifier.offset(x = textOffsetX, y = textOffsetY), color = textColor, style = style, textAlign = textAlign, maxLines = maxLines)
    }
}

@Composable
private fun GoogleGMark(sizeDp: androidx.compose.ui.unit.Dp) {
    Image(
        painter = painterResource(R.drawable.google_g_logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(sizeDp)
    )
}
