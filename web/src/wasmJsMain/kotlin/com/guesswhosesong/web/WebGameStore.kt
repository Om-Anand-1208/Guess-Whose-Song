package com.guesswhosesong.web

import com.guesswhosesong.shared.dto.CastVote
import com.guesswhosesong.shared.dto.ChatReceived
import com.guesswhosesong.shared.dto.ClientMessage
import com.guesswhosesong.shared.dto.EndRoom
import com.guesswhosesong.shared.dto.ErrorMessage
import com.guesswhosesong.shared.dto.GameResults
import com.guesswhosesong.shared.dto.HostChanged
import com.guesswhosesong.shared.dto.Kicked
import com.guesswhosesong.shared.dto.LockSong
import com.guesswhosesong.shared.dto.PlayAgain
import com.guesswhosesong.shared.dto.RoomJoined
import com.guesswhosesong.shared.dto.RoomUpdated
import com.guesswhosesong.shared.dto.RoundPreviewStarted
import com.guesswhosesong.shared.dto.RoundRevealed
import com.guesswhosesong.shared.dto.SendChat
import com.guesswhosesong.shared.dto.ServerMessage
import com.guesswhosesong.shared.dto.StartGame
import com.guesswhosesong.shared.dto.SubmissionStarted
import com.guesswhosesong.shared.dto.UpdatePendingSongs
import com.guesswhosesong.shared.dto.UpdateSettings
import com.guesswhosesong.shared.dto.VotingStarted
import com.guesswhosesong.shared.dto.RoomEnded
import com.guesswhosesong.shared.models.ChatMessage
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.Room
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.SpotifySuggestion
import com.guesswhosesong.shared.models.TrackSearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WebPage { JOIN, LOBBY, SUBMISSION, GAME, RESULTS }

