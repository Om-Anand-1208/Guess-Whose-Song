package com.guesswhosesong.web

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.AvatarCatalog
import com.guesswhosesong.shared.models.GameConstants
import com.guesswhosesong.web.generated.resources.Res
import com.guesswhosesong.web.generated.resources.save_player_hero
import com.guesswhosesong.web.generated.resources.welcome_hero
import com.guesswhosesong.web.generated.resources.comfortaa_bold
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import kotlinx.browser.document

private val WebColorScheme = lightColorScheme(
    primary = Color(0xFF2980B9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F4F8),
    onPrimaryContainer = Color(0xFF173B52),
    secondary = Color(0xFF6DD5FA),
    onSecondary = Color(0xFF0F0F23),
    tertiary = Color(0xFFE8F4F8),
    onTertiary = Color(0xFF212529),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF212529),
    surface = Color.White,
    onSurface = Color(0xFF212529),
    surfaceVariant = Color(0xFFEFF2F4),
    onSurfaceVariant = Color(0xFF555D64),
    error = Color(0xFFDC3545),
    onError = Color.White,
    outline = Color(0xFF9AA4AA),
    outlineVariant = Color(0xFFD7DDE1)
)

private val PostJoinColorScheme = lightColorScheme(
    primary = Color(0xFF7668E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCEC9FF),
    onPrimaryContainer = Color(0xFF17161A),
    secondary = Color(0xFFFFAA3D),
    onSecondary = Color(0xFF17161A),
    secondaryContainer = Color(0xFFFFE49A),
    onSecondaryContainer = Color(0xFF17161A),
    tertiary = Color(0xFFB8F45D),
    onTertiary = Color(0xFF17161A),
    background = Color(0xFFFFFBF2),
    onBackground = Color(0xFF17161A),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF17161A),
    surfaceVariant = Color(0xFFF1EBDD),
    onSurfaceVariant = Color(0xFF5D5860),
    error = Color(0xFFB3261E),
    onError = Color.White,
    outline = Color(0xFF2D2930),
    outlineVariant = Color(0xFFCFC6B9)
)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    (document.getElementById("loader-container") as? HTMLElement)?.remove()
    ComposeViewport(viewportContainerId = "ComposeTarget") { WebApp() }
}

@Composable
private fun WebApp() {
    val store = remember { WebGameStore() }
    val state by store.state.collectAsState()
    val authStatus by store.auth.status.collectAsState()
    val user by store.auth.user.collectAsState()
    var savePlayerPage by remember { mutableStateOf(false) }
    LaunchedEffect(state.page) {
        if (state.page != WebPage.JOIN) savePlayerPage = false
    }
    DisposableEffect(Unit) { onDispose { store.close() } }

    MaterialTheme(
        colorScheme = WebColorScheme,
        shapes = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(20.dp)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (state.page == WebPage.JOIN) {
                MaterialTheme(
                    colorScheme = PostJoinColorScheme,
                    shapes = Shapes(
                        small = RoundedCornerShape(12.dp),
                        medium = RoundedCornerShape(20.dp),
                        large = RoundedCornerShape(28.dp)
                    )
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (savePlayerPage) {
                            SavePlayerPage(
                                store = store,
                                state = state,
                                status = authStatus,
                                user = user,
                                onBackToGame = { savePlayerPage = false },
                                onKeepGuest = { savePlayerPage = false }
                            )
                        } else {
                            JoinPage(store, state, authStatus, user, onSavePlayer = { savePlayerPage = true })
                        }
                    }
                }
            } else {
                MaterialTheme(
                    colorScheme = PostJoinColorScheme,
                    shapes = Shapes(
                        small = RoundedCornerShape(12.dp),
                        medium = RoundedCornerShape(20.dp),
                        large = RoundedCornerShape(28.dp)
                    )
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when (state.page) {
                            WebPage.JOIN -> Unit
                            WebPage.LOBBY -> LobbyPage(store, state)
                            WebPage.SUBMISSION -> SubmissionPage(store, state)
                            WebPage.GAME -> GamePage(store, state)
                            WebPage.RESULTS -> ResultsPage(store, state)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageFrame(title: String, state: WebUiState, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val sidePadding = if (maxWidth > 760.dp) (maxWidth - 760.dp) / 2 else 16.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            if (title.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding, vertical = 16.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                content()
            }
        }
    }
}

@Composable
private fun PostJoinFrame(
    title: String,
    kicker: String,
    description: String,
    state: WebUiState,
    content: @Composable (wide: Boolean) -> Unit
) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 980.dp
        val horizontalPadding = when {
            maxWidth >= 1280.dp -> 48.dp
            maxWidth >= 720.dp -> 32.dp
            else -> 18.dp
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .widthIn(max = 1280.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.width(12.dp).height(12.dp)
                    ) {}
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "GUESS WHOSE SONG",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(Modifier.weight(1f))
                    state.room?.joinCode?.let { code ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text("ROOM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(code, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                            }
                        }
                    }
                }
                Text(kicker.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
                state.notice?.let { MessageBanner(it, isError = false) }
                state.error?.let { MessageBanner(it, isError = true) }
                content(wide)
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun MessageBanner(message: String, isError: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        color = if (isError) Color(0xFFFBE7E4) else Color(0xFFECE9FF),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Text(
            message,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun GamePanel(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            content()
        }
    }
}

@Composable
private fun FrontPageFrame(
    state: WebUiState,
    isSavePlayer: Boolean,
    onHeaderAction: () -> Unit,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1120.dp
        val compactHeader = maxWidth < 320.dp
        val horizontalPadding = if (wide) 36.dp else 18.dp
        val headline = if (isSavePlayer) {
            "Your songs.\nYour people. Your game."
        } else {
            "Your friends picked the songs.\nCan you guess who?"
        }
        val headlineSize = when {
            !wide -> 36
            maxWidth >= 1320.dp -> 48
            else -> 40
        }
        val artwork = if (isSavePlayer) Res.drawable.save_player_hero else Res.drawable.welcome_hero

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .widthIn(max = 1320.dp)
                    .padding(horizontal = horizontalPadding, vertical = 18.dp)
                    .align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (wide) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FrontPageWordmark(wide = true)
                        Spacer(Modifier.weight(1f))
                        FrontPageHeaderAction(isSavePlayer, onHeaderAction)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FrontPageWordmark(wide = false, compact = compactHeader)
                        FrontPageHeaderAction(isSavePlayer, onHeaderAction, Modifier.align(Alignment.End))
                    }
                }
                state.notice?.let { MessageBanner(it, isError = false) }
                state.error?.let { MessageBanner(it, isError = true) }
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(26.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            FrontPageCopy(headline, wide = true, headlineSize = headlineSize)
                            FrontPageArtwork(artwork, isSavePlayer)
                        }
                        FrontPageCard(Modifier.weight(0.8f), content)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FrontPageCopy(headline, wide = false, headlineSize = headlineSize)
                        FrontPageArtwork(artwork, isSavePlayer)
                        FrontPageCard(Modifier.fillMaxWidth(), content)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun FrontPageWordmark(wide: Boolean, compact: Boolean = false) {
    val logoSize = when {
        wide -> 40.sp
        compact -> 28.sp
        else -> 34.sp
    }
    val logoLineHeight = when {
        wide -> 40.sp
        compact -> 30.sp
        else -> 35.sp
    }
    val logoFont = FontFamily(Font(Res.font.comfortaa_bold, FontWeight.Bold))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)) {
        FrontPageConfetti(left = true, compact = compact)
        Column {
            FrontPageDisplayText(
                "Guess",
                fontSize = logoSize,
                lineHeight = logoLineHeight,
                fontFamily = logoFont,
                letterSpacing = (-1.1).sp,
                strokeWidth = 2.dp,
                maxLines = 1,
                textAlign = TextAlign.Start
            )
            FrontPageDisplayText(
                "Whose Song",
                fontSize = logoSize,
                lineHeight = logoLineHeight,
                fontFamily = logoFont,
                letterSpacing = (-1.1).sp,
                strokeWidth = 2.dp,
                maxLines = 1,
                textAlign = TextAlign.Start
            )
        }
        FrontPageConfetti(left = false, compact = compact)
    }
}

