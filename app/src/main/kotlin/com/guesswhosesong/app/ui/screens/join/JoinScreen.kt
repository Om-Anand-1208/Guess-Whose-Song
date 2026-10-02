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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.R
import com.guesswhosesong.app.ui.components.AvatarCharacter
import com.guesswhosesong.app.ui.components.AppearanceToggleButton
import com.guesswhosesong.app.ui.components.GuessWhoseSongWordmark
import com.guesswhosesong.app.ui.components.SettingsHeaderButton
import com.guesswhosesong.app.ui.theme.AppearanceModePicker
import com.guesswhosesong.app.ui.theme.LocalAppearanceSettings
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
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
    var showingPlayerSettings by rememberSaveable { mutableStateOf(false) }
    var showingCharacterEditor by rememberSaveable { mutableStateOf(false) }
    var showingAppearanceMenu by rememberSaveable { mutableStateOf(false) }
    var scanError by rememberSaveable { mutableStateOf<String?>(null) }
    var profileDraftName by remember { mutableStateOf(uiState.displayName) }
    var profileDraftAvatar by remember { mutableStateOf(uiState.avatarCustomization) }
    var characterDraftAvatar by remember { mutableStateOf(uiState.avatarCustomization) }
    val scannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }

    val scanRoomCode: () -> Unit = {
        GmsBarcodeScanning.getClient(context, scannerOptions).startScan()
            .addOnSuccessListener { barcode ->
                val code = JoinCodeParser.parse(barcode.rawValue)
                if (code == null) {
                    scanError = "This QR code does not contain a valid room code."
                } else {
                    scanError = null
                    viewModel.onJoinCodeChanged(code)
                }
            }
            .addOnFailureListener {
                scanError = "QR scanner is unavailable. Check Google Play services and try again."
            }
            .addOnCanceledListener { /* Keep the current room code unchanged. */ }
    }

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

    BackHandler(enabled = showingCharacterEditor || showingPlayerSettings || showingAppearanceMenu) {
        when {
            showingCharacterEditor -> showingCharacterEditor = false
            showingAppearanceMenu -> showingAppearanceMenu = false
            else -> showingPlayerSettings = false
        }
    }

    if (showingCharacterEditor) {
        CharacterEditorPage(
            customization = characterDraftAvatar,
            statusMessage = uiState.accountStatus,
            statusIsError = uiState.accountStatusIsError,
            onCustomizationChange = { characterDraftAvatar = it },
            onCancel = { showingCharacterEditor = false },
            onSave = {
                profileDraftAvatar = characterDraftAvatar
                showingCharacterEditor = false
            }
        )
    } else if (showingPlayerSettings) {
        SavePlayerPage(
            draftName = profileDraftName,
            draftAvatar = profileDraftAvatar,
            onNameChange = { profileDraftName = it },
            onEditCharacter = {
                characterDraftAvatar = profileDraftAvatar
                showingCharacterEditor = true
            },
            isSpotifyConnected = uiState.isSpotifyConnected,
            isAccountLinked = uiState.isAccountLinked,
            statusMessage = uiState.accountStatus,
            statusIsError = uiState.accountStatusIsError,
            error = null,
            onLinkGoogle = {
                val activity = context as? Activity
                if (activity != null) googleLauncher.launch(viewModel.googleSignInIntent(activity))
            },
            onRecoverGoogle = {
                val activity = context as? Activity
                if (activity != null) recoveryLauncher.launch(viewModel.googleSignInIntent(activity))
            },
            onDisconnectGoogle = viewModel::disconnectGoogle,
            onConnectSpotify = viewModel::connectSpotify,
            onDisconnectSpotify = viewModel::disconnectSpotify,
            onRefreshSpotify = viewModel::refreshSpotifyStatus,
            onSaveProfile = { name, avatar ->
                if (viewModel.savePlayerProfile(name, avatar)) {
                    profileDraftName = name.trim()
                    profileDraftAvatar = avatar
                    showingPlayerSettings = false
                }
            },
            onBackToGame = { showingPlayerSettings = false }
        )
    } else {
        WelcomePage(
            joinCode = uiState.joinCode,
            isLoading = uiState.isLoading,
            error = scanError ?: uiState.error,
            onJoinCodeChange = {
                scanError = null
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
            onScanRoomCode = scanRoomCode,
            onSettings = { showingAppearanceMenu = true }
        )
    }

    if (showingAppearanceMenu) {
        JoinAppearanceMenu(
            onDismiss = { showingAppearanceMenu = false },
            onOpenPlayer = {
                viewModel.refreshSavedProfile()
                profileDraftName = viewModel.uiState.value.displayName
                profileDraftAvatar = viewModel.uiState.value.avatarCustomization
                showingAppearanceMenu = false
                showingPlayerSettings = true
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun JoinAppearanceMenu(onDismiss: () -> Unit, onOpenPlayer: () -> Unit) {
    val appearance = LocalAppearanceSettings.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            AppearanceModePicker(appearance.mode, appearance.setMode)
            OutlinedButton(
                onClick = onOpenPlayer,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Your player", modifier = Modifier.weight(1f), textAlign = TextAlign.Start, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
            Spacer(Modifier.height(8.dp))
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
    onScanRoomCode: () -> Unit,
    onSettings: () -> Unit
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .statusBarsPadding().navigationBarsPadding().imePadding()) {
        val availableWidth = maxWidth
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
                JoinDisplayText(
                    "Your friends picked the songs.\nCan you guess who?",
                    fontSize = if (availableWidth < 400.dp) 24.sp else 25.sp,
                    lineHeight = if (availableWidth < 400.dp) 28.75.sp else 30.sp,
                    letterSpacing = (-1.42).sp,
                    strokeWidth = 0.55.dp,
                    modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 35.dp, bottom = 10.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = if (availableWidth < 380.dp) 24.dp else 32.dp).padding(top = 31.5.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val headlineSize = if (availableWidth < 380.dp) 32.sp else 35.sp
                val headlineLine = if (availableWidth < 380.dp) 38.sp else 40.sp
                val questionSize = when {
                    availableWidth < 360.dp -> 28.sp
                    availableWidth < 400.dp -> 30.25.sp
                    else -> 33.sp
                }
                val questionLine = when {
                    availableWidth < 360.dp -> 33.sp
                    availableWidth < 400.dp -> 35.sp
                    else -> 38.sp
                }
                JoinDisplayText(
                    "Your friends picked",
                    fontSize = headlineSize,
                    lineHeight = headlineLine,
                    letterSpacing = (-1.4).sp,
                    strokeWidth = 1.5.dp,
                    textOffsetY = 1.dp
                )
                JoinDisplayText(
                    "the songs.",
                    fontSize = headlineSize,
                    lineHeight = headlineLine,
                    letterSpacing = (-1.4).sp,
                    strokeWidth = 1.5.dp,
                    textOffsetY = 1.dp,
                    modifier = Modifier.offset(y = (-3.25).dp)
                )
                JoinDisplayText(
                    "Can you guess who?",
                    fontSize = questionSize,
                    lineHeight = questionLine,
                    letterSpacing = (-1.17).sp,
                    textAlign = TextAlign.Center,
                    strokeWidth = 1.5.dp,
                    maxLines = 1,
                    textOffsetX = 4.dp,
                    textOffsetY = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.976f)
                        .offset(x = (-13).dp, y = (-1.75).dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 2.25.dp)
                )
            }
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
            onScanRoomCode = onScanRoomCode,
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
private fun ScanCornersIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.105f
        val inset = size.minDimension * 0.16f
        val arm = size.minDimension * 0.25f
        val right = size.width - inset
        val bottom = size.height - inset

        drawLine(color, Offset(inset, inset + arm), Offset(inset, inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(inset, inset), Offset(inset + arm, inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(right - arm, inset), Offset(right, inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(right, inset), Offset(right, inset + arm), stroke, StrokeCap.Round)
        drawLine(color, Offset(inset, bottom - arm), Offset(inset, bottom), stroke, StrokeCap.Round)
        drawLine(color, Offset(inset, bottom), Offset(inset + arm, bottom), stroke, StrokeCap.Round)
        drawLine(color, Offset(right - arm, bottom), Offset(right, bottom), stroke, StrokeCap.Round)
        drawLine(color, Offset(right, bottom), Offset(right, bottom - arm), stroke, StrokeCap.Round)
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
    onScanRoomCode: () -> Unit,
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
            trailingIcon = {
                if (!darkTheme) {
                    IconButton(onClick = onScanRoomCode, enabled = !isLoading) {
                        ScanCornersIcon(
                            Modifier.size(if (darkTheme) 27.dp else 28.dp)
                                .offset(x = (-4).dp, y = if (darkTheme) (-3.5).dp else (-2).dp),
                            color = Color(0xFF65636C)
                        )
                    }
                }
            }
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
    onNameChange: (String) -> Unit,
    onEditCharacter: () -> Unit,
    isSpotifyConnected: Boolean,
    isAccountLinked: Boolean,
    statusMessage: String?,
    statusIsError: Boolean,
    error: String?,
    onLinkGoogle: () -> Unit,
    onRecoverGoogle: () -> Unit,
    onDisconnectGoogle: () -> Unit,
    onConnectSpotify: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    onRefreshSpotify: () -> Unit,
    onSaveProfile: (String, AvatarCustomization) -> Unit,
    onBackToGame: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val darkTheme = colorScheme.background.luminance() < 0.5f
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        val compactScreen = maxHeight < 760.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    start = if (compactScreen) 12.dp else if (darkTheme) 14.dp else 18.5.dp,
                    top = if (compactScreen) 4.dp else 8.dp,
                    end = if (compactScreen) 12.dp else if (darkTheme) 14.dp else 18.5.dp,
                    bottom = if (compactScreen || darkTheme) 6.dp else 0.dp
                )
        ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GuessWhoseSongWordmark(
                modifier = Modifier.weight(1f),
                fontSize = if (compactScreen) 22.sp else 25.5.sp,
                lineHeight = if (compactScreen) 25.sp else 29.5.sp
            )
            TextButton(onClick = onBackToGame, modifier = Modifier.offset(y = (-5).dp)) {
                if (darkTheme) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = colorScheme.primary)
                }
                Text("Back", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .padding(top = if (compactScreen) 6.dp else if (darkTheme) 37.dp else 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (compactScreen) 6.dp else if (darkTheme) 10.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier.offset(y = if (compactScreen) 0.dp else if (darkTheme) (-4).dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = if (darkTheme) 10.dp else 6.dp)) {
                    Text(
                        "Your player",
                        style = if (compactScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                        color = colorScheme.onBackground
                    )
                }
                if (!compactScreen) PlayerHeadingArtwork(darkTheme)
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = if (compactScreen) 6.dp else if (darkTheme) 16.dp else 14.dp),
                color = colorScheme.surface.copy(alpha = if (darkTheme) 0.9f else 0.8f),
                shape = RoundedCornerShape(30.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = if (darkTheme) 15.dp else 11.dp,
                        end = if (darkTheme) 15.dp else 10.dp,
                        top = if (compactScreen) 9.dp else if (darkTheme) 18.dp else 14.dp,
                        bottom = if (compactScreen) 9.dp else if (darkTheme) 18.dp else 14.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compactScreen) 8.dp else if (darkTheme) 16.dp else 18.dp)
                ) {
                    Surface(
                            modifier = Modifier
                                .offset(y = if (darkTheme) (-5).dp else 0.dp)
                                .size(
                                    width = if (compactScreen) 76.dp else if (darkTheme) 108.dp else 130.dp,
                                    height = if (compactScreen) 88.dp else if (darkTheme) 125.dp else 132.dp
                                ),
                        color = Color(0xFFFFF2D5),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ProfileAvatarGround()
                            AvatarCharacter(
                                draftAvatar.shapeId,
                                Modifier.size(if (compactScreen) 76.dp else if (darkTheme) 108.dp else 116.dp)
                                    .offset(y = if (compactScreen) (-4).dp else if (darkTheme) (-8.5).dp else (-4).dp),
                                customization = draftAvatar
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(if (compactScreen) 4.dp else if (darkTheme) 5.dp else 0.dp)
                    ) {
                        Text(
                            "Display name",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .offset(x = if (darkTheme) (-1.5).dp else 0.dp, y = if (darkTheme) (-1.5).dp else 0.dp)
                        )
                        if (darkTheme) {
                            Row(
                                modifier = Modifier
                                    .offset(x = (-1.5).dp, y = (-1.5).dp)
                                    .fillMaxWidth()
                                    .height(if (compactScreen) 40.dp else 46.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(50.dp))
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = draftName,
                                    onValueChange = { onNameChange(it.take(24)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        imeAction = ImeAction.Done
                                    ),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = colorScheme.onSurface,
                                        lineHeight = 20.sp
                                    ),
                                    cursorBrush = SolidColor(colorScheme.primary),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (compactScreen) 40.dp else 44.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = draftName,
                                    onValueChange = { onNameChange(it.take(24)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        imeAction = ImeAction.Done
                                    ),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = colorScheme.onSurface,
                                        lineHeight = 20.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (!darkTheme && !compactScreen) Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onEditCharacter,
                            modifier = Modifier.padding(top = if (darkTheme && !compactScreen) 9.dp else 0.dp).fillMaxWidth().height(if (compactScreen) 42.dp else 50.dp),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primaryContainer,
                                contentColor = colorScheme.primary
                            )
                        ) {
                            if (darkTheme) {
                                ProfileShirtIcon(Modifier.offset(x = (-9).dp).size(22.dp), color = colorScheme.primary)
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Edit character",
                                modifier = Modifier.weight(1f).then(if (darkTheme) Modifier.padding(start = 2.dp) else Modifier),
                                color = if (darkTheme) Color(0xFFF5F4F8) else colorScheme.primary,
                                textAlign = if (darkTheme) TextAlign.Start else TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = if (darkTheme) Modifier.offset(x = 14.dp) else Modifier,
                                tint = if (darkTheme) Color(0xFFF5F4F8) else colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = if (compactScreen) 0.dp else 4.dp, bottom = if (compactScreen) 0.dp else if (darkTheme) 2.dp else 4.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = if (darkTheme) 10.dp else 6.dp)) {
                    Text(
                        "Connections",
                        style = if (compactScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                        color = colorScheme.onBackground
                    )
                }
                if (!compactScreen) MusicNotesDecoration(darkTheme)
            }
            GoogleConnectionCard(
                connected = isAccountLinked,
                onManage = if (isAccountLinked) onDisconnectGoogle else onLinkGoogle,
                onRecover = onRecoverGoogle,
                compact = compactScreen
            )
            SpotifyProfileCard(
                connected = isSpotifyConnected,
                onManage = if (isSpotifyConnected) onDisconnectSpotify else onConnectSpotify,
                onRefresh = onRefreshSpotify,
                compact = compactScreen,
                modifier = Modifier.offset(y = if (darkTheme) 0.dp else 4.dp)
            )
            statusMessage?.takeIf { statusIsError }?.let { message ->
                Surface(
                    color = if (statusIsError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        message,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = if (statusIsError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = if (darkTheme) 5.dp else 0.dp, end = if (darkTheme) 1.dp else 0.dp)
            .padding(top = if (compactScreen) 6.dp else 10.dp, bottom = if (compactScreen) 2.dp else 4.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactScreen) 8.dp else 10.dp)
        ) {
            OutlinedButton(
                onClick = onBackToGame,
                modifier = Modifier.weight(if (darkTheme) 0.8f else 1f).height(if (compactScreen) 48.dp else 56.dp),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
            Button(
                onClick = { onSaveProfile(draftName, draftAvatar) },
                modifier = Modifier.weight(1f).height(if (compactScreen) 48.dp else 56.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFFF8733), Color(0xFFFFA041))),
                        RoundedCornerShape(50)
                    ),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF17191F)
                )
        ) { Text("Save settings", fontWeight = FontWeight.ExtraBold) }
        }
    }
}
}

@Composable
private fun PlayerHeadingArtwork(darkTheme: Boolean) {
    if (darkTheme) {
        PlayerHeadingCharacters(brightLimbs = true)
        return
    }
    Box(
        modifier = Modifier.size(width = 108.dp, height = 84.dp).offset(x = (-3).dp, y = 12.dp)
    ) {
        Canvas(
            Modifier.size(width = 108.dp, height = 78.dp).offset(y = 6.dp).graphicsLayer {
                transformOrigin = TransformOrigin(0f, 1f)
                scaleX = 0.99f
                scaleY = 0.85f
                translationY = 2.dp.toPx()
            }
        ) {
            val blob = Color(0xFFFFEFC9)
            drawRoundRect(
                blob,
                topLeft = Offset(size.width * 0.06f, size.height * 0.40f),
                size = Size(size.width * 0.88f, size.height * 0.46f),
                cornerRadius = CornerRadius(size.height * 0.23f)
            )
            drawCircle(blob, size.height * 0.22f, Offset(size.width * 0.30f, size.height * 0.52f))
            drawCircle(blob, size.height * 0.30f, Offset(size.width * 0.54f, size.height * 0.43f))
            drawCircle(blob, size.height * 0.23f, Offset(size.width * 0.76f, size.height * 0.56f))
        }

        Canvas(Modifier.size(width = 108.dp, height = 78.dp).offset(y = 6.dp)) {
            val greenWave = Path().apply {
                moveTo(size.width * 0.25f, size.height * 0.4535f)
                cubicTo(
                    size.width * 0.338f, size.height * 0.173f,
                    size.width * 0.408f, size.height * 0.80f,
                    size.width * 0.505f, size.height * 0.4865f
                )
            }
            drawPath(greenWave, Color(0xFF38C979), style = Stroke(size.width * 0.045f, cap = StrokeCap.Round))

            val noteX = size.width * 0.76f
            val noteY = size.height * 0.54f
            drawLine(Color(0xFFFF9B22), Offset(noteX, noteY), Offset(noteX, noteY - size.height * 0.25f), size.width * 0.04f, StrokeCap.Round)
            drawLine(Color(0xFFFF9B22), Offset(noteX, noteY - size.height * 0.25f), Offset(noteX + size.width * 0.16f, noteY - size.height * 0.20f), size.width * 0.04f, StrokeCap.Round)
            drawOval(Color(0xFFFF9B22), Offset(noteX - size.width * 0.08f, noteY - size.height * 0.05f), Size(size.width * 0.13f, size.height * 0.09f))

            drawLine(Color(0xFF8065F5), Offset(size.width * 0.67f, size.height * 0.35f), Offset(size.width * 0.67f, size.height * 0.23f), size.width * 0.035f, StrokeCap.Round)
            drawLine(Color(0xFF8065F5), Offset(size.width * 0.77f, size.height * 0.37f), Offset(size.width * 0.77f, size.height * 0.29f), size.width * 0.035f, StrokeCap.Round)
        }
    }
}

@Composable
private fun ProfileShirtIcon(modifier: Modifier = Modifier, color: Color) {
    val collarColor = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        val shirt = Path().apply {
            moveTo(size.width * 0.36f, size.height * 0.12f)
            lineTo(size.width * 0.43f, size.height * 0.23f)
            quadraticTo(size.width * 0.50f, size.height * 0.29f, size.width * 0.57f, size.height * 0.23f)
            lineTo(size.width * 0.64f, size.height * 0.12f)
            lineTo(size.width * 0.78f, size.height * 0.18f)
            lineTo(size.width * 0.98f, size.height * 0.36f)
            lineTo(size.width * 0.84f, size.height * 0.54f)
            lineTo(size.width * 0.72f, size.height * 0.46f)
            lineTo(size.width * 0.72f, size.height * 0.91f)
            lineTo(size.width * 0.28f, size.height * 0.91f)
            lineTo(size.width * 0.28f, size.height * 0.46f)
            lineTo(size.width * 0.16f, size.height * 0.54f)
            lineTo(size.width * 0.02f, size.height * 0.36f)
            lineTo(size.width * 0.22f, size.height * 0.18f)
            close()
        }
        drawPath(shirt, color)
        drawArc(
            color = collarColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.42f, size.height * 0.08f),
            size = Size(size.width * 0.16f, size.height * 0.18f),
            style = Stroke(width = size.width * 0.055f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PlayerHeadingCharacters(brightLimbs: Boolean, compact: Boolean = false) {
    val groundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .size(width = if (compact) 93.dp else 160.dp, height = if (compact) 58.dp else 100.dp)
            .offset(x = if (compact) 0.dp else (-10).dp, y = if (compact) 0.dp else (-4.5).dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawOval(
                color = groundColor,
                topLeft = Offset(size.width * 0.03f, size.height * if (compact) 0.77f else 0.84f),
                size = Size(size.width * 0.94f, size.height * 0.16f)
            )
            val noteStroke = size.width * 0.03f
            val violetStem = Offset(size.width * 0.18f, size.height * 0.28f)
            drawLine(Color(0xFF8B69FF), violetStem, Offset(violetStem.x, violetStem.y + size.height * 0.16f), noteStroke, StrokeCap.Round)
            drawLine(Color(0xFF8B69FF), violetStem, Offset(violetStem.x + size.width * 0.10f, violetStem.y + size.height * 0.04f), noteStroke, StrokeCap.Round)
            drawOval(Color(0xFF8B69FF), Offset(violetStem.x - size.width * 0.065f, violetStem.y + size.height * 0.12f), Size(size.width * 0.14f, size.height * 0.08f))

            val yellowStem = Offset(size.width * 0.60f, size.height * 0.13f)
            drawLine(Color(0xFFFFC52F), yellowStem, Offset(yellowStem.x, yellowStem.y + size.height * 0.18f), noteStroke, StrokeCap.Round)
            drawLine(Color(0xFFFFC52F), yellowStem, Offset(yellowStem.x + size.width * 0.12f, yellowStem.y + size.height * 0.04f), noteStroke, StrokeCap.Round)
            drawOval(Color(0xFFFFC52F), Offset(yellowStem.x - size.width * 0.065f, yellowStem.y + size.height * 0.14f), Size(size.width * 0.14f, size.height * 0.08f))

            drawLine(Color(0xFFFF8B3D), Offset(size.width * 0.91f, size.height * 0.31f), Offset(size.width * 0.91f, size.height * 0.20f), noteStroke, StrokeCap.Round)
            drawLine(Color(0xFFFF8B3D), Offset(size.width * 0.91f, size.height * 0.20f), Offset(size.width * 0.99f, size.height * 0.23f), noteStroke, StrokeCap.Round)
        }
        AvatarCharacter(
            "lime",
            Modifier.size(if (compact) 58.dp else 100.dp)
                .offset(
                    x = if (compact) 0.dp else 5.5.dp,
                    y = if (compact) 0.dp else 5.5.dp
                )
                .align(Alignment.BottomStart),
            heroPose = true,
            brightLimbs = brightLimbs
        )
        AvatarCharacter(
            "sunny",
            Modifier.size(if (compact) 56.dp else 96.dp)
                .offset(y = if (compact) 0.dp else 8.dp)
                .align(Alignment.BottomEnd),
            heroPose = true,
            brightLimbs = brightLimbs
        )
    }
}

@Composable
private fun ProfileAvatarGround() {
    Canvas(Modifier.fillMaxSize()) {
        drawOval(
            color = Color(0xFFFFD987).copy(alpha = 0.48f),
            topLeft = Offset(size.width * 0.12f, size.height * 0.78f),
            size = Size(size.width * 0.76f, size.height * 0.14f)
        )
        drawLine(
            color = Color(0xFFFFB52E),
            start = Offset(size.width * 0.20f, size.height * 0.22f),
            end = Offset(size.width * 0.12f, size.height * 0.15f),
            strokeWidth = size.width * 0.035f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF8B69FF),
            start = Offset(size.width * 0.78f, size.height * 0.22f),
            end = Offset(size.width * 0.86f, size.height * 0.15f),
            strokeWidth = size.width * 0.035f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MusicNotesDecoration(darkTheme: Boolean) {
    Box(modifier = Modifier.size(width = 84.dp, height = 68.dp).offset(y = if (darkTheme) (-22).dp else 0.dp)) {
        Text("♫", color = Color(0xFF8B69FF), fontSize = if (darkTheme) 42.sp else 36.sp, modifier = Modifier.align(Alignment.CenterStart))
        Text("♫", color = Color(0xFFFFB72F), fontSize = if (darkTheme) 34.sp else 29.sp, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun GoogleConnectionCard(
    connected: Boolean,
    onManage: () -> Unit,
    onRecover: () -> Unit,
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
        Column(
            modifier = Modifier.padding(start = if (darkTheme) 16.dp else 12.dp, end = 10.dp).padding(
                top = if (compact) 9.dp else 15.5.dp,
                bottom = if (compact) 9.dp else if (darkTheme) 15.5.dp else 6.5.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(if (compact) 42.dp else if (darkTheme) 54.dp else 56.dp), color = Color.White, shape = RoundedCornerShape(50)) {
                    Box(contentAlignment = Alignment.Center) { GoogleGMark(if (compact) 23.dp else if (darkTheme) 28.dp else 30.dp) }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = if (compact) 10.dp else 15.dp)) {
                    Text("Google account", modifier = Modifier.offset(y = if (darkTheme) 0.dp else (-5).dp), color = colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    ConnectionState(connected)
                }
                androidx.compose.material3.TextButton(
                    onClick = onManage,
                    modifier = Modifier
                        .height(if (compact) 34.dp else if (darkTheme) 42.dp else 38.dp)
                        .width(if (compact) 82.dp else if (darkTheme) 91.dp else 94.dp)
                        .offset(x = if (darkTheme) (-4).dp else 0.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = colorScheme.primaryContainer.copy(alpha = if (darkTheme) 0.55f else 0.42f),
                        contentColor = colorScheme.primary
                    )
                ) {
                    Text(if (connected) "Disconnect" else "Connect", fontWeight = FontWeight.Bold)
                }
            }
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(horizontal = if (darkTheme) 0.dp else 4.dp).padding(top = if (darkTheme) 0.dp else 4.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.42f)
            )
            if (!connected) {
                androidx.compose.material3.TextButton(
                    onClick = onRecover,
                    modifier = Modifier.fillMaxWidth().height(if (compact) 38.dp else if (darkTheme) 48.dp else 43.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (darkTheme) Color.Transparent else colorScheme.primaryContainer.copy(alpha = 0.40f),
                        contentColor = colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Log in to existing account", modifier = Modifier.weight(1f), textAlign = TextAlign.Start, fontWeight = if (darkTheme) FontWeight.Normal else FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun SpotifyProfileCard(
    connected: Boolean,
    onManage: () -> Unit,
    onRefresh: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val darkTheme = colorScheme.background.luminance() < 0.5f
    Surface(
        modifier = Modifier.fillMaxWidth().then(modifier),
        color = if (darkTheme) colorScheme.surface else colorScheme.surface.copy(alpha = 0.84f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = if (darkTheme) 0.34f else 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(start = if (darkTheme) 16.dp else 12.dp, end = 10.dp).padding(
                top = if (compact) 9.dp else if (darkTheme) 16.dp else 17.dp,
                bottom = if (compact) 9.dp else if (darkTheme) 16.dp else 14.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(if (compact) 42.dp else if (darkTheme) 54.dp else 56.dp), color = Color.White, shape = RoundedCornerShape(50)) {
                    Box(contentAlignment = Alignment.Center) {
                        SpotifyMark(sizeDp = if (compact) 31.dp else if (darkTheme) 38.dp else 40.dp)
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = if (compact) 10.dp else 15.dp)) {
                    Text("Spotify", modifier = Modifier.offset(y = if (darkTheme) 0.dp else (-5).dp), color = colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    ConnectionState(connected)
                }
                androidx.compose.material3.TextButton(
                    onClick = onManage,
                    modifier = Modifier
                        .height(if (compact) 34.dp else if (darkTheme) 42.dp else 38.dp)
                        .width(if (compact) 82.dp else if (darkTheme) 91.dp else 94.dp)
                        .offset(x = if (darkTheme) (-4).dp else 0.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = colorScheme.primaryContainer.copy(alpha = if (darkTheme) 0.55f else 0.42f),
                        contentColor = colorScheme.primary
                    )
                ) {
                    Text(if (connected) "Disconnect" else "Connect", fontWeight = FontWeight.Bold)
                }
            }
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(horizontal = if (darkTheme) 0.dp else 4.dp).padding(top = if (darkTheme) 0.dp else 4.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.42f)
            )
            androidx.compose.material3.TextButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth().height(if (compact) 38.dp else if (darkTheme) 48.dp else 43.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (darkTheme) Color.Transparent else colorScheme.primaryContainer.copy(alpha = 0.40f),
                    contentColor = colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh Spotify status", modifier = Modifier.weight(1f), textAlign = TextAlign.Start, fontWeight = if (darkTheme) FontWeight.Normal else FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ConnectionState(connected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(9.dp),
            color = if (connected) Color(0xFF35C76F) else MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(50)
        ) { }
        Text(
            if (connected) "Connected" else "Not connected",
            modifier = Modifier.padding(start = 7.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
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

@Composable
private fun SpotifyMark(sizeDp: androidx.compose.ui.unit.Dp = 33.dp) {
    Image(
        painter = painterResource(R.drawable.spotify_official_icon),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(sizeDp)
    )
}