data class WebUiState(
    val page: WebPage = WebPage.JOIN,
    val displayName: String = sessionGet("gws.displayName").orEmpty(),
    val joinCode: String = sessionGet("gws.joinCode").orEmpty(),
    val room: Room? = null,
    val selfPlayerId: String = "",
    val pendingSongs: List<SongEntry> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<TrackSearchResult> = emptyList(),
    val spotifySuggestions: List<SpotifySuggestion> = emptyList(),
    val spotifyConnected: Boolean = false,
    val deadlineEpochMillis: Long = 0L,
    val preview: RoundPreviewStarted? = null,
    val voting: VotingStarted? = null,
    val reveal: RoundRevealed? = null,
    val results: GameResults? = null,
    val chat: List<ChatMessage> = emptyList(),
    val chatDraft: String = "",
    val isBusy: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

class WebGameStore {
    val auth = WebAuthManager()
    private val api = WebApiClient(auth)
    private val socket = WebSocketGameClient(api)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(WebUiState())
    val state: StateFlow<WebUiState> = _state.asStateFlow()
    val connectionState: StateFlow<ConnectionState> = socket.state

    init {
        scope.launch { socket.messages.collect(::applyMessage) }
        scope.launch {
            socket.lastError.collect { message ->
                if (!message.isNullOrBlank()) fail(connectionError(message))
            }
        }
        scope.launch {
            auth.start()
            if (auth.status.value == AuthStatus.READY) refreshSpotify()
        }
    }

    fun setDisplayName(value: String) {
        _state.update { it.copy(displayName = value.take(24), error = null) }
    }

    fun setJoinCode(value: String) {
        _state.update { it.copy(joinCode = value.uppercase().filter { c -> c in "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" }.take(6), error = null) }
    }

    fun setSearchQuery(value: String) = _state.update { it.copy(searchQuery = value.take(200)) }

    fun setChatDraft(value: String) = _state.update { it.copy(chatDraft = value.take(500)) }

    fun createRoom() {
        scope.launch {
            val name = validatedName() ?: return@launch
            busy {
                val created = api.createRoom(name)
                connect(created.joinCode, name)
            }
        }
    }

    fun joinRoom() {
        scope.launch {
            val name = validatedName() ?: return@launch
            val code = state.value.joinCode.trim()
            if (code.length != 6) return@launch fail("Enter a valid six-character room code")
            busy { connect(code, name) }
        }
    }

    private suspend fun connect(code: String, name: String) {
        _state.update {
            it.copy(
                page = WebPage.LOBBY,
                room = null,
                selfPlayerId = "",
                joinCode = code,
                displayName = name,
                error = null,
                notice = "Connecting to room…"
            )
        }
        sessionSet("gws.joinCode", code)
        sessionSet("gws.displayName", name)
        socket.connect(code, name)
    }

    fun startGame() = send(StartGame())

    fun updateSettings(settings: RoomSettings) = send(UpdateSettings(settings))

    fun kick(playerId: String) = send(com.guesswhosesong.shared.dto.KickPlayer(playerId))

    fun playAgain() {
        _state.update { it.copy(pendingSongs = emptyList(), reveal = null, results = null) }
        send(PlayAgain())
    }

    fun endRoom() = send(EndRoom())

    fun selectTrack(track: TrackSearchResult) {
        val current = state.value.pendingSongs
        if (current.any { it.songId == track.id || (it.title.equals(track.title, true) && it.artist.equals(track.artist, true)) }) {
            fail("That song is already selected")
            return
        }
        val maxSongs = state.value.room?.settings?.roundLengthPreset?.songsPerPlayer ?: 1
        if (current.size >= maxSongs) return fail("You can select at most $maxSongs songs")
        _state.update {
            it.copy(
                pendingSongs = current + SongEntry(
                    songId = track.id,
                    title = track.title,
                    artist = track.artist,
                    albumArtUrl = track.albumArtUrl,
                    previewUrl = track.previewUrl,
                    submitterId = it.selfPlayerId
                ),
                searchResults = emptyList(),
                searchQuery = ""
            )
        }
        syncPendingSongs()
    }

    fun selectSpotifySuggestion(suggestion: SpotifySuggestion) {
        val current = state.value.pendingSongs
        val maxSongs = state.value.room?.settings?.roundLengthPreset?.songsPerPlayer ?: 1
        if (current.size >= maxSongs) return fail("You can select at most $maxSongs songs")
        if (current.any { it.title.equals(suggestion.title, true) && it.artist.equals(suggestion.artist, true) }) {
            return fail("That song is already selected")
        }
        val safeId = "spotify-${suggestion.title}-${suggestion.artist}"
            .lowercase()
            .replace(Regex("[^a-z0-9:_-]"), "-")
            .take(200)
        _state.update {
            it.copy(pendingSongs = current + SongEntry(
                songId = safeId,
                title = suggestion.title,
                artist = suggestion.artist,
                albumArtUrl = suggestion.albumArtUrl,
                previewUrl = "",
                submitterId = it.selfPlayerId
            ))
        }
        syncPendingSongs()
    }

    fun removeSong(songId: String) {
        _state.update { it.copy(pendingSongs = it.pendingSongs.filterNot { song -> song.songId == songId }) }
        syncPendingSongs()
    }

    fun lockSongs() = send(LockSong())

    fun castVote(playerId: String) = send(CastVote(playerId))

    fun sendChat() {
        val text = state.value.chatDraft.trim()
        if (text.isBlank()) return
        _state.update { it.copy(chatDraft = "") }
        send(SendChat(text))
    }

    fun search() {
        val query = state.value.searchQuery.trim()
        if (query.isBlank()) return
        scope.launch {
            busy {
                _state.update { it.copy(searchResults = api.search(query)) }
            }
        }
    }

    fun loadSpotifySuggestions() {
        scope.launch {
            runCatching { api.spotifySuggestions() }
                .onSuccess { _state.update { state -> state.copy(spotifySuggestions = it) } }
                .onFailure { fail("Spotify suggestions are unavailable") }
        }
    }

    fun connectSpotify() {
        scope.launch {
            runCatching { openExternal(api.spotifyAuthUrl()) }
                .onFailure { fail(it.message ?: "Could not start Spotify authorization") }
        }
    }

    fun disconnectSpotify() {
        scope.launch {
            runCatching { api.disconnectSpotify(); _state.update { it.copy(spotifyConnected = false) } }
                .onFailure { fail(it.message ?: "Could not disconnect Spotify") }
        }
    }

    fun refreshSpotify() {
        scope.launch {
            runCatching { api.spotifyStatus() }
                .onSuccess { connected -> _state.update { it.copy(spotifyConnected = connected) } }
        }
    }

    fun linkGoogle() {
        scope.launch {
            auth.linkGoogle().onSuccess { _state.update { it.copy(notice = "Google account linked for recovery", error = null) } }
                .onFailure { fail(it.message ?: "Google linking failed") }
        }
    }

    fun recoverGoogle() {
        scope.launch {
            auth.recoverGoogle().onSuccess {
                _state.update { state -> state.copy(notice = "Recovered your linked Google identity", error = null) }
            }.onFailure { fail(it.message ?: "Google recovery failed") }
        }
    }

    fun playCurrentPreview() {
        state.value.preview?.previewUrl?.takeIf { it.isNotBlank() }?.let(::playPreview)
    }

    fun stopCurrentPreview() = stopPreview()

    fun leaveRoom() {
        socket.disconnect()
        _state.value = WebUiState()
    }

    private fun syncPendingSongs() {
        send(UpdatePendingSongs(state.value.pendingSongs))
    }

    private fun send(message: ClientMessage) {
        scope.launch {
            try {
                socket.send(message)
            } catch (error: Throwable) {
                fail(error.message ?: "Connection unavailable")
            }
        }
    }

    private suspend fun busy(block: suspend () -> Unit) {
        _state.update { it.copy(isBusy = true, error = null) }
        runCatching { block() }.onFailure { fail(it.message ?: "Request failed") }
        _state.update { it.copy(isBusy = false) }
    }

    private fun validatedName(): String? {
        val name = state.value.displayName.trim()
        if (name.isBlank() || name.any(Char::isISOControl)) {
            fail("Enter a display name without control characters")
            return null
        }
        if (name.length > 24) {
            fail("Display names must be 24 characters or fewer")
            return null
        }
        return name
    }

    private fun fail(message: String) = _state.update { it.copy(error = message, notice = null, isBusy = false) }

    private fun connectionError(message: String): String = when {
        message == "AUTH_REQUIRED" -> "Could not authenticate this room connection. Refresh and try again."
        message == "JOIN_REQUIRED" -> "The room connection did not complete. Please try again."
        message.contains("before joining", ignoreCase = true) -> "Could not join the room. Check the server connection and try again."
        else -> "Room connection failed: $message"
    }

    private fun applyMessage(message: ServerMessage) {
        when (message) {
            is RoomJoined -> _state.update {
                it.copy(
                    room = message.room,
                    selfPlayerId = message.selfPlayerId,
                    page = pageFor(message.room.state),
                    error = null,
                    notice = null
                )
            }
            is RoomUpdated -> _state.update { current -> current.copy(room = message.room, page = pageFor(message.room.state)) }
            is SubmissionStarted -> _state.update { it.copy(page = WebPage.SUBMISSION, deadlineEpochMillis = message.deadlineEpochMillis, reveal = null) }
            is RoundPreviewStarted -> {
                _state.update { it.copy(page = WebPage.GAME, preview = message, voting = null, reveal = null) }
                message.previewUrl.takeIf { it.isNotBlank() }?.let(::playPreview)
            }
            is VotingStarted -> _state.update { it.copy(page = WebPage.GAME, voting = message, preview = null, reveal = null) }
            is RoundRevealed -> _state.update { it.copy(page = WebPage.GAME, reveal = message) }
            is GameResults -> _state.update { it.copy(page = WebPage.RESULTS, results = message, reveal = null) }
            is ChatReceived -> _state.update { it.copy(chat = (it.chat + message.message).takeLast(100)) }
            is HostChanged -> _state.update { current -> current.copy(notice = "${message.newHostName} is now the host") }
            is ErrorMessage -> fail(message.message)
            is Kicked -> {
                socket.disconnect()
                _state.update { it.copy(page = WebPage.JOIN, room = null, error = "You were removed from the room") }
            }
            is RoomEnded -> {
                socket.disconnect()
                _state.update { it.copy(page = WebPage.JOIN, room = null, error = "This room has ended") }
            }
            else -> Unit
        }
    }

    private fun pageFor(state: com.guesswhosesong.shared.models.RoomState): WebPage = when (state) {
        com.guesswhosesong.shared.models.RoomState.LOBBY -> WebPage.LOBBY
        com.guesswhosesong.shared.models.RoomState.SUBMISSION -> WebPage.SUBMISSION
        com.guesswhosesong.shared.models.RoomState.PLAYING -> WebPage.GAME
        com.guesswhosesong.shared.models.RoomState.RESULTS -> WebPage.RESULTS
        com.guesswhosesong.shared.models.RoomState.ENDED -> WebPage.JOIN
    }

    fun close() {
        socket.close()
        scope.coroutineContext[Job]?.cancel()
    }
}
