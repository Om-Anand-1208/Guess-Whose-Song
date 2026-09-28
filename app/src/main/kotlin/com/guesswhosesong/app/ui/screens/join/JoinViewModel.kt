package com.guesswhosesong.app.ui.screens.join

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.app.data.player.AccountLinkResult
import com.guesswhosesong.app.data.repository.RoomRepository
import com.guesswhosesong.app.data.spotify.SpotifyAuthManager
import com.guesswhosesong.shared.models.AvatarCatalog
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.PlayerIdentityDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinUiState(
    val displayName: String = "",
    val avatarId: String = AvatarCatalog.DEFAULT_ID,
    val avatarCustomization: AvatarCustomization = AvatarCustomization.defaultsFor(AvatarCatalog.DEFAULT_ID),
    val joinCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val accountStatus: String? = null,
    val accountStatusIsError: Boolean = false,
    val isSpotifyConnected: Boolean = false,
    val isAccountLinked: Boolean = false
)

sealed class JoinEvent {
    data class NavigateToLobby(
        val joinCode: String,
        val displayName: String,
        val avatarCustomization: AvatarCustomization
    ) : JoinEvent()
}

@HiltViewModel
class JoinViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val playerIdentityManager: PlayerIdentityManager,
    private val spotifyAuthManager: SpotifyAuthManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val avatarPreferences = context.getSharedPreferences("player_avatar", Context.MODE_PRIVATE)
    private val initialCustomization = loadAvatarCustomization()
    private val initialDisplayName = loadPlayerName()
    private val _uiState = MutableStateFlow(
        JoinUiState(
            displayName = initialDisplayName,
            avatarId = initialCustomization.shapeId,
            avatarCustomization = initialCustomization,
            isAccountLinked = playerIdentityManager.isAccountLinked()
        )
    )
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

    fun savePlayerProfile(displayName: String, customization: AvatarCustomization): Boolean {
        val name = displayName.trim()
        if (name.isBlank() || name.length > 24 || name.any(Char::isISOControl)) {
            _uiState.update { it.copy(error = "Enter a name with 1 to 24 characters") }
            return false
        }
        val normalized = AvatarCustomization.normalize(customization, _uiState.value.avatarId)
        saveAvatarCustomization(normalized)
        avatarPreferences.edit().putString("displayName", name).apply()
        _uiState.update {
            it.copy(displayName = name, avatarId = normalized.shapeId, avatarCustomization = normalized, error = null)
        }
        return true
    }

    fun refreshSavedProfile() {
        val name = loadPlayerName()
        val customization = loadAvatarCustomization()
        _uiState.update {
            it.copy(
                displayName = name,
                avatarId = customization.shapeId,
                avatarCustomization = customization,
                isAccountLinked = playerIdentityManager.isAccountLinked()
            )
        }
    }

    fun onAvatarSelected(avatarId: String) {
        val customization = AvatarCustomization.defaultsFor(AvatarCatalog.normalize(avatarId))
        saveAvatarCustomization(customization)
        _uiState.update {
            it.copy(avatarId = customization.shapeId, avatarCustomization = customization, error = null)
        }
    }

    fun onAvatarCustomizationChanged(customization: AvatarCustomization) {
        val normalized = AvatarCustomization.normalize(customization, _uiState.value.avatarId)
        saveAvatarCustomization(normalized)
        _uiState.update {
            it.copy(avatarId = normalized.shapeId, avatarCustomization = normalized, error = null)
        }
    }

    fun onJoinCodeChanged(code: String) {
        _uiState.update { it.copy(joinCode = code.uppercase().take(6), error = null) }
    }

    fun createRoom() {
        refreshSavedProfile()
        val name = _uiState.value.displayName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Enter your name first") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val customization = _uiState.value.avatarCustomization
                val result = roomRepository.createRoom(name, customization.shapeId, customization)
                result.fold(
                    onSuccess = { response ->
                        _events.emit(JoinEvent.NavigateToLobby(response.joinCode, name, customization))
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
        refreshSavedProfile()
        val name = _uiState.value.displayName.trim()
        val code = _uiState.value.joinCode.trim()
        val customization = _uiState.value.avatarCustomization
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
                _events.emit(JoinEvent.NavigateToLobby(code, name, customization))
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

    private fun loadAvatarCustomization(): AvatarCustomization {
        val savedShapeId = avatarPreferences.getString("shapeId", null)
        val shapeId = savedShapeId?.let(AvatarCatalog::normalize)
            ?: PlayerIdentityDefaults.randomAvatarCustomization().shapeId
        val defaults = AvatarCustomization.defaultsFor(shapeId)
        val normalized = AvatarCustomization.normalize(
            AvatarCustomization(
                shapeId = shapeId,
                colorId = avatarPreferences.getString("colorId", defaults.colorId) ?: defaults.colorId,
                eyesId = avatarPreferences.getString("eyesId", defaults.eyesId) ?: defaults.eyesId,
                mouthId = avatarPreferences.getString("mouthId", defaults.mouthId) ?: defaults.mouthId,
                accessoryId = avatarPreferences.getString("accessoryId", defaults.accessoryId) ?: defaults.accessoryId
            ),
            shapeId
        )
        saveAvatarCustomization(normalized)
        return normalized
    }

    private fun loadPlayerName(): String = avatarPreferences.getString("displayName", null)
        ?.trim()
        ?.takeIf { it.isNotBlank() && it.length <= 24 }
        ?: PlayerIdentityDefaults.randomDisplayName().also { generated ->
            avatarPreferences.edit().putString("displayName", generated).apply()
        }

    private fun saveAvatarCustomization(customization: AvatarCustomization) {
        val normalized = AvatarCustomization.normalize(customization, AvatarCatalog.DEFAULT_ID)
        avatarPreferences.edit()
            .putString("shapeId", normalized.shapeId)
            .putString("colorId", normalized.colorId)
            .putString("eyesId", normalized.eyesId)
            .putString("mouthId", normalized.mouthId)
            .putString("accessoryId", normalized.accessoryId)
            .apply()
    }
}
