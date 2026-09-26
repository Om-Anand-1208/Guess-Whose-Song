package com.guesswhosesong.app.ui.screens.submission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.app.data.repository.GameRepository
import com.guesswhosesong.app.data.spotify.SpotifyAuthManager
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubmissionUiState(
    val room: Room? = null,
    val selfPlayerId: String = "",
    val searchQuery: String = "",
    val searchResults: List<TrackSearchResult> = emptyList(),
    val popularSuggestions: List<TrackSearchResult> = emptyList(),
    val spotifySuggestions: List<SpotifySuggestion> = emptyList(),
    val pendingSongs: List<SongEntry> = emptyList(),
    val pendingSong: SongEntry? = null,
    val maxSongs: Int = 3,
    val songLocked: Boolean = false,
    val isSearching: Boolean = false,
    val lockedCount: Int = 0,
    val totalCount: Int = 0,
    val submissionDeadlineEpochMs: Long = 0L,
    val error: String? = null,
    val spotifyConnected: Boolean = false
)

sealed class SubmissionEvent {
    data object NavigateToGame : SubmissionEvent()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SubmissionViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val httpClient: HttpClient,
    private val playerIdentityManager: PlayerIdentityManager,
    private val spotifyAuthManager: SpotifyAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmissionUiState())
    val uiState: StateFlow<SubmissionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SubmissionEvent>()
    val events: SharedFlow<SubmissionEvent> = _events.asSharedFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private val baseUrl = com.guesswhosesong.app.di.NetworkModule.BASE_URL

    init {
        val cachedRoom = gameRepository.currentRoom.value
        val selfId = gameRepository.selfPlayerId.value
        val isSpotify = cachedRoom?.players?.find { it.id == selfId }?.spotifyConnected == true
        val maxPicks = cachedRoom?.settings?.roundLengthPreset?.songsPerPlayer ?: 3
        val existingSongs = gameRepository.pendingSongs.value

        _uiState.update {
            it.copy(
                room = cachedRoom,
                selfPlayerId = selfId,
                totalCount = cachedRoom?.players?.size ?: 0,
                spotifyConnected = isSpotify,
                maxSongs = maxPicks,
                pendingSongs = existingSongs,
                pendingSong = existingSongs.firstOrNull()
            )
        }

        observeMessages()
        loadPopularSuggestions()
        observeSearchQuery()
        if (isSpotify) {
            loadSpotifySuggestions()
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            gameRepository.messages.collect { message ->
                when (message) {
                    is RoomJoined -> {
                        val selfId = message.selfPlayerId
                        val isSpotify = message.room.players.find { it.id == selfId }?.spotifyConnected == true
                        val maxPicks = message.room.settings.roundLengthPreset.songsPerPlayer
                        _uiState.update {
                            it.copy(
                                room = message.room,
                                selfPlayerId = selfId,
                                totalCount = message.room.players.size,
                                spotifyConnected = isSpotify,
                                maxSongs = maxPicks
                            )
                        }
                        if (isSpotify) loadSpotifySuggestions()
                    }
                    is RoomUpdated -> {
                        val room = message.room
                        val selfId = _uiState.value.selfPlayerId.ifBlank { gameRepository.selfPlayerId.value }
                        val isSpotify = room.players.find { it.id == selfId }?.spotifyConnected == true
                        val maxPicks = room.settings.roundLengthPreset.songsPerPlayer
                        _uiState.update {
                            it.copy(
                                room = room,
                                selfPlayerId = selfId,
                                totalCount = room.players.size,
                                spotifyConnected = isSpotify,
                                maxSongs = maxPicks
                            )
                        }
                        if (isSpotify && _uiState.value.spotifySuggestions.isEmpty()) {
                            loadSpotifySuggestions()
                        }
                        if (room.state == RoomState.PLAYING) {
                            _events.emit(SubmissionEvent.NavigateToGame)
                        }
                    }
                    is SubmissionStarted -> _uiState.update {
                        it.copy(submissionDeadlineEpochMs = message.deadlineEpochMillis)
                    }
                    is SubmissionProgress -> _uiState.update {
                        it.copy(lockedCount = message.lockedCount, totalCount = message.totalCount)
                    }
                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            spotifyAuthManager.isConnected.collect { connected ->
                if (connected) {
                    _uiState.update { it.copy(spotifyConnected = true) }
                    gameRepository.refreshSpotify()
                    loadSpotifySuggestions()
                }
            }
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(350)
                .distinctUntilChanged()
                .collect { query ->
                    val trimmed = query.trim()
                    if (trimmed.isNotBlank()) {
                        executeSearch(trimmed)
                    } else {
                        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                    }
                }
        }
    }

    fun loadPopularSuggestions() {
        viewModelScope.launch {
            try {
                val response = httpClient.get("$baseUrl/music/top") {
                    bearerAuth(playerIdentityManager.getIdToken())
                }
                if (response.status == HttpStatusCode.OK) {
                    val result = response.body<Map<String, List<TrackSearchResult>>>()
                    val tracks = result["tracks"] ?: emptyList()
                    _uiState.update { it.copy(popularSuggestions = tracks) }
                }
            } catch (_: Exception) {
                // Ignore top suggestions loading error
            }
        }
    }

    fun connectSpotify() {
        spotifyAuthManager.launchOAuth()
    }

    fun loadSpotifySuggestions() {
        viewModelScope.launch {
            try {
                val response = httpClient.get("$baseUrl/spotify/top") {
                    bearerAuth(playerIdentityManager.getIdToken())
                }
                if (response.status == HttpStatusCode.OK) {
                    val result = response.body<Map<String, List<SpotifySuggestion>>>()
                    val tracks = result["tracks"] ?: emptyList()
                    _uiState.update { it.copy(spotifySuggestions = tracks) }
                }
            } catch (_: Exception) {
                // If Spotify top fails, gracefully degrade
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, error = null) }
        searchQueryFlow.value = query
    }

    fun searchSong() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            executeSearch(query)
        }
    }

    private suspend fun executeSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, error = null) }
        try {
            val response = httpClient.get("$baseUrl/music/search") {
                parameter("q", query)
                bearerAuth(playerIdentityManager.getIdToken())
            }
            if (response.status == HttpStatusCode.OK) {
                val result = response.body<Map<String, List<TrackSearchResult>>>()
                _uiState.update { it.copy(searchResults = result["tracks"] ?: emptyList()) }
            } else {
                _uiState.update { it.copy(error = "Search returned status ${response.status.value}") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Search failed: ${e.message}") }
        } finally {
            _uiState.update { it.copy(isSearching = false) }
        }
    }

    fun selectSong(track: TrackSearchResult) {
        val current = _uiState.value.pendingSongs
        val max = _uiState.value.maxSongs

        if (current.any { it.songId == track.id || (it.title.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true)) }) {
            _uiState.update { it.copy(error = "\"${track.title}\" is already in your picks") }
            return
        }

        val selfId = _uiState.value.selfPlayerId.ifBlank { gameRepository.selfPlayerId.value }
        val entry = SongEntry(
            songId = track.id,
            title = track.title,
            artist = track.artist,
            albumArtUrl = track.albumArtUrl,
            previewUrl = track.previewUrl,
            submitterId = selfId
        )

        val updated = if (max == 1) {
            listOf(entry)
        } else if (current.size < max) {
            current + entry
        } else {
            _uiState.update { it.copy(error = "All $max song slots filled. Remove a song to pick another.") }
            return
        }

        _uiState.update {
            it.copy(
                pendingSongs = updated,
                pendingSong = updated.firstOrNull(),
                searchResults = emptyList(),
                searchQuery = "",
                error = null
            )
        }
        viewModelScope.launch {
            gameRepository.updatePendingSongs(updated)
        }
    }

    fun selectSpotifyTrack(suggestion: SpotifySuggestion) {
        val current = _uiState.value.pendingSongs
        val max = _uiState.value.maxSongs

        if (current.any { it.title.equals(suggestion.title, ignoreCase = true) && it.artist.equals(suggestion.artist, ignoreCase = true) }) {
            _uiState.update { it.copy(error = "\"${suggestion.title}\" is already in your picks") }
            return
        }

        val selfId = _uiState.value.selfPlayerId.ifBlank { gameRepository.selfPlayerId.value }
        val entry = SongEntry(
            songId = "${suggestion.title}_${suggestion.artist}",
            title = suggestion.title,
            artist = suggestion.artist,
            albumArtUrl = suggestion.albumArtUrl,
            previewUrl = "", // will be resolved server-side on lock
            submitterId = selfId
        )

        val updated = if (max == 1) {
            listOf(entry)
        } else if (current.size < max) {
            current + entry
        } else {
            _uiState.update { it.copy(error = "All $max song slots filled. Remove a song to pick another.") }
            return
        }

        _uiState.update {
            it.copy(
                pendingSongs = updated,
                pendingSong = updated.firstOrNull(),
                searchResults = emptyList(),
                error = null
            )
        }
        viewModelScope.launch {
            gameRepository.updatePendingSongs(updated)
        }
    }

    fun removeSong(songId: String) {
        val current = _uiState.value.pendingSongs
        val updated = current.filterNot { it.songId == songId }
        _uiState.update {
            it.copy(
                pendingSongs = updated,
                pendingSong = updated.firstOrNull(),
                error = null
            )
        }
        viewModelScope.launch {
            gameRepository.updatePendingSongs(updated)
        }
    }

    fun lockSong() {
        val current = _uiState.value.pendingSongs
        if (current.isEmpty() && _uiState.value.pendingSong == null) {
            _uiState.update { it.copy(error = "Pick at least one song first") }
            return
        }
        _uiState.update { it.copy(songLocked = true) }
        viewModelScope.launch { gameRepository.lockSong() }
    }

    /**
     * Surprise Me: fills empty pick slots using personal Spotify top tracks if connected,
     * or popular top tracks from the server if not. If already full, replaces all with a fresh set.
     */
    fun surpriseMe() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val max = _uiState.value.maxSongs
                val current = _uiState.value.pendingSongs.toMutableList()
                val slotsNeeded = if (current.size >= max) max else (max - current.size)
                if (current.size >= max) {
                    current.clear()
                }

                val spotifyList = _uiState.value.spotifySuggestions
                val topList = if (_uiState.value.popularSuggestions.isNotEmpty()) {
                    _uiState.value.popularSuggestions
                } else {
                    val resp = httpClient.get("$baseUrl/music/top")
                    if (resp.status == HttpStatusCode.OK) {
                        val result = resp.body<Map<String, List<TrackSearchResult>>>()
                        result["tracks"] ?: emptyList()
                    } else emptyList()
                }

                val newEntries = mutableListOf<SongEntry>()
                if (_uiState.value.spotifyConnected && spotifyList.isNotEmpty()) {
                    val candidates = spotifyList.filterNot { s ->
                        current.any { it.title.equals(s.title, ignoreCase = true) && it.artist.equals(s.artist, ignoreCase = true) }
                    }.shuffled()
                    for (cand in candidates.take(slotsNeeded)) {
                        newEntries.add(
                            SongEntry(
                                songId = "${cand.title}_${cand.artist}",
                                title = cand.title,
                                artist = cand.artist,
                                albumArtUrl = cand.albumArtUrl,
                                previewUrl = "",
                                submitterId = _uiState.value.selfPlayerId
                            )
                        )
                    }
                }

                val stillNeeded = slotsNeeded - newEntries.size
                if (stillNeeded > 0 && topList.isNotEmpty()) {
                    val candidates = topList.filterNot { t ->
                        current.any { it.songId == t.id } || newEntries.any { it.songId == t.id }
                    }.shuffled()
                    for (track in candidates.take(stillNeeded)) {
                        newEntries.add(
                            SongEntry(
                                songId = track.id,
                                title = track.title,
                                artist = track.artist,
                                albumArtUrl = track.albumArtUrl,
                                previewUrl = track.previewUrl,
                                submitterId = _uiState.value.selfPlayerId
                            )
                        )
                    }
                }

                if (newEntries.isEmpty() && current.isEmpty()) {
                    _uiState.update { it.copy(error = "No suggestions available") }
                    return@launch
                }

                val updated = current + newEntries
                _uiState.update {
                    it.copy(
                        pendingSongs = updated,
                        pendingSong = updated.firstOrNull(),
                        searchResults = emptyList(),
                        error = null
                    )
                }
                gameRepository.updatePendingSongs(updated)
                updated.firstOrNull()?.let { gameRepository.updatePendingSong(it) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Surprise Me failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }
}
