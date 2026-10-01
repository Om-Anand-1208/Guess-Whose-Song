package com.guesswhosesong.app.ui.screens.lobby

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.guesswhosesong.app.data.player.AccountLinkResult
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.network.WebSocketManager
import com.guesswhosesong.app.data.repository.GameRepository
import com.guesswhosesong.app.data.spotify.SpotifyAuthManager
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LobbyUiState(
    val room: Room? = null,
    val selfPlayerId: String = "",
    val isConnected: Boolean = false,
    val error: String? = null,
    val isSpotifyConnected: Boolean = false,
    val isAccountLinked: Boolean = false,
    val accountStatus: String? = null,
    val accountStatusIsError: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList()
)

sealed class LobbyEvent {
    data object NavigateToSubmission : LobbyEvent()
    data object NavigateToGame : LobbyEvent()
    data object Kicked : LobbyEvent()
}

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val spotifyAuthManager: SpotifyAuthManager,
    private val playerIdentityManager: PlayerIdentityManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val playerPreferences = context.getSharedPreferences("player_avatar", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LobbyEvent>()
    val events: SharedFlow<LobbyEvent> = _events.asSharedFlow()

    init {
        _uiState.update { it.copy(isAccountLinked = playerIdentityManager.isAccountLinked()) }
    }

    fun connect(
        joinCode: String,
        displayName: String,
        avatarCustomization: AvatarCustomization
    ) {
        viewModelScope.launch {
            try {
                gameRepository.connect(
                    joinCode,
                    displayName,
                    avatarCustomization.shapeId,
                    avatarCustomization
                )
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
                    is ErrorMessage -> _uiState.update { it.copy(error = message.message) }
                    is ChatReceived -> _uiState.update { it.copy(chatMessages = it.chatMessages + message.message) }
                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            gameRepository.connectionState.collect { state ->
                val isConnected = state == WebSocketManager.ConnectionState.CONNECTED
                _uiState.update {
                    it.copy(
                        isConnected = isConnected,
                        error = if (isConnected) null else it.error
                    )
                }
            }
        }
        viewModelScope.launch {
            gameRepository.lastError.collect { err ->
                _uiState.update { it.copy(error = err) }
            }
        }
        viewModelScope.launch {
            spotifyAuthManager.isConnected.collect { connected ->
                if (connected) {
                    _uiState.update { it.copy(isSpotifyConnected = true) }
                    gameRepository.refreshSpotify()
                }
            }
        }
    }

    fun connectSpotify() {
        spotifyAuthManager.launchOAuth()
    }

    fun disconnectSpotify() {
        spotifyAuthManager.disconnect()
    }

    fun googleSignInIntent(activity: Activity) = playerIdentityManager.googleSignInIntent(activity)

    fun linkGoogle(data: Intent) {
        viewModelScope.launch {
            when (val result = playerIdentityManager.linkGoogle(data)) {
                AccountLinkResult.Linked -> _uiState.update {
                    it.copy(isAccountLinked = true, accountStatus = "Google account linked for recovery", accountStatusIsError = false)
                }
                AccountLinkResult.SignedIn -> _uiState.update {
                    it.copy(isAccountLinked = true, accountStatus = "Google account signed in", accountStatusIsError = false)
                }
                AccountLinkResult.Collision -> _uiState.update {
                    it.copy(accountStatus = "That Google account is already linked to another player", accountStatusIsError = true)
                }
                is AccountLinkResult.Failed -> _uiState.update {
                    it.copy(accountStatus = result.message, accountStatusIsError = true)
                }
            }
        }
    }

    fun updatePlayerProfile(displayName: String, avatarCustomization: AvatarCustomization): Boolean {
        val name = displayName.trim()
        if (name.isBlank() || name.length > 24 || name.any(Char::isISOControl)) {
            _uiState.update { it.copy(error = "Enter a name with 1 to 24 characters") }
            return false
        }
        val normalized = AvatarCustomization.normalize(avatarCustomization, avatarCustomization.shapeId)
        playerPreferences.edit()
            .putString("displayName", name)
            .putString("shapeId", normalized.shapeId)
            .putString("colorId", normalized.colorId)
            .putString("eyesId", normalized.eyesId)
            .putString("mouthId", normalized.mouthId)
            .putString("accessoryId", normalized.accessoryId)
            .apply()
        viewModelScope.launch {
            runCatching { gameRepository.updatePlayerProfile(name, normalized) }
                .onFailure { _uiState.update { current -> current.copy(error = it.message ?: "Could not update your player") } }
        }
        return true
    }

    fun startGame() {
        viewModelScope.launch { gameRepository.startGame() }
    }

    fun addComputerPlayer() {
        viewModelScope.launch { gameRepository.addComputerPlayer() }
    }

    fun updateSettings(settings: RoomSettings) {
        viewModelScope.launch { gameRepository.updateSettings(settings) }
    }

    fun kickPlayer(playerId: String) {
        viewModelScope.launch { gameRepository.kickPlayer(playerId) }
    }

    fun sendChat(text: String) {
        val message = text.trim()
        if (message.isNotBlank()) viewModelScope.launch { gameRepository.sendChat(message) }
    }

    override fun onCleared() {
        super.onCleared()
        gameRepository.disconnect()
    }
}
