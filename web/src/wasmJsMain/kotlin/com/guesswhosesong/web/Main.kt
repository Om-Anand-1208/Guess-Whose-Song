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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.AvatarCustomizationCatalog
import com.guesswhosesong.shared.models.GameConstants
import com.guesswhosesong.shared.models.GameMode
import com.guesswhosesong.shared.models.RoundCountRules
import com.guesswhosesong.web.generated.resources.Res
import com.guesswhosesong.web.generated.resources.google_g_logo
import com.guesswhosesong.web.generated.resources.spotify_official_icon
import com.guesswhosesong.web.generated.resources.welcome_hero_desktop
import com.guesswhosesong.web.generated.resources.welcome_hero_mobile
import com.guesswhosesong.web.generated.resources.welcome_hero_dark_scene
import com.guesswhosesong.web.generated.resources.comfortaa_bold
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import kotlinx.browser.document
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin

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

private val PostJoinDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B69FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF30284F),
    onPrimaryContainer = Color(0xFFE8E1FF),
    secondary = Color(0xFFFFD456),
    onSecondary = Color(0xFF171717),
    secondaryContainer = Color(0xFF55491F),
    onSecondaryContainer = Color(0xFFFFEAA8),
    tertiary = Color(0xFF9BEA61),
    onTertiary = Color(0xFF162100),
    background = Color(0xFF17191F),
    onBackground = Color(0xFFF6F5F8),
    surface = Color(0xFF202229),
    onSurface = Color(0xFFF6F5F8),
    surfaceVariant = Color(0xFF2A2D36),
    onSurfaceVariant = Color(0xFFB7B9C7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF555966),
    outlineVariant = Color(0xFF383B45)
)

