package com.guesswhosesong.app.data.repository

import com.guesswhosesong.app.data.network.WebSocketManager
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.Room
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.SongEntry
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
                    }
                    is GameResults -> {
                        _latestGameResults.value = message
                    }
                    is RoomEnded -> {
                        _currentRoom.value = null
                        _pendingSongs.value = emptyList()
                    }
                    else -> {}
                }
            }
        }
    }

    fun connect(joinCode: String, token: String, displayName: String) {
        wsManager.connect(joinCode, token, displayName)
    }

    fun disconnect() {
        wsManager.disconnect()
        _currentRoom.value = null
        _latestGameResults.value = null
        _pendingSongs.value = emptyList()
    }

    suspend fun startGame() = wsManager.send(StartGame())
    suspend fun submitSong(song: SongEntry) = wsManager.send(SubmitSong(song = song))
    suspend fun updatePendingSong(song: SongEntry) = wsManager.send(UpdatePendingSong(song = song))
    suspend fun updatePendingSongs(songs: List<SongEntry>) {
        _pendingSongs.value = songs
        wsManager.send(UpdatePendingSongs(songs = songs))
    }
    suspend fun lockSong() = wsManager.send(LockSong())
    suspend fun castVote(guessedPlayerId: String) = wsManager.send(CastVote(guessedPlayerId = guessedPlayerId))
    suspend fun sendChat(text: String) = wsManager.send(SendChat(text = text))
    suspend fun updateSettings(settings: RoomSettings) = wsManager.send(UpdateSettings(settings = settings))
    suspend fun kickPlayer(targetId: String) = wsManager.send(KickPlayer(targetPlayerId = targetId))
    suspend fun playAgain() = wsManager.send(PlayAgain())
    suspend fun endRoom() = wsManager.send(EndRoom())
    suspend fun connectSpotify(accessToken: String) = wsManager.send(ConnectSpotify(accessToken = accessToken))
}
