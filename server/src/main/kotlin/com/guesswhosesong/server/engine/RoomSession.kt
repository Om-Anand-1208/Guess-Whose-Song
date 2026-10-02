package com.guesswhosesong.server.engine

import com.guesswhosesong.server.music.MusicService
import com.guesswhosesong.server.redis.RedisClient
import com.guesswhosesong.server.redis.RoomRepository
import com.guesswhosesong.server.redis.RoomRuntime
import com.guesswhosesong.server.redis.RoomRuntimeRepository
import com.guesswhosesong.server.security.InputValidation
import com.guesswhosesong.server.spotify.SpotifyClient
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.UUID

private const val DISCONNECT_GRACE_MS = 90_000L   // 90 seconds
private const val REVEAL_HOLD_MS = 5_000L          // 5 seconds hold on reveal screen
private const val MIN_PLAYERS_TO_START = 2
private const val SUBMISSION_TIMEOUT_MS = 120_000L // 2 minutes for submission phase

/**
 * Manages all state and coroutine-based phase transitions for a single room.
 *
 * Thread-safety: all mutations go through [mutex]. Coroutines are scoped to
 * [scope] which is cancelled when the room ends.
 */
class RoomSession(
    initialRoom: Room,
    private val redisClient: RedisClient,
    private val onEnded: (String) -> Unit = {},
    private val musicService: MusicService = MusicService(),
    private val spotifyClient: SpotifyClient = SpotifyClient()
) {
    private val logger = LoggerFactory.getLogger(RoomSession::class.java)
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val repository = RoomRepository(redisClient)
    private val runtimeRepository = RoomRuntimeRepository(redisClient)
    private val ended = AtomicBoolean(false)
    private var gameJob: Job? = null
    private var runtime: RoomRuntime = runtimeRepository.load(initialRoom.joinCode) ?: RoomRuntime()
    @Volatile
    private var currentRoundPhase: RoundPhase = runCatching { RoundPhase.valueOf(runtime.phase) }
        .getOrDefault(RoundPhase.PLAYING_PREVIEW)
    private var currentVotingDeadline: Long = runtime.deadlineEpochMillis

    @Volatile
    var room: Room = initialRoom
        private set

    /** Map of playerId -> pending room events. Multiple tabs are not supported (last join wins). */
    private val eventQueues = ConcurrentHashMap<String, Channel<ServerMessage>>()
    private val lastActivity = ConcurrentHashMap<String, Long>()

    /** Pending disconnect grace-period jobs */
    private val disconnectJobs = ConcurrentHashMap<String, Job>()

    // ─── Connection management ────────────────────────────────────────────────

    suspend fun onPlayerConnect(
        playerId: String,
        displayName: String,
        avatarId: String,
        avatarCustomization: AvatarCustomization? = null
    ) {
        eventQueues.put(playerId, Channel(capacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST))?.close()
        markPlayerActive(playerId)

        val hasSpotify = redisClient.get("spotify_token:$playerId") != null

        mutex.withLock {
            val existingPlayer = room.players.find { it.id == playerId }
            val playerCustomization = AvatarCustomization.normalize(
                avatarCustomization ?: existingPlayer?.avatarCustomization,
                avatarId
            )
            room = if (existingPlayer != null) {
                // Reconnect: mark as connected
                room.copy(players = room.players.map { p ->
                    if (p.id == playerId) p.copy(
                        connected = true,
                        avatarId = playerCustomization.shapeId,
                        avatarCustomization = playerCustomization,
                        spotifyConnected = p.spotifyConnected || hasSpotify
                    ) else p
                })
            } else {
                // New player joining
                val newPlayer = Player(
                    id = playerId,
                    displayName = displayName,
                    avatarId = playerCustomization.shapeId,
                    avatarCustomization = playerCustomization,
                    isHost = room.players.isEmpty(),
                    spotifyConnected = hasSpotify,
                    joinedAt = System.currentTimeMillis()
                )
                room.copy(players = room.players + newPlayer)
            }
            persist()
        }

        // Send full room state to the joining player
        sendToPlayer(
            playerId,
            RoomJoined(room = sanitizedRoom(playerId), selfPlayerId = playerId)
        )

        // Broadcast updated player list to all others
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))

        // If game is in progress, sync current round to the player
        if (room.state == RoomState.PLAYING) {
            val song = room.songPool.getOrNull(room.currentRoundIndex)
            if (song != null) {
                if (currentRoundPhase == RoundPhase.PLAYING_PREVIEW) {
                    sendToPlayer(
                        playerId,
                        RoundPreviewStarted(
                            roundIndex = room.currentRoundIndex,
                            totalRounds = room.songPool.size,
                            title = song.title,
                            artist = song.artist,
                            albumArtUrl = song.albumArtUrl,
                            previewUrl = song.previewUrl,
                            previewDurationMs = 30_000L
                        )
                    )
                } else if (currentRoundPhase == RoundPhase.VOTING) {
                    val players = room.players.map { it.copy(pendingSong = null, pendingSongs = emptyList()) }
                    sendToPlayer(
                        playerId,
                        VotingStarted(
                            roundIndex = room.currentRoundIndex,
                            players = players,
                            votingDeadlineEpochMillis = currentVotingDeadline
                        )
                    )
                }
            }
        }
        if (room.state == RoomState.RESULTS) {
            sendToPlayer(
                playerId,
                GameResults(
                    players = room.players.sortedByDescending { it.score }
                        .map { it.copy(pendingSong = null, pendingSongs = emptyList()) }
                )
            )
        }

        logger.info("[${room.joinCode}] Player connected")
    }

    suspend fun onPlayerDisconnect(playerId: String) {
        val channel = eventQueues.remove(playerId) ?: return
        channel.close()
        lastActivity.remove(playerId)
        val currentJob = currentCoroutineContext()[Job]
        disconnectJobs.remove(playerId)?.takeIf { it !== currentJob }?.cancel()

        val player = room.players.find { it.id == playerId } ?: return
        logger.info("[${room.joinCode}] Player disconnected; starting ${DISCONNECT_GRACE_MS}ms grace period")

        // Mark as disconnected immediately so the live counter is accurate
        mutex.withLock {
            room = room.copy(players = room.players.map { p ->
                if (p.id == playerId) p.copy(connected = false) else p
            })
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))

        // Start grace timer
        scheduleGraceExpiry(playerId)
    }

    private fun markPlayerActive(playerId: String) {
        if (!eventQueues.containsKey(playerId)) return
        lastActivity[playerId] = System.currentTimeMillis()
        disconnectJobs.remove(playerId)?.cancel()
        scheduleGraceExpiry(playerId)
    }

    private fun scheduleGraceExpiry(playerId: String) {
        disconnectJobs[playerId] = scope.launch {
            delay(DISCONNECT_GRACE_MS)
            if (!eventQueues.containsKey(playerId)) {
                handleGraceExpired(playerId)
            } else {
                val elapsed = System.currentTimeMillis() - (lastActivity[playerId] ?: return@launch)
                if (elapsed >= DISCONNECT_GRACE_MS) {
                    onPlayerDisconnect(playerId)
                } else {
                    delay(DISCONNECT_GRACE_MS - elapsed)
                    if (eventQueues.containsKey(playerId) &&
                        System.currentTimeMillis() - (lastActivity[playerId] ?: return@launch) >= DISCONNECT_GRACE_MS
                    ) {
                        onPlayerDisconnect(playerId)
                    }
                }
            }
        }
    }

    suspend fun pollMessages(playerId: String, waitMillis: Long): List<ServerMessage> {
        markPlayerActive(playerId)
        val channel = eventQueues[playerId] ?: return emptyList()
        val first = withTimeoutOrNull(waitMillis) { channel.receiveCatching().getOrNull() } ?: return emptyList()
        return buildList {
            add(first)
            while (true) add(channel.tryReceive().getOrNull() ?: break)
        }
    }

    fun hasPlayerConnection(playerId: String): Boolean = eventQueues.containsKey(playerId)

    private suspend fun handleGraceExpired(playerId: String) {
        disconnectJobs.remove(playerId)
        val player = room.players.find { it.id == playerId } ?: return
        if (player.connected) return // reconnected during grace

        logger.info("[${room.joinCode}] Player grace period expired")

        var reassignedHost: Player? = null
        var shouldEnd = false
        mutex.withLock {
            var updatedPlayers = room.players.filter { it.id != playerId }

            // If this was the host, reassign
            if (player.isHost) {
                val nextHost = HostReassignment.findNextHost(updatedPlayers, playerId)
                if (nextHost != null) {
                    updatedPlayers = HostReassignment.applyHostChange(updatedPlayers, playerId, nextHost.id)
                    room = room.copy(players = updatedPlayers, hostId = nextHost.id)
                    reassignedHost = nextHost
                    logger.info("[${room.joinCode}] Host reassigned")
                } else {
                    room = room.copy(players = emptyList())
                    shouldEnd = true
                }
            } else {
                room = room.copy(players = updatedPlayers)
            }
            if (!shouldEnd) persist()
        }
        if (shouldEnd) {
            endRoom()
            return
        }
        reassignedHost?.let {
            broadcastAll(HostChanged(newHostId = it.id, newHostName = it.displayName))
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
    }

    // ─── Message dispatch ─────────────────────────────────────────────────────

    suspend fun handleMessage(playerId: String, message: ClientMessage) {
        markPlayerActive(playerId)
        val player = room.players.find { it.id == playerId }
        if (player == null) {
            sendToPlayer(playerId, ErrorMessage(code = "UNKNOWN_PLAYER", message = "Player not found in room"))
            return
        }

        when (message) {
            is StartGame -> handleStartGame(playerId)
            is AddComputerPlayer -> handleAddComputerPlayer(playerId)
            is UpdateSettings -> handleUpdateSettings(playerId, message.settings)
            is UpdatePlayerProfile -> handleUpdatePlayerProfile(playerId, message.displayName, message.avatarCustomization)
            is KickPlayer -> handleKickPlayer(playerId, message.targetPlayerId)
            is UpdatePendingSong -> handleUpdatePendingSong(playerId, message.song)
            is UpdatePendingSongs -> handleUpdatePendingSongs(playerId, message.songs)
            is LockSong -> handleLockSong(playerId)
            is CastVote -> handleCastVote(playerId, message.guessedPlayerId)
            is PlayAgain -> handlePlayAgain(playerId)
            is EndRoom -> handleEndRoom(playerId)
            is RefreshSpotify -> handleRefreshSpotify(playerId)
            else -> logger.warn("[${room.joinCode}] Unhandled message type: ${message::class.simpleName}")
        }
    }

    // ─── Host-only actions ────────────────────────────────────────────────────

    private suspend fun handleAddComputerPlayer(playerId: String) {
        var error: ErrorMessage? = null
        var added = false
        mutex.withLock {
            val host = room.players.find { it.id == playerId }
            when {
                host == null -> error = ErrorMessage("UNKNOWN_PLAYER", "Player not found in room")
                !host.isHost -> error = ErrorMessage("NOT_HOST", "Only the host can add a computer player")
                room.state != RoomState.LOBBY -> error = ErrorMessage("WRONG_STATE", "Computer players can only join from the lobby")
                room.settings.gameMode != GameMode.MANUAL -> {
                    error = ErrorMessage("COMPUTER_UNAVAILABLE", "Computer players are only available in Manual picks mode")
                }
                room.players.any { it.isComputer } -> error = ErrorMessage("COMPUTER_ALREADY_ADDED", "A computer player is already in the room")
                room.players.size >= room.settings.playerLimit -> error = ErrorMessage("ROOM_FULL", "There are no open player spots")
                else -> {
                    val computer = Player(
                        id = "computer-${UUID.randomUUID()}",
                        displayName = "Computer",
                        connected = true,
                        isComputer = true,
                        joinedAt = System.currentTimeMillis()
                    )
                    room = room.copy(players = room.players + computer)
                    persist()
                    added = true
                }
            }
        }
        error?.let { sendToPlayer(playerId, it) }
        if (added) broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
    }

    private suspend fun handleStartGame(playerId: String) {
        var error: ErrorMessage? = null
        mutex.withLock {
            val player = room.players.find { it.id == playerId }
            when {
                player == null -> error = ErrorMessage("UNKNOWN_PLAYER", "Player not found in room")
                !player.isHost -> error = ErrorMessage("NOT_HOST", "Only the host can start the game")
                room.state != RoomState.LOBBY -> error = ErrorMessage("WRONG_STATE", "Game is not in lobby")
                room.players.count { it.connected } < MIN_PLAYERS_TO_START -> {
                    error = ErrorMessage("NOT_ENOUGH_PLAYERS", "Need at least $MIN_PLAYERS_TO_START players to start")
                }
                !RoundCountRules.isValid(room.settings.roundCount, room.players.size) -> {
                    error = ErrorMessage(
                        "INVALID_SETTINGS",
                        "Choose at least ${room.players.size} rounds for the current room roster"
                    )
                }
                gameJob?.isActive == true -> Unit
                else -> {
                    room = room.copy(state = RoomState.SUBMISSION)
                    persist()
                    gameJob = scope.launch { runSubmissionPhase() }
                }
            }
        }
        error?.let { sendToPlayer(playerId, it) }
    }

    private suspend fun handleUpdateSettings(playerId: String, settings: RoomSettings) {
        val player = room.players.find { it.id == playerId } ?: return
        if (!player.isHost) return
        if (room.state != RoomState.LOBBY) {
            sendToPlayer(playerId, ErrorMessage("WRONG_STATE", "Settings can only change in the lobby"))
            return
        }
        if (!InputValidation.settings(settings)) {
            sendToPlayer(playerId, ErrorMessage("INVALID_SETTINGS", "Invalid round count, player limit, or voting timer"))
            return
        }
        if (settings.gameMode == GameMode.SPOTIFY_RECENT && room.players.any { it.isComputer }) {
            sendToPlayer(playerId, ErrorMessage(
                "COMPUTER_UNAVAILABLE",
                "Remove the computer player before switching to Recently Played"
            ))
            return
        }
        if (!RoundCountRules.isValid(settings.roundCount, room.players.size)) {
            sendToPlayer(
                playerId,
                ErrorMessage("INVALID_SETTINGS", "Choose at least ${room.players.size} rounds for the current room roster")
            )
            return
        }

        mutex.withLock {
            room = room.copy(settings = settings)
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
    }

    private suspend fun handleUpdatePlayerProfile(
        playerId: String,
        displayName: String,
        avatarCustomization: AvatarCustomization
    ) {
        if (!InputValidation.displayName(displayName) || !AvatarCustomization.isValid(avatarCustomization)) {
            sendToPlayer(playerId, ErrorMessage("INVALID_PROFILE", "Enter a valid name and avatar"))
            return
        }

        var error: ErrorMessage? = null
        mutex.withLock {
            when {
                room.state != RoomState.LOBBY -> error = ErrorMessage("PROFILE_LOCKED", "Your player profile can only change in the lobby")
                room.players.none { it.id == playerId } -> error = ErrorMessage("UNKNOWN_PLAYER", "Player not found in room")
                else -> {
                    room = room.copy(players = room.players.map { player ->
                        if (player.id == playerId) player.copy(
                            displayName = displayName.trim(),
                            avatarId = avatarCustomization.shapeId,
                            avatarCustomization = avatarCustomization
                        ) else player
                    })
                    persist()
                }
            }
        }
        error?.let { sendToPlayer(playerId, it); return }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
    }

    private suspend fun handleKickPlayer(hostId: String, targetId: String) {
        val host = room.players.find { it.id == hostId } ?: return
        if (!host.isHost) return
        if (hostId == targetId) return // can't kick yourself

        sendToPlayer(targetId, Kicked())
        eventQueues.remove(targetId)?.close()
        lastActivity.remove(targetId)
        disconnectJobs.remove(targetId)?.cancel()

        mutex.withLock {
            room = room.copy(players = room.players.filter { it.id != targetId })
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
    }

    // ─── Submission ───────────────────────────────────────────────────────────

    private suspend fun handleUpdatePendingSong(playerId: String, song: SongEntry) {
        if (room.state != RoomState.SUBMISSION) return
        val player = room.players.find { it.id == playerId } ?: return
        if (player.songLocked) {
            sendToPlayer(playerId, ErrorMessage("SONG_LOCKED", "Song is already locked"))
            return
        }
        if (!InputValidation.song(song)) {
            sendToPlayer(playerId, ErrorMessage("INVALID_SONG", "Invalid song payload"))
            return
        }
        if (player.pendingSongs.any {
                it.songId == song.songId ||
                    (it.title.equals(song.title, ignoreCase = true) && it.artist.equals(song.artist, ignoreCase = true))
            }) {
            sendToPlayer(playerId, ErrorMessage("DUPLICATE_SONG", "Song was already submitted"))
            return
        }
        val entry = song.copy(submitterId = playerId)
        val maxSongs = RoundCountRules.maxSongsPerPlayer(room.settings.roundCount, room.players.size)
        if (player.pendingSongs.size >= maxSongs) {
            sendToPlayer(playerId, ErrorMessage("TOO_MANY_SONGS", "You can submit up to $maxSongs songs"))
            return
        }

        mutex.withLock {
            room = room.copy(players = room.players.map { p ->
                if (p.id == playerId) {
                    val updatedSongs = if (p.pendingSongs.any { it.songId == entry.songId || (it.title.equals(entry.title, ignoreCase = true) && it.artist.equals(entry.artist, ignoreCase = true)) }) {
                        p.pendingSongs
                    } else if (p.pendingSongs.size < maxSongs) {
                        p.pendingSongs + entry
                    } else {
                        p.pendingSongs
                    }
                    p.copy(pendingSong = entry, pendingSongs = updatedSongs)
                }
                else p
            })
            // No persist on every reroll — only persist on lock or timer expiry
        }
        // Broadcast progress
        val lockedCount = room.players.count { it.songLocked }
        broadcastAll(SubmissionProgress(lockedCount = lockedCount, totalCount = room.players.size))
    }

    private suspend fun handleUpdatePendingSongs(playerId: String, songs: List<SongEntry>) {
        if (room.state != RoomState.SUBMISSION) return
        val player = room.players.find { it.id == playerId } ?: return
        if (player.songLocked) {
            sendToPlayer(playerId, ErrorMessage("SONG_LOCKED", "Song is already locked"))
            return
        }
        val maxSongs = RoundCountRules.maxSongsPerPlayer(room.settings.roundCount, room.players.size)
        if (!SubmissionRules.isValid(songs, room.settings.roundCount, room.players.size)) {
            sendToPlayer(playerId, ErrorMessage("INVALID_SONGS", "Submission must contain unique valid songs"))
            return
        }
        val limited = songs.take(maxSongs).map { it.copy(submitterId = playerId) }

        mutex.withLock {
            room = room.copy(players = room.players.map { p ->
                if (p.id == playerId) p.copy(
                    pendingSongs = limited,
                    pendingSong = limited.firstOrNull()
                )
                else p
            })
        }
        val lockedCount = room.players.count { it.songLocked }
        broadcastAll(SubmissionProgress(lockedCount = lockedCount, totalCount = room.players.size))
    }

    private suspend fun handleLockSong(playerId: String) {
        if (room.state != RoomState.SUBMISSION) return
        val player = room.players.find { it.id == playerId } ?: return
        if (player.songLocked) return
        if (player.pendingSongs.isEmpty() && player.pendingSong == null) {
            sendToPlayer(playerId, ErrorMessage("EMPTY_SUBMISSION", "Submit at least one song before locking"))
            return
        }

        mutex.withLock {
            room = room.copy(players = room.players.map { p ->
                if (p.id == playerId) p.copy(songLocked = true) else p
            })
            persist()
        }

        val lockedCount = room.players.count { it.songLocked }
        broadcastAll(SubmissionProgress(lockedCount = lockedCount, totalCount = room.players.size))

        // If all players have locked in, advance immediately
        if (lockedCount == room.players.size) {
            submissionCompleteSignal.complete(Unit)
        }
    }

    // ─── Voting ───────────────────────────────────────────────────────────────

    /** Accumulates votes for the current round */
    private val currentVotes = ConcurrentHashMap<String, String>() // voterId -> guessedPlayerId
    private var allVotedSignal = CompletableDeferred<Unit>()

    /** Rehydrates only the server-side state machine; public room data remains sanitized. */
    fun resumeAfterRestart() {
        if (room.state == RoomState.SUBMISSION) {
            gameJob = scope.launch { runSubmissionPhase() }
        } else if (room.state == RoomState.PLAYING && room.songPool.isNotEmpty()) {
            val startIndex = if (runtime.roundResolved) runtime.roundIndex + 1 else runtime.roundIndex
            gameJob = scope.launch {
                if (startIndex > room.songPool.lastIndex) showResults()
                else runPlayingPhase(startIndex)
            }
        }
    }

    private suspend fun handleCastVote(voterId: String, guessedPlayerId: String) {
        if (room.state != RoomState.PLAYING) {
            logger.warn("[${room.joinCode}] Vote rejected: room not in PLAYING state (${room.state})")
            return
        }
        val currentRound = getCurrentRound() ?: return
        if (currentRound.phase != RoundPhase.VOTING) {
            logger.warn("[${room.joinCode}] Vote rejected: not in VOTING phase (current: ${currentRound.phase})")
            return
        }
        if (currentVotes.containsKey(voterId)) {
            logger.warn("[${room.joinCode}] Duplicate vote ignored")
            return
        }
        val validTarget = if (room.settings.gameMode == GameMode.SPOTIFY_RECENT) {
            room.songPool.any { it.submitterId == guessedPlayerId }
        } else {
            guessedPlayerId == ScoreEngine.DECOY_ID || room.players.any { it.id == guessedPlayerId }
        }
        if (!validTarget || guessedPlayerId.length > 128) {
            sendToPlayer(voterId, ErrorMessage("INVALID_VOTE", "Unknown vote target"))
            return
        }

        currentVotes[voterId] = guessedPlayerId
        val votedCount = currentVotes.size
        val totalCount = room.players.count { it.connected }

        logger.info("[${room.joinCode}] Vote cast ($votedCount/$totalCount)")
        broadcastAll(VoteCountUpdated(votedCount = votedCount, totalCount = totalCount))

        if (votedCount >= totalCount) {
            allVotedSignal.complete(Unit)
        }
    }

    // ─── Play Again / End ─────────────────────────────────────────────────────

    private suspend fun handlePlayAgain(playerId: String) {
        val player = room.players.find { it.id == playerId } ?: return
        if (!player.isHost) return
        if (room.state != RoomState.RESULTS) return

        mutex.withLock {
            room = room.copy(
                state = RoomState.SUBMISSION,
                songPool = emptyList(),
                currentRoundIndex = 0,
                players = room.players.map { p ->
                    p.copy(score = 0, pendingSong = null, pendingSongs = emptyList(), songLocked = false)
                }
            )
            persist()
        }
        currentRoundPhase = RoundPhase.PLAYING_PREVIEW
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
        mutex.withLock {
            if (gameJob?.isActive != true) gameJob = scope.launch { runSubmissionPhase() }
        }
    }

    private suspend fun handleEndRoom(playerId: String) {
        val player = room.players.find { it.id == playerId } ?: return
        if (!player.isHost) return
        endRoom()
    }

    private suspend fun handleRefreshSpotify(playerId: String) {
        // The actual token exchange happens server-side via SpotifyRoutes.
        // This message just flags the player as Spotify-connected.
        mutex.withLock {
            val connected = redisClient.get("spotify_token:$playerId") != null
            room = room.copy(players = room.players.map { p ->
                if (p.id == playerId) p.copy(spotifyConnected = connected) else p
            })
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
    }

    // ─── State Machine ────────────────────────────────────────────────────────

    private var submissionCompleteSignal = CompletableDeferred<Unit>()

    private suspend fun markComputerReady() {
        mutex.withLock {
            room = room.copy(players = room.players.map { player ->
                if (player.isComputer) {
                    player.copy(pendingSong = null, pendingSongs = emptyList(), songLocked = true)
                } else player
            })
            persist()
        }
    }

    private suspend fun prepareComputerSongs(poolSongs: List<SongEntry>) {
        val computers = room.players.filter { it.isComputer }
        if (computers.isEmpty()) return

        val maxSongs = RoundCountRules.maxSongsPerPlayer(room.settings.roundCount, room.players.size)
        val tracks = try {
            musicService.getContextualTracks(poolSongs, maxSongs)
                .filter { it.previewUrl.isNotBlank() }
                .take(maxSongs)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            logger.warn("[${room.joinCode}] Could not load songs for the computer player: ${e.message}")
            emptyList()
        }

        mutex.withLock {
            room = room.copy(players = room.players.map { player ->
                if (!player.isComputer) player else {
                    val songs = tracks.map { track ->
                        SongEntry(
                            songId = track.id,
                            title = track.title,
                            artist = track.artist,
                            albumArtUrl = track.albumArtUrl,
                            previewUrl = track.previewUrl,
                            submitterId = player.id
                        )
                    }
                    player.copy(
                        pendingSong = songs.firstOrNull(),
                        pendingSongs = songs,
                        songLocked = true
                    )
                }
            })
            persist()
        }
    }

    private suspend fun runSubmissionPhase() {
        if (room.settings.gameMode == GameMode.SPOTIFY_RECENT) {
            runRecentlyPlayedPhase()
            return
        }

        submissionCompleteSignal = CompletableDeferred()
        mutex.withLock {
            room = room.copy(state = RoomState.SUBMISSION)
            persist()
        }
        markComputerReady()
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))

        while (true) {
            val currentSubmissionSignal = submissionCompleteSignal
            val deadlineMs = System.currentTimeMillis() + SUBMISSION_TIMEOUT_MS
            persistRuntime(RoomRuntime("SUBMISSION", 0, deadlineMs, UUID.randomUUID().toString()))
            broadcastAll(SubmissionStarted(deadlineEpochMillis = deadlineMs))

            // Wait for all players to lock OR timer to expire.
            withTimeoutOrNull(SUBMISSION_TIMEOUT_MS) {
                currentSubmissionSignal.await()
            }

            val missingPlayerIds = mutex.withLock { SubmissionRules.playersMissingSongs(room.players) }
            if (missingPlayerIds.isEmpty()) break

            // Keep the room in song selection until every player has contributed at
            // least one choice. Already locked players keep their picks while the
            // missing players receive another submission window.
            submissionCompleteSignal = CompletableDeferred()
            broadcastAll(
                ErrorMessage(
                    "SONG_REQUIRED",
                    "Every player needs at least one song. Waiting for ${missingPlayerIds.size} player(s) to choose."
                )
            )
            logger.info("[${room.joinCode}] Extending song selection for ${missingPlayerIds.size} player(s)")
        }

        // Auto-lock any players who haven't locked in
        mutex.withLock {
            room = room.copy(players = room.players.map { p ->
                val hasSongs = p.pendingSongs.isNotEmpty() || p.pendingSong != null
                if (!p.songLocked && hasSongs) p.copy(songLocked = true)
                else p
            })
            persist()
        }

        // Resolve each human pick before choosing computer songs and decoys, so
        // generated tracks can follow the language/style represented in the pool.
        logger.info("[${room.joinCode}] Resolving song preview URLs...")
        val resolvedEntries = mutableListOf<SongEntry>()
        room.players.filter { it.songLocked && !it.isComputer }.forEach { player ->
            val songsToResolve = if (player.pendingSongs.isNotEmpty()) {
                player.pendingSongs
            } else {
                listOfNotNull(player.pendingSong)
            }
            songsToResolve.forEach { pending ->
                val resolved = musicService.resolveEntry(pending)
                if (resolved != null && resolved.previewUrl.isNotBlank()) {
                    resolvedEntries.add(resolved.copy(submitterId = player.id))
                } else {
                    logger.warn("[${room.joinCode}] A submitted song could not be resolved")
                    // Song is dropped from the pool (player chose an unresolvable song)
                }
            }
        }

        prepareComputerSongs(resolvedEntries)
        room.players.filter { it.isComputer }.forEach { player ->
            player.pendingSongs.forEach { song ->
                if (song.previewUrl.isNotBlank()) resolvedEntries.add(song.copy(submitterId = player.id))
            }
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))

        // Add up to two decoy tracks; the pool builder applies the selected round cap.
        try {
            val numDecoys = if (resolvedEntries.size <= 4) 1 else 2
            val contextualTracks = musicService.getContextualTracks(resolvedEntries, numDecoys)

            for (track in contextualTracks) {
                val decoyEntry = SongEntry(
                    songId = track.id,
                    title = track.title,
                    artist = track.artist,
                    albumArtUrl = track.albumArtUrl,
                    previewUrl = track.previewUrl,
                    submitterId = ScoreEngine.DECOY_ID
                )
                if (decoyEntry.previewUrl.isNotBlank()) {
                    resolvedEntries.add(decoyEntry)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            logger.warn("[${room.joinCode}] Could not inject decoy tracks")
        }

        val pool = RoundPoolBuilder.build(
            playerSongs = resolvedEntries,
            decoySongs = resolvedEntries.filter { it.submitterId == GameConstants.DECOY_ID },
            playerIds = room.players.map { it.id },
            roundCount = room.settings.roundCount
        )
        if (pool.isEmpty()) {
            logger.error("[${room.joinCode}] No playable songs resolved. Ending room.")
            endRoom()
            return
        }
        startPlayingWithPool(pool)
    }

    private suspend fun runRecentlyPlayedPhase() {
        mutex.withLock {
            room = room.copy(state = RoomState.SUBMISSION)
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
        persistRuntime(RoomRuntime("SUBMISSION", 0, 0L, UUID.randomUUID().toString()))
        broadcastAll(SubmissionStarted(deadlineEpochMillis = 0L))

        val playedTracks = mutableListOf<PlayerRecentTrack>()
        room.players.forEach { player ->
            val token = try {
                redisClient.get("spotify_token:${player.id}")
            } catch (_: Exception) {
                null
            } ?: return@forEach

            val recentTracks = try {
                spotifyClient.getRecentlyPlayed(token, limit = 50)
            } catch (_: SpotifyClient.UnauthorizedException) {
                try { redisClient.del("spotify_token:${player.id}") } catch (_: Exception) { }
                emptyList()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
            recentTracks.forEach { track -> playedTracks += PlayerRecentTrack(player.id, track) }
        }

        val uniqueTracks = RecentTrackDeduplicator.uniqueToOnePlayer(playedTracks)
        val tracksByPlayer = uniqueTracks.groupBy { it.playerId }
        val resolvedSongs = mutableListOf<SongEntry>()
        val candidateOwners = room.players.map { it.id }.filter { !tracksByPlayer[it].isNullOrEmpty() }
        val candidateOffsets = ConcurrentHashMap<String, Int>().apply {
            candidateOwners.forEach { put(it, 0) }
        }

        suspend fun resolveNextPlayable(playerId: String): SongEntry? {
            val candidates = tracksByPlayer[playerId].orEmpty()
            while (candidateOffsets.getValue(playerId) < candidates.size) {
                val index = candidateOffsets.getValue(playerId)
                candidateOffsets[playerId] = index + 1
                val track = candidates[index].track
                val pending = SongEntry(
                    songId = "recent-${track.spotifyTrackId?.takeIf(String::isNotBlank) ?: "$playerId-$index"}",
                    title = track.title,
                    artist = track.artist,
                    albumArtUrl = track.albumArtUrl,
                    previewUrl = "",
                    submitterId = playerId
                )
                val resolved = try {
                    musicService.resolveEntry(pending)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                if (resolved != null && resolved.previewUrl.isNotBlank()) {
                    return resolved.copy(submitterId = playerId)
                }
            }
            return null
        }

        var recentPool: RecentPlayedPool? = null
        while (candidateOwners.isNotEmpty()) {
            // Resolve in fair passes so a full pool does not require catalog-searching
            // every track in every player's 50-item Spotify history.
            val resolvedThisPass = coroutineScope {
                candidateOwners.map { ownerId -> async { resolveNextPlayable(ownerId) } }.awaitAll()
            }
            resolvedSongs += resolvedThisPass.filterNotNull()
            recentPool = RecentPlayedPoolBuilder.build(resolvedSongs, room.settings.roundCount)
            if (recentPool?.songs?.size == room.settings.roundCount) break

            val hasMoreCandidates = candidateOwners.any { ownerId ->
                candidateOffsets.getValue(ownerId) < tracksByPlayer[ownerId].orEmpty().size
            }
            if (!hasMoreCandidates) break
        }

        if (recentPool == null) {
            mutex.withLock {
                room = room.copy(
                    state = RoomState.LOBBY,
                    songPool = emptyList(),
                    currentRoundIndex = 0,
                    players = room.players.map { it.copy(songLocked = false, pendingSong = null, pendingSongs = emptyList()) }
                )
                persist()
            }
            persistRuntime(RoomRuntime())
            broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
            broadcastAll(
                ErrorMessage(
                    "RECENT_TRACKS_UNAVAILABLE",
                    "Recently Played mode needs unique playable tracks from at least two players. The game is still in the lobby."
                )
            )
            return
        }

        startPlayingWithPool(recentPool.songs)
    }

    private suspend fun startPlayingWithPool(pool: List<SongEntry>) {
        mutex.withLock {
            room = room.copy(
                state = RoomState.PLAYING,
                songPool = pool,
                currentRoundIndex = 0
            )
            persist()
        }
        persistRuntime(RoomRuntime("PLAYING_PREVIEW", 0, 0L, runtime.runId))
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))

        // Allow clients time to receive RoomUpdated and navigate from submission to game screen
        delay(1_500)

        runPlayingPhase()
    }

    private suspend fun runPlayingPhase(startIndex: Int = 0) {
        for (index in startIndex until room.songPool.size) {
            mutex.withLock {
                room = room.copy(currentRoundIndex = index)
            }
            runRound(index)
            delay(1_000) // brief pause between rounds
        }
        showResults()
    }

    private suspend fun runRound(index: Int) {
        val song = room.songPool[index]

        // ── Phase 1: PLAYING_PREVIEW ──────────────────────────────────────────
        currentRoundPhase = RoundPhase.PLAYING_PREVIEW
        persistRuntime(runtime.copy(
            phase = RoundPhase.PLAYING_PREVIEW.name,
            roundIndex = index,
            deadlineEpochMillis = System.currentTimeMillis() + 31_000L,
            votes = emptyMap(),
            roundResolved = false
        ))
        broadcastAll(
            RoundPreviewStarted(
                roundIndex = index,
                totalRounds = room.songPool.size,
                title = song.title,
                artist = song.artist,
                albumArtUrl = song.albumArtUrl,
                previewUrl = song.previewUrl,
                previewDurationMs = 30_000L
            )
        )
        // Wait for the clip to finish (~30s + small buffer)
        delay(31_000L)

        // ── Phase 2: VOTING ───────────────────────────────────────────────────
        currentRoundPhase = RoundPhase.VOTING
        currentVotes.clear()
        allVotedSignal = CompletableDeferred()

        val votingDeadline = System.currentTimeMillis() + room.settings.votingTimerSeconds * 1000L
        currentVotingDeadline = votingDeadline
        persistRuntime(runtime.copy(
            phase = RoundPhase.VOTING.name,
            roundIndex = index,
            deadlineEpochMillis = votingDeadline,
            votes = emptyMap(),
            roundResolved = false
        ))
        val players = room.players.map { it.copy(pendingSong = null, pendingSongs = emptyList()) } // anonymized

        broadcastAll(
            VotingStarted(
                roundIndex = index,
                players = players,
                votingDeadlineEpochMillis = votingDeadline,
                eligibleOwnerIds = if (room.settings.gameMode == GameMode.SPOTIFY_RECENT) {
                    room.songPool.map { it.submitterId }.distinct()
                } else {
                    emptyList()
                }
            )
        )

        val computerPlayers = room.players.filter { it.isComputer }
        if (computerPlayers.isNotEmpty()) {
            val choices = if (room.settings.gameMode == GameMode.SPOTIFY_RECENT) {
                room.songPool.map { it.submitterId }.distinct()
            } else {
                room.players.map { it.id } + GameConstants.DECOY_ID
            }
            computerPlayers.forEach { computer ->
                currentVotes[computer.id] = choices.randomOrNull() ?: ""
            }
            persistRuntime(runtime.copy(votes = currentVotes.toMap()))
            val totalVotes = room.players.count { it.connected }
            broadcastAll(VoteCountUpdated(votedCount = currentVotes.size, totalCount = totalVotes))
            if (currentVotes.size >= totalVotes) allVotedSignal.complete(Unit)
        }

        // Wait for all votes OR timer
        withTimeoutOrNull(room.settings.votingTimerSeconds * 1000L) {
            allVotedSignal.await()
        }

        // Record no-votes as empty string (automatic wrong guess)
        val finalVotes = room.players.associate { player ->
            player.id to (currentVotes[player.id] ?: "")
        }

        // ── Phase 3: REVEALING ────────────────────────────────────────────────
        currentRoundPhase = RoundPhase.REVEALING
        persistRuntime(runtime.copy(
            phase = RoundPhase.REVEALING.name,
            roundIndex = index,
            deadlineEpochMillis = 0L,
            votes = finalVotes,
            roundResolved = false
        ))
        val (voteResults, scoreDeltas) = ScoreEngine.computeRoundResults(
            song,
            finalVotes,
            room.players,
            allowSubmitterSelfGuess = room.settings.gameMode == GameMode.SPOTIFY_RECENT
        )

        mutex.withLock {
            room = room.copy(
                players = ScoreEngine.applyDeltas(room.players, scoreDeltas)
            )
            persist()
        }
        // Mark the round resolved only after the score write. Recovery skips only this exact round.
        persistRuntime(runtime.copy(roundResolved = true))

        val submitterName = if (song.submitterId == ScoreEngine.DECOY_ID) {
            ScoreEngine.DECOY_NAME
        } else {
            room.players.find { it.id == song.submitterId }?.displayName ?: "Unknown"
        }

        broadcastAll(
            RoundRevealed(
                roundIndex = index,
                songEntry = song, // includes submitterId — now safe to reveal
                submitterName = submitterName,
                voteResults = voteResults,
                scoreDeltas = scoreDeltas
            )
        )

        // Hold reveal screen for 5 seconds
        delay(REVEAL_HOLD_MS)
    }

    private suspend fun showResults() {
        mutex.withLock {
            room = room.copy(state = RoomState.RESULTS)
            persist()
        }
        persistRuntime(runtime.copy(phase = "RESULTS", deadlineEpochMillis = 0L))
        val sortedPlayers = room.players.sortedByDescending { it.score }
            .map { it.copy(pendingSong = null, pendingSongs = emptyList()) }
        broadcastAll(GameResults(players = sortedPlayers))
    }

    private suspend fun endRoom() {
        if (!ended.compareAndSet(false, true)) return
        mutex.withLock {
            room = room.copy(state = RoomState.ENDED)
        }
        broadcastAll(RoomEnded())
        // Close all pending event queues after delivering the final room event.
        val queues = eventQueues.values.toList()
        eventQueues.clear()
        queues.forEach { it.close() }
        // Delete from Redis immediately
        repository.delete(room.joinCode)
        runtimeRepository.delete(room.joinCode)
        disconnectJobs.values.forEach { it.cancel() }
        disconnectJobs.clear()
        onEnded(room.joinCode)
        scope.cancel()
        logger.info("[${room.joinCode}] Room ended and cleaned up.")
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun getCurrentRound(): Round? {
        val song = room.songPool.getOrNull(room.currentRoundIndex) ?: return null
        return Round(roundIndex = room.currentRoundIndex, songEntry = song, phase = currentRoundPhase)
    }

    /**
     * Returns a Room snapshot safe to send to a specific player.
     * Strips submitterIds from the song pool.
     */
    private fun sanitizedRoom(playerId: String): Room {
        return room.copy(
            songPool = emptyList(),
            players = room.players.map { it.copy(pendingSong = null, pendingSongs = emptyList()) }
        )
    }

    /**
     * Returns a Room snapshot safe to broadcast to all clients.
     * Strips submitterIds and pending songs.
     */
    private fun sanitizedRoomForBroadcast(): Room {
        return room.copy(
            songPool = emptyList(),
            players = room.players.map { it.copy(pendingSong = null, pendingSongs = emptyList()) }
        )
    }

    private fun persist() {
        repository.save(room)
    }

    private fun persistRuntime(value: RoomRuntime) {
        runtime = value
        runtimeRepository.save(room.joinCode, value)
    }

    suspend fun broadcastAll(message: ServerMessage) {
        eventQueues.values.forEach { it.trySend(message) }
    }

    private suspend fun sendToPlayer(playerId: String, message: ServerMessage) {
        eventQueues[playerId]?.trySend(message)
    }
}
