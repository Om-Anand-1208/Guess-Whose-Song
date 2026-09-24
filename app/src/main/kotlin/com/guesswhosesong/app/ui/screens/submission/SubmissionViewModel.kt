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
    val pendingSong: SongEntry? = null,
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
        observeMessages()
        loadPopularSuggestions()
        observeSearchQuery()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            gameRepository.messages.collect { message ->
                when (message) {
                    is RoomJoined -> {
                        val selfId = message.selfPlayerId
                        val isSpotify = message.room.players.find { it.id == selfId }?.spotifyConnected == true
                        _uiState.update {
                            it.copy(
                                room = message.room,
                                selfPlayerId = selfId,
                                totalCount = message.room.players.size,
                                spotifyConnected = isSpotify
                            )
                        }
                        if (isSpotify) loadSpotifySuggestions()
                    }
                    is RoomUpdated -> {
                        val room = message.room
                        val selfId = _uiState.value.selfPlayerId
                        val isSpotify = room.players.find { it.id == selfId }?.spotifyConnected == true
                        _uiState.update {
                            it.copy(
                                room = room,
                                totalCount = room.players.size,
                                spotifyConnected = isSpotify
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
                    gameRepository.connectSpotify("")
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
                val response = httpClient.get("$baseUrl/music/top")
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
        val playerId = _uiState.value.selfPlayerId
        if (playerId.isNotBlank()) {
            spotifyAuthManager.launchOAuth(playerId)
        }
    }

    fun loadSpotifySuggestions() {
        viewModelScope.launch {
            try {
                val token = playerIdentityManager.getToken()
                val response = httpClient.get("$baseUrl/spotify/top") {
                    header("Authorization", "Bearer $token")
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
        val entry = SongEntry(
            songId = track.id,
            title = track.title,
            artist = track.artist,
            albumArtUrl = track.albumArtUrl,
            previewUrl = track.previewUrl,
            submitterId = _uiState.value.selfPlayerId
        )
        _uiState.update { it.copy(pendingSong = entry, searchResults = emptyList()) }
        viewModelScope.launch { gameRepository.updatePendingSong(entry) }
    }

    fun selectSpotifyTrack(suggestion: SpotifySuggestion) {
        val entry = SongEntry(
            songId = "${suggestion.title}_${suggestion.artist}",
            title = suggestion.title,
            artist = suggestion.artist,
            albumArtUrl = suggestion.albumArtUrl,
            previewUrl = "", // will be resolved server-side on lock
            submitterId = _uiState.value.selfPlayerId
        )
        _uiState.update { it.copy(pendingSong = entry, searchResults = emptyList()) }
        viewModelScope.launch { gameRepository.updatePendingSong(entry) }
    }

    fun lockSong() {
        if (_uiState.value.pendingSong == null) {
            _uiState.update { it.copy(error = "Pick a song first") }
            return
        }
        _uiState.update { it.copy(songLocked = true) }
        viewModelScope.launch { gameRepository.lockSong() }
    }

    /**
     * Surprise Me: picks from user's personal Spotify top tracks if connected,
     * or popular top tracks from the server if not.
     */
    fun surpriseMe() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val spotifyList = _uiState.value.spotifySuggestions
                if (_uiState.value.spotifyConnected && spotifyList.isNotEmpty()) {
                    val pick = spotifyList.random()
                    selectSpotifyTrack(pick)
                } else {
                    val topList = if (_uiState.value.popularSuggestions.isNotEmpty()) {
                        _uiState.value.popularSuggestions
                    } else {
                        val resp = httpClient.get("$baseUrl/music/top")
                        if (resp.status == HttpStatusCode.OK) {
                            val result = resp.body<Map<String, List<TrackSearchResult>>>()
                            result["tracks"] ?: emptyList()
                        } else emptyList()
                    }
                    if (topList.isEmpty()) {
                        _uiState.update { it.copy(error = "No suggestions found") }
                        return@launch
                    }
                    val pick = topList.random()
                    selectSong(pick)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Surprise Me failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }
}