private enum class WebAppearanceMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark");

    companion object {
        fun parse(value: String?): WebAppearanceMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

private data class WebAppearanceControls(val dark: Boolean, val toggle: () -> Unit)

private val LocalWebAppearanceControls = staticCompositionLocalOf {
    WebAppearanceControls(dark = false, toggle = {})
}

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
    var settingsPageOpen by remember { mutableStateOf(false) }
    var appearanceMode by remember { mutableStateOf(WebAppearanceMode.parse(localGet("gws_post_join_appearance"))) }
    val updateAppearance: (WebAppearanceMode) -> Unit = { mode ->
        localSet("gws_post_join_appearance", mode.name)
        appearanceMode = mode
    }
    val appearanceDark = when (appearanceMode) {
        WebAppearanceMode.SYSTEM -> systemPrefersDark()
        WebAppearanceMode.LIGHT -> false
        WebAppearanceMode.DARK -> true
    }
    val usePostJoinDark = state.page in setOf(WebPage.LOBBY, WebPage.SUBMISSION, WebPage.GAME, WebPage.RESULTS) && appearanceDark
    val appearanceControls = remember(appearanceDark) {
        WebAppearanceControls(appearanceDark) {
            updateAppearance(if (appearanceDark) WebAppearanceMode.LIGHT else WebAppearanceMode.DARK)
        }
    }
    LaunchedEffect(state.page) {
        if (state.page !in setOf(WebPage.JOIN, WebPage.LOBBY, WebPage.SUBMISSION, WebPage.GAME)) settingsPageOpen = false
    }
    DisposableEffect(Unit) { onDispose { store.close() } }

    CompositionLocalProvider(LocalWebAppearanceControls provides appearanceControls) {
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
                    colorScheme = if (appearanceDark) PostJoinDarkColorScheme else PostJoinColorScheme,
                    shapes = Shapes(
                        small = RoundedCornerShape(12.dp),
                        medium = RoundedCornerShape(20.dp),
                        large = RoundedCornerShape(28.dp)
                    )
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (settingsPageOpen) {
                            PlayerSettingsPage(
                                store = store,
                                state = state,
                                status = authStatus,
                                user = user,
                                onBackToGame = { settingsPageOpen = false },
                                appearanceMode = appearanceMode,
                                onAppearanceChange = updateAppearance
                            )
                        } else {
                            JoinPage(store, state, onSettings = { settingsPageOpen = true })
                        }
                    }
                }
            } else {
                MaterialTheme(
                    colorScheme = if (usePostJoinDark) PostJoinDarkColorScheme else PostJoinColorScheme,
                    shapes = Shapes(
                        small = RoundedCornerShape(12.dp),
                        medium = RoundedCornerShape(20.dp),
                        large = RoundedCornerShape(28.dp)
                    )
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when (state.page) {
                            WebPage.JOIN -> Unit
                            WebPage.LOBBY -> if (settingsPageOpen) {
                                PlayerSettingsPage(
                                    store, state, authStatus, user,
                                    onBackToGame = { settingsPageOpen = false },
                                    appearanceMode = appearanceMode,
                                    onAppearanceChange = updateAppearance
                                )
                            } else {
                                LobbyPage(store, state, onSettings = { settingsPageOpen = true })
                            }
                            WebPage.SUBMISSION -> if (settingsPageOpen) {
                                PlayerSettingsPage(store, state, authStatus, user, { settingsPageOpen = false }, appearanceMode, updateAppearance)
                            } else {
                                SubmissionPage(store, state, onSettings = { settingsPageOpen = true })
                            }
                            WebPage.GAME -> if (settingsPageOpen) {
                                PlayerSettingsPage(store, state, authStatus, user, { settingsPageOpen = false }, appearanceMode, updateAppearance)
                            } else {
                                GamePage(store, state, onSettings = { settingsPageOpen = true })
                            }
                            WebPage.RESULTS -> ResultsPage(store, state)
                        }
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
    state: WebUiState,
    onSettings: (() -> Unit)? = null,
    useSharedHeader: Boolean = true,
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
                if (useSharedHeader) {
                    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp)) {
                        val compactHeader = maxWidth < 460.dp
                        Column(verticalArrangement = Arrangement.spacedBy(if (compactHeader) 3.dp else 0.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                WebWordmark(
                                    modifier = Modifier.weight(1f),
                                    fontSize = if (compactHeader) 18.sp else 22.sp,
                                    lineHeight = if (compactHeader) 20.sp else 25.sp
                                )
                                if (!compactHeader) state.room?.joinCode?.let { WebRoomCodeBadge(it) }
                                WebAppearanceToggleButton()
                                onSettings?.let { openSettings -> WebSettingsHeaderButton(openSettings) }
                            }
                            if (compactHeader) {
                                state.room?.joinCode?.let { code ->
                                    Box(Modifier.fillMaxWidth()) {
                                        WebRoomCodeBadge(code, Modifier.align(Alignment.CenterEnd))
                                    }
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                        state.room?.joinCode?.let { code ->
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text("ROOM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Text(code, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                                }
                            }
                        }
                        onSettings?.let { openSettings ->
                            TextButton(onClick = openSettings) { Text("⚙", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
                state.notice?.let { MessageBanner(it, isError = false) }
                state.error?.let { MessageBanner(it, isError = true) }
                content(wide)
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun WebWordmark(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 25.sp
) {
    val family = FontFamily(Font(Res.font.comfortaa_bold))
    val compact = fontSize.value <= 20f
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "Guess Whose Song" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        WebBrandConfetti(left = true, compact = compact)
        Spacer(Modifier.width(if (compact) 3.dp else 4.dp))
        Column(verticalArrangement = Arrangement.spacedBy((-3).dp)) {
            Text("Guess", fontFamily = family, fontSize = fontSize, lineHeight = lineHeight, letterSpacing = (-0.8).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Text("Whose Song", fontFamily = family, fontSize = fontSize, lineHeight = lineHeight, letterSpacing = (-0.8).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
        }
        Spacer(Modifier.width(3.dp))
        WebBrandConfetti(left = false, compact = compact)
    }
}

@Composable
private fun WebBrandConfetti(left: Boolean, compact: Boolean) {
    Canvas(Modifier.size(width = if (compact) 16.dp else 19.dp, height = if (compact) 48.dp else 58.dp)) {
        val stroke = (if (compact) 3.5.dp else 4.dp).toPx()
        val cap = StrokeCap.Round
        if (left) {
            drawLine(Color(0xFFFFAA3D), Offset(size.width * 0.55f, size.height * 0.15f), Offset(size.width * 0.9f, size.height * 0.36f), stroke, cap)
            drawLine(Color(0xFFFF72A7), Offset(size.width * 0.08f, size.height * 0.55f), Offset(size.width * 0.46f, size.height * 0.62f), stroke, cap)
        } else {
            drawLine(Color(0xFF9DEBC1), Offset(size.width * 0.12f, size.height * 0.19f), Offset(size.width * 0.48f, size.height * 0.36f), stroke, cap)
            drawLine(Color(0xFFFFAA3D), Offset(size.width * 0.57f, size.height * 0.1f), Offset(size.width * 0.92f, size.height * 0.34f), stroke, cap)
            drawLine(Color(0xFF8F7CF7), Offset(size.width * 0.58f, size.height * 0.65f), Offset(size.width * 0.89f, size.height * 0.58f), stroke, cap)
        }
    }
}

@Composable
private fun WebAppearanceToggleButton(modifier: Modifier = Modifier) {
    val appearance = LocalWebAppearanceControls.current
    WebHeaderIconButton(
        icon = if (appearance.dark) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
        description = if (appearance.dark) "Switch to light mode" else "Switch to dark mode",
        onClick = appearance.toggle,
        modifier = modifier
    )
}

@Composable
private fun WebSettingsHeaderButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    WebHeaderIconButton(
        icon = Icons.Filled.Settings,
        description = "Appearance and player settings",
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun WebHeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = description }
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun WebRoomCodeBadge(code: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(17.dp)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("ROOM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(code.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun FrontPageFrame(
    state: WebUiState,
    isSavePlayer: Boolean,
    onHeaderAction: () -> Unit,
    wrapContent: Boolean = true,
    footer: @Composable () -> Unit = {},
    content: @Composable (compact: Boolean, wide: Boolean) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Use the same side-by-side hero composition on landscape tablets and desktop.
        val availableHeight = maxHeight
        val wide = maxWidth >= 960.dp
        val compactHeader = maxWidth < 380.dp
        val compactActions = compactHeader || availableHeight < 620.dp
        val appearance = LocalWebAppearanceControls.current
        val horizontalPadding = when {
            wide -> 36.dp
            maxWidth < 360.dp -> 14.dp
            else -> 18.dp
        }
        if (!isSavePlayer) {
            Image(
                painter = painterResource(
                    when {
                        appearance.dark -> Res.drawable.welcome_hero_dark_scene
                        wide -> Res.drawable.welcome_hero_desktop
                        else -> Res.drawable.welcome_hero_mobile
                    }
                ),
                contentDescription = null,
                contentScale = if (appearance.dark) ContentScale.Crop else ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!wide && !isSavePlayer) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .heightIn(max = (availableHeight * 0.36f).coerceAtLeast(200.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compactHeader) 10.dp else 14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FrontPageWordmark(wide = false, compact = compactHeader)
                        Spacer(Modifier.weight(1f))
                        FrontPageHeaderAction(isSavePlayer = false, onClick = onHeaderAction)
                    }
                    FrontPageTagline(compact = compactHeader, wide = false)
                    state.notice?.let { MessageBanner(it, isError = false) }
                    state.error?.let { MessageBanner(it, isError = true) }
                }

                FrontPageCard(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 12.dp),
                    contentPadding = if (compactActions) 12.dp else 16.dp
                ) {
                    content(compactActions, wide)
                }
            }
        } else if (isSavePlayer && !wrapContent) {
            val compactSettings = compactHeader || availableHeight < 760.dp
            Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .widthIn(max = 1320.dp)
                        .padding(horizontal = horizontalPadding, vertical = if (compactSettings) 8.dp else 18.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FrontPageWordmark(wide = wide, compact = compactSettings)
                        Spacer(Modifier.weight(1f))
                        FrontPageHeaderAction(isSavePlayer = true, onClick = onHeaderAction)
                    }
                    state.notice?.let { MessageBanner(it, isError = false) }
                    state.error?.let { MessageBanner(it, isError = true) }
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .widthIn(max = 1320.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = horizontalPadding)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(if (compactSettings) 8.dp else 14.dp)
                    ) {
                        content(compactSettings, wide)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .widthIn(max = 1320.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = horizontalPadding, vertical = if (compactSettings) 6.dp else 10.dp)
                ) {
                    footer()
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .widthIn(max = 1320.dp)
                        .padding(horizontal = horizontalPadding, vertical = if (compactHeader) 10.dp else 18.dp)
                        .align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(if (compactHeader) 12.dp else 18.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FrontPageWordmark(wide = wide, compact = compactHeader)
                        Spacer(Modifier.weight(1f))
                        FrontPageHeaderAction(isSavePlayer, onHeaderAction)
                    }
                    if (!isSavePlayer) FrontPageTagline(compact = compactHeader, wide = wide)
                    state.notice?.let { MessageBanner(it, isError = false) }
                    state.error?.let { MessageBanner(it, isError = true) }
                    if (wide) {
                        if (isSavePlayer) {
                            if (wrapContent) {
                                FrontPageCard(
                                    Modifier.fillMaxWidth().widthIn(max = 960.dp)
                                        .align(Alignment.CenterHorizontally)
                                ) { content(compactHeader, wide) }
                            } else {
                                content(compactHeader, wide)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .heightIn(min = (availableHeight - 150.dp).coerceAtLeast(420.dp)),
                                horizontalArrangement = Arrangement.spacedBy(26.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Spacer(Modifier.weight(1.2f))
                                FrontPageCard(
                                    Modifier.weight(0.8f).align(Alignment.CenterVertically)
                                ) { content(compactActions, wide) }
                            }
                        }
                    } else {
                        if (isSavePlayer && !wrapContent) {
                            content(compactHeader, wide)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                FrontPageCard(Modifier.fillMaxWidth()) { content(compactHeader, wide) }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun FrontPageWordmark(wide: Boolean, compact: Boolean = false) {
    val logoSize = when {
        wide -> 28.sp
        compact -> 19.sp
        else -> 23.sp
    }
    val logoLineHeight = when {
        wide -> 31.sp
        compact -> 21.sp
        else -> 26.sp
    }
    WebWordmark(fontSize = logoSize, lineHeight = logoLineHeight)
}

@Composable
private fun FrontPageTagline(compact: Boolean, wide: Boolean) {
    val fontSize = when {
        wide -> 40.sp
        compact -> 18.sp
        else -> 24.sp
    }
    val lineHeight = when {
        wide -> 48.sp
        compact -> 23.sp
        else -> 30.sp
    }
    FrontPageDisplayText(
        "Your friends picked the songs.\nCan you guess who?",
        fontSize = fontSize,
        lineHeight = lineHeight,
        strokeWidth = if (compact) 0.9.dp else 1.2.dp,
        maxLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
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
        Text(text, modifier = Modifier.clearAndSetSemantics { }, color = MaterialTheme.colorScheme.onBackground, style = style.copy(drawStyle = Stroke(width = strokeWidthPx)), maxLines = maxLines, textAlign = textAlign)
        Text(text, color = MaterialTheme.colorScheme.onBackground, style = style, maxLines = maxLines, textAlign = textAlign)
    }
}

@Composable
private fun FrontPageHeaderAction(isSavePlayer: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (isSavePlayer) {
        TextButton(onClick = onClick, modifier = modifier) {
            Text("Back", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    } else {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            WebAppearanceToggleButton()
            WebSettingsHeaderButton(onClick)
        }
    }
}

@Composable
private fun FrontPageCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 22.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            content()
        }
    }
}

@Composable
private fun JoinPage(
    store: WebGameStore,
    state: WebUiState,
    onSettings: () -> Unit
) {
    FrontPageFrame(state, isSavePlayer = false, onHeaderAction = onSettings) { compact, _ ->
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
            Button(
                onClick = store::createRoom,
                enabled = !state.isBusy,
                border = BorderStroke(1.dp, Color(0xFFC96B24)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF983D), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 54.dp)
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
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = store::joinRoom,
                enabled = !state.isBusy,
                border = BorderStroke(1.dp, Color(0xFF7769D9)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAEA4FF), contentColor = Color(0xFF17161A)),
                modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 52.dp)
            ) { Text("Join room", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PlayerSettingsPage(
    store: WebGameStore,
    state: WebUiState,
    status: AuthStatus,
    user: WebUser?,
    onBackToGame: () -> Unit,
    appearanceMode: WebAppearanceMode = WebAppearanceMode.SYSTEM,
    onAppearanceChange: (WebAppearanceMode) -> Unit = {}
) {
    var draftName by remember(state.displayName) { mutableStateOf(state.displayName) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var characterEditorOpen by remember { mutableStateOf(false) }
    val isGoogleLinked = user?.isGoogleLinked == true
    val isHost = state.room?.hostId == state.selfPlayerId

    if (characterEditorOpen) {
        CharacterEditorPage(
            state = state,
            status = status,
            user = user,
            onLinkGoogle = store::linkGoogle,
            onBackToSettings = { characterEditorOpen = false },
            onSaveCharacter = { customization ->
                store.savePlayerProfile(state.displayName, customization)
                characterEditorOpen = false
            }
        )
    } else {
        FrontPageFrame(
            state,
            isSavePlayer = true,
            onHeaderAction = onBackToGame,
            wrapContent = false,
            footer = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onBackToGame, modifier = Modifier.weight(1f).height(48.dp)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val cleanName = draftName.trim()
                            if (cleanName.isBlank() || cleanName.length > 24 || cleanName.any(Char::isISOControl)) {
                                nameError = "Enter a name with 1 to 24 characters"
                            } else {
                                store.savePlayerProfile(cleanName, state.avatarCustomization)
                                onBackToGame()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF983D), contentColor = Color(0xFF17161A)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("Save settings", fontWeight = FontWeight.Black) }
                }
            }
        ) { compact, wide ->
            Column(verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 14.dp)) {
                if (state.page in setOf(WebPage.LOBBY, WebPage.SUBMISSION, WebPage.GAME)) {
                    GamePanel("Appearance") {
                        SettingChoices("Theme", listOf("System", "Light", "Dark"), appearanceMode.label) { selected ->
                            onAppearanceChange(WebAppearanceMode.entries.first { it.label == selected })
                        }
                    }
                }
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(Modifier.weight(1f)) {
                            PlayerIdentityPanel(
                                state = state,
                                draftName = draftName,
                                nameError = nameError,
                                compact = compact,
                                onNameChange = { draftName = it.take(24); nameError = null },
                                onEditCharacter = { characterEditorOpen = true }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            PlayerConnectionsPanel(
                                status = status,
                                isGoogleLinked = isGoogleLinked,
                                allowRecovery = state.room == null,
                                spotifyConnected = state.spotifyConnected,
                                compact = compact,
                                onLinkGoogle = store::linkGoogle,
                                onDisconnectGoogle = store::disconnectGoogle,
                                onRecoverGoogle = store::recoverGoogle,
                                onConnectSpotify = store::connectSpotify,
                                onDisconnectSpotify = store::disconnectSpotify,
                                onRefreshSpotify = store::refreshSpotify
                            )
                        }
                    }
                } else {
                    PlayerIdentityPanel(
                        state = state,
                        draftName = draftName,
                        nameError = nameError,
                        compact = compact,
                        onNameChange = { draftName = it.take(24); nameError = null },
                        onEditCharacter = { characterEditorOpen = true }
                    )
                    PlayerConnectionsPanel(
                        status = status,
                        isGoogleLinked = isGoogleLinked,
                        allowRecovery = state.room == null,
                        spotifyConnected = state.spotifyConnected,
                        compact = compact,
                        onLinkGoogle = store::linkGoogle,
                        onDisconnectGoogle = store::disconnectGoogle,
                        onRecoverGoogle = store::recoverGoogle,
                        onConnectSpotify = store::connectSpotify,
                        onDisconnectSpotify = store::disconnectSpotify,
                        onRefreshSpotify = store::refreshSpotify
                    )
                }

                if (isHost) {
                    GamePanel("Room settings") {
                        LobbySettings(
                            store,
                            state.room.settings,
                            state.room.players.size,
                            state.room.players.any { it.isComputer }
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun PlayerIdentityPanel(
    state: WebUiState,
    draftName: String,
    nameError: String?,
    compact: Boolean,
    onNameChange: (String) -> Unit,
    onEditCharacter: () -> Unit
) {
    GamePanel("Your player") {
        OutlinedTextField(
            value = draftName,
            onValueChange = onNameChange,
            label = { Text("Display name") },
            placeholder = { Text("dashingbuilder") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        nameError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(
                modifier = Modifier.size(if (compact) 60.dp else 78.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFF8EC),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    WebAvatarCharacter(state.avatarCustomization, Modifier.fillMaxSize().padding(if (compact) 4.dp else 6.dp))
                }
            }
            OutlinedButton(onClick = onEditCharacter, modifier = Modifier.weight(1f).height(if (compact) 44.dp else 52.dp)) {
                Text("Edit character")
            }
        }
    }
}

@Composable
private fun PlayerConnectionsPanel(
    status: AuthStatus,
    isGoogleLinked: Boolean,
    allowRecovery: Boolean,
    spotifyConnected: Boolean,
    compact: Boolean,
    onLinkGoogle: () -> Unit,
    onDisconnectGoogle: () -> Unit,
    onRecoverGoogle: () -> Unit,
    onConnectSpotify: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    onRefreshSpotify: () -> Unit
) {
    GamePanel("Connections") {
        when (status) {
            AuthStatus.LOADING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                Text("Checking Google account…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AuthStatus.ERROR -> Text("Google account linking is unavailable. Check Firebase web configuration.", color = MaterialTheme.colorScheme.error)
            AuthStatus.READY -> {
                OutlinedButton(
                    onClick = if (isGoogleLinked) onDisconnectGoogle else onLinkGoogle,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 52.dp)
                ) {
                    GoogleGMark()
                    Spacer(Modifier.width(10.dp))
                    Text(if (isGoogleLinked) "Disconnect" else "Connect Google")
                }
                if (!isGoogleLinked && allowRecovery) TextButton(onClick = onRecoverGoogle, modifier = Modifier.fillMaxWidth().height(if (compact) 40.dp else 48.dp)) {
                    Text("Log in to existing account")
                }
            }
        }
        OutlinedButton(
            onClick = if (spotifyConnected) onDisconnectSpotify else onConnectSpotify,
            modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 52.dp),
            border = BorderStroke(1.dp, Color(0xFF1DB954))
        ) {
            SpotifyMark()
            Spacer(Modifier.width(12.dp))
            Text(if (spotifyConnected) "Disconnect" else "Connect Spotify")
        }
        Box(Modifier.fillMaxWidth().height(if (compact) 32.dp else 40.dp), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onRefreshSpotify) { Text("Refresh Spotify status") }
        }
    }
}

@Composable
private fun GoogleGMark() {
    Image(
        painter = painterResource(Res.drawable.google_g_logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun SpotifyMark() {
    Image(
        painter = painterResource(Res.drawable.spotify_official_icon),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(24.dp)
    )
}

private enum class AvatarEditorPart(val title: String) {
    SHAPE("Shape"), COLOR("Color"), EYES("Eyes"), MOUTH("Mouth"), ACCESSORY("Accessory")
}

@Composable
private fun CharacterEditorPage(
    state: WebUiState,
    status: AuthStatus,
    user: WebUser?,
    onLinkGoogle: () -> Unit,
    onBackToSettings: () -> Unit,
    onSaveCharacter: (AvatarCustomization) -> Unit
) {
    var customization by remember(state.avatarCustomization) { mutableStateOf(state.avatarCustomization) }
    var selectedPart by remember { mutableStateOf(AvatarEditorPart.SHAPE) }
    val isGoogleLinked = user?.isGoogleLinked == true

    FrontPageFrame(state, isSavePlayer = true, onHeaderAction = onBackToSettings) { _, _ ->
        if (isGoogleLinked) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Character editor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 142.dp, max = 190.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        WebAvatarCharacter(customization, Modifier.size(142.dp).padding(8.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarEditorPart.values().forEach { part ->
                        AvatarEditorPartButton(
                            part = part,
                            selected = part == selectedPart,
                            customization = customization,
                            onClick = { selectedPart = part }
                        )
                    }
                }
                AvatarOptionsGrid(
                    part = selectedPart,
                    customization = customization,
                    onSelected = { customization = it }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onBackToSettings, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSaveCharacter(customization) },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Save character", fontWeight = FontWeight.Bold) }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Character editor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                when (status) {
                    AuthStatus.LOADING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                    AuthStatus.ERROR -> Text("Google sign-in is unavailable.", color = MaterialTheme.colorScheme.error)
                    AuthStatus.READY -> OutlinedButton(
                        onClick = onLinkGoogle,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        GoogleGMark()
                        Spacer(Modifier.width(10.dp))
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarEditorPartButton(
    part: AvatarEditorPart,
    selected: Boolean,
    customization: AvatarCustomization,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFFF8EC))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${part.title} options${if (selected) ", selected" else ""}"
                role = Role.RadioButton
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        if (part == AvatarEditorPart.SHAPE) {
            WebAvatarCharacter(customization, Modifier.size(32.dp))
        } else {
            Canvas(Modifier.size(25.dp)) {
                val ink = Color(0xFF17161A)
                val strokeWidth = 2.2.dp.toPx()
                when (part) {
                    AvatarEditorPart.SHAPE -> Unit
                    AvatarEditorPart.COLOR -> {
                        val radius = size.minDimension * 0.19f
                        listOf(
                            Color(0xFFFFAA3D), Color(0xFFB8F45D),
                            Color(0xFF9B8CFF), Color(0xFF8FD8F2)
                        ).forEachIndexed { index, color ->
                            val x = if (index % 2 == 0) 0.34f else 0.66f
                            val y = if (index < 2) 0.34f else 0.66f
                            drawCircle(color, radius, Offset(size.width * x, size.height * y))
                        }
                    }
                    AvatarEditorPart.EYES -> {
                        drawCircle(ink, size.minDimension * 0.105f, Offset(size.width * 0.34f, size.height * 0.45f))
                        drawCircle(ink, size.minDimension * 0.105f, Offset(size.width * 0.66f, size.height * 0.45f))
                    }
                    AvatarEditorPart.MOUTH -> {
                        val path = Path().apply {
                            moveTo(size.width * 0.22f, size.height * 0.45f)
                            quadraticTo(size.width * 0.5f, size.height * 0.78f, size.width * 0.78f, size.height * 0.45f)
                        }
                        drawPath(path, ink, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    }
                    AvatarEditorPart.ACCESSORY -> {
                        val path = Path().apply {
                            moveTo(size.width * 0.18f, size.height * 0.62f)
                            cubicTo(size.width * 0.2f, size.height * 0.15f, size.width * 0.8f, size.height * 0.15f, size.width * 0.82f, size.height * 0.62f)
                        }
                        drawPath(path, ink, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                        drawRoundRect(ink, Offset(size.width * 0.1f, size.height * 0.5f), Size(size.width * 0.18f, size.height * 0.34f), CornerRadius(strokeWidth))
                        drawRoundRect(ink, Offset(size.width * 0.72f, size.height * 0.5f), Size(size.width * 0.18f, size.height * 0.34f), CornerRadius(strokeWidth))
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarOptionsGrid(
    part: AvatarEditorPart,
    customization: AvatarCustomization,
    onSelected: (AvatarCustomization) -> Unit
) {
    val options = when (part) {
        AvatarEditorPart.SHAPE -> AvatarCustomizationCatalog.shapeIds
        AvatarEditorPart.COLOR -> AvatarCustomizationCatalog.colorIds
        AvatarEditorPart.EYES -> AvatarCustomizationCatalog.eyesIds
        AvatarEditorPart.MOUTH -> AvatarCustomizationCatalog.mouthIds
        AvatarEditorPart.ACCESSORY -> AvatarCustomizationCatalog.accessoryIds
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(4).forEach { rowOptions ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    val selected = when (part) {
                        AvatarEditorPart.SHAPE -> customization.shapeId == option
                        AvatarEditorPart.COLOR -> customization.colorId == option
                        AvatarEditorPart.EYES -> customization.eyesId == option
                        AvatarEditorPart.MOUTH -> customization.mouthId == option
                        AvatarEditorPart.ACCESSORY -> customization.accessoryId == option
                    }
                    val appearance = when (part) {
                        AvatarEditorPart.SHAPE -> if (selected) customization else AvatarCustomization.defaultsFor(option)
                        AvatarEditorPart.COLOR -> customization.copy(colorId = option)
                        AvatarEditorPart.EYES -> customization.copy(eyesId = option)
                        AvatarEditorPart.MOUTH -> customization.copy(mouthId = option)
                        AvatarEditorPart.ACCESSORY -> customization.copy(accessoryId = option)
                    }
                    val accessibleName = when (part) {
                        AvatarEditorPart.SHAPE -> option.avatarShapeLabel()
                        AvatarEditorPart.COLOR -> option.optionLabel("Color")
                        else -> option.avatarLabel()
                    }
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFFFF8EC))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onSelected(appearance) }
                            .semantics {
                                contentDescription = "$accessibleName${if (selected) ", selected" else ""}"
                                role = Role.RadioButton
                                this.selected = selected
                            }
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WebAvatarCharacter(appearance, Modifier.fillMaxSize())
                    }
                }
                repeat(4 - rowOptions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun String.avatarLabel(): String = replaceFirstChar { it.uppercase() }

private fun String.avatarShapeLabel(): String = when (this) {
    "violet" -> "Triangle"
    "tangerine" -> "Honey"
    "berry" -> "Diamond"
    "mint" -> "Heart"
    else -> avatarLabel()
}

private fun String.optionLabel(group: String): String = when (group) {
    "Shape" -> avatarShapeLabel()
    "Color" -> when (this) {
        "sunny" -> "Orange"
        "violet" -> "Lavender"
        "cloud" -> "Sky blue"
        "star" -> "Yellow"
        "tangerine" -> "Coral"
        "berry" -> "Pink"
        "mint" -> "Mint"
        else -> "Lime"
    }
    else -> avatarLabel()
}

@Composable
private fun LobbyPage(store: WebGameStore, state: WebUiState, onSettings: () -> Unit) {
    val room = state.room
    val isHost = room?.hostId == state.selfPlayerId
    PostJoinFrame(
        title = "Room lobby",
        state = state,
        onSettings = onSettings
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
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 86.dp).padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WebAvatarCharacter(
                            AvatarCustomization.defaultsFor("lime").copy(mouthId = "open"),
                            Modifier.size(70.dp)
                        )
                        WebAvatarCharacter(
                            AvatarCustomization.defaultsFor("sunny").copy(eyesId = "sleepy", mouthId = "open", accessoryId = "headphones"),
                            Modifier.size(74.dp)
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Players", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 10.dp))
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                        Text("${room.players.size} / ${room.settings.playerLimit}", modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { copyTextToClipboard(room.joinCode) }) {
                        Text("♙ Invite friends", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                GamePanel("", "${room.players.count { it.connected }} connected · ${room.players.size} of ${room.settings.playerLimit} spots filled") {
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
                Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Game settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    TextButton(onClick = onSettings) { Text("✎ Edit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                }
                GamePanel("", "Make the round feel like your group.") {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        listOf(
                            Triple("♫", "Game mode", if (room.settings.gameMode == GameMode.SPOTIFY_RECENT) "Recent" else "Manual picks"),
                            Triple("▱", "Total rounds", room.settings.roundCount.toString()),
                            Triple("◷", "Voting time", "${room.settings.votingTimerSeconds}s"),
                            Triple("♟", "Player limit", room.settings.playerLimit.toString())
                        ).forEachIndexed { index, (icon, label, value) ->
                            if (index > 0) Box(Modifier.width(1.dp).height(68.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            Column(Modifier.weight(1f).padding(horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(icon, fontSize = 23.sp, fontWeight = FontWeight.Black, color = when (index) { 1 -> Color(0xFFFFB82E); 2 -> Color(0xFFFF7049); else -> MaterialTheme.colorScheme.primary })
                                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                if (isHost) {
                    val enoughRounds = RoundCountRules.isValid(room.settings.roundCount, room.players.size)
                    if (room.settings.gameMode == GameMode.MANUAL && room.players.none { it.isComputer } && room.players.size < room.settings.playerLimit) {
                        OutlinedButton(onClick = store::addComputerPlayer, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Add Computer Player") }
                    }
                    Button(
                        onClick = store::startGame,
                        enabled = room.players.size >= 2 && enoughRounds,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(64.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7557F4), contentColor = Color.White)
                    ) { Text("▶  Start game", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
                    when {
                        !enoughRounds -> Text("Set at least ${room.players.size} rounds in settings before starting.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        room.players.size < 2 -> Text("Invite one more player to unlock song selection.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                } else {
                    GamePanel("") {
                        Text("Waiting for host to start…", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        val sideColumn: @Composable () -> Unit = {
            if (room != null) {
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
private fun LobbySettings(store: WebGameStore, settings: RoomSettings, playerCount: Int, hasComputerPlayer: Boolean) {
    val minimumRounds = RoundCountRules.minimumForPlayerCount(playerCount)
    var selectedRounds by remember(settings.roundCount) { mutableStateOf(settings.roundCount) }
    val effectiveSettings = settings.copy(roundCount = selectedRounds)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingChoices(
            title = "Game mode",
            choices = if (hasComputerPlayer) listOf("Manual picks") else listOf("Manual picks", "Recently Played"),
            selectedValue = if (settings.gameMode == GameMode.SPOTIFY_RECENT) "Recently Played" else "Manual picks"
        ) { selected ->
            store.updateSettings(
                effectiveSettings.copy(
                    gameMode = if (selected == "Recently Played") GameMode.SPOTIFY_RECENT else GameMode.MANUAL
                )
            )
        }
        Text("Total rounds", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                onClick = {
                    selectedRounds = (selectedRounds - 1).coerceAtLeast(minimumRounds)
                    store.updateSettings(settings.copy(roundCount = selectedRounds))
                },
                enabled = selectedRounds > minimumRounds,
                modifier = Modifier.semantics { contentDescription = "Decrease rounds" }
            ) { Text("−", fontSize = 20.sp, fontWeight = FontWeight.Black) }
            Text("$selectedRounds rounds", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = {
                    selectedRounds = (selectedRounds + 1).coerceAtMost(RoundCountRules.MAX_ROUNDS)
                    store.updateSettings(settings.copy(roundCount = selectedRounds))
                },
                enabled = selectedRounds < RoundCountRules.MAX_ROUNDS,
                modifier = Modifier.semantics { contentDescription = "Increase rounds" }
            ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Black) }
        }
        SettingChoices("Player limit", listOf("2", "4", "6", "10", "20"), effectiveSettings.playerLimit.toString()) { selected ->
            store.updateSettings(effectiveSettings.copy(playerLimit = selected.toInt()))
        }
        SettingChoices("Voting time", listOf("10s", "15s", "20s", "30s"), "${effectiveSettings.votingTimerSeconds}s") { selected ->
            store.updateSettings(effectiveSettings.copy(votingTimerSeconds = selected.dropLast(1).toInt()))
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
private fun PlayerRow(player: Player, isSelf: Boolean, canKick: Boolean, onKick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        WebAvatarSwatch(player.avatarId, customization = player.avatarCustomization)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    player.displayName + if (isSelf) " (you)" else "",
                    fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Medium
                )
                if (player.isHost) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                    Text("HOST", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
                if (player.isComputer) Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                    Text("🤖 COMPUTER", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
            }
            Text(
                if (player.isComputer) "Picks songs and votes automatically" else if (player.connected) "Connected" else "Reconnecting…",
                style = MaterialTheme.typography.bodySmall,
                color = if (player.connected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        }
        if (canKick) TextButton(onClick = onKick) { Text("Remove", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun WebAvatarSwatch(
    avatarId: String,
    selected: Boolean = false,
    customization: AvatarCustomization? = null,
    size: Dp = 46.dp
) {
    val appearance = AvatarCustomization.normalize(customization, avatarId)
    val fillColor = webAvatarFill(appearance.colorId)
    Box(
        modifier = Modifier.size(size)
            .clip(RoundedCornerShape(15.dp))
            .background(fillColor.copy(alpha = 0.19f))
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(15.dp))
    ) {
        WebAvatarCharacter(appearance, Modifier.fillMaxSize().padding(size / 10f))
    }
}

@Composable
private fun WebAvatarCharacter(customization: AvatarCustomization, modifier: Modifier = Modifier) {
    val appearance = AvatarCustomization.normalize(customization, customization.shapeId)
    Canvas(modifier = modifier.aspectRatio(1f)) { drawWebAvatar(appearance) }
}

private val WebAvatarColors = mapOf(
    "sunny" to Color(0xFFFFAE29), "lime" to Color(0xFFB6F45B), "violet" to Color(0xFFA394F1),
    "tangerine" to Color(0xFFEF9387), "cloud" to Color(0xFFAED5F4), "star" to Color(0xFFFFD43B),
    "berry" to Color(0xFFF17FAC), "mint" to Color(0xFF73D9CA)
)

private val WebAvatarAccents = mapOf(
    "sunny" to Color(0xFFEF8F18), "lime" to Color(0xFF77C945), "violet" to Color(0xFF7060C8),
    "tangerine" to Color(0xFFCD675D), "cloud" to Color(0xFF79ACD7), "star" to Color(0xFFD49E22),
    "berry" to Color(0xFFC94C78), "mint" to Color(0xFF39AD9B)
)

private fun webAvatarFill(colorId: String): Color = WebAvatarColors[colorId] ?: WebAvatarColors.getValue("sunny")

private fun DrawScope.drawWebAvatar(customization: AvatarCustomization) {
    val s = size.minDimension
    val x = (size.width - s) / 2f
    val y = (size.height - s) / 2f
    val ink = Color(0xFF17161A)
    val fill = webAvatarFill(customization.colorId)
    val accent = WebAvatarAccents[customization.colorId] ?: Color(0xFFFFD56D)
    val stroke = (s * 0.034f).coerceAtLeast(1.1f)
    val outline = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(nx: Float, ny: Float) = Offset(x + s * nx, y + s * ny)

    fun drawBody(path: Path) {
        drawPath(path, fill)
        drawPath(path, ink, style = outline)
    }

    fun drawLegs(leftX: Float, rightX: Float, startY: Float) {
        drawLine(ink, p(leftX, startY), p(leftX - 0.02f, 0.93f), strokeWidth = stroke * 0.9f, cap = StrokeCap.Round)
        drawLine(ink, p(rightX, startY), p(rightX + 0.02f, 0.93f), strokeWidth = stroke * 0.9f, cap = StrokeCap.Round)
        drawOval(ink, p(leftX - 0.055f, 0.92f), Size(s * 0.09f, s * 0.035f))
        drawOval(ink, p(rightX - 0.035f, 0.92f), Size(s * 0.09f, s * 0.035f))
    }

    fun drawArms(leftX: Float, rightX: Float, armY: Float, leftHandY: Float, rightHandY: Float) {
        val leftHand = p(leftX - 0.12f, leftHandY)
        val rightHand = p(rightX + 0.12f, rightHandY)
        drawLine(ink, p(leftX, armY), leftHand, strokeWidth = stroke * 0.85f, cap = StrokeCap.Round)
        drawLine(ink, p(rightX, armY), rightHand, strokeWidth = stroke * 0.85f, cap = StrokeCap.Round)
        listOf(leftHand, rightHand).forEach { hand ->
            drawCircle(Color(0xFFFFFBF3), s * 0.023f, hand)
            drawCircle(ink, s * 0.023f, hand, style = Stroke(stroke * 0.55f))
        }
    }

    when (customization.shapeId) {
        "lime" -> { drawArms(0.22f, 0.78f, 0.55f, 0.56f, 0.56f); drawLegs(0.42f, 0.58f, 0.73f) }
        "violet" -> { drawArms(0.25f, 0.75f, 0.58f, 0.65f, 0.36f); drawLegs(0.42f, 0.58f, 0.75f) }
        "tangerine" -> { drawArms(0.24f, 0.76f, 0.56f, 0.59f, 0.59f); drawLegs(0.42f, 0.58f, 0.75f) }
        "berry" -> { drawArms(0.29f, 0.71f, 0.53f, 0.61f, 0.40f); drawLegs(0.44f, 0.56f, 0.79f) }
        "cloud" -> drawLegs(0.42f, 0.58f, 0.70f)
        "star" -> drawLegs(0.43f, 0.57f, 0.76f)
        "mint" -> drawLegs(0.43f, 0.57f, 0.77f)
        else -> drawLegs(0.43f, 0.57f, 0.75f)
    }

    when (customization.shapeId) {
        "sunny" -> {
            drawCircle(fill, s * 0.30f, p(0.5f, 0.48f))
            drawCircle(ink, s * 0.30f, p(0.5f, 0.48f), style = outline)
        }
        "lime" -> {
            val square = Size(s * 0.56f, s * 0.52f)
            drawRoundRect(fill, p(0.22f, 0.23f), square, CornerRadius(s * 0.055f))
            drawRoundRect(ink, p(0.22f, 0.23f), square, CornerRadius(s * 0.055f), style = outline)
            drawLine(accent, p(0.28f, 0.29f), p(0.72f, 0.29f), strokeWidth = stroke * 1.35f, cap = StrokeCap.Round)
        }
        "violet" -> {
            val triangle = Path().apply {
                val top = p(0.5f, 0.17f); moveTo(top.x, top.y)
                val right = p(0.83f, 0.77f); lineTo(right.x, right.y)
                val left = p(0.17f, 0.77f); lineTo(left.x, left.y)
                close()
            }
            drawBody(triangle)
        }
        "cloud" -> {
            val cloud = Path().apply {
                val start = p(0.14f, 0.62f); moveTo(start.x, start.y)
                cubicTo(p(0.10f, 0.53f).x, p(0.10f, 0.53f).y, p(0.18f, 0.43f).x, p(0.18f, 0.43f).y, p(0.32f, 0.46f).x, p(0.32f, 0.46f).y)
                cubicTo(p(0.34f, 0.29f).x, p(0.34f, 0.29f).y, p(0.51f, 0.25f).x, p(0.51f, 0.25f).y, p(0.59f, 0.41f).x, p(0.59f, 0.41f).y)
                cubicTo(p(0.73f, 0.34f).x, p(0.73f, 0.34f).y, p(0.87f, 0.44f).x, p(0.87f, 0.44f).y, p(0.84f, 0.56f).x, p(0.84f, 0.56f).y)
                cubicTo(p(0.91f, 0.64f).x, p(0.91f, 0.64f).y, p(0.83f, 0.73f).x, p(0.83f, 0.73f).y, p(0.70f, 0.72f).x, p(0.70f, 0.72f).y)
                lineTo(p(0.28f, 0.72f).x, p(0.28f, 0.72f).y)
                cubicTo(p(0.15f, 0.74f).x, p(0.15f, 0.74f).y, p(0.10f, 0.68f).x, p(0.10f, 0.68f).y, p(0.14f, 0.62f).x, p(0.14f, 0.62f).y)
                close()
            }
            drawBody(cloud)
        }
        "star" -> {
            val star = Path()
            repeat(10) { index ->
                val angle = (-PI / 2.0 + index * PI / 5.0).toFloat()
                val radius = if (index % 2 == 0) 0.39f else 0.18f
                val point = p(0.5f + cos(angle) * radius, 0.51f + sin(angle) * radius)
                if (index == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
            }
            star.close()
            drawBody(star)
        }
        "tangerine" -> {
            val honeycomb = Path().apply {
                listOf(0.35f to 0.29f, 0.65f to 0.29f, 0.78f to 0.42f, 0.78f to 0.61f,
                    0.65f to 0.75f, 0.35f to 0.75f, 0.22f to 0.61f, 0.22f to 0.42f).forEachIndexed { index, point ->
                    val at = p(point.first, point.second)
                    if (index == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
                }
                close()
            }
            drawBody(honeycomb)
        }
        "berry" -> {
            val diamond = Path().apply {
                val top = p(0.50f, 0.16f); moveTo(top.x, top.y)
                cubicTo(p(0.59f, 0.27f).x, p(0.59f, 0.27f).y, p(0.73f, 0.42f).x, p(0.73f, 0.42f).y, p(0.80f, 0.50f).x, p(0.80f, 0.50f).y)
                cubicTo(p(0.71f, 0.60f).x, p(0.71f, 0.60f).y, p(0.58f, 0.76f).x, p(0.58f, 0.76f).y, p(0.50f, 0.82f).x, p(0.50f, 0.82f).y)
                cubicTo(p(0.42f, 0.76f).x, p(0.42f, 0.76f).y, p(0.29f, 0.60f).x, p(0.29f, 0.60f).y, p(0.20f, 0.50f).x, p(0.20f, 0.50f).y)
                cubicTo(p(0.27f, 0.42f).x, p(0.27f, 0.42f).y, p(0.41f, 0.27f).x, p(0.41f, 0.27f).y, p(0.50f, 0.16f).x, p(0.50f, 0.16f).y)
                close()
            }
            drawBody(diamond)
        }
        else -> {
            val heart = Path().apply {
                val tip = p(0.50f, 0.78f); moveTo(tip.x, tip.y)
                cubicTo(p(0.43f, 0.72f).x, p(0.43f, 0.72f).y, p(0.19f, 0.54f).x, p(0.19f, 0.54f).y, p(0.19f, 0.40f).x, p(0.19f, 0.40f).y)
                cubicTo(p(0.19f, 0.24f).x, p(0.19f, 0.24f).y, p(0.36f, 0.22f).x, p(0.36f, 0.22f).y, p(0.50f, 0.36f).x, p(0.50f, 0.36f).y)
                cubicTo(p(0.64f, 0.22f).x, p(0.64f, 0.22f).y, p(0.81f, 0.24f).x, p(0.81f, 0.24f).y, p(0.81f, 0.40f).x, p(0.81f, 0.40f).y)
                cubicTo(p(0.81f, 0.54f).x, p(0.81f, 0.54f).y, p(0.57f, 0.72f).x, p(0.57f, 0.72f).y, p(0.50f, 0.78f).x, p(0.50f, 0.78f).y)
                close()
            }
            drawBody(heart)
        }
    }

    if (customization.accessoryId == "headphones") {
        val band = Path().apply {
            val left = p(0.23f, 0.46f); moveTo(left.x, left.y)
            cubicTo(p(0.20f, 0.11f).x, p(0.20f, 0.11f).y, p(0.80f, 0.11f).x, p(0.80f, 0.11f).y, p(0.77f, 0.46f).x, p(0.77f, 0.46f).y)
        }
        drawPath(band, ink, style = Stroke(stroke * 1.7f, cap = StrokeCap.Round))
        drawRoundRect(ink, p(0.14f, 0.40f), Size(s * 0.12f, s * 0.19f), CornerRadius(s * 0.045f))
        drawRoundRect(Color(0xFF466EE6), p(0.17f, 0.43f), Size(s * 0.055f, s * 0.12f), CornerRadius(s * 0.025f))
        drawRoundRect(ink, p(0.74f, 0.40f), Size(s * 0.12f, s * 0.19f), CornerRadius(s * 0.045f))
        drawRoundRect(Color(0xFF466EE6), p(0.77f, 0.43f), Size(s * 0.055f, s * 0.12f), CornerRadius(s * 0.025f))
    }
    if (customization.accessoryId == "cap") drawWebCap(x, y, s, ink, stroke)

    val faceY = when (customization.shapeId) {
        "cloud" -> 0.54f
        "star" -> 0.51f
        "tangerine" -> 0.53f
        "berry" -> 0.49f
        "mint" -> 0.48f
        else -> 0.49f
    }
    val faceScale = 0.82f
    val eyeY = faceY - 0.045f * faceScale
    val eyeOffset = s * 0.115f * faceScale
    drawWebEyes(customization.eyesId, p(0.5f, eyeY).x - eyeOffset, p(0.5f, eyeY).x + eyeOffset, p(0.5f, eyeY).y, ink, stroke, s)
    drawWebMouth(customization.mouthId, p(0.5f, faceY + 0.075f).x, p(0.5f, faceY + 0.075f).y, ink, stroke, s)
    if (customization.accessoryId == "glasses") drawWebGlasses(p(0.5f, eyeY).x, p(0.5f, eyeY).y, eyeOffset, s, ink, stroke)
    if (customization.accessoryId == "bow") drawWebBow(x, y, s, ink, accent, stroke)
    if (customization.accessoryId == "flower") drawWebFlower(x, y, s, ink, accent, stroke)
}

private fun DrawScope.drawWebEyes(eyesId: String, leftX: Float, rightX: Float, y: Float, ink: Color, stroke: Float, s: Float) {
    when (eyesId) {
        "happy" -> listOf(leftX, rightX).forEach { x ->
            val eye = Path().apply {
                moveTo(x - s * 0.045f, y)
                cubicTo(x - s * 0.02f, y + s * 0.045f, x + s * 0.02f, y + s * 0.045f, x + s * 0.045f, y)
            }
            drawPath(eye, ink, style = Stroke(stroke * 1.2f, cap = StrokeCap.Round))
        }
        "sleepy" -> listOf(leftX, rightX).forEach { x ->
            drawLine(ink, Offset(x - s * 0.04f, y), Offset(x + s * 0.04f, y + s * 0.01f), strokeWidth = stroke * 1.15f, cap = StrokeCap.Round)
        }
        "wink" -> {
            drawCircle(ink, s * 0.021f, Offset(leftX, y))
            val wink = Path().apply {
                moveTo(rightX - s * 0.045f, y)
                cubicTo(rightX - s * 0.02f, y + s * 0.045f, rightX + s * 0.02f, y + s * 0.045f, rightX + s * 0.045f, y)
            }
            drawPath(wink, ink, style = Stroke(stroke * 1.2f, cap = StrokeCap.Round))
        }
        "sunglasses" -> {
            listOf(leftX, rightX).forEach { x ->
                drawRoundRect(ink, Offset(x - s * 0.075f, y - s * 0.045f), Size(s * 0.15f, s * 0.09f), CornerRadius(s * 0.025f))
            }
            drawLine(ink, Offset(leftX + s * 0.07f, y), Offset(rightX - s * 0.07f, y), strokeWidth = stroke * 0.9f, cap = StrokeCap.Round)
        }
        else -> {
            drawCircle(ink, s * 0.021f, Offset(leftX, y))
            drawCircle(ink, s * 0.021f, Offset(rightX, y))
        }
    }
}

private fun DrawScope.drawWebMouth(mouthId: String, centerX: Float, y: Float, ink: Color, stroke: Float, s: Float) {
    when (mouthId) {
        "grin" -> {
            drawOval(ink, Offset(centerX - s * 0.07f, y - s * 0.025f), Size(s * 0.14f, s * 0.12f))
        }
        "open", "tongue" -> {
            drawOval(ink, Offset(centerX - s * 0.055f, y - s * 0.025f), Size(s * 0.11f, s * 0.10f))
            if (mouthId == "tongue") drawOval(Color(0xFFEF7187), Offset(centerX - s * 0.035f, y + s * 0.025f), Size(s * 0.07f, s * 0.045f))
        }
        else -> {
            val smile = Path().apply {
                moveTo(centerX - s * 0.075f, y)
                cubicTo(centerX - s * 0.04f, y + s * 0.065f, centerX + s * 0.04f, y + s * 0.065f, centerX + s * 0.075f, y)
            }
            drawPath(smile, ink, style = Stroke(stroke * 1.2f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawWebCap(x: Float, y: Float, s: Float, ink: Color, stroke: Float) {
    fun p(nx: Float, ny: Float) = Offset(x + s * nx, y + s * ny)
    val cap = Path().apply {
        val start = p(0.29f, 0.33f); moveTo(start.x, start.y)
        cubicTo(p(0.30f, 0.16f).x, p(0.30f, 0.16f).y, p(0.67f, 0.15f).x, p(0.67f, 0.15f).y, p(0.72f, 0.31f).x, p(0.72f, 0.31f).y)
        lineTo(p(0.82f, 0.35f).x, p(0.82f, 0.35f).y)
        cubicTo(p(0.72f, 0.41f).x, p(0.72f, 0.41f).y, p(0.39f, 0.41f).x, p(0.39f, 0.41f).y, p(0.27f, 0.35f).x, p(0.27f, 0.35f).y)
        close()
    }
    drawPath(cap, Color(0xFF3969E8))
    drawPath(cap, ink, style = Stroke(stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawWebGlasses(centerX: Float, y: Float, offset: Float, s: Float, ink: Color, stroke: Float) {
    listOf(centerX - offset, centerX + offset).forEach { eyeX ->
        drawRoundRect(ink, Offset(eyeX - s * 0.075f, y - s * 0.045f), Size(s * 0.15f, s * 0.09f), CornerRadius(s * 0.025f), style = Stroke(stroke))
    }
    drawLine(ink, Offset(centerX - offset + s * 0.07f, y), Offset(centerX + offset - s * 0.07f, y), strokeWidth = stroke)
}

private fun DrawScope.drawWebBow(x: Float, y: Float, s: Float, ink: Color, accent: Color, stroke: Float) {
    fun p(nx: Float, ny: Float) = Offset(x + s * nx, y + s * ny)
    val bow = Path().apply {
        val middle = p(0.50f, 0.24f); moveTo(middle.x, middle.y)
        cubicTo(p(0.39f, 0.13f).x, p(0.39f, 0.13f).y, p(0.31f, 0.18f).x, p(0.31f, 0.18f).y, p(0.42f, 0.31f).x, p(0.42f, 0.31f).y)
        cubicTo(p(0.45f, 0.34f).x, p(0.45f, 0.34f).y, p(0.48f, 0.29f).x, p(0.48f, 0.29f).y, p(0.50f, 0.24f).x, p(0.50f, 0.24f).y)
        cubicTo(p(0.61f, 0.13f).x, p(0.61f, 0.13f).y, p(0.69f, 0.18f).x, p(0.69f, 0.18f).y, p(0.58f, 0.31f).x, p(0.58f, 0.31f).y)
        close()
    }
    drawPath(bow, accent)
    drawPath(bow, ink, style = Stroke(stroke * 0.75f))
    drawCircle(ink, s * 0.025f, p(0.50f, 0.27f))
}

private fun DrawScope.drawWebFlower(x: Float, y: Float, s: Float, ink: Color, accent: Color, stroke: Float) {
    fun p(nx: Float, ny: Float) = Offset(x + s * nx, y + s * ny)
    repeat(5) { index ->
        val angle = (-PI / 2.0 + index * 2.0 * PI / 5.0).toFloat()
        val center = p(0.73f + cos(angle) * 0.045f, 0.27f + sin(angle) * 0.045f)
        drawCircle(Color(0xFFFFF3F6), s * 0.045f, center)
        drawCircle(ink, s * 0.045f, center, style = Stroke(stroke * 0.55f))
    }
    drawCircle(accent, s * 0.027f, p(0.73f, 0.27f))
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
private fun SubmissionPage(store: WebGameStore, state: WebUiState, onSettings: () -> Unit) {
    val now = rememberClockNow()
    val room = state.room
    if (room?.settings?.gameMode == GameMode.SPOTIFY_RECENT) {
    PostJoinFrame(
        title = "Building the song pool",
        state = state,
            onSettings = onSettings
        ) { _ ->
            GamePanel("") {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        return
    }
    val maxSongs = room?.let { RoundCountRules.maxSongsPerPlayer(it.settings.roundCount, it.players.size) } ?: 1
    val selfPlayer = state.room?.players?.firstOrNull { it.id == state.selfPlayerId }
    val locked = selfPlayer?.songLocked == true
    val totalPlayers = state.room?.players?.size ?: 0
    val lockedPlayers = state.room?.players?.count { it.songLocked } ?: 0
    PostJoinFrame(
        title = "Add songs",
        state = state,
        onSettings = onSettings
    ) { wide ->
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 74.dp).padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            Text("♪", color = MaterialTheme.colorScheme.primary, fontSize = 30.sp, fontWeight = FontWeight.Black)
            WebAvatarCharacter(
                AvatarCustomization.defaultsFor("lime").copy(mouthId = "open"),
                Modifier.size(74.dp)
            )
            Text("♫", color = Color(0xFFFFB82E), fontSize = 28.sp, fontWeight = FontWeight.Black)
            WebAvatarCharacter(
                AvatarCustomization.defaultsFor("sunny").copy(eyesId = "sleepy", mouthId = "open", accessoryId = "headphones"),
                Modifier.size(80.dp)
            )
        }
        val songColumn: @Composable () -> Unit = {
            val countLabel = "${state.pendingSongs.size} song${if (state.pendingSongs.size == 1) "" else "s"} added"
            GamePanel("Your picks", "${state.pendingSongs.size} / $maxSongs songs") {
                state.pendingSongs.forEachIndexed { index, song ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                }
                            }
                            CoverThumb(song.albumArtUrl, Modifier.padding(start = 8.dp).size(52.dp), "${song.title} cover")
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!locked) TextButton(onClick = { store.removeSong(song.songId) }) { Text("⌫", color = Color(0xFFFF5267), fontSize = 20.sp, fontWeight = FontWeight.Black) }
                        }
                    }
                }
                if (locked) Text("Your picks are locked. Waiting for the other players…", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            if (!locked) SongSearchPanel(store, state, maxSongs, wide)
        }
        val progressPanel: @Composable () -> Unit = {
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
        }
        val spotifyPanel: @Composable () -> Unit = {
            SpotifySuggestionsPanel(store, state, locked, maxSongs)
        }
        val lockButton: @Composable () -> Unit = {
            if (!locked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = store::lockSongs,
                        enabled = state.pendingSongs.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7557F4), contentColor = Color.White)
                    ) { Text("▶  Lock in picks · ${state.pendingSongs.size}/$maxSongs", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) }
                }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(16.dp)) { songColumn() }
                Column(Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    progressPanel()
                    spotifyPanel()
                    lockButton()
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                progressPanel()
                spotifyPanel()
                songColumn()
                lockButton()
            }
        }
    }
}

@Composable
private fun SongSearchPanel(store: WebGameStore, state: WebUiState, maxSongs: Int, wide: Boolean) {
    GamePanel("") {
        val field = @Composable {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = store::setSearchQuery,
                label = { Text("Search songs to add") },
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CoverThumb(track.albumArtUrl, Modifier.size(42.dp), "${track.title} cover")
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp), horizontalAlignment = Alignment.Start) {
                        Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("+", fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
        }
        OutlinedButton(onClick = { store.loadSpotifySuggestions(); store.refreshSpotify() }, modifier = Modifier.fillMaxWidth()) { Text("Load Spotify picks") }
    }
}

@Composable
private fun SpotifySuggestionsPanel(store: WebGameStore, state: WebUiState, locked: Boolean, maxSongs: Int) {
    GamePanel("Spotify picks", "A shortcut from your top tracks.") {
        if (!state.spotifyConnected) {
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
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        CoverThumb(suggestion.albumArtUrl, Modifier.size(44.dp), "${suggestion.title} cover")
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp), horizontalAlignment = Alignment.Start) {
                            Text(suggestion.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                            Text(suggestion.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("+", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GamePage(store: WebGameStore, state: WebUiState, onSettings: () -> Unit) {
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
    PostJoinFrame(title, state, onSettings = onSettings) { wide ->
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
    GamePanel("") {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
            Text("ROUND ${preview.roundIndex + 1} OF ${preview.totalRounds}", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer, letterSpacing = 0.8.sp)
        }
        AlbumArt(preview.albumArtUrl)
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
private fun AlbumArt(
    albumArtUrl: String,
    maxSize: androidx.compose.ui.unit.Dp = 300.dp,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var failed by remember(albumArtUrl) { mutableStateOf(albumArtUrl.isBlank()) }
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
            if (albumArtUrl.isNotBlank() && !failed) {
                AsyncImage(
                    model = albumArtUrl,
                    contentDescription = "Album cover",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(30.dp)),
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Error) {
                            failed = true
                            reportAlbumArtFailure(albumArtUrl)
                        }
                    }
                )
            }
            if (failed) Text("♫", modifier = Modifier.align(Alignment.Center), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CoverThumb(url: String, modifier: Modifier = Modifier, description: String? = "Album cover") {
    var failed by remember(url) { mutableStateOf(url.isBlank()) }
    Box(
        modifier = modifier.clip(RoundedCornerShape(11.dp)).background(
            Brush.linearGradient(listOf(Color(0xFF34295F), Color(0xFF6F58D9), Color(0xFFFFB13B)))
        ),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank() && !failed) {
            AsyncImage(
                model = url,
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) {
                        failed = true
                        reportAlbumArtFailure(url)
                    }
                }
            )
        }
        if (failed) Text("♫", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
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
    val recentMode = state.room?.settings?.gameMode == GameMode.SPOTIFY_RECENT
    val eligibleOwnerIds = voting.eligibleOwnerIds.toSet()
    val eligiblePlayers = if (recentMode) {
        voting.players.filter { it.id in eligibleOwnerIds }
    } else {
        voting.players
    }
    val choices = eligiblePlayers.map { VoteChoice(it.id, it.displayName, it.avatarId, it.avatarCustomization, isSelf = it.id == state.selfPlayerId) } +
        if (recentMode) emptyList() else listOf(VoteChoice(GameConstants.DECOY_ID, "Nobody / Decoy", "cloud", isDecoy = true))
    GamePanel(
        "Who submitted this song?",
        if (recentMode) "Choose whose recent listening history this track came from." else "Choose the player you think picked the track."
    ) {
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
        choices.forEach { choice ->
            val selected = choice.id == selectedVoteId
            val enabled = !submitted && (recentMode || !choice.isSelf) && secondsLeft > 0
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp)
                    .clickable(enabled = enabled) { selectedVoteId = choice.id },
                color = when {
                    selected -> MaterialTheme.colorScheme.primaryContainer
                    choice.isSelf -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    else -> MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    WebAvatarSwatch(choice.avatarId, size = 78.dp, customization = choice.avatarCustomization)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(choice.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                    Surface(
                        modifier = Modifier.size(31.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape,
                        border = if (selected) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                    ) {
                        if (selected) Box(contentAlignment = Alignment.Center) { Text("✓", color = Color.White, fontWeight = FontWeight.Black) }
                    }
                }
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
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7557F4), contentColor = Color.White)
        ) { Text(if (submitted) "Vote submitted" else "▶  Confirm vote", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun VoteTrackSummary(preview: com.guesswhosesong.shared.dto.RoundPreviewStarted, wide: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            AlbumArt(preview.albumArtUrl, maxSize = 132.dp, modifier = Modifier.width(132.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(preview.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(preview.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private data class VoteChoice(
    val id: String,
    val name: String,
    val avatarId: String,
    val avatarCustomization: AvatarCustomization? = null,
    val isSelf: Boolean = false,
    val isDecoy: Boolean = false
)

@Composable
private fun RevealStage(reveal: com.guesswhosesong.shared.dto.RoundRevealed, startedAt: Long, now: Long) {
    val revealSeconds = ((5_000L - (now - startedAt).coerceAtLeast(0L)).coerceAtLeast(0L) + 999L) / 1_000L
    GamePanel("") {
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
        AlbumArt(reveal.songEntry.albumArtUrl, maxSize = 240.dp)
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
                WebAvatarSwatch(player.avatarId, customization = player.avatarCustomization)
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
        state = state,
        useSharedHeader = true
    ) { _ ->
        GamePanel("") {
            if (players.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp), strokeWidth = 2.dp)
                    Text("The final standings are loading.")
                }
            } else {
                players.forEachIndexed { index, player ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}.", modifier = Modifier.width(34.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        WebAvatarSwatch(player.avatarId, customization = player.avatarCustomization)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            player.displayName + if (player.id == state.selfPlayerId) " (you)" else "",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("${player.score} pts", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        GamePanel("Continue", if (isHost) "Start another game or close the room." else "Wait for the host to continue.") {
            if (isHost) {
                Button(onClick = store::playAgain, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Play Again", fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = store::endRoom, modifier = Modifier.fillMaxWidth()) { Text("End Game") }
            }
            OutlinedButton(onClick = store::leaveRoom, modifier = Modifier.fillMaxWidth()) { Text("Leave Room") }
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
        WebAvatarSwatch(player.avatarId, customization = player.avatarCustomization)
        Text(player.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text("${player.score} pts", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
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
