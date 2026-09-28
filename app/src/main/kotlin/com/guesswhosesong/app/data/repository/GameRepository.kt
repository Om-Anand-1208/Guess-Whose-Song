package com.guesswhosesong.app.data.repository

import com.guesswhosesong.app.data.network.WebSocketManager
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.Room
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.AvatarCustomization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Single facade that the ViewModels use for all game interactions.
 * Delegates to [WebSocketManager] for realtime operations and caches
 * active session state across Jetpack Compose navigation boundaries.
 */
class GameRepository(private val wsManager: WebSocketManager) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val messages: SharedFlow<ServerMessage> = wsManager.messages
    val connectionState = wsManager.connectionState
    val lastError = wsManager.lastError

    private val _currentRoom = MutableStateFlow<Room?>(null)
    val currentRoom: StateFlow<Room?> = _currentRoom.asStateFlow()

    private val _selfPlayerId = MutableStateFlow("")
    val selfPlayerId: StateFlow<String> = _selfPlayerId.asStateFlow()

    private val _latestGameResults = MutableStateFlow<GameResults?>(null)
    val latestGameResults: StateFlow<GameResults?> = _latestGameResults.asStateFlow()

    private val _pendingSongs = MutableStateFlow<List<SongEntry>>(emptyList())
    val pendingSongs: StateFlow<List<SongEntry>> = _pendingSongs.asStateFlow()

    private val _mySubmittedSongs = MutableStateFlow<List<SongEntry>>(emptyList())
    val mySubmittedSongs: StateFlow<List<SongEntry>> = _mySubmittedSongs.asStateFlow()

    private val _currentRoundPreview = MutableStateFlow<RoundPreviewStarted?>(null)
    val currentRoundPreview: StateFlow<RoundPreviewStarted?> = _currentRoundPreview.asStateFlow()

    private val _currentVotingStarted = MutableStateFlow<VotingStarted?>(null)
    val currentVotingStarted: StateFlow<VotingStarted?> = _currentVotingStarted.asStateFlow()

    private val _currentRoundRevealed = MutableStateFlow<RoundRevealed?>(null)
    val currentRoundRevealed: StateFlow<RoundRevealed?> = _currentRoundRevealed.asStateFlow()

    init {
        scope.launch {
            wsManager.messages.collect { message ->
                when (message) {
                    is RoomJoined -> {
                        _selfPlayerId.value = message.selfPlayerId
                        _currentRoom.value = message.room
                        _latestGameResults.value = null
                    }
                    is RoomUpdated -> {
                        _currentRoom.value = message.room
                        if (message.room.state == com.guesswhosesong.shared.models.RoomState.SUBMISSION) {
                            resetRoundState()
                            _mySubmittedSongs.value = emptyList()
                            _pendingSongs.value = emptyList()
                        }
                    }
                    is RoundPreviewStarted -> {
                        _currentRoundPreview.value = message
                        _currentRoundRevealed.value = null
                    }
                    is VotingStarted -> {
                        _currentVotingStarted.value = message
                    }
                    is RoundRevealed -> {
                        _currentRoundRevealed.value = message
                    }
                    is GameResults -> {
                        _latestGameResults.value = message
                    }
                    is RoomEnded -> {
                        _currentRoom.value = null
                        _pendingSongs.value = emptyList()
                        _mySubmittedSongs.value = emptyList()
                        resetRoundState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun resetRoundState() {
        _currentRoundPreview.value = null
        _currentVotingStarted.value = null
        _currentRoundRevealed.value = null
    }

    fun isMySong(title: String, artist: String): Boolean {
        if (title.isBlank()) return false
        return _mySubmittedSongs.value.any {
            it.title.trim().equals(title.trim(), ignoreCase = true) &&
            (artist.isBlank() || it.artist.isBlank() || it.artist.trim().equals(artist.trim(), ignoreCase = true))
        }
    }

    fun connect(
        joinCode: String,
        displayName: String,
        avatarId: String,
        avatarCustomization: AvatarCustomization? = null
    ) {
        wsManager.connect(joinCode, displayName, avatarId, avatarCustomization)
    }

    fun disconnect() {
        wsManager.disconnect()
        _currentRoom.value = null
        _latestGameResults.value = null
        _pendingSongs.value = emptyList()
        _mySubmittedSongs.value = emptyList()
        resetRoundState()
    }

    suspend fun startGame() = wsManager.send(StartGame())
    suspend fun submitSong(song: SongEntry) {
        _mySubmittedSongs.value = listOf(song)
        wsManager.send(UpdatePendingSong(song = song))
    }
    suspend fun updatePendingSong(song: SongEntry) = wsManager.send(UpdatePendingSong(song = song))
    suspend fun updatePendingSongs(songs: List<SongEntry>) {
        _pendingSongs.value = songs
        _mySubmittedSongs.value = songs
        wsManager.send(UpdatePendingSongs(songs = songs))
    }
    suspend fun lockSong() = wsManager.send(LockSong())
    suspend fun castVote(guessedPlayerId: String) = wsManager.send(CastVote(guessedPlayerId = guessedPlayerId))
    suspend fun sendChat(text: String) = wsManager.send(SendChat(text = text))
    suspend fun updateSettings(settings: RoomSettings) = wsManager.send(UpdateSettings(settings = settings))
    suspend fun kickPlayer(targetId: String) = wsManager.send(KickPlayer(targetPlayerId = targetId))
    suspend fun playAgain() {
        resetRoundState()
        _pendingSongs.value = emptyList()
        _mySubmittedSongs.value = emptyList()
        wsManager.send(PlayAgain())
    }
    suspend fun endRoom() = wsManager.send(EndRoom())
    suspend fun refreshSpotify() = wsManager.send(RefreshSpotify())
}
