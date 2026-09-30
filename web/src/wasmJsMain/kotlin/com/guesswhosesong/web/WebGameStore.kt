package com.guesswhosesong.web

import com.guesswhosesong.shared.dto.CastVote
import com.guesswhosesong.shared.dto.AddComputerPlayer
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
import com.guesswhosesong.shared.dto.UpdatePlayerProfile
import com.guesswhosesong.shared.dto.UpdateSettings
import com.guesswhosesong.shared.dto.VotingStarted
import com.guesswhosesong.shared.dto.VoteCountUpdated
import com.guesswhosesong.shared.dto.RoomEnded
import com.guesswhosesong.shared.models.ChatMessage
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.Room
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.SpotifySuggestion
import com.guesswhosesong.shared.models.TrackSearchResult
import com.guesswhosesong.shared.models.AvatarCatalog
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.AvatarCustomizationCatalog
import com.guesswhosesong.shared.models.PlayerIdentityDefaults
import com.guesswhosesong.shared.models.RoundCountRules
import com.guesswhosesong.shared.dto.GWSJson
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

enum class WebPage { JOIN, LOBBY, SUBMISSION, GAME, RESULTS }

data class WebUiState(
    val page: WebPage = WebPage.JOIN,
    val displayName: String = loadPlayerDisplayName(),
    val avatarId: String = loadPlayerAvatarId(),
    val avatarCustomization: AvatarCustomization = loadAvatarCustomization(avatarId),
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
    val votesCast: Int = 0,
    val totalVotes: Int = 0,
    val voteErrorSequence: Int = 0,
    val voteErrorRoundIndex: Int? = null,
    val pendingVoteRoundIndex: Int? = null,
    val reveal: RoundRevealed? = null,
    val revealStartedAtEpochMillis: Long = 0L,
    val results: GameResults? = null,
    val chat: List<ChatMessage> = emptyList(),
    val chatDraft: String = "",
    val isBusy: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

class WebGameStore internal constructor(
    initialState: WebUiState = WebUiState(),
    private val testMessageSink: ((ClientMessage) -> Unit)? = null,
    startServices: Boolean = true
) {
    val auth = WebAuthManager()
    private val api = WebApiClient(auth)
    private val socket = WebSocketGameClient(api)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<WebUiState> = _state.asStateFlow()
    val connectionState: StateFlow<ConnectionState> = socket.state

    init {
        persistPlayerProfile(state.value.displayName, state.value.avatarCustomization)
        if (startServices) {
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
    }

    fun setDisplayName(value: String) {
        _state.update { it.copy(displayName = value.take(24), error = null) }
    }

    fun savePlayerProfile(displayName: String, value: AvatarCustomization) {
        val name = displayName.trim()
        if (name.isBlank() || name.length > 24 || name.any(Char::isISOControl)) {
            fail("Enter a name with 1 to 24 characters")
            return
        }
        val customization = normalizeAvatarCustomization(value)
        _state.update { current ->
            val room = current.room?.let { existing ->
                existing.copy(players = existing.players.map { player ->
                    if (player.id == current.selfPlayerId) player.copy(
                        displayName = name,
                        avatarId = customization.shapeId,
                        avatarCustomization = customization
                    ) else player
                })
            }
            current.copy(
                displayName = name,
                avatarId = customization.shapeId,
                avatarCustomization = customization,
                room = room,
                error = null,
                notice = null
            )
        }
        persistPlayerProfile(name, customization)
        if (state.value.page == WebPage.LOBBY) {
            send(UpdatePlayerProfile(name, customization))
        }
    }

    fun setAvatarId(value: String) {
        val shapeId = AvatarCatalog.normalize(value)
        val customization = AvatarCustomization.defaultsFor(shapeId)
        _state.update { it.copy(avatarId = shapeId, avatarCustomization = customization, error = null) }
        persistAvatarCustomization(customization)
    }

    fun setAvatarCustomization(value: AvatarCustomization) {
        val customization = normalizeAvatarCustomization(value)
        _state.update { it.copy(avatarId = customization.shapeId, avatarCustomization = customization, error = null) }
        persistAvatarCustomization(customization)
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
                val avatarId = state.value.avatarId
                val avatarCustomization = state.value.avatarCustomization
                val created = api.createRoom(name, avatarId, avatarCustomization)
                connect(created.joinCode, name, avatarId, avatarCustomization)
            }
        }
    }

    fun joinRoom() {
        scope.launch {
            val name = validatedName() ?: return@launch
            val code = state.value.joinCode.trim()
            if (code.length != 6) return@launch fail("Enter a valid six-character room code")
            busy { connect(code, name, state.value.avatarId, state.value.avatarCustomization) }
        }
    }

    private suspend fun connect(code: String, name: String, avatarId: String, avatarCustomization: AvatarCustomization) {
        _state.update {
            it.copy(
                page = WebPage.LOBBY,
                room = null,
                selfPlayerId = "",
                joinCode = code,
                displayName = name,
                avatarId = avatarId,
                avatarCustomization = avatarCustomization,
                error = null,
                notice = "Connecting to room…"
            )
        }
        sessionSet("gws.joinCode", code)
        sessionSet("gws.displayName", name)
        sessionSet("gws.avatarId", avatarId)
        persistAvatarCustomization(avatarCustomization)
        socket.connect(code, name, avatarId, avatarCustomization)
    }

    fun startGame() = send(StartGame())

    fun addComputerPlayer() = send(AddComputerPlayer())

    fun updateSettings(settings: RoomSettings) = send(UpdateSettings(settings))

    fun kick(playerId: String) = send(com.guesswhosesong.shared.dto.KickPlayer(playerId))

    fun playAgain() {
        _state.update { it.copy(pendingSongs = emptyList(), voting = null, votesCast = 0, totalVotes = 0, pendingVoteRoundIndex = null, reveal = null, revealStartedAtEpochMillis = 0L, results = null) }
        send(PlayAgain())
    }

    fun endRoom() = send(EndRoom())

    fun selectTrack(track: TrackSearchResult) {
        val current = state.value.pendingSongs
        if (current.any { it.songId == track.id || (it.title.equals(track.title, true) && it.artist.equals(track.artist, true)) }) {
            fail("That song is already selected")
            return
        }
        val maxSongs = state.value.room?.let {
            RoundCountRules.maxSongsPerPlayer(it.settings.roundCount, it.players.size)
        } ?: 1
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
        val maxSongs = state.value.room?.let {
            RoundCountRules.maxSongsPerPlayer(it.settings.roundCount, it.players.size)
        } ?: 1
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

    fun castVote(playerId: String) {
        _state.update { current ->
            val roundIndex = current.voting?.roundIndex
            val retryingFailedVote = current.voteErrorRoundIndex != null && current.voteErrorRoundIndex == roundIndex
            current.copy(
                pendingVoteRoundIndex = roundIndex,
                error = if (retryingFailedVote) null else current.error,
                voteErrorRoundIndex = if (retryingFailedVote) null else current.voteErrorRoundIndex
            )
        }
        send(CastVote(playerId))
    }

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
        auth.linkGoogle { result ->
            result.onSuccess { _state.update { it.copy(notice = "Google account linked for recovery", error = null) } }
                .onFailure { fail(it.message ?: "Google linking failed") }
        }
    }

    fun recoverGoogle() {
        auth.recoverGoogle { result ->
            result.onSuccess {
                _state.update { state -> state.copy(notice = "Recovered your linked Google identity", error = null) }
            }.onFailure { fail(it.message ?: "Google recovery failed") }
        }
    }

    fun leaveRoom() {
        socket.disconnect()
        _state.update { current ->
            WebUiState(
                displayName = current.displayName,
                avatarId = current.avatarId,
                avatarCustomization = current.avatarCustomization
            )
        }
    }

    private fun syncPendingSongs() {
        send(UpdatePendingSongs(state.value.pendingSongs))
    }

    private fun send(message: ClientMessage) {
        testMessageSink?.let { sink ->
            sink(message)
            return
        }
        scope.launch {
            try {
                socket.send(message)
            } catch (error: Throwable) {
                val description = error.message ?: "Connection unavailable"
                if (message is CastVote) failVote(description) else fail(description)
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

    private fun failVote(message: String) = _state.update {
        it.copy(
            error = message,
            notice = null,
            isBusy = false,
            voteErrorSequence = it.voteErrorSequence + 1,
            voteErrorRoundIndex = it.pendingVoteRoundIndex ?: it.voting?.roundIndex,
            pendingVoteRoundIndex = null
        )
    }

    private fun connectionError(message: String): String = when {
        message == "AUTH_REQUIRED" -> "Could not authenticate this room connection. Refresh and try again."
        message == "JOIN_REQUIRED" -> "The room connection did not complete. Please try again."
        message.contains("before joining", ignoreCase = true) -> "Could not join the room. Check the server connection and try again."
        else -> "Room connection failed: $message"
    }

    private fun applyMessage(message: ServerMessage) {
        when (message) {
            is RoomJoined -> _state.update {
                val self = message.room.players.find { player -> player.id == message.selfPlayerId }
                it.copy(
                    room = message.room,
                    selfPlayerId = message.selfPlayerId,
                    displayName = self?.displayName ?: it.displayName,
                    avatarId = self?.avatarId ?: it.avatarId,
                    avatarCustomization = self?.avatarCustomization?.let { avatar -> normalizeAvatarCustomization(avatar) } ?: it.avatarCustomization,
                    page = pageFor(message.room.state),
                    error = null,
                    notice = null
                )
            }
            is RoomUpdated -> _state.update { current ->
                val self = message.room.players.find { player -> player.id == current.selfPlayerId }
                if (self != null) persistPlayerProfile(self.displayName, self.avatarCustomization?.let(::normalizeAvatarCustomization) ?: AvatarCustomization.defaultsFor(self.avatarId))
                current.copy(
                    room = message.room,
                    displayName = self?.displayName ?: current.displayName,
                    avatarId = self?.avatarId ?: current.avatarId,
                    avatarCustomization = self?.avatarCustomization?.let(::normalizeAvatarCustomization) ?: current.avatarCustomization,
                    page = pageFor(message.room.state)
                )
            }
            is SubmissionStarted -> _state.update { it.copy(page = WebPage.SUBMISSION, deadlineEpochMillis = message.deadlineEpochMillis, reveal = null, revealStartedAtEpochMillis = 0L) }
            is RoundPreviewStarted -> {
                _state.update { it.copy(page = WebPage.GAME, preview = message, voting = null, votesCast = 0, totalVotes = 0, reveal = null, revealStartedAtEpochMillis = 0L) }
                message.previewUrl.takeIf { it.isNotBlank() }?.let(::playPreview)
            }
            is VotingStarted -> {
                stopPreview()
                _state.update {
                it.copy(
                    page = WebPage.GAME,
                    voting = message,
                    votesCast = 0,
                    totalVotes = message.players.count { player -> player.connected },
                    pendingVoteRoundIndex = null,
                    reveal = null,
                    revealStartedAtEpochMillis = 0L
                )
                }
            }
            is VoteCountUpdated -> _state.update { it.copy(votesCast = message.votedCount, totalVotes = message.totalCount) }
            is RoundRevealed -> _state.update { it.copy(page = WebPage.GAME, reveal = message, revealStartedAtEpochMillis = currentEpochMillis()) }
            is GameResults -> _state.update { it.copy(page = WebPage.RESULTS, results = message, reveal = null, revealStartedAtEpochMillis = 0L) }
            is ChatReceived -> _state.update { it.copy(chat = (it.chat + message.message).takeLast(100)) }
            is HostChanged -> _state.update { current -> current.copy(notice = "${message.newHostName} is now the host") }
            is ErrorMessage -> if (message.code == "INVALID_VOTE") failVote(message.message) else fail(message.message)
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

private fun loadAvatarCustomization(avatarId: String): AvatarCustomization {
    val defaults = AvatarCustomization.defaultsFor(avatarId)
    val saved = playerPreferenceGet("gws.avatarCustomization")
        ?.let { encoded -> runCatching { GWSJson.decodeFromString<AvatarCustomization>(encoded) }.getOrNull() }
        ?: return defaults
    return normalizeAvatarCustomization(saved)
}

private fun loadPlayerDisplayName(): String =
    playerPreferenceGet("gws.displayName")?.trim()?.takeIf { it.isNotBlank() && it.length <= 24 }
        ?: PlayerIdentityDefaults.randomDisplayName()

private fun loadPlayerAvatarId(): String {
    playerPreferenceGet("gws.avatarId")?.let { saved ->
        if (saved in AvatarCustomizationCatalog.shapeIds) return saved
    }
    playerPreferenceGet("gws.avatarCustomization")
        ?.let { encoded -> runCatching { GWSJson.decodeFromString<AvatarCustomization>(encoded) }.getOrNull() }
        ?.shapeId
        ?.takeIf { it in AvatarCustomizationCatalog.shapeIds }
        ?.let { return it }
    return PlayerIdentityDefaults.randomAvatarCustomization().shapeId
}

private fun playerPreferenceGet(key: String): String? = localGet(key) ?: sessionGet(key)

private fun playerPreferenceSet(key: String, value: String) {
    localSet(key, value)
    sessionSet(key, value)
}

private fun persistPlayerProfile(displayName: String, customization: AvatarCustomization) {
    playerPreferenceSet("gws.displayName", displayName)
    playerPreferenceSet("gws.avatarId", customization.shapeId)
    playerPreferenceSet("gws.avatarCustomization", GWSJson.encodeToString(customization))
}

private fun normalizeAvatarCustomization(value: AvatarCustomization): AvatarCustomization {
    val shapeId = value.shapeId.takeIf { it in AvatarCustomizationCatalog.shapeIds }
        ?: AvatarCatalog.normalize(value.shapeId)
    val defaults = AvatarCustomization.defaultsFor(shapeId)
    return value.copy(
        shapeId = shapeId,
        colorId = value.colorId.takeIf { it in AvatarCustomizationCatalog.colorIds } ?: defaults.colorId,
        eyesId = value.eyesId.takeIf { it in AvatarCustomizationCatalog.eyesIds } ?: defaults.eyesId,
        mouthId = value.mouthId.takeIf { it in AvatarCustomizationCatalog.mouthIds } ?: defaults.mouthId,
        accessoryId = value.accessoryId.takeIf { it in AvatarCustomizationCatalog.accessoryIds } ?: defaults.accessoryId
    )
}

private fun persistAvatarCustomization(value: AvatarCustomization) {
    playerPreferenceSet("gws.avatarId", value.shapeId)
    playerPreferenceSet("gws.avatarCustomization", GWSJson.encodeToString(value))
}
