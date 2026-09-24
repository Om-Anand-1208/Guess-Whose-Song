package com.guesswhosesong.app.ui.screens.submission

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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.guesswhosesong.shared.models.TrackSearchResult
import com.guesswhosesong.shared.models.SongEntry

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
        topBar = {
            TopAppBar(
                title = {
                    Text("Pick Your Songs (${songs.size}/${uiState.maxSongs})")
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp)
        ) {
            Text(
                "${uiState.lockedCount}/${uiState.totalCount} players ready",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            LinearProgressIndicator(
                progress = { if (uiState.totalCount > 0) uiState.lockedCount.toFloat() / uiState.totalCount else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Spotify Banner
            if (!uiState.spotifyConnected) {
                OutlinedCard(
                    onClick = viewModel::connectSpotify,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = Color(0xFF1DB954).copy(alpha = 0.08f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(Color(0xFF1DB954))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎧", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Connect Spotify",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1DB954)
                            )
                            Text(
                                "Auto-import your top songs for this round",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Text("→", color = Color(0xFF1DB954), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                if (uiState.spotifySuggestions.isNotEmpty()) {
                    Text(
                        "Your Spotify Top Songs:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1DB954),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(uiState.spotifySuggestions.take(15)) { suggestion ->
                            val isAdded = songs.any {
                                it.title.equals(suggestion.title, ignoreCase = true) &&
                                it.artist.equals(suggestion.artist, ignoreCase = true)
                            }
                            SuggestionChip(
                                onClick = { viewModel.selectSpotifyTrack(suggestion) },
                                label = {
                                    Text(
                                        "${if (isAdded) "✓ " else "+ "}${suggestion.title} - ${suggestion.artist}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = if (isAdded) SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF1DB954).copy(alpha = 0.2f),
                                    labelColor = Color(0xFF1DB954)
                                ) else SuggestionChipDefaults.suggestionChipColors()
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "✓ Spotify Connected",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1DB954)
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = viewModel::loadSpotifySuggestions) {
                            Text(
                                "Refresh top songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1DB954)
                            )
                        }
                    }
                }
            }

            // Selected Songs List
            if (songs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your Song Picks (${songs.size}/${uiState.maxSongs}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!uiState.songLocked && songs.size < uiState.maxSongs) {
                        Text(
                            "Pick ${uiState.maxSongs - songs.size} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
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

            if (!uiState.songLocked) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    label = {
                        Text(
                            if (songs.size < uiState.maxSongs)
                                "Search song ${songs.size + 1} of ${uiState.maxSongs}"
                            else
                                "Search songs"
                        )
                    },
                    placeholder = { Text("e.g. Blinding Lights, Attention...") },
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, "Clear search")
                            }
                        } else {
                            IconButton(onClick = viewModel::searchSong) {
                                Icon(Icons.Default.Search, "Search")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.searchSong() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                if (uiState.isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                }

                if (uiState.searchQuery.isBlank()) {
                    if (uiState.popularSuggestions.isNotEmpty()) {
                        Text(
                            "Popular Suggestions:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.popularSuggestions) { track ->
                                val isAdded = songs.any { it.songId == track.id || (it.title.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true)) }
                                TrackListItem(
                                    track = track,
                                    isAdded = isAdded,
                                    onClick = { viewModel.selectSong(track) }
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                } else {
                    if (!uiState.isSearching && uiState.searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No songs found for \"${uiState.searchQuery}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.searchResults) { track ->
                                val isAdded = songs.any { it.songId == track.id || (it.title.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true)) }
                                TrackListItem(
                                    track = track,
                                    isAdded = isAdded,
                                    onClick = { viewModel.selectSong(track) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.surpriseMe() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎲 Surprise me")
                    }

                    Button(
                        onClick = viewModel::lockSong,
                        enabled = songs.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✓ Lock in (${songs.size}/${uiState.maxSongs})")
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
                Text(
                    "All ${songs.size} song(s) locked! Waiting for others...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.weight(1f))
            }

            uiState.error?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SelectedSongCard(
    index: Int,
    song: SongEntry,
    locked: Boolean,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (locked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (locked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$index",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (locked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            AsyncImage(
                model = song.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!locked) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove song",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "✓ Ready",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TrackListItem(
    track: TrackSearchResult,
    isAdded: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            AsyncImage(
                model = track.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        },
        trailingContent = {
            if (isAdded) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "✓ Added",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("+ Add", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
