package com.guesswhosesong.app.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.repository.GameRepository
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    val players: List<Player> = emptyList(),
    val selfPlayerId: String = "",
    val isHost: Boolean = false
)

sealed class ResultsEvent {
    data object NavigateToSubmission : ResultsEvent()
    data object NavigateToJoin : ResultsEvent()
}

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ResultsEvent>()
    val events: SharedFlow<ResultsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            gameRepository.messages.collect { message ->
                when (message) {
                    is GameResults -> {
                        _uiState.update { it.copy(players = message.players) }
                    }
                    is RoomJoined -> {
                        val isHost = message.room.players.find { it.id == message.selfPlayerId }?.isHost ?: false
                        _uiState.update { it.copy(selfPlayerId = message.selfPlayerId, isHost = isHost) }
                    }
                    is RoomUpdated -> {
                        val selfId = _uiState.value.selfPlayerId
                        val isHost = message.room.players.find { it.id == selfId }?.isHost ?: false
                        _uiState.update { it.copy(isHost = isHost) }
                        if (message.room.state == RoomState.SUBMISSION) {
                            _events.emit(ResultsEvent.NavigateToSubmission)
                        }
                    }
                    is RoomEnded -> _events.emit(ResultsEvent.NavigateToJoin)
                    else -> {}
                }
            }
        }
    }

    fun playAgain() {
        viewModelScope.launch { gameRepository.playAgain() }
    }

    fun endRoom() {
        viewModelScope.launch { gameRepository.endRoom() }
    }
}