@Composable
private fun FrontPageDisplayText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    fontFamily: FontFamily = FontFamily(Font(Res.font.comfortaa_bold, FontWeight.Bold)),
    letterSpacing: androidx.compose.ui.unit.TextUnit = (-1.1).sp,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.1.dp,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier
) {
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
    val style = TextStyle(
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = letterSpacing
    )
    Box(modifier = modifier) {
        Text(text, modifier = Modifier.clearAndSetSemantics { }, color = Color(0xFF17161A), style = style.copy(drawStyle = Stroke(width = strokeWidthPx)), maxLines = maxLines, textAlign = textAlign)
        Text(text, color = Color(0xFF17161A), style = style, maxLines = maxLines, textAlign = textAlign)
    }
}

@Composable
private fun FrontPageConfetti(left: Boolean, compact: Boolean = false) {
    Canvas(Modifier.size(width = if (compact) 16.dp else 21.dp, height = if (compact) 48.dp else 62.dp)) {
        val stroke = (if (compact) 4.dp else 5.dp).toPx()
        val cap = StrokeCap.Round
        if (left) {
            drawLine(Color(0xFFFFAA3D), Offset(size.width * 0.58f, size.height * 0.15f), Offset(size.width * 0.92f, size.height * 0.38f), stroke, cap)
            drawLine(Color(0xFFFF72A7), Offset(size.width * 0.08f, size.height * 0.55f), Offset(size.width * 0.48f, size.height * 0.62f), stroke, cap)
        } else {
            drawLine(Color(0xFF9DEBC1), Offset(size.width * 0.12f, size.height * 0.19f), Offset(size.width * 0.48f, size.height * 0.36f), stroke, cap)
            drawLine(Color(0xFFFFAA3D), Offset(size.width * 0.57f, size.height * 0.10f), Offset(size.width * 0.91f, size.height * 0.34f), stroke, cap)
            drawLine(Color(0xFF8F7CF7), Offset(size.width * 0.58f, size.height * 0.65f), Offset(size.width * 0.89f, size.height * 0.58f), stroke, cap)
        }
    }
}

