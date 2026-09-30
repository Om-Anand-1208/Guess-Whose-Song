package com.guesswhosesong.app.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesswhosesong.app.data.repository.GameRepository
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameUiState(
    val roundPhase: RoundPhase = RoundPhase.PLAYING_PREVIEW,
    val roundIndex: Int = 0,
    val totalRounds: Int = 0,
    val title: String = "",
    val artist: String = "",
    val albumArtUrl: String = "",
    val previewUrl: String = "",
    val players: List<Player> = emptyList(),
    val eligibleOwnerIds: List<String> = emptyList(),
    val selfPlayerId: String = "",
    val votedPlayerId: String? = null,
    val votedCount: Int = 0,
    val totalVoters: Int = 0,
    val votingDeadlineEpochMs: Long = 0L,
    val revealData: RoundRevealed? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val room: Room? = null,
    val isSelfSong: Boolean = false
)

sealed class GameEvent {
    data object NavigateToResults : GameEvent()
    data object NavigateToSubmission : GameEvent() // Play Again
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    init {
        val cachedRoom = gameRepository.currentRoom.value
        val selfId = gameRepository.selfPlayerId.value
        val cachedPreview = gameRepository.currentRoundPreview.value
        val cachedVoting = gameRepository.currentVotingStarted.value
        val cachedReveal = gameRepository.currentRoundRevealed.value

        val previewTitle = cachedPreview?.title ?: ""
        val previewArtist = cachedPreview?.artist ?: ""
        val isSelfSong = gameRepository.isMySong(previewTitle, previewArtist)

        _uiState.update {
            it.copy(
                selfPlayerId = selfId,
                room = cachedRoom,
                roundPhase = when {
                    cachedReveal != null -> RoundPhase.REVEALING
                    cachedVoting != null -> RoundPhase.VOTING
                    else -> RoundPhase.PLAYING_PREVIEW
                },
                roundIndex = cachedPreview?.roundIndex ?: cachedRoom?.currentRoundIndex ?: 0,
                totalRounds = cachedPreview?.totalRounds ?: cachedRoom?.songPool?.size ?: 0,
                title = previewTitle,
                artist = previewArtist,
                albumArtUrl = cachedPreview?.albumArtUrl ?: "",
                previewUrl = cachedPreview?.previewUrl ?: "",
                players = cachedVoting?.players ?: cachedRoom?.players ?: emptyList(),
                eligibleOwnerIds = cachedVoting?.eligibleOwnerIds ?: emptyList(),
                votingDeadlineEpochMs = cachedVoting?.votingDeadlineEpochMillis ?: 0L,
                totalVoters = cachedVoting?.players?.count { it.connected } ?: 0,
                revealData = cachedReveal,
                isSelfSong = isSelfSong
            )
        }

        observeMessages()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            gameRepository.messages.collect { message ->
                when (message) {
                    is RoomJoined -> _uiState.update {
                        it.copy(
                            selfPlayerId = message.selfPlayerId,
                            room = message.room,
                            totalRounds = if (it.totalRounds > 0) it.totalRounds else message.room.songPool.size
                        )
                    }

                    is RoundPreviewStarted -> _uiState.update {
                        val isSelfSong = gameRepository.isMySong(message.title, message.artist)
                        it.copy(
                            roundPhase = RoundPhase.PLAYING_PREVIEW,
                            roundIndex = message.roundIndex,
                            totalRounds = message.totalRounds,
                            title = message.title,
                            artist = message.artist,
                            albumArtUrl = message.albumArtUrl,
                            previewUrl = message.previewUrl,
                            votedPlayerId = null,
                            votedCount = 0,
                            revealData = null,
                            isSelfSong = isSelfSong
                        )
                    }

                    is VotingStarted -> _uiState.update {
                        it.copy(
                            roundPhase = RoundPhase.VOTING,
                            players = message.players,
                            eligibleOwnerIds = message.eligibleOwnerIds,
                            votingDeadlineEpochMs = message.votingDeadlineEpochMillis,
                            totalVoters = message.players.count { it.connected }
                        )
                    }

                    is VoteCountUpdated -> _uiState.update {
                        it.copy(votedCount = message.votedCount, totalVoters = message.totalCount)
                    }

                    is RoundRevealed -> _uiState.update {
                        it.copy(roundPhase = RoundPhase.REVEALING, revealData = message)
                    }

                    is GameResults -> _events.emit(GameEvent.NavigateToResults)

                    is ChatReceived -> _uiState.update {
                        it.copy(chatMessages = it.chatMessages + message.message)
                    }

                    is RoomUpdated -> {
                        val room = message.room
                        _uiState.update {
                            it.copy(
                                room = room,
                                totalRounds = if (it.totalRounds > 0) it.totalRounds else room.songPool.size
                            )
                        }
                        if (room.state == RoomState.SUBMISSION) {
                            _events.emit(GameEvent.NavigateToSubmission)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun castVote(playerId: String) {
        if (_uiState.value.roundPhase != RoundPhase.VOTING) return
        if (_uiState.value.votedPlayerId != null) return // already voted
        _uiState.update { it.copy(votedPlayerId = playerId) }
        viewModelScope.launch { gameRepository.castVote(playerId) }
    }

    fun sendChat(text: String) {
        viewModelScope.launch { gameRepository.sendChat(text) }
    }

    fun kickPlayer(targetId: String) {
        viewModelScope.launch { gameRepository.kickPlayer(targetId) }
    }

    fun updateSettings(settings: RoomSettings) {
        viewModelScope.launch { gameRepository.updateSettings(settings) }
    }
}
