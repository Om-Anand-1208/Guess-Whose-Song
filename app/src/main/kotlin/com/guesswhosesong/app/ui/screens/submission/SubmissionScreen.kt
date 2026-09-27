package com.guesswhosesong.app.ui.screens.submission

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.guesswhosesong.app.ui.components.GuessWhoseSongWordmark
import com.guesswhosesong.app.ui.theme.GwsPalette
import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.TrackSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionScreen(
    viewModel: SubmissionViewModel = hiltViewModel(),
    onNavigateToGame: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val songs = if (uiState.pendingSongs.isNotEmpty()) uiState.pendingSongs
    else listOfNotNull(uiState.pendingSong)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SubmissionEvent.NavigateToGame -> onNavigateToGame()
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = GwsPalette.Paper,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GuessWhoseSongWordmark()
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = GwsPalette.Lavender.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "${songs.size}/${uiState.maxSongs}",
                            fontWeight = FontWeight.Bold,
                            color = GwsPalette.Ink,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Choose your songs", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(3.dp))
                Text(
                    "${uiState.lockedCount}/${uiState.totalCount} players ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = GwsPalette.Ink.copy(alpha = 0.66f)
                )
                LinearProgressIndicator(
                    progress = {
                        if (uiState.totalCount > 0) uiState.lockedCount.toFloat() / uiState.totalCount else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                    color = GwsPalette.LavenderDeep,
                    trackColor = GwsPalette.Lavender.copy(alpha = 0.25f)
                )
            }
        },
        bottomBar = {
            if (!uiState.songLocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GwsPalette.Paper,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::surpriseMe,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GwsPalette.LavenderDeep.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GwsPalette.LavenderDeep)
                        ) {
                            Text("Surprise me", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = viewModel::lockSong,
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier.weight(1.25f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GwsPalette.Tangerine, contentColor = GwsPalette.Ink)
                        ) {
                            Text("Lock in (${songs.size}/${uiState.maxSongs})", fontWeight = FontWeight.Black)
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
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (songs.isNotEmpty()) {
                item(key = "selected-songs") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        songs.forEachIndexed { index, song ->
                            SelectedSongCard(
                                index = index + 1,
                                song = song,
                                locked = uiState.songLocked,
                                onRemove = { viewModel.removeSong(song.songId) }
                            )
                        }
                    }
                }
            }

            if (!uiState.songLocked && songs.size < uiState.maxSongs) {
                item(key = "add-song") { AddSongCard(slotNumber = songs.size + 1) }
            }

            if (!uiState.songLocked && !uiState.spotifyConnected) {
                item(key = "spotify-connect") { SpotifyConnectCard(onClick = viewModel::connectSpotify) }
            } else if (!uiState.songLocked && uiState.spotifyConnected && uiState.spotifySuggestions.isNotEmpty()) {
                item(key = "spotify-suggestions") {
                    Column {
                        Text(
                            "FROM YOUR SPOTIFY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF32805A),
                            modifier = Modifier.padding(top = 9.dp, bottom = 2.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            contentPadding = PaddingValues(bottom = 2.dp)
                        ) {
                            items(uiState.spotifySuggestions.take(12)) { suggestion ->
                                val isAdded = songs.any {
                                    it.title.equals(suggestion.title, ignoreCase = true) &&
                                        it.artist.equals(suggestion.artist, ignoreCase = true)
                                }
                                SuggestionChip(
                                    onClick = {
                                        if (!isAdded && songs.size < uiState.maxSongs) {
                                            viewModel.selectSpotifyTrack(suggestion)
                                        }
                                    },
                                    enabled = isAdded || songs.size < uiState.maxSongs,
                                    label = {
                                        Text(
                                            if (isAdded) "Added · ${suggestion.title}" else suggestion.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isAdded) GwsPalette.Lime.copy(alpha = 0.45f) else GwsPalette.Paper,
                                        labelColor = GwsPalette.Ink
                                    )
                                )
                            }
                        }
                    }
                }
            } else if (!uiState.songLocked && uiState.spotifyConnected) {
                item(key = "spotify-refresh") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spotify connected", color = Color(0xFF32805A), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = viewModel::loadSpotifySuggestions) {
                            Text("Refresh picks", color = GwsPalette.LavenderDeep)
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
                        border = BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.12f))
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("PICKS LOCKED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(Modifier.height(5.dp))
                            Text("You’re all set", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Waiting for everyone else to finish choosing.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GwsPalette.Ink.copy(alpha = 0.68f)
                            )
                        }
                    }
                }
            } else {
                item(key = "song-search") {
                    SearchSongsField(
                        query = uiState.searchQuery,
                        songCount = songs.size,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        onSearch = viewModel::searchSong
                    )
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
                            color = GwsPalette.LavenderDeep
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
                                color = GwsPalette.Ink.copy(alpha = 0.62f),
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
}

@Composable
private fun AddSongCard(slotNumber: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 5.dp),
        color = GwsPalette.Lavender.copy(alpha = 0.13f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GwsPalette.LavenderDeep.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = GwsPalette.Lavender.copy(alpha = 0.34f),
                shape = RoundedCornerShape(13.dp)
            ) { }
            Spacer(Modifier.width(11.dp))
            Column {
                Text("Add a song · slot $slotNumber", fontWeight = FontWeight.Bold)
                Text("Search for a song or pick a suggestion", style = MaterialTheme.typography.bodySmall, color = GwsPalette.Ink.copy(alpha = 0.62f))
            }
        }
    }
}

@Composable
private fun SpotifyConnectCard(onClick: () -> Unit) {
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
                Text("Optional · use your top songs", style = MaterialTheme.typography.bodySmall, color = GwsPalette.Ink.copy(alpha = 0.65f))
            }
            Text("Connect", style = MaterialTheme.typography.labelLarge, color = Color(0xFF28784E))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSongsField(
    query: String,
    songCount: Int,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search for a song…") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GwsPalette.LavenderDeep) },
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
            focusedBorderColor = GwsPalette.LavenderDeep,
            unfocusedBorderColor = GwsPalette.Ink.copy(alpha = 0.17f),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp)
    )
}

@Composable
fun SelectedSongCard(index: Int, song: SongEntry, locked: Boolean, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumArtUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(11.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = GwsPalette.Ink.copy(alpha = 0.63f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!locked) {
                IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove song", tint = GwsPalette.Ink.copy(alpha = 0.65f))
                }
            } else {
                Text("READY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GwsPalette.LavenderDeep)
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
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GwsPalette.Ink.copy(alpha = 0.07f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = track.albumArtUrl,
                contentDescription = null,
                modifier = Modifier.size(43.dp).clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = GwsPalette.Ink.copy(alpha = 0.62f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            when {
                isAdded -> Text("ADDED", style = MaterialTheme.typography.labelSmall, color = GwsPalette.LavenderDeep, fontWeight = FontWeight.Black)
                isFull -> Text("FULL", style = MaterialTheme.typography.labelSmall, color = GwsPalette.Ink.copy(alpha = 0.38f), fontWeight = FontWeight.Bold)
                else -> Surface(
                    color = GwsPalette.Lavender.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("+", color = GwsPalette.LavenderDeep, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
            }
        }
    }
}
