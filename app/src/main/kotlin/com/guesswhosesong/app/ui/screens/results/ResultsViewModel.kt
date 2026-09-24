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

    private val _uiState: MutableStateFlow<ResultsUiState>
    val uiState: StateFlow<ResultsUiState>

    private val _events = MutableSharedFlow<ResultsEvent>()
    val events: SharedFlow<ResultsEvent> = _events.asSharedFlow()

    init {
        val cachedResults = gameRepository.latestGameResults.value
        val cachedRoom = gameRepository.currentRoom.value
        val selfId = gameRepository.selfPlayerId.value
        val initialPlayers = cachedResults?.players
            ?: cachedRoom?.players?.sortedByDescending { it.score }
            ?: emptyList()
        val isHost = cachedRoom?.players?.find { it.id == selfId }?.isHost ?: false

        _uiState = MutableStateFlow(
            ResultsUiState(
                players = initialPlayers,
                selfPlayerId = selfId,
                isHost = isHost
            )
        )
        uiState = _uiState.asStateFlow()

        viewModelScope.launch {
            gameRepository.messages.collect { message ->
                when (message) {
                    is GameResults -> {
                        _uiState.update { it.copy(players = message.players) }
                    }
                    is RoomJoined -> {
                        val host = message.room.players.find { it.id == message.selfPlayerId }?.isHost ?: false
                        _uiState.update { it.copy(selfPlayerId = message.selfPlayerId, isHost = host) }
                    }
                    is RoomUpdated -> {
                        val currentSelfId = _uiState.value.selfPlayerId.ifBlank { gameRepository.selfPlayerId.value }
                        val host = message.room.players.find { it.id == currentSelfId }?.isHost ?: false
                        _uiState.update { it.copy(isHost = host) }
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
