package com.guesswhosesong.app.ui.screens.lobby

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.guesswhosesong.app.data.player.AccountLinkResult
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.network.RoomPollingClient
import com.guesswhosesong.app.data.repository.GameRepository
import com.guesswhosesong.app.data.spotify.SpotifyAuthManager
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    val accountStatusIsError: Boolean = false
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
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "Can't connect to server") }
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
                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            gameRepository.connectionState.collect { state ->
                val isConnected = state == RoomPollingClient.ConnectionState.CONNECTED
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
            combine(spotifyAuthManager.isConnected, gameRepository.connectionState) { spotifyConnected, roomState ->
                spotifyConnected to roomState
            }.collect { (spotifyConnected, roomState) ->
                _uiState.update { it.copy(isSpotifyConnected = spotifyConnected) }
                if (spotifyConnected && roomState == RoomPollingClient.ConnectionState.CONNECTED) {
                    try {
                        gameRepository.refreshSpotify()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w("LobbyViewModel", "Spotify refresh failed", error)
                    }
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

    fun refreshSpotifyStatus() {
        spotifyAuthManager.refreshStatus()
    }

    fun disconnectGoogle() {
        viewModelScope.launch {
            runCatching { playerIdentityManager.disconnectGoogle() }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isAccountLinked = playerIdentityManager.isAccountLinked(),
                            accountStatus = null,
                            accountStatusIsError = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAccountLinked = playerIdentityManager.isAccountLinked(),
                            accountStatus = error.message ?: "Google disconnect failed",
                            accountStatusIsError = true
                        )
                    }
                }
        }
    }

    fun googleSignInIntent(activity: Activity) = playerIdentityManager.googleSignInIntent(activity)

    fun linkGoogle(data: Intent) {
        viewModelScope.launch {
            when (val result = playerIdentityManager.linkGoogle(data)) {
                AccountLinkResult.Linked -> _uiState.update {
                    it.copy(isAccountLinked = true, accountStatus = null, accountStatusIsError = false)
                }
                AccountLinkResult.SignedIn -> _uiState.update {
                    it.copy(isAccountLinked = true, accountStatus = null, accountStatusIsError = false)
                }
                AccountLinkResult.Collision -> _uiState.update {
                    it.copy(accountStatus = "That Google account belongs to another player. Leave the room to log in from the home screen.", accountStatusIsError = true)
                }
                is AccountLinkResult.Failed -> _uiState.update {
                    it.copy(
                        isAccountLinked = playerIdentityManager.isAccountLinked(),
                        accountStatus = result.message,
                        accountStatusIsError = true
                    )
                }
            }
        }
    }

    suspend fun updatePlayerProfile(displayName: String, avatarCustomization: AvatarCustomization): Boolean {
        val name = displayName.trim()
        if (name.isBlank() || name.length > 24 || name.any(Char::isISOControl)) {
            _uiState.update { it.copy(error = "Enter a name with 1 to 24 characters") }
            return false
        }
        val current = _uiState.value
        val currentPlayer = current.room?.players?.firstOrNull { it.id == current.selfPlayerId }
        val normalized = AvatarCustomization.normalize(avatarCustomization, avatarCustomization.shapeId)
        val updatedCustomization = if (name != currentPlayer?.displayName?.trim()) {
            normalized.copy(colorId = PlayerIdentityDefaults.randomInitialColorId(excludingColorId = normalized.colorId))
        } else {
            normalized
        }
        return try {
            if (!gameRepository.updatePlayerProfile(name, updatedCustomization)) {
                _uiState.update { it.copy(error = "Room connection is unavailable") }
                false
            } else {
                playerPreferences.edit()
                    .putString("displayName", name)
                    .putString("shapeId", updatedCustomization.shapeId)
                    .putString("colorId", updatedCustomization.colorId)
                    .putString("eyesId", updatedCustomization.eyesId)
                    .putString("mouthId", updatedCustomization.mouthId)
                    .putString("accessoryId", updatedCustomization.accessoryId)
                    .apply()
                _uiState.update { state ->
                    val room = state.room?.copy(players = state.room.players.map { player ->
                        if (player.id == state.selfPlayerId) player.copy(
                            displayName = name,
                            avatarId = updatedCustomization.shapeId,
                            avatarCustomization = updatedCustomization
                        ) else player
                    })
                    state.copy(room = room, error = null)
                }
                true
            }
        } catch (error: Exception) {
            _uiState.update { it.copy(error = error.message ?: "Could not update your player") }
            false
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

}
