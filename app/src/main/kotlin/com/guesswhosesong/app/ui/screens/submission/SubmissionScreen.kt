package com.guesswhosesong.app.ui.screens.submission

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.ui.components.AlbumArtwork
import com.guesswhosesong.app.ui.components.PostJoinHeader
import com.guesswhosesong.app.ui.components.PostJoinHeroCharacter
import com.guesswhosesong.app.ui.components.PostJoinHeroFloorShadow
import com.guesswhosesong.app.ui.components.PostJoinMusicNote
import com.guesswhosesong.app.ui.theme.AppearanceSettingsSheet
import com.guesswhosesong.app.ui.theme.GwsPalette
import com.guesswhosesong.app.ui.theme.PostJoinPalette
import com.guesswhosesong.shared.models.GameMode
import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.TrackSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionScreen(
    viewModel: SubmissionViewModel = hiltViewModel(),
    onNavigateToGame: () -> Unit,
    onReturnToLobby: (String, String, com.guesswhosesong.shared.models.AvatarCustomization, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var lobbyReturnHandled by remember { mutableStateOf(false) }
    var showAppearanceSettings by remember { mutableStateOf(false) }
    val songs = if (uiState.pendingSongs.isNotEmpty()) uiState.pendingSongs
    else listOfNotNull(uiState.pendingSong)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SubmissionEvent.NavigateToGame -> onNavigateToGame()
            }
        }
    }

    val recentRoom = uiState.room?.takeIf { it.settings.gameMode == GameMode.SPOTIFY_RECENT }
    LaunchedEffect(recentRoom?.joinCode, recentRoom?.state, uiState.recentPlayedFailure) {
        val room = recentRoom ?: return@LaunchedEffect
        val failureExplanation = uiState.recentPlayedFailure
        if (room.state == com.guesswhosesong.shared.models.RoomState.LOBBY &&
            failureExplanation != null && !lobbyReturnHandled
        ) {
            val self = room.players.find { it.id == uiState.selfPlayerId } ?: return@LaunchedEffect
            lobbyReturnHandled = true
            onReturnToLobby(
                room.joinCode,
                self.displayName,
                com.guesswhosesong.shared.models.AvatarCustomization.normalize(
                    self.avatarCustomization,
                    self.avatarId
                ),
                failureExplanation
            )
        }
    }

    if (recentRoom != null) {
        Scaffold(containerColor = PostJoinPalette.Background) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    "Building the song pool",
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PostJoinPalette.Ink
                )
                Text(
                    "Checking for unique, playable tracks in players’ recent Spotify history. Everyone can still vote, even if they have no eligible tracks.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PostJoinPalette.Ink.copy(alpha = 0.72f)
                )
                uiState.error?.let { error ->
                    Text(
                        error,
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        return
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = PostJoinPalette.Background,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(PostJoinPalette.Background)
                    .statusBarsPadding().padding(horizontal = 22.dp).padding(top = 8.dp)
            ) {
                PostJoinHeader(
                    uiState.room?.joinCode.orEmpty(),
                    onSettings = { showAppearanceSettings = true },
                    modifier = Modifier.padding(top = 3.dp),
                    roomHorizontalPadding = 8.5.dp
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(end = 21.dp)) {
                        Text("Add songs", color = PostJoinPalette.Ink, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 37.sp, letterSpacing = (-0.7).sp, maxLines = 1)
                    }
                    Box(Modifier.offset(x = (-5).dp, y = 6.dp)) {
                        SubmissionHeroArtwork()
                    }
                }
            }
        },
        bottomBar = {
            if (!uiState.songLocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PostJoinPalette.Background,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                            .background(PostJoinPalette.Background)
                            .padding(horizontal = 16.dp)
                            .padding(top = 10.dp, bottom = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(46.5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF7655F6), Color(0xFF704DF1))))
                                .clickable(enabled = songs.isNotEmpty(), onClick = viewModel::lockSong),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Text("Lock in picks · ${songs.size}/${uiState.maxSongs}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "submission-progress") {
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 4.25.dp)
                        .padding(top = 4.5.dp, bottom = 3.dp),
                    color = PostJoinPalette.Surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, PostJoinPalette.Outline.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(horizontal = 13.dp, vertical = 8.5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Round progress", style = MaterialTheme.typography.titleSmall, color = PostJoinPalette.Ink, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${uiState.lockedCount} of ${uiState.totalCount} players ready", style = MaterialTheme.typography.bodySmall, color = PostJoinPalette.Muted)
                        }
                        LinearProgressIndicator(
                            progress = {
                                if (uiState.totalCount > 0) uiState.lockedCount.toFloat() / uiState.totalCount else 0f
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = PostJoinPalette.Selected.copy(alpha = 0.25f)
                        )
                    }
                }
            }

            if (!uiState.songLocked && !uiState.spotifyConnected) {
                item(key = "spotify-connect") { SpotifyConnectCard(onClick = viewModel::connectSpotify) }
            } else if (!uiState.songLocked && uiState.spotifyConnected) {
                item(key = "spotify-suggestions") {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpotifyMark(Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Spotify picks", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), fontWeight = FontWeight.Black, color = PostJoinPalette.Ink)
                            Spacer(Modifier.width(9.dp))
                            Box(Modifier.size(8.dp).background(Color(0xFF37D267), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("Connected", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = PostJoinPalette.Muted)
                            Spacer(Modifier.weight(1f))
                            Row(
                                modifier = Modifier.height(30.dp).clip(RoundedCornerShape(50))
                                    .clickable(onClick = viewModel::loadSpotifySuggestions)
                                    .padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Refresh", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (uiState.spotifySuggestions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = PostJoinPalette.Surface.copy(alpha = 0.82f),
                                shape = RoundedCornerShape(17.dp),
                                border = BorderStroke(1.dp, PostJoinPalette.Outline.copy(alpha = 0.28f))
                            ) {
                                Column(Modifier.padding(horizontal = 6.dp, vertical = 1.dp)) {
                                    uiState.spotifySuggestions.take(6).forEach { suggestion ->
                                        val isAdded = songs.any {
                                            it.title.equals(suggestion.title, ignoreCase = true) &&
                                                it.artist.equals(suggestion.artist, ignoreCase = true)
                                        }
                                        val suggestionTrack = TrackSearchResult(
                                            id = suggestion.spotifyTrackId ?: "spotify-${suggestion.title}-${suggestion.artist}",
                                            title = suggestion.title,
                                            artist = suggestion.artist,
                                            albumArtUrl = suggestion.albumArtUrl,
                                            previewUrl = ""
                                        )
                                        TrackListItem(
                                            track = suggestionTrack,
                                            isAdded = isAdded,
                                            isFull = songs.size >= uiState.maxSongs && !isAdded,
                                            onClick = { viewModel.selectSpotifyTrack(suggestion) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (songs.isNotEmpty()) {
                item(key = "selected-songs") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your picks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = PostJoinPalette.Ink, modifier = Modifier.weight(1f))
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                                Text("${songs.size} / ${uiState.maxSongs} songs", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        val isLightAppearance = PostJoinPalette.Background.luminance() > 0.5f
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isLightAppearance) PostJoinPalette.Background else PostJoinPalette.Surface.copy(alpha = 0.82f),
                            shape = RoundedCornerShape(17.dp),
                            border = if (isLightAppearance) BorderStroke(0.5.dp, PostJoinPalette.Background)
                            else BorderStroke(1.dp, PostJoinPalette.Outline.copy(alpha = 0.28f))
                        ) {
                            Column(Modifier.padding(horizontal = 6.dp, vertical = 1.dp)) {
                                songs.forEachIndexed { index, song ->
                                    SelectedSongCard(
                                        index = index,
                                        song = song,
                                        locked = uiState.songLocked,
                                        onRemove = { viewModel.removeSong(song.songId) },
                                        showDivider = index < songs.lastIndex
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.songLocked) {
                item(key = "locked-status") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        color = GwsPalette.Lime.copy(alpha = 0.38f),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, PostJoinPalette.Ink.copy(alpha = 0.12f))
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                "Waiting for everyone else to finish choosing.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PostJoinPalette.Ink.copy(alpha = 0.68f)
                            )
                        }
                    }
                }
            } else {
                item(key = "song-search") {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                        color = PostJoinPalette.Surface.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(17.dp),
                        border = BorderStroke(1.dp, PostJoinPalette.Outline.copy(alpha = 0.28f))
                    ) {
                        Column(Modifier.padding(horizontal = 10.5.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.padding(top = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(Modifier.weight(1f)) {
                                    SearchSongsField(
                                        query = uiState.searchQuery,
                                        onQueryChange = viewModel::onSearchQueryChanged,
                                        onSearch = viewModel::searchSong
                                    )
                                }
                                Button(
                                    onClick = viewModel::searchSong,
                                    enabled = uiState.searchQuery.isNotBlank() && !uiState.isSearching,
                                    modifier = Modifier.width(90.dp).height(32.dp),
                                    shape = RoundedCornerShape(15.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        disabledContentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        "Search Spotify",
                                        fontSize = 11.sp,
                                        letterSpacing = (-0.15).sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.isSearching) {
                    item(key = "search-loading") {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 7.dp)
                                .height(3.dp),
                            color = GwsPalette.Tangerine,
                            trackColor = GwsPalette.Butter.copy(alpha = 0.4f)
                        )
                    }
                }

                item(key = "suggestion-heading") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 11.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (uiState.searchQuery.isBlank()) "Suggested for you" else "Search results",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (songs.size >= uiState.maxSongs) "PICKS FULL" else "ADD A FAVORITE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val tracks = if (uiState.searchQuery.isBlank()) uiState.popularSuggestions else uiState.searchResults
                if (uiState.searchQuery.isNotBlank() && !uiState.isSearching && tracks.isEmpty()) {
                    item(key = "no-search-results") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No songs found for ‘${uiState.searchQuery}’",
                                color = PostJoinPalette.Ink.copy(alpha = 0.62f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(items = tracks, key = { track -> track.id }) { track ->
                        val isAdded = songs.any {
                            it.songId == track.id ||
                                (it.title.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true))
                        }
                        TrackListItem(
                            track = track,
                            isAdded = isAdded,
                            isFull = songs.size >= uiState.maxSongs && !isAdded,
                            onClick = { viewModel.selectSong(track) }
                        )
                    }
                }

            }

            uiState.error?.let { error ->
                item(key = "submission-error") {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
    if (showAppearanceSettings) {
        AppearanceSettingsSheet(onDismiss = { showAppearanceSettings = false })
    }
}

@Composable
private fun SubmissionHeroArtwork() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(Modifier.width(150.dp).height(94.dp)) {
        PostJoinHeroFloorShadow(
            Modifier.align(Alignment.BottomStart).padding(start = 2.dp, bottom = 1.dp).size(width = 78.dp, height = 14.dp),
            color = if (isDark) Color(0xFF2B2E37) else Color(0xFFFFE8A1)
        )
        PostJoinHeroFloorShadow(
            Modifier.align(Alignment.BottomEnd).padding(end = 1.dp, bottom = 1.dp).size(width = 79.dp, height = 14.dp),
            color = if (isDark) Color(0xFF2B2E37) else Color(0xFFFFE8A1),
            alternate = true
        )
        PostJoinMusicNote(Modifier.align(Alignment.TopStart).padding(start = 7.dp, top = 7.dp).size(27.dp), color = Color(0xFF8B69FF), double = true)
        PostJoinMusicNote(Modifier.align(Alignment.TopCenter).padding(top = 1.dp).size(26.dp), color = Color(0xFFFFB92E), double = true)
        PostJoinMusicNote(Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 3.dp).size(27.dp), color = Color(0xFF8B69FF))
        Box(
            Modifier.align(Alignment.TopStart).padding(start = 25.dp, top = 21.dp)
                .size(width = 3.dp, height = 9.dp).rotate(-20f)
                .background(Color(0xFF8B69FF), RoundedCornerShape(50))
        )
        Box(
            Modifier.align(Alignment.TopStart).padding(start = 34.dp, top = 27.dp)
                .size(width = 3.dp, height = 7.dp).rotate(18f)
                .background(Color(0xFF8B69FF), RoundedCornerShape(50))
        )
        Box(
            Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 25.dp)
                .size(width = 3.dp, height = 9.dp).rotate(25f)
                .background(Color(0xFFFFA536), RoundedCornerShape(50))
        )
        Box(
            Modifier.align(Alignment.TopEnd).padding(end = 2.dp, top = 34.dp)
                .size(width = 3.dp, height = 7.dp).rotate(62f)
                .background(Color(0xFFFFA536), RoundedCornerShape(50))
        )
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            PostJoinHeroCharacter(
                "lime",
                Modifier.size(77.dp),
                contentDescription = "Lime character",
                customization = com.guesswhosesong.shared.models.AvatarCustomization.defaultsFor("lime").copy(mouthId = "tongue"),
                tiltBodyOnly = true
            )
            PostJoinHeroCharacter(
                "sunny",
                Modifier.size(84.dp),
                contentDescription = "Sunny character wearing headphones",
                customization = com.guesswhosesong.shared.models.AvatarCustomization.defaultsFor("sunny").copy(eyesId = "happy", mouthId = "tongue", accessoryId = "headphones"),
                tiltBodyOnly = true
            )
        }
    }
}

@Composable
private fun SpotifyMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(Color(0xFF1ED760))
        val waveColor = Color.White
        val waves = listOf(
            Triple(0.27f, 0.33f, 0.31f),
            Triple(0.31f, 0.48f, 0.43f),
            Triple(0.37f, 0.61f, 0.56f)
        )
        waves.forEach { (startY, endY, controlY) ->
            val wave = Path().apply {
                moveTo(size.width * 0.22f, size.height * startY)
                cubicTo(
                    size.width * 0.37f, size.height * (startY - 0.04f),
                    size.width * 0.60f, size.height * controlY,
                    size.width * 0.78f, size.height * endY
                )
            }
            drawPath(wave, waveColor, style = Stroke(size.width * 0.075f, cap = StrokeCap.Round))
        }
    }
}

@Composable
internal fun SpotifyConnectCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 5.dp)
            .clickable(onClick = onClick),
        color = GwsPalette.Lime.copy(alpha = 0.19f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF32805A).copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpotifyMark(Modifier.size(28.dp))
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Spotify", fontWeight = FontWeight.Bold)
            }
            Text("Connect", style = MaterialTheme.typography.labelLarge, color = Color(0xFF28784E))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchSongsField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = PostJoinPalette.Ink.copy(alpha = 0.17f),
        focusedContainerColor = PostJoinPalette.Surface,
        unfocusedContainerColor = PostJoinPalette.Surface
    )
    val shape = RoundedCornerShape(14.dp)
    val textStyle = LocalTextStyle.current.merge(
        TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    )
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary
    else PostJoinPalette.Ink.copy(alpha = 0.17f)

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = textStyle,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(PostJoinPalette.Surface)
                        .border(1.dp, borderColor, shape)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 7.5.dp)
                            .size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 35.dp, end = if (query.isEmpty()) 8.dp else 36.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                "Search songs to add",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                                color = PostJoinPalette.Muted,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 2.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(onClick = { onQueryChange("") }),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun SelectedSongCard(
    song: SongEntry,
    locked: Boolean,
    onRemove: () -> Unit,
    index: Int = 0,
    showDivider: Boolean = true
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 3.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(11.dp)) {
                Box(Modifier.size(27.dp), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            AlbumArtwork(song.albumArtUrl, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, lineHeight = 17.sp)
                Text(song.artist, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp), color = PostJoinPalette.Ink.copy(alpha = 0.63f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!locked) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ${song.title}", tint = Color(0xFFFF667A), modifier = Modifier.size(22.dp))
                }
            } else {
                Text("READY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().padding(start = 47.dp, end = 4.dp).height(1.dp).background(PostJoinPalette.Outline.copy(alpha = 0.42f)))
        }
    }
}

@Composable
fun TrackListItem(track: TrackSearchResult, isAdded: Boolean, isFull: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp)
                .clickable(enabled = !isFull && !isAdded, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtwork(track.albumArtUrl, modifier = Modifier.size(38.dp))
            Spacer(Modifier.width(15.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, lineHeight = 17.sp)
                Text(track.artist, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp), color = PostJoinPalette.Ink.copy(alpha = 0.62f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            when {
                isAdded -> Text("ADDED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                isFull -> Text("FULL", style = MaterialTheme.typography.labelSmall, color = PostJoinPalette.Ink.copy(alpha = 0.38f), fontWeight = FontWeight.Bold)
                else -> Surface(
                    modifier = Modifier.size(29.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 25.sp)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(start = 49.dp, end = 4.dp).height(1.dp).background(PostJoinPalette.Outline.copy(alpha = 0.36f)))
    }
}
