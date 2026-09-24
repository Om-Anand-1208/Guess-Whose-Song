package com.guesswhosesong.app.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.firebase.FirebaseAuthManager
import com.guesswhosesong.app.data.network.WebSocketManager
import com.guesswhosesong.app.data.repository.GameRepository
import com.guesswhosesong.app.data.spotify.SpotifyAuthManager
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LobbyUiState(
    val room: Room? = null,
    val selfPlayerId: String = "",
    val isConnected: Boolean = false,
    val error: String? = null,
    val isSpotifyConnected: Boolean = false
)

sealed class LobbyEvent {
    data object NavigateToSubmission : LobbyEvent()
    data object NavigateToGame : LobbyEvent()
    data object Kicked : LobbyEvent()
}

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val authManager: FirebaseAuthManager,
    private val spotifyAuthManager: SpotifyAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LobbyEvent>()
    val events: SharedFlow<LobbyEvent> = _events.asSharedFlow()

    fun connect(joinCode: String, displayName: String) {
        viewModelScope.launch {
            try {
                val uid = authManager.signInAnonymously()
                val token = authManager.getIdToken()
                _uiState.update { it.copy(selfPlayerId = uid) }
                gameRepository.connect(joinCode, token, displayName)
                observeMessages()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Connection failed: ${e.message}") }
            }
        }
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
                                isConnected = true,
                                isSpotifyConnected = isSpotify
                            )
                        }
                    }
                    is RoomUpdated -> {
                        val newRoom = message.room
                        val selfId = _uiState.value.selfPlayerId
                        val isSpotify = newRoom.players.find { it.id == selfId }?.spotifyConnected == true
                        _uiState.update { it.copy(room = newRoom, isSpotifyConnected = isSpotify) }
                        // Navigate when state changes
                        when (newRoom.state) {
                            RoomState.SUBMISSION -> _events.emit(LobbyEvent.NavigateToSubmission)
                            RoomState.PLAYING -> _events.emit(LobbyEvent.NavigateToGame)
                            else -> {}
                        }
                    }
                    is Kicked -> _events.emit(LobbyEvent.Kicked)
                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            gameRepository.connectionState.collect { state ->
                _uiState.update {
                    it.copy(isConnected = state == WebSocketManager.ConnectionState.CONNECTED)
                }
            }
        }
        viewModelScope.launch {
            gameRepository.lastError.collect { err ->
                if (err != null) {
                    _uiState.update { it.copy(error = err) }
                }
            }
        }
        viewModelScope.launch {
            spotifyAuthManager.isConnected.collect { connected ->
                if (connected) {
                    _uiState.update { it.copy(isSpotifyConnected = true) }
                    gameRepository.connectSpotify("")
                }
            }
        }
    }

    fun connectSpotify() {
        val uid = _uiState.value.selfPlayerId
        if (uid.isNotBlank()) {
            spotifyAuthManager.launchOAuth(uid)
        }
    }

    fun startGame() {
        viewModelScope.launch { gameRepository.startGame() }
    }

    fun updateSettings(settings: RoomSettings) {
        viewModelScope.launch { gameRepository.updateSettings(settings) }
    }

    fun kickPlayer(playerId: String) {
        viewModelScope.launch { gameRepository.kickPlayer(playerId) }
    }

    override fun onCleared() {
        super.onCleared()
        gameRepository.disconnect()
    }
}
