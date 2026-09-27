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
import com.guesswhosesong.app.ui.components.AvatarBadge
import com.guesswhosesong.app.ui.theme.GwsPalette
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
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MAKE YOUR PICK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Choose your songs", style = MaterialTheme.typography.headlineSmall)
                    }
                    AvatarBadge(uiState.room?.players?.find { it.id == uiState.selfPlayerId }?.avatarId ?: "sunny", size = 48.dp)
                    Spacer(Modifier.width(8.dp))
                    Surface(color = GwsPalette.Butter, shape = RoundedCornerShape(50)) {
                        Text("${songs.size}/${uiState.maxSongs}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                    }
                }
            }
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
                            val slotIndex = songs.indexOfFirst {
                                it.title.equals(suggestion.title, ignoreCase = true) &&
                                it.artist.equals(suggestion.artist, ignoreCase = true)
                            }
                            val isAdded = slotIndex != -1
                            val isFull = songs.size >= uiState.maxSongs && !isAdded
                            SuggestionChip(
                                onClick = { if (!isAdded && !isFull) viewModel.selectSpotifyTrack(suggestion) },
                                enabled = !isFull || isAdded,
                                label = {
                                    Text(
                                        when {
                                            isAdded -> "✓ #${slotIndex + 1}: ${suggestion.title}"
                                            isFull -> suggestion.title
                                            else -> "+ #${songs.size + 1}: ${suggestion.title}"
                                        },
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

            // Song Slots Section (Always visible so users see all required slots)
            SongSlotsSection(
                songs = songs,
                maxSongs = uiState.maxSongs,
                locked = uiState.songLocked,
                onRemoveSong = viewModel::removeSong
            )

            if (!uiState.songLocked) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    label = {
                        Text(
                            if (songs.size < uiState.maxSongs)
                                "Search song for Slot #${songs.size + 1}"
                            else
                                "Search songs (all slots full)"
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
                                val slotIndex = songs.indexOfFirst {
                                    it.songId == track.id || (it.title.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true))
                                }
                                val isAdded = slotIndex != -1
                                val isFull = songs.size >= uiState.maxSongs && !isAdded
                                TrackListItem(
                                    track = track,
                                    isAdded = isAdded,
                                    slotNumber = if (isAdded) slotIndex + 1 else if (!isFull) songs.size + 1 else null,
                                    isFull = isFull,
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
                                val slotIndex = songs.indexOfFirst {
                                    it.songId == track.id || (it.title.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true))
                                }
                                val isAdded = slotIndex != -1
                                val isFull = songs.size >= uiState.maxSongs && !isAdded
                                TrackListItem(
                                    track = track,
                                    isAdded = isAdded,
                                    slotNumber = if (isAdded) slotIndex + 1 else if (!isFull) songs.size + 1 else null,
                                    isFull = isFull,
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
fun SongSlotsSection(
    songs: List<SongEntry>,
    maxSongs: Int,
    locked: Boolean,
    onRemoveSong: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Your Song Picks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = if (songs.size >= maxSongs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${songs.size} / $maxSongs",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (songs.size >= maxSongs) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (!locked) {
                if (songs.size < maxSongs) {
                    Text(
                        "Pick ${maxSongs - songs.size} more",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Text(
                        "All slots filled!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0 until maxSongs) {
                if (i < songs.size) {
                    val song = songs[i]
                    SelectedSongCard(
                        index = i + 1,
                        song = song,
                        locked = locked,
                        onRemove = { onRemoveSong(song.songId) }
                    )
                } else if (!locked) {
                    val isNext = i == songs.size
                    EmptySongSlotCard(
                        slotNumber = i + 1,
                        isNextSlot = isNext
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySongSlotCard(
    slotNumber: Int,
    isNextSlot: Boolean
) {
    OutlinedCard(
        border = BorderStroke(
            width = if (isNextSlot) 1.5.dp else 1.dp,
            color = if (isNextSlot) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isNextSlot) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                             else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isNextSlot) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$slotNumber",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isNextSlot) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isNextSlot) "👉 Tap a song below for slot #$slotNumber" else "Slot #$slotNumber (Empty)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isNextSlot) FontWeight.Medium else FontWeight.Normal,
                    color = if (isNextSlot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = if (isNextSlot) "Search or pick from suggestions" else "Waiting for previous slot",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
    slotNumber: Int?,
    isFull: Boolean,
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
                        if (slotNumber != null) "✓ Slot #$slotNumber" else "✓ Picked",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (isFull) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "Full",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        if (slotNumber != null) "+ Pick #$slotNumber" else "+ Pick",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        modifier = Modifier.clickable(enabled = !isAdded && !isFull, onClick = onClick)
    )
}
