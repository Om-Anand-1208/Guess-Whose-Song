package com.guesswhosesong.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.repository.GameRepository
import com.guesswhosesong.app.data.repository.RoomChatRepository
import com.guesswhosesong.shared.models.RoomChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomChatUiState(
    val roomCode: String? = null,
    val selfPlayerId: String = "",
    val isHost: Boolean = false,
    val messages: List<RoomChatMessage> = emptyList(),
    val hasOlder: Boolean = false,
    val loadingOlder: Boolean = false,
    val unreadCount: Int = 0,
    val isOpen: Boolean = false,
    val draft: String = "",
    val sending: Boolean = false,
    val endingRoom: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RoomChatViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val chatRepository: RoomChatRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RoomChatUiState())
    val uiState: StateFlow<RoomChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            gameRepository.selfPlayerId.collect { playerId ->
                _uiState.update { it.copy(selfPlayerId = playerId) }
            }
        }
        viewModelScope.launch {
            combine(gameRepository.currentRoom, gameRepository.selfPlayerId) { room, playerId ->
                room?.players?.any { it.id == playerId && it.isHost } == true
            }.collect { isHost ->
                _uiState.update { it.copy(isHost = isHost) }
            }
        }
        viewModelScope.launch {
            gameRepository.currentRoom.map { it?.joinCode }.distinctUntilChanged().collectLatest { code ->
                _uiState.update { current ->
                    current.copy(
                        roomCode = code,
                        messages = emptyList(),
                        hasOlder = false,
                        loadingOlder = false,
                        unreadCount = 0,
                        isOpen = false,
                        draft = "",
                        sending = false,
                        endingRoom = false,
                        error = null
                    )
                }
                if (code == null) return@collectLatest
                var firstLoad = true
                while (true) {
                    try {
                        val latest = chatRepository.recent(code)
                        _uiState.update { current ->
                            val knownIds = current.messages.mapTo(HashSet()) { it.id }
                            val unread = if (firstLoad || current.isOpen) 0 else latest.count {
                                it.id !in knownIds && it.senderId != current.selfPlayerId
                            }
                            val merged = (current.messages + latest)
                                .distinctBy { it.id }
                                .sortedWith(compareBy(RoomChatMessage::sentAtEpochMillis, RoomChatMessage::id))
                            current.copy(
                                messages = merged,
                                hasOlder = if (firstLoad) latest.size == 50 else current.hasOlder,
                                unreadCount = current.unreadCount + unread
                            )
                        }
                        firstLoad = false
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // A room reconnect is handled by the next poll.
                    }
                    delay(3_000)
                }
            }
        }
    }

    fun setOpen(open: Boolean) {
        _uiState.update { it.copy(isOpen = open, unreadCount = if (open) 0 else it.unreadCount, error = null) }
    }

    fun setDraft(value: String) {
        _uiState.update { it.copy(draft = value.take(280), error = null) }
    }

    fun loadOlder() {
        val snapshot = _uiState.value
        val code = snapshot.roomCode ?: return
        val oldestId = snapshot.messages.firstOrNull()?.id ?: return
        if (!snapshot.hasOlder || snapshot.loadingOlder) return
        _uiState.update { it.copy(loadingOlder = true, error = null) }
        viewModelScope.launch {
            try {
                val older = chatRepository.recent(code, oldestId)
                _uiState.update { current ->
                    if (current.roomCode != code) current else current.copy(
                        messages = (current.messages + older)
                            .distinctBy { it.id }
                            .sortedWith(compareBy(RoomChatMessage::sentAtEpochMillis, RoomChatMessage::id)),
                        hasOlder = older.size == 50
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { current ->
                    if (current.roomCode == code) current.copy(error = "Couldn't load earlier messages.") else current
                }
            } finally {
                _uiState.update { current ->
                    if (current.roomCode == code) current.copy(loadingOlder = false) else current
                }
            }
        }
    }

    fun endRoom() {
        val snapshot = _uiState.value
        val code = snapshot.roomCode ?: return
        if (!snapshot.isHost || snapshot.endingRoom) return
        _uiState.update { it.copy(endingRoom = true, error = null) }
        viewModelScope.launch {
            try {
                if (!gameRepository.endRoom()) {
                    _uiState.update { current ->
                        if (current.roomCode == code) current.copy(error = "Couldn't end the room. Try again.") else current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { current ->
                    if (current.roomCode == code) current.copy(error = "Couldn't end the room. Try again.") else current
                }
            } finally {
                _uiState.update { current ->
                    if (current.roomCode == code) current.copy(endingRoom = false) else current
                }
            }
        }
    }

    fun send() {
        val snapshot = _uiState.value
        val code = snapshot.roomCode ?: return
        val text = snapshot.draft.trim()
        if (text.isEmpty() || snapshot.sending) return
        _uiState.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            try {
                val posted = chatRepository.send(code, text)
                _uiState.update { current ->
                    if (current.roomCode != code) current else current.copy(
                        draft = if (current.draft.trim() == text) "" else current.draft,
                        messages = (current.messages + posted).distinctBy { it.id }
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { current ->
                    if (current.roomCode == code) current.copy(error = "Couldn't send message. Try again.") else current
                }
            } finally {
                _uiState.update { current ->
                    if (current.roomCode == code) current.copy(sending = false) else current
                }
            }
        }
    }
}
