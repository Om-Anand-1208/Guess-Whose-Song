package com.guesswhosesong.app.ui.screens.submission

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.ui.components.AlbumArtwork
import com.guesswhosesong.app.ui.components.AvatarCharacter
import com.guesswhosesong.app.ui.components.PostJoinHeader
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
                    .statusBarsPadding().padding(horizontal = 18.dp)
            ) {
                PostJoinHeader(uiState.room?.joinCode.orEmpty(), onSettings = { showAppearanceSettings = true })
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("YOUR TASTE, YOUR TURN", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp)
                        Text("Add songs", style = MaterialTheme.typography.displaySmall, color = PostJoinPalette.Ink, fontWeight = FontWeight.Black)
                        Text(
                            "Add up to ${uiState.maxSongs} songs for the group to guess. Pick what you love!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PostJoinPalette.Muted
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarCharacter("lime", Modifier.size(42.dp), customization = com.guesswhosesong.shared.models.AvatarCustomization.defaultsFor("lime").copy(mouthId = "open"))
                        AvatarCharacter("sunny", Modifier.size(48.dp), customization = com.guesswhosesong.shared.models.AvatarCustomization.defaultsFor("sunny").copy(eyesId = "sleepy", mouthId = "open", accessoryId = "headphones"))
                    }
                }
            }
        },
        bottomBar = {
            if (!uiState.songLocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PostJoinPalette.Background,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                            .background(PostJoinPalette.Background)
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = viewModel::lockSong,
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7557F4), contentColor = Color.White)
                        ) { Text("▶  Lock in picks · ${songs.size}/${uiState.maxSongs}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) }
                        Text("Choose your songs, then lock them in.", modifier = Modifier.padding(top = 5.dp), color = PostJoinPalette.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item(key = "submission-progress") {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = PostJoinPalette.Surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, PostJoinPalette.Outline.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Round progress", style = MaterialTheme.typography.titleSmall, color = PostJoinPalette.Ink, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${uiState.lockedCount} of ${uiState.totalCount} players ready", style = MaterialTheme.typography.bodySmall, color = PostJoinPalette.Muted)
                        }
                        LinearProgressIndicator(
                            progress = {
                                if (uiState.totalCount > 0) uiState.lockedCount.toFloat() / uiState.totalCount else 0f
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = PostJoinPalette.Selected.copy(alpha = 0.25f)
                        )
                    }
                }
            }

            if (!uiState.songLocked && !uiState.spotifyConnected) {
                item(key = "spotify-connect") { SpotifyConnectCard(onClick = viewModel::connectSpotify) }
            } else if (!uiState.songLocked && uiState.spotifyConnected && uiState.spotifySuggestions.isNotEmpty()) {
                item(key = "spotify-suggestions") {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Spotify picks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Color(0xFF28C76F).copy(alpha = 0.17f), shape = RoundedCornerShape(50)) {
                                Text("Connected", modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = Color(0xFF247447), style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = viewModel::loadSpotifySuggestions) { Text("Refresh") }
                        }
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
                            Spacer(Modifier.height(5.dp))
                        }
                    }
                }
            } else if (!uiState.songLocked && uiState.spotifyConnected) {
                item(key = "spotify-refresh") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spotify connected", color = Color(0xFF32805A), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = viewModel::loadSpotifySuggestions) { Text("Refresh picks", color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }

            if (songs.isNotEmpty()) {
                item(key = "selected-songs") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Your picks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                                Text("${songs.size} / ${uiState.maxSongs} songs", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        songs.forEachIndexed { index, song ->
                            SelectedSongCard(
                                index = index,
                                song = song,
                                locked = uiState.songLocked,
                                onRemove = { viewModel.removeSong(song.songId) }
                            )
                        }
                    }
                }
            }

            if (!uiState.songLocked) {
                item(key = "add-song-heading") {
                    Text("Add more songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            Text("PICKS LOCKED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(Modifier.height(5.dp))
                            Text("You’re all set", style = MaterialTheme.typography.titleLarge)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            modifier = Modifier.padding(top = 5.dp).height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary)
                        ) { Text("Search", fontWeight = FontWeight.Bold) }
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
            Surface(modifier = Modifier.size(9.dp), color = Color(0xFF31A66A), shape = RoundedCornerShape(50)) { }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Spotify connected?", fontWeight = FontWeight.Bold)
                Text("Optional · use your top songs", style = MaterialTheme.typography.bodySmall, color = PostJoinPalette.Ink.copy(alpha = 0.65f))
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
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search for a song…") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = PostJoinPalette.Ink.copy(alpha = 0.17f),
            focusedContainerColor = PostJoinPalette.Surface,
            unfocusedContainerColor = PostJoinPalette.Surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp)
    )
}

@Composable
fun SelectedSongCard(song: SongEntry, locked: Boolean, onRemove: () -> Unit, index: Int = 0) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PostJoinPalette.Surface,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, PostJoinPalette.Ink.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(11.dp)) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.width(8.dp))
            AlbumArtwork(song.albumArtUrl, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = PostJoinPalette.Ink.copy(alpha = 0.63f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!locked) {
                TextButton(onClick = onRemove, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Delete", color = Color(0xFF9D2922), fontWeight = FontWeight.Bold)
                }
            } else {
                Text("READY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun TrackListItem(track: TrackSearchResult, isAdded: Boolean, isFull: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isFull && !isAdded, onClick = onClick),
        color = PostJoinPalette.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PostJoinPalette.Ink.copy(alpha = 0.07f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtwork(track.albumArtUrl, modifier = Modifier.size(43.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = PostJoinPalette.Ink.copy(alpha = 0.62f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            when {
                isAdded -> Text("ADDED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                isFull -> Text("FULL", style = MaterialTheme.typography.labelSmall, color = PostJoinPalette.Ink.copy(alpha = 0.38f), fontWeight = FontWeight.Bold)
                else -> Surface(
                    color = PostJoinPalette.Selected.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("+", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
            }
        }
    }
}
