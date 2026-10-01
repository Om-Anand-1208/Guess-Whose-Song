package com.guesswhosesong.app.ui.screens.join

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.R
import com.guesswhosesong.app.ui.components.AvatarCharacter
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
        WelcomeSceneryBackground(Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 12.dp)
                .widthIn(max = 620.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JoinWordmark(Modifier.weight(1f))
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)
                ) { Icon(Icons.Default.Settings, contentDescription = "Appearance and player settings", tint = MaterialTheme.colorScheme.onSurface) }
            }
        Text(
            "Your friends picked the songs.\nCan you guess who?",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = GwsDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = if (availableWidth < 360.dp) 26.sp else 30.sp,
            lineHeight = if (availableWidth < 360.dp) 32.sp else 37.sp,
            letterSpacing = (-1).sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 10.dp)
        )
        Image(
            painter = painterResource(if (darkTheme) R.drawable.welcome_hero_dark else R.drawable.welcome_hero),
            contentDescription = "Friends playing a music guessing game",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).aspectRatio(1.18f)
                .clip(RoundedCornerShape(28.dp))
        )
        WelcomeRoomActions(
            joinCode = joinCode,
            isLoading = isLoading,
            error = error,
            onJoinCodeChange = onJoinCodeChange,
            onCreateRoom = onCreateRoom,
            onJoinRoom = onJoinRoom,
            onScanRoomCode = onScanRoomCode,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)
        )
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
    onScanRoomCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), shape = RoundedCornerShape(28.dp)) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onCreateRoom,
            enabled = !isLoading,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF983D),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(58.dp),
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
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = onScanRoomCode, enabled = !isLoading) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan room QR code", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
        Button(
            onClick = onJoinRoom,
            enabled = !isLoading,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth().height(58.dp)
        ) {
            Text("▶  Join room", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
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

@Composable
private fun WelcomeSceneryBackground(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    val dark = background.luminance() < 0.5f
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val grassStart = h * 0.65f
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
    onConnectSpotify: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    onRefreshSpotify: () -> Unit,
    onSaveProfile: (String, AvatarCustomization) -> Unit,
    onBackToGame: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            JoinWordmark(Modifier.weight(1f))
            TextButton(onClick = onBackToGame) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Back", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Your player", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "This is how you’ll appear in the room.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(112.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AvatarCharacter(draftAvatar.shapeId, Modifier.size(92.dp), customization = draftAvatar)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = draftName,
                            onValueChange = { onNameChange(it.take(24)) },
                            label = { Text("Display name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Done
                            ),
                            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = onEditCharacter,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Edit character", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            Text("Connections", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Link your accounts to make it easier to play and discover songs with friends.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GoogleConnectionCard(
                connected = isAccountLinked,
                onManage = if (isAccountLinked) onRecoverGoogle else onLinkGoogle,
                onRecover = onRecoverGoogle
            )
            SpotifyProfileCard(
                connected = isSpotifyConnected,
                onManage = if (isSpotifyConnected) onDisconnectSpotify else onConnectSpotify,
                onRefresh = onRefreshSpotify
            )
            statusMessage?.let { message ->
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
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBackToGame,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
            Button(
                onClick = { onSaveProfile(draftName, draftAvatar) },
                modifier = Modifier.weight(1.15f).height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) { Text("Save settings", fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun GoogleConnectionCard(connected: Boolean, onManage: () -> Unit, onRecover: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(54.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
                    Box(contentAlignment = Alignment.Center) { GoogleGMark() }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Google account", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    ConnectionState(connected)
                }
                androidx.compose.material3.TextButton(onClick = onManage) {
                    Text(if (connected) "Manage" else "Connect", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "Used to sign in and recover your account.",
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            androidx.compose.material3.TextButton(onClick = onRecover, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Recover an existing Google player", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SpotifyProfileCard(connected: Boolean, onManage: () -> Unit, onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(54.dp), color = Color(0xFF1DB954), shape = RoundedCornerShape(50)) {
                    Box(contentAlignment = Alignment.Center) { SpotifyMark() }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Spotify", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    ConnectionState(connected)
                }
                androidx.compose.material3.TextButton(onClick = onManage) {
                    Text(if (connected) "Manage" else "Connect", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "Used to pick songs and create a better experience.",
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            androidx.compose.material3.TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh Spotify status", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
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
        Text(text, modifier = Modifier.clearAndSetSemantics { }, color = textColor, style = style.copy(drawStyle = Stroke(width = strokeWidthPx)), textAlign = textAlign, maxLines = maxLines)
        Text(text, color = textColor, style = style, textAlign = textAlign, maxLines = maxLines)
    }
}

@Composable
internal fun JoinWordmark(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        JoinLogoConfetti(left = true)
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            JoinDisplayText("Guess", 23.sp, 25.sp, (-0.8).sp, strokeWidth = 1.3.dp, maxLines = 1)
            JoinDisplayText("Whose Song", 23.sp, 25.sp, (-0.8).sp, strokeWidth = 1.3.dp, maxLines = 1)
        }
        JoinLogoConfetti(left = false)
    }
}

@Composable
internal fun JoinLogoConfetti(left: Boolean) {
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
