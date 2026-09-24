package com.guesswhosesong.app.ui.screens.submission

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SubmissionEvent.NavigateToGame -> onNavigateToGame()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pick Your Song") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                        Text("\uD83C\uDFA7", style = MaterialTheme.typography.titleMedium)
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
                        Text("\u2192", color = Color(0xFF1DB954), fontWeight = FontWeight.Bold)
                    }
                }
            } else if (uiState.spotifySuggestions.isNotEmpty()) {
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
                        SuggestionChip(
                            onClick = { viewModel.selectSpotifyTrack(suggestion) },
                            label = {
                                Text(
                                    "${suggestion.title} - ${suggestion.artist}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            uiState.pendingSong?.let { song ->
                SelectedSongCard(song = song, locked = uiState.songLocked)
                Spacer(Modifier.height(12.dp))
            }

            if (!uiState.songLocked) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    label = { Text("Search any song") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = viewModel::searchSong) {
                            Icon(Icons.Default.Search, "Search")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.searchSong() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                if (uiState.isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.searchResults) { track ->
                        TrackListItem(track = track, onClick = { viewModel.selectSong(track) })
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
                        Text("\uD83C\uDFB2 Surprise me")
                    }

                    Button(
                        onClick = viewModel::lockSong,
                        enabled = uiState.pendingSong != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("\u2713 Lock in")
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
                Text(
                    "Song locked! Waiting for others...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.weight(1f))
            }

            uiState.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SelectedSongCard(song: SongEntry, locked: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (locked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = song.albumArtUrl,
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold)
                Text(song.artist, style = MaterialTheme.typography.bodySmall)
            }
            if (locked) Text("\u2713", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun TrackListItem(track: TrackSearchResult, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(track.title) },
        supportingContent = { Text(track.artist) },
        leadingContent = {
            AsyncImage(
                model = track.albumArtUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