@Composable
private fun FrontPageHeaderAction(isSavePlayer: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(if (isSavePlayer) "Back to game" else "Save your player", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FrontPageCopy(headline: String, wide: Boolean, headlineSize: Int) {
    FrontPageDisplayText(
        headline,
        fontSize = headlineSize.sp,
        lineHeight = if (wide) (headlineSize + 8).sp else 41.sp,
        letterSpacing = (-1.1).sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FrontPageArtwork(artwork: org.jetbrains.compose.resources.DrawableResource, isSavePlayer: Boolean) {
    Image(
        painter = painterResource(artwork),
        contentDescription = if (isSavePlayer) "Colorful music characters playing together" else "Friends enjoying music together outdoors",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(28.dp))
    )
}

@Composable
private fun FrontPageCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            content()
        }
    }
}

@Composable
private fun JoinPage(
    store: WebGameStore,
    state: WebUiState,
    status: AuthStatus,
    user: WebUser?,
    onSavePlayer: () -> Unit
) {
    FrontPageFrame(state, isSavePlayer = false, onHeaderAction = onSavePlayer) {
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text(
                when (status) {
                    AuthStatus.LOADING -> "Preparing your guest player…"
                    AuthStatus.ERROR -> "Guest sign-in is unavailable. Check Firebase web configuration."
                    AuthStatus.READY -> if (user?.isAnonymous == true) "Playing as a guest" else "Your Google player is connected."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = state.displayName,
                onValueChange = store::setDisplayName,
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pick your character", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                WebAvatarPicker(selectedId = state.avatarId, onSelected = store::setAvatarId)
            }
            Button(
                onClick = store::createRoom,
                enabled = status == AuthStatus.READY && !state.isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF983D), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.isBusy) CircularProgressIndicator(modifier = Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Create room", fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text("OR JOIN A ROOM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            OutlinedTextField(
                value = state.joinCode,
                onValueChange = store::setJoinCode,
                label = { Text("Room code") },
                placeholder = { Text("ABCD12") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = store::joinRoom,
                enabled = status == AuthStatus.READY && !state.isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAEA4FF), contentColor = Color(0xFF17161A)),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Join room", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SavePlayerPage(
    store: WebGameStore,
    state: WebUiState,
    status: AuthStatus,
    user: WebUser?,
    onBackToGame: () -> Unit,
    onKeepGuest: () -> Unit
) {
    FrontPageFrame(state, isSavePlayer = true, onHeaderAction = onBackToGame) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 280.dp
                if (compact) {
                    FrontPageDisplayText(
                        "Save your player",
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        FrontPageConfetti(left = true)
                        FrontPageDisplayText("Save your player", fontSize = 29.sp, lineHeight = 34.sp, textAlign = TextAlign.Center, maxLines = 1)
                        FrontPageConfetti(left = false)
                    }
                }
            }
            Text("Link Google to pick up where you left off.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            when (status) {
                AuthStatus.LOADING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                    Text("Getting your player ready…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AuthStatus.ERROR -> Text("Account linking is unavailable. Check Firebase web configuration.", color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                AuthStatus.READY -> if (user?.isAnonymous != true) {
                    Surface(color = Color(0xFFE2F5E8), shape = RoundedCornerShape(50)) {
                        Text("GOOGLE ACCOUNT LINKED", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF176B34), fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                    }
                }
            }
            if (status == AuthStatus.READY && user?.isAnonymous == true) {
                Button(
                    onClick = store::linkGoogle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF983D), contentColor = Color(0xFF17161A)),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoogleGMark()
                        Spacer(Modifier.width(10.dp))
                        Text("Link Google account", fontWeight = FontWeight.Black)
                    }
                }
            }
            Button(
                onClick = onKeepGuest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1EBDD), contentColor = Color(0xFF17161A)),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text(if (user?.isAnonymous == false) "Continue playing" else "Keep playing as guest", fontWeight = FontWeight.Bold) }
            if (status == AuthStatus.READY && user?.isAnonymous == true) {
                Text("You can link an account later from settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            if (status == AuthStatus.READY && user?.isAnonymous == true) {
                TextButton(onClick = store::recoverGoogle, modifier = Modifier.fillMaxWidth()) { Text("Recover an existing Google player") }
            }
        }
    }
}

