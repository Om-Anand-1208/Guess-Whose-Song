package com.guesswhosesong.app.ui.screens.join

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.app.data.player.AccountLinkResult
import com.guesswhosesong.app.data.repository.RoomRepository
import com.guesswhosesong.app.data.spotify.SpotifyAuthManager
import com.guesswhosesong.shared.models.AvatarCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinUiState(
    val displayName: String = "",
    val avatarId: String = AvatarCatalog.DEFAULT_ID,
    val joinCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val accountStatus: String? = null,
    val accountStatusIsError: Boolean = false,
    val isSpotifyConnected: Boolean = false,
    val isAccountLinked: Boolean = false
)

sealed class JoinEvent {
    data class NavigateToLobby(val joinCode: String, val displayName: String, val avatarId: String) : JoinEvent()
}

@HiltViewModel
class JoinViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val playerIdentityManager: PlayerIdentityManager,
    private val spotifyAuthManager: SpotifyAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinUiState())
    val uiState: StateFlow<JoinUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<JoinEvent>()
    val events: SharedFlow<JoinEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            spotifyAuthManager.isConnected.collect { connected ->
                _uiState.update { it.copy(isSpotifyConnected = connected) }
            }
        }
    }

    fun connectSpotify() {
        spotifyAuthManager.launchOAuth()
    }

    fun disconnectSpotify() {
        spotifyAuthManager.disconnect()
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name.take(24), error = null) }
    }

    fun onAvatarSelected(avatarId: String) {
        _uiState.update { it.copy(avatarId = AvatarCatalog.normalize(avatarId), error = null) }
    }

    fun onJoinCodeChanged(code: String) {
        _uiState.update { it.copy(joinCode = code.uppercase().take(6), error = null) }
    }

    fun createRoom() {
        val name = _uiState.value.displayName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Enter your name first") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val avatarId = _uiState.value.avatarId
                val result = roomRepository.createRoom(name, avatarId)
                result.fold(
                    onSuccess = { response ->
                        _events.emit(JoinEvent.NavigateToLobby(response.joinCode, name, avatarId))
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = "Failed to create room: ${e.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun joinRoom() {
        val name = _uiState.value.displayName.trim()
        val code = _uiState.value.joinCode.trim()
        val avatarId = _uiState.value.avatarId
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Enter your name first") }
            return
        }
        if (code.length != 6) {
            _uiState.update { it.copy(error = "Enter the 6-character room code") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                _events.emit(JoinEvent.NavigateToLobby(code, name, avatarId))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun googleSignInIntent(activity: Activity) = playerIdentityManager.googleSignInIntent(activity)

    fun linkGoogle(data: Intent) {
        viewModelScope.launch {
            _uiState.update { it.copy(accountStatus = null, accountStatusIsError = false) }
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

    fun recoverWithGoogle(data: Intent) {
        viewModelScope.launch {
            _uiState.update { it.copy(accountStatus = null, accountStatusIsError = false) }
            when (val result = playerIdentityManager.signInWithGoogle(data)) {
                AccountLinkResult.SignedIn -> _uiState.update {
                    it.copy(isAccountLinked = true, accountStatus = "Recovered linked Google identity", accountStatusIsError = false)
                }
                else -> {
                    val message = (result as? AccountLinkResult.Failed)?.message ?: "Google sign-in failed"
                    _uiState.update { it.copy(accountStatus = message, accountStatusIsError = true) }
                }
            }
        }
    }
}
