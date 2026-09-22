package com.guesswhosesong.app.ui.screens.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.firebase.FirebaseAuthManager
import com.guesswhosesong.app.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinUiState(
    val displayName: String = "",
    val joinCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class JoinEvent {
    data class NavigateToLobby(val joinCode: String, val playerId: String, val displayName: String) : JoinEvent()
}

@HiltViewModel
class JoinViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val authManager: FirebaseAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinUiState())
    val uiState: StateFlow<JoinUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<JoinEvent>()
    val events: SharedFlow<JoinEvent> = _events.asSharedFlow()

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name.take(24), error = null) }
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
                val uid = authManager.signInAnonymously()
                val token = authManager.getIdToken()
                val result = roomRepository.createRoom(name, token)
                result.fold(
                    onSuccess = { response ->
                        _events.emit(JoinEvent.NavigateToLobby(response.joinCode, uid, name))
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = "Failed to create room: ${e.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Auth error: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun joinRoom() {
        val name = _uiState.value.displayName.trim()
        val code = _uiState.value.joinCode.trim()
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
                val uid = authManager.signInAnonymously()
                _events.emit(JoinEvent.NavigateToLobby(code, uid, name))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Auth error: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