@Composable
private fun GoogleGMark() {
    Canvas(modifier = Modifier.size(24.dp).semantics { contentDescription = "Google" }) {
        val strokeWidth = 3.5.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val ringStroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        drawArc(
            color = Color(0xFF4285F4), startAngle = -43f, sweepAngle = 86f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = ringStroke
        )
        drawArc(
            color = Color(0xFF34A853), startAngle = 47f, sweepAngle = 86f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = ringStroke
        )
        drawArc(
            color = Color(0xFFFBBC05), startAngle = 137f, sweepAngle = 86f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = ringStroke
        )
        drawArc(
            color = Color(0xFFEA4335), startAngle = 227f, sweepAngle = 86f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = ringStroke
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width / 2f, size.height / 2f),
            end = Offset(size.width - strokeWidth / 2f, size.height / 2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}

@Composable
private fun WebAvatarPicker(selectedId: String, onSelected: (String) -> Unit) {
    val colors = mapOf(
        "sunny" to Color(0xFFFFB43E), "lime" to Color(0xFFB8F45D), "violet" to Color(0xFF8F7CF7),
        "tangerine" to Color(0xFFFF8B3D), "cloud" to Color(0xFF9CCBFF), "star" to Color(0xFFFFDF72),
        "berry" to Color(0xFFFF91B3), "mint" to Color(0xFF8CE7C1)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AvatarCatalog.ids.chunked(4).forEachIndexed { rowIndex, rowIds ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowIds.forEachIndexed { columnIndex, id ->
                    val index = rowIndex * 4 + columnIndex
                    val isSelected = id == selectedId
                    Box(modifier = Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier.size(38.dp)
                                .clip(CircleShape)
                                .background(colors.getValue(id))
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color(0xFF17161A) else Color(0xFFCFC6B9), CircleShape)
                                .clickable { onSelected(id) }
                                .semantics {
                                    contentDescription = "Avatar color ${index + 1}"
                                    role = Role.RadioButton
                                    selected = isSelected
                                }
                        )
                    }
                }
                repeat(4 - rowIds.size) {
                    Spacer(modifier = Modifier.weight(1f).height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun LobbyPage(store: WebGameStore, state: WebUiState) {
    val room = state.room
    val isHost = room?.hostId == state.selfPlayerId
    var settingsExpanded by remember { mutableStateOf(false) }
    PostJoinFrame(
        title = "Room lobby",
        kicker = "THE HANGOUT",
        description = "Get your people in the room, then let the music do the talking.",
        state = state
    ) { wide ->
        val mainColumn: @Composable () -> Unit = {
            if (room == null) {
                GamePanel("Getting the room ready", "Your room code is saved. The lobby is connecting.") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                        Text("Connecting to room…")
                    }
                    OutlinedButton(onClick = store::leaveRoom) { Text("Cancel") }
                }
            } else {
                GamePanel("Room code", "Share this with friends so they can join your game.") {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(18.dp)) {
                        Text(room.joinCode, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    }
                    Text("Waiting for everyone to arrive", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                GamePanel("Players", "${room.players.size} of ${room.settings.playerLimit} spots filled") {
                    room.players.forEachIndexed { index, player ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        PlayerRow(
                            player = player,
                            isSelf = player.id == state.selfPlayerId,
                            canKick = isHost && player.id != state.selfPlayerId,
                            onKick = { store.kick(player.id) }
                        )
                    }
                }
                if (isHost) {
                    GamePanel("Ready to start?", "Song selection opens for everyone when the host starts the game.") {
                        Button(onClick = store::startGame, enabled = room.players.size >= 2, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text("Start game", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (room.players.size < 2) Text("Invite one more player to unlock song selection.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    GamePanel("You're all set", "The host will start song selection when everyone's ready.") {
                        Text("Waiting for host to start…", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        val sideColumn: @Composable () -> Unit = {
            if (isHost) {
                    GamePanel("Room settings", "Shape the next round for your group.") {
                        OutlinedButton(onClick = { settingsExpanded = !settingsExpanded }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (settingsExpanded) "Hide settings" else "${room.settings.roundLengthPreset.songsPerPlayer} songs · ${room.settings.playerLimit} players · ${room.settings.votingTimerSeconds}s voting")
                    }
                    if (settingsExpanded) LobbySettings(store, room.settings)
                }
            }
            SpotifyLobbyControl(store, state)
            if (room != null) {
                ChatPanel(store, state)
                OutlinedButton(onClick = store::leaveRoom, modifier = Modifier.fillMaxWidth()) { Text("Leave room") }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1.45f), verticalArrangement = Arrangement.spacedBy(16.dp)) { mainColumn() }
                Column(Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(16.dp)) { sideColumn() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                mainColumn()
                sideColumn()
            }
        }
    }
}

@Composable
private fun LobbySettings(store: WebGameStore, settings: RoomSettings) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingChoices(
            title = "Songs per player",
            choices = com.guesswhosesong.shared.models.RoundLengthPreset.entries.map { it.songsPerPlayer.toString() },
            selectedValue = settings.roundLengthPreset.songsPerPlayer.toString()
        ) { selected ->
            val preset = com.guesswhosesong.shared.models.RoundLengthPreset.entries.first { it.songsPerPlayer.toString() == selected }
            store.updateSettings(settings.copy(roundLengthPreset = preset))
        }
        SettingChoices("Player limit", listOf("2", "4", "6", "10", "20"), settings.playerLimit.toString()) { selected ->
            store.updateSettings(settings.copy(playerLimit = selected.toInt()))
        }
        SettingChoices("Voting time", listOf("10s", "15s", "20s", "30s"), "${settings.votingTimerSeconds}s") { selected ->
            store.updateSettings(settings.copy(votingTimerSeconds = selected.dropLast(1).toInt()))
        }
    }
}

@Composable
private fun SettingChoices(title: String, choices: List<String>, selectedValue: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            choices.forEach { choice ->
                val selected = choice == selectedValue
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(50),
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    TextButton(onClick = { onSelect(choice) }) {
                        Text(
                            choice,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyLobbyControl(store: WebGameStore, state: WebUiState) {
    GamePanel("Spotify", if (state.spotifyConnected) "Your top tracks are ready to add to the game." else "Connect for quick song picks. This is optional.") {
        if (!state.spotifyConnected) {
            Button(onClick = store::connectSpotify, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)), modifier = Modifier.fillMaxWidth()) {
                Text("Connect Spotify", color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Color(0xFFE2F5E8), shape = RoundedCornerShape(50)) {
                    Text("CONNECTED", modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF176B34), fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
                }
                Text("Top tracks ready", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PlayerRow(player: Player, isSelf: Boolean, canKick: Boolean, onKick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        WebAvatarSwatch(player.avatarId)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(player.displayName + if (isSelf) " (you)" else "", fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Medium)
                if (player.isHost) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                    Text("HOST", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
            }
            Text(if (player.connected) "Connected" else "Reconnecting…", style = MaterialTheme.typography.bodySmall, color = if (player.connected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
        }
        if (canKick) TextButton(onClick = onKick) { Text("Remove", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun WebAvatarSwatch(avatarId: String, selected: Boolean = false) {
    val colors = mapOf(
        "sunny" to Color(0xFFFFE9C8), "lime" to Color(0xFFEDF3D8), "violet" to Color(0xFFEAE6FF),
        "tangerine" to Color(0xFFF8E6DA), "cloud" to Color(0xFFE4EDF1), "star" to Color(0xFFFFF2CF),
        "berry" to Color(0xFFF5E5EA), "mint" to Color(0xFFE5F1E9)
    )
    Box(
        modifier = Modifier.width(46.dp).height(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(colors[avatarId] ?: Color(0xFFEAE6FF))
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(15.dp))
    )
}

@Composable
private fun SpotifyControls(store: WebGameStore, state: WebUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Spotify", fontWeight = FontWeight.Bold)
                Text(if (state.spotifyConnected) "Connected" else "Not connected")
            }
            if (state.spotifyConnected) {
                TextButton(onClick = store::disconnectSpotify) { Text("Disconnect") }
            } else {
                Button(onClick = store::connectSpotify) { Text("Connect") }
            }
            TextButton(onClick = store::refreshSpotify) { Text("Refresh") }
        }
    }
}

@Composable
private fun SubmissionPage(store: WebGameStore, state: WebUiState) {
    val now = rememberClockNow()
    val maxSongs = state.room?.settings?.roundLengthPreset?.songsPerPlayer ?: 1
    val selfPlayer = state.room?.players?.firstOrNull { it.id == state.selfPlayerId }
    val locked = selfPlayer?.songLocked == true
    val totalPlayers = state.room?.players?.size ?: 0
    val lockedPlayers = state.room?.players?.count { it.songLocked } ?: 0
    PostJoinFrame(
        title = "Pick your songs",
        kicker = "YOUR TASTE, YOUR TURN",
        description = "Add up to $maxSongs songs your friends will have to recognize.",
        state = state
    ) { wide ->
        val songColumn: @Composable () -> Unit = {
            GamePanel("Your song picks", "${state.pendingSongs.size} of $maxSongs selected") {
                repeat(maxSongs) { index ->
                    val song = state.pendingSongs.getOrNull(index)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (song == null) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        if (song == null) {
                            Row(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(12.dp))
                                Text(if (!locked && index == state.pendingSongs.size) "Choose a song for this spot" else "Empty song slot", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, fontWeight = FontWeight.Bold)
                                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!locked) TextButton(onClick = { store.removeSong(song.songId) }) { Text("Remove") }
                            }
                        }
                    }
                }
                if (locked) Text("Your picks are locked. Waiting for the other players…", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            if (!locked) SongSearchPanel(store, state, maxSongs, wide)
        }
        val sideColumn: @Composable () -> Unit = {
            GamePanel(
                "Round progress",
                if (state.deadlineEpochMillis > 0L) "${deadlineLabel(state.deadlineEpochMillis, now)} · Song picks close when time runs out." else "Song picks close when time runs out."
            ) {
                Text("$lockedPlayers of $totalPlayers players ready", fontWeight = FontWeight.SemiBold)
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { if (totalPlayers == 0) 0f else lockedPlayers.toFloat() / totalPlayers },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            SpotifySuggestionsPanel(store, state, locked, maxSongs)
            if (!locked) {
                Button(onClick = store::lockSongs, enabled = state.pendingSongs.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Lock in picks · ${state.pendingSongs.size}/$maxSongs", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(16.dp)) { songColumn() }
                Column(Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(16.dp)) { sideColumn() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                sideColumn()
                songColumn()
            }
        }
    }
}

@Composable
private fun SongSearchPanel(store: WebGameStore, state: WebUiState, maxSongs: Int, wide: Boolean) {
    GamePanel("Find a song", "Search the catalog or load a pick from Spotify.") {
        val field = @Composable {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = store::setSearchQuery,
                label = { Text("Song for slot ${state.pendingSongs.size + 1}") },
                placeholder = { Text("Song title or artist") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (wide) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { field() }
                Button(onClick = store::search, enabled = state.searchQuery.isNotBlank() && !state.isBusy, modifier = Modifier.height(56.dp)) { Text("Search") }
            }
        } else {
            field()
            Button(onClick = store::search, enabled = state.searchQuery.isNotBlank() && !state.isBusy, modifier = Modifier.fillMaxWidth()) { Text("Search songs") }
        }
        if (state.isBusy) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
        state.searchResults.forEach { track ->
            OutlinedButton(onClick = { store.selectTrack(track) }, enabled = state.pendingSongs.size < maxSongs, modifier = Modifier.fillMaxWidth()) {
                Text("Add · ${track.title} — ${track.artist}", maxLines = 1)
            }
        }
        OutlinedButton(onClick = { store.loadSpotifySuggestions(); store.refreshSpotify() }, modifier = Modifier.fillMaxWidth()) { Text("Load Spotify picks") }
    }
}

@Composable
private fun SpotifySuggestionsPanel(store: WebGameStore, state: WebUiState, locked: Boolean, maxSongs: Int) {
    GamePanel("Spotify picks", "A shortcut from your top tracks.") {
        if (!state.spotifyConnected) {
            Text("Connect Spotify to see song suggestions here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = store::connectSpotify, modifier = Modifier.fillMaxWidth()) { Text("Connect Spotify") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Connected", color = Color(0xFF176B34), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = store::loadSpotifySuggestions) { Text("Refresh") }
            }
            state.spotifySuggestions.take(6).forEach { suggestion ->
                OutlinedButton(
                    onClick = { store.selectSpotifySuggestion(suggestion) },
                    enabled = !locked && state.pendingSongs.size < maxSongs,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add · ${suggestion.title} — ${suggestion.artist}", maxLines = 1) }
            }
            if (state.spotifySuggestions.isEmpty()) Text("No suggestions loaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GamePage(store: WebGameStore, state: WebUiState) {
    val now = rememberClockNow()
    val stage = when {
        state.reveal != null -> GameStage.REVEAL
        state.voting != null -> GameStage.VOTE
        state.preview != null -> GameStage.LISTEN
        else -> GameStage.WAITING
    }
    val currentRound = state.reveal?.roundIndex ?: state.voting?.roundIndex ?: state.preview?.roundIndex ?: 0
    val title = when (stage) {
        GameStage.LISTEN -> "Listen close"
        GameStage.VOTE -> "Make your guess"
        GameStage.REVEAL -> "The reveal"
        GameStage.WAITING -> "Round in progress"
    }
    val kicker = when (stage) {
        GameStage.LISTEN -> "LISTEN"
        GameStage.VOTE -> "VOTE"
        GameStage.REVEAL -> "REVEAL"
        GameStage.WAITING -> "GAME ROOM"
    }
    val description = when (stage) {
        GameStage.LISTEN -> "Catch the clues. This song belongs to someone in the room."
        GameStage.VOTE -> "Who picked this one? Trust your music memory."
        GameStage.REVEAL -> "See who knew the song, and how the scores changed."
        GameStage.WAITING -> "The next song will appear here when the round begins."
    }
    PostJoinFrame(title, kicker, description, state) { wide ->
        RoundSteps(stage, currentRound)
        val mainColumn: @Composable () -> Unit = {
            when (stage) {
                GameStage.LISTEN -> state.preview?.let { preview -> ListenStage(preview) }
                GameStage.VOTE -> state.voting?.let { voting -> VoteStage(store, state, voting, state.preview, wide, now) }
                GameStage.REVEAL -> state.reveal?.let { reveal -> RevealStage(reveal, state.revealStartedAtEpochMillis, now) }
                GameStage.WAITING -> GamePanel("Waiting for the next song", "Keep the room open while the next round loads.") {
                    CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp), strokeWidth = 2.dp)
                }
            }
        }
        val sideColumn: @Composable () -> Unit = {
            GamePlayersPanel(state)
            ChatPanel(store, state)
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 16.dp)) {
                Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(16.dp)) { mainColumn() }
                Column(Modifier.weight(0.82f), verticalArrangement = Arrangement.spacedBy(16.dp)) { sideColumn() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 16.dp)) {
                mainColumn()
                sideColumn()
            }
        }
    }
}

private enum class GameStage { LISTEN, VOTE, REVEAL, WAITING }

@Composable
private fun RoundSteps(stage: GameStage, roundIndex: Int) {
    val active = when (stage) {
        GameStage.LISTEN -> 0
        GameStage.VOTE -> 1
        GameStage.REVEAL -> 2
        GameStage.WAITING -> 0
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        listOf("Listen", "Vote", "Reveal").forEachIndexed { index, label ->
            val selected = index == active
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    if (selected) "${roundIndex + 1} · $label" else label,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ListenStage(preview: com.guesswhosesong.shared.dto.RoundPreviewStarted) {
    var playbackPosition by remember(preview.previewUrl) { mutableStateOf(0.0) }
    var playbackDuration by remember(preview.previewUrl) { mutableStateOf(0.0) }
    LaunchedEffect(preview.previewUrl) {
        while (isActive) {
            playbackPosition = previewPlaybackPositionSeconds()
            playbackDuration = previewPlaybackDurationSeconds()
            delay(200L)
        }
    }
    val displayDuration = playbackDuration.takeIf { it > 0.0 } ?: preview.previewDurationMs / 1_000.0
    val progress = if (displayDuration > 0.0) (playbackPosition / displayDuration).toFloat().coerceIn(0f, 1f) else 0f
    GamePanel("A song is playing", "Listen for a detail you recognize before the choices appear.") {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
            Text("ROUND ${preview.roundIndex + 1} OF ${preview.totalRounds}", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer, letterSpacing = 0.8.sp)
        }
        AbstractCoverPlaceholder()
        Text(preview.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(preview.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.material3.LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(formatPlaybackTime(playbackPosition), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(formatPlaybackTime(displayDuration), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AbstractCoverPlaceholder(
    maxSize: androidx.compose.ui.unit.Dp = 300.dp,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    BoxWithConstraints(Modifier.widthIn(max = maxSize).then(modifier).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        val side = maxWidth.coerceAtMost(maxSize)
        Box(
            modifier = Modifier.size(side)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFB8AFFF), Color(0xFFDFD9FF), Color(0xFFFFD29C))))
        ) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(22.dp)
                    .size(side * 0.42f).clip(CircleShape)
                    .background(Color(0xFFFFAA3D).copy(alpha = 0.88f))
            )
            Box(
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 28.dp)
                    .size(width = side * 0.68f, height = side * 0.34f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFB8F45D).copy(alpha = 0.92f))
            )
            Box(
                modifier = Modifier.align(Alignment.Center).size(side * 0.25f)
                    .clip(CircleShape).background(Color(0xFF302B43).copy(alpha = 0.9f))
            )
            Box(
                modifier = Modifier.align(Alignment.Center).size(side * 0.50f)
                    .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
            )
        }
    }
}

private fun formatPlaybackTime(seconds: Double): String {
    val wholeSeconds = seconds.takeIf { it.isFinite() }?.toInt()?.coerceAtLeast(0) ?: 0
    return "${wholeSeconds / 60}:${(wholeSeconds % 60).toString().padStart(2, '0')}"
}

@Composable
private fun VoteStage(
    store: WebGameStore,
    state: WebUiState,
    voting: com.guesswhosesong.shared.dto.VotingStarted,
    preview: com.guesswhosesong.shared.dto.RoundPreviewStarted?,
    wide: Boolean,
    now: Long
) {
    var selectedVoteId by remember(voting.roundIndex) { mutableStateOf<String?>(null) }
    var submitted by remember(voting.roundIndex) { mutableStateOf(false) }
    var observedVoteErrorSequence by remember(voting.roundIndex) { mutableStateOf(state.voteErrorSequence) }
    LaunchedEffect(state.voteErrorSequence, state.voteErrorRoundIndex, voting.roundIndex) {
        if (state.voteErrorSequence > observedVoteErrorSequence) {
            if (state.voteErrorRoundIndex == voting.roundIndex) {
                selectedVoteId = null
                submitted = false
            }
            observedVoteErrorSequence = state.voteErrorSequence
        }
    }
    val totalVotes = state.totalVotes.takeIf { it > 0 } ?: voting.players.count { it.connected }
    val secondsLeft = secondsRemaining(voting.votingDeadlineEpochMillis, now)
    val choices = voting.players.map { VoteChoice(it.id, it.displayName, it.avatarId, isSelf = it.id == state.selfPlayerId) } +
        VoteChoice(GameConstants.DECOY_ID, "Nobody / Decoy", "cloud", isDecoy = true)
    val optionRows = if (wide) choices.chunked(2) else choices.map { listOf(it) }
    GamePanel("Who submitted this song?", "Choose the player you think picked the track.") {
        preview?.let { VoteTrackSummary(it, wide) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                Text("${secondsLeft}s left", modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontWeight = FontWeight.Black)
            }
            Text("${state.votesCast.coerceAtMost(totalVotes)}/$totalVotes voted", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { if (totalVotes == 0) 0f else (state.votesCast.toFloat() / totalVotes).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        optionRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { choice ->
                    val selected = choice.id == selectedVoteId
                    val enabled = !submitted && !choice.isSelf && secondsLeft > 0
                    Surface(
                        modifier = Modifier.weight(1f).heightIn(min = 88.dp)
                            .clickable(enabled = enabled) { selectedVoteId = choice.id },
                        color = when {
                            selected -> MaterialTheme.colorScheme.primaryContainer
                            choice.isSelf -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            WebAvatarSwatch(choice.avatarId)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(choice.name, fontWeight = FontWeight.Bold)
                                Text(
                                    when {
                                        choice.isSelf -> "You · choose someone else"
                                        choice.isDecoy -> "The song belongs to nobody here"
                                        selected -> "Selected"
                                        submitted -> "Vote locked"
                                        else -> "Tap to choose"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (wide && row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Button(
            onClick = {
                selectedVoteId?.let { voteId ->
                    store.castVote(voteId)
                    submitted = true
                }
            },
            enabled = selectedVoteId != null && !submitted && secondsLeft > 0,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (submitted) "Vote submitted" else "Confirm vote", fontWeight = FontWeight.Bold) }
        Text(
            when {
                submitted -> "Your vote is locked in. Waiting for the rest of the room…"
                secondsLeft == 0L -> "Voting has closed. The answer is about to be revealed."
                selectedVoteId == null -> "Choose one card, then confirm your vote."
                else -> "Your choice is ready. Confirm when you're sure."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VoteTrackSummary(preview: com.guesswhosesong.shared.dto.RoundPreviewStarted, wide: Boolean) {
    if (wide) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AbstractCoverPlaceholder(maxSize = 180.dp, modifier = Modifier.width(180.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Now guessing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Text(preview.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(preview.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        AbstractCoverPlaceholder(maxSize = 220.dp)
        Text("Now guessing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        Text(preview.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(preview.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class VoteChoice(val id: String, val name: String, val avatarId: String, val isSelf: Boolean = false, val isDecoy: Boolean = false)

@Composable
private fun RevealStage(reveal: com.guesswhosesong.shared.dto.RoundRevealed, startedAt: Long, now: Long) {
    val revealSeconds = ((5_000L - (now - startedAt).coerceAtLeast(0L)).coerceAtLeast(0L) + 999L) / 1_000L
    GamePanel("The song was", "Round ${reveal.roundIndex + 1} is in the books.") {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
            Text(
                if (revealSeconds > 0) "Next round in ${revealSeconds}s" else "Next round starting…",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(reveal.songEntry.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(reveal.songEntry.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Picked by ${reveal.submitterName}", modifier = Modifier.padding(top = 5.dp), fontWeight = FontWeight.Bold)
            }
        }
        Text("Round scores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        reveal.scoreDeltas.forEach { score ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(score.playerName, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Text("+${score.delta}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(12.dp))
                Text("${score.newTotal} pts", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Everyone's guesses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
        reveal.voteResults.forEach { vote ->
            Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(vote.voterName, fontWeight = FontWeight.SemiBold)
                        Text("Guessed ${vote.guessedPlayerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (vote.correct) "Correct" else "Miss", color = if (vote.correct) Color(0xFF397B3F) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GamePlayersPanel(state: WebUiState) {
    GamePanel("In this round", "${state.room?.players?.size ?: 0} players in the room.") {
        state.room?.players.orEmpty().forEach { player ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WebAvatarSwatch(player.avatarId)
                Spacer(Modifier.width(10.dp))
                Text(player.displayName, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Text("${player.score}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ResultsPage(store: WebGameStore, state: WebUiState) {
    val room = state.room
    val isHost = room?.hostId == state.selfPlayerId
    val players = state.results?.players.orEmpty()
    PostJoinFrame(
        title = "Final scores",
        kicker = "THAT'S THE GAME",
        description = players.firstOrNull()?.let { "${it.displayName} takes the top spot. How well did you know your friends' taste?" } ?: "The final standings will show here.",
        state = state
    ) { wide ->
        val scoreColumn: @Composable () -> Unit = {
            if (players.isEmpty()) {
                GamePanel("Counting up the scores", "The final standings will be ready in a moment.") {
                    CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp), strokeWidth = 2.dp)
                }
            } else {
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        players.take(3).forEachIndexed { index, player ->
                            ResultPodiumCard(index, player, state.selfPlayerId, Modifier.weight(1f))
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        players.take(3).forEachIndexed { index, player ->
                            ResultPodiumCard(index, player, state.selfPlayerId, Modifier.fillMaxWidth())
                        }
                    }
                }
                if (players.size > 3) {
                    GamePanel("Full standings", "Every player, in final order.") {
                        players.drop(3).forEachIndexed { offset, player ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${(offset + 4).toString().padStart(2, '0')}", modifier = Modifier.width(40.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                                WebAvatarSwatch(player.avatarId)
                                Spacer(Modifier.width(12.dp))
                                Text(player.displayName + if (player.id == state.selfPlayerId) " (you)" else "", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                Text("${player.score} pts", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        val actionColumn: @Composable () -> Unit = {
            GamePanel("One more round?", if (isHost) "Start another game with this room." else "The host can start another game whenever you're ready.") {
                if (isHost) {
                    Button(onClick = store::playAgain, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Play again", fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = store::endRoom, modifier = Modifier.fillMaxWidth()) { Text("End room") }
                }
                OutlinedButton(onClick = store::leaveRoom, modifier = Modifier.fillMaxWidth()) { Text("Leave room") }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 8.dp)) {
                Column(Modifier.weight(1.45f), verticalArrangement = Arrangement.spacedBy(16.dp)) { scoreColumn() }
                Column(Modifier.weight(0.78f), verticalArrangement = Arrangement.spacedBy(16.dp)) { actionColumn() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                scoreColumn()
                actionColumn()
            }
        }
    }
}

@Composable
private fun ResultPodiumCard(index: Int, player: Player, selfPlayerId: String, modifier: Modifier) {
    GamePanel(
        title = "${index + 1} place",
        subtitle = if (player.id == selfPlayerId) "You" else "Final score",
        modifier = modifier
    ) {
        WebAvatarSwatch(player.avatarId)
        Text(player.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text("${player.score} pts", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ChatPanel(store: WebGameStore, state: WebUiState) {
    GamePanel("Room chat", "Talk it out while the songs play.") {
        if (state.chat.isEmpty()) {
            Text("No messages yet. Say hi to the room.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.chat.takeLast(10).forEach { message ->
                Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(message.senderName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(message.text)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = state.chatDraft, onValueChange = store::setChatDraft, label = { Text("Message") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = store::sendChat, enabled = state.chatDraft.isNotBlank()) { Text("Send") }
        }
    }
}

@Composable
private fun rememberClockNow(): Long {
    var now by remember { mutableStateOf(currentEpochMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = currentEpochMillis()
            delay(250L)
        }
    }
    return now
}

private fun secondsRemaining(deadline: Long, now: Long): Long {
    if (deadline <= 0L) return 0L
    return ((deadline - now).coerceAtLeast(0L) + 999L) / 1_000L
}

private fun deadlineLabel(deadline: Long, now: Long = currentEpochMillis()): String {
    if (deadline <= 0L) return ""
    val seconds = secondsRemaining(deadline, now)
    return "$seconds seconds remaining"
}
