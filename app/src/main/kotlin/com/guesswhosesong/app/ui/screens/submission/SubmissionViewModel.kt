package com.guesswhosesong.app.ui.screens.submission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.repository.GameRepository
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubmissionUiState(
    val room: Room? = null,
    val selfPlayerId: String = "",
    val searchQuery: String = "",
    val searchResults: List<TrackSearchResult> = emptyList(),
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

@HiltViewModel
class SubmissionViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val httpClient: HttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmissionUiState())
    val uiState: StateFlow<SubmissionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SubmissionEvent>()
    val events: SharedFlow<SubmissionEvent> = _events.asSharedFlow()

    private val baseUrl = com.guesswhosesong.app.di.NetworkModule.BASE_URL

    init {
        observeMessages()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            gameRepository.messages.collect { message ->
                when (message) {
                    is RoomJoined -> _uiState.update {
                        it.copy(
                            room = message.room,
                            selfPlayerId = message.selfPlayerId,
                            totalCount = message.room.players.size
                        )
                    }
                    is RoomUpdated -> {
                        val room = message.room
                        _uiState.update { it.copy(room = room, totalCount = room.players.size) }
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
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, error = null) }
    }

    fun searchSong() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val result = httpClient.get("$baseUrl/music/search") {
                    parameter("q", query)
                }.body<Map<String, List<TrackSearchResult>>>()
                _uiState.update { it.copy(searchResults = result["tracks"] ?: emptyList()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Search failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun selectSong(track: TrackSearchResult) {
        val entry = SongEntry(
            songId = track.id.toString(),
            title = track.title,
            artist = track.artist,
            albumArtUrl = track.albumArtUrl,
            previewUrl = track.previewUrl,
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
     * Called for Surprise Me: fetches Spotify top tracks from backend.
     * Updates pendingSong (reroll = call again).
     */
    fun surpriseMe() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                // If the user hasn't authenticated Spotify on the backend, this might fail or return a fallback
                val result = httpClient.get("$baseUrl/spotify/suggestions").body<Map<String, List<SpotifySuggestion>>>()
                val suggestions = result["suggestions"] ?: emptyList()
                if (suggestions.isEmpty()) {
                    _uiState.update { it.copy(error = "No Spotify suggestions found") }
                    return@launch
                }
                val pick = suggestions.random()
                val entry = SongEntry(
                    songId = "${pick.title}_${pick.artist}",
                    title = pick.title,
                    artist = pick.artist,
                    albumArtUrl = pick.albumArtUrl,
                    previewUrl = "", // will be resolved server-side on lock
                    submitterId = _uiState.value.selfPlayerId
                )
                _uiState.update { it.copy(pendingSong = entry, searchResults = emptyList()) }
                gameRepository.updatePendingSong(entry)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Surprise Me failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }
}
