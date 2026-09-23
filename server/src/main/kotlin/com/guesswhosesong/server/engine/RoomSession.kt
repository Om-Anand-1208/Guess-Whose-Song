package com.guesswhosesong.server.engine

import com.guesswhosesong.server.music.MusicService
import com.guesswhosesong.server.redis.ChatRepository
import com.guesswhosesong.server.redis.RedisClient
import com.guesswhosesong.server.redis.RoomRepository
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import io.ktor.websocket.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

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
    redisClient: RedisClient
) {
    private val logger = LoggerFactory.getLogger(RoomSession::class.java)
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val repository = RoomRepository(redisClient)
    private val chatRepository = ChatRepository(redisClient)
    private val musicService = MusicService()

    @Volatile
    var room: Room = initialRoom
        private set

    /** Map of playerId -> WebSocketSession. Multiple tabs not supported (last wins). */
    private val connections = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    /** Pending disconnect grace-period jobs */
    private val disconnectJobs = ConcurrentHashMap<String, Job>()

    // ─── Connection management ────────────────────────────────────────────────

    suspend fun onPlayerConnect(
        playerId: String,
        displayName: String,
        socket: DefaultWebSocketServerSession
    ) {
        connections[playerId] = socket

        // Cancel any pending disconnect grace timer
        disconnectJobs.remove(playerId)?.cancel()

        mutex.withLock {
            val existingPlayer = room.players.find { it.id == playerId }
            room = if (existingPlayer != null) {
                // Reconnect: mark as connected
                room.copy(players = room.players.map { p ->
                    if (p.id == playerId) p.copy(connected = true) else p
                })
            } else {
                // New player joining
                val newPlayer = Player(
                    id = playerId,
                    displayName = displayName,
                    isHost = room.players.isEmpty(),
                    joinedAt = System.currentTimeMillis()
                )
                room.copy(players = room.players + newPlayer)
            }
            persist()
        }

        // Send full room state to the joining player
        val selfPlayer = room.players.find { it.id == playerId }!!
        sendToPlayer(
            playerId,
            RoomJoined(room = sanitizedRoom(playerId), selfPlayerId = playerId)
        )

        // Broadcast updated player list to all others
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))

        // Send recent chat history to reconnecting player
        val recentChat = chatRepository.getRecent(room.joinCode)
        recentChat.forEach { msg ->
            sendToPlayer(playerId, ChatReceived(message = msg))
        }

        logger.info("[${room.joinCode}] Player connected: $displayName ($playerId)")
    }

    suspend fun onPlayerDisconnect(playerId: String) {
        connections.remove(playerId)

        val player = room.players.find { it.id == playerId } ?: return
        logger.info("[${room.joinCode}] Player disconnected: ${player.displayName} ($playerId), starting ${DISCONNECT_GRACE_MS}ms grace period")

        // Mark as disconnected immediately so the live counter is accurate
        mutex.withLock {
            room = room.copy(players = room.players.map { p ->
                if (p.id == playerId) p.copy(connected = false) else p
            })
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))

        // Start grace timer
        val gracJob = scope.launch {
            delay(DISCONNECT_GRACE_MS)
            handleGraceExpired(playerId)
        }
        disconnectJobs[playerId] = gracJob
    }

    private suspend fun handleGraceExpired(playerId: String) {
        val player = room.players.find { it.id == playerId } ?: return
        if (player.connected) return // reconnected during grace

        logger.info("[${room.joinCode}] Grace expired for ${player.displayName} ($playerId)")

        mutex.withLock {
            var updatedPlayers = room.players.filter { it.id != playerId }

            // If this was the host, reassign
            if (player.isHost) {
                val nextHost = HostReassignment.findNextHost(updatedPlayers, playerId)
                if (nextHost != null) {
                    updatedPlayers = HostReassignment.applyHostChange(updatedPlayers, playerId, nextHost.id)
                    room = room.copy(players = updatedPlayers, hostId = nextHost.id)
                    broadcastAll(HostChanged(newHostId = nextHost.id, newHostName = nextHost.displayName))
                    logger.info("[${room.joinCode}] Host reassigned to ${nextHost.displayName}")
                } else {
                    // No players left
                    endRoom()
                    return
                }
            } else {
                room = room.copy(players = updatedPlayers)
            }
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
    }

    // ─── Message dispatch ─────────────────────────────────────────────────────

    suspend fun handleMessage(playerId: String, message: ClientMessage) {
        val player = room.players.find { it.id == playerId }
        if (player == null) {
            sendToPlayer(playerId, ErrorMessage(code = "UNKNOWN_PLAYER", message = "Player not found in room"))
            return
        }

        when (message) {
            is StartGame -> handleStartGame(playerId)
            is UpdateSettings -> handleUpdateSettings(playerId, message.settings)
            is KickPlayer -> handleKickPlayer(playerId, message.targetPlayerId)
            is UpdatePendingSong -> handleUpdatePendingSong(playerId, message.song)
            is LockSong -> handleLockSong(playerId)
            is CastVote -> handleCastVote(playerId, message.guessedPlayerId)
            is SendChat -> handleSendChat(playerId, player.displayName, message.text)
            is PlayAgain -> handlePlayAgain(playerId)
            is EndRoom -> handleEndRoom(playerId)
            is ConnectSpotify -> handleConnectSpotify(playerId)
            is SubmitSong -> { /* handled via UpdatePendingSong + LockSong */ }
            else -> logger.warn("[${room.joinCode}] Unhandled message type: ${message.type}")
        }
    }

    // ─── Host-only actions ────────────────────────────────────────────────────

    private suspend fun handleStartGame(playerId: String) {
        val player = room.players.find { it.id == playerId } ?: return
        if (!player.isHost) {
            sendToPlayer(playerId, ErrorMessage(code = "NOT_HOST", message = "Only the host can start the game"))
            return
        }
        if (room.state != RoomState.LOBBY) {
            sendToPlayer(playerId, ErrorMessage(code = "WRONG_STATE", message = "Game is not in lobby"))
            return
        }
        val connectedCount = room.players.count { it.connected }
        if (connectedCount < MIN_PLAYERS_TO_START) {
            sendToPlayer(playerId, ErrorMessage(code = "NOT_ENOUGH_PLAYERS",
                message = "Need at least $MIN_PLAYERS_TO_START players to start"))
            return
        }

        scope.launch { runSubmissionPhase() }
    }

    private suspend fun handleUpdateSettings(playerId: String, settings: RoomSettings) {
        val player = room.players.find { it.id == playerId } ?: return
        if (!player.isHost) return

        mutex.withLock {
            room = room.copy(settings = settings)
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
    }

    private suspend fun handleKickPlayer(hostId: String, targetId: String) {
        val host = room.players.find { it.id == hostId } ?: return
        if (!host.isHost) return
        if (hostId == targetId) return // can't kick yourself

        sendToPlayer(targetId, Kicked())
        connections[targetId]?.close(CloseReason(CloseReason.Codes.NORMAL, "kicked"))
        connections.remove(targetId)

        mutex.withLock {
            room = room.copy(players = room.players.filter { it.id != targetId })
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
    }

    // ─── Submission ───────────────────────────────────────────────────────────

    private suspend fun handleUpdatePendingSong(playerId: String, song: SongEntry) {
        if (room.state != RoomState.SUBMISSION) return

        mutex.withLock {
            room = room.copy(players = room.players.map { p ->
                if (p.id == playerId) p.copy(pendingSong = song.copy(submitterId = playerId))
                else p
            })
            // No persist on every reroll — only persist on lock or timer expiry
        }
        // Broadcast progress
        val lockedCount = room.players.count { it.songLocked }
        broadcastAll(SubmissionProgress(lockedCount = lockedCount, totalCount = room.players.size))
    }

    private suspend fun handleLockSong(playerId: String) {
        if (room.state != RoomState.SUBMISSION) return
        val player = room.players.find { it.id == playerId } ?: return
        if (player.songLocked) return

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

    private suspend fun handleCastVote(voterId: String, guessedPlayerId: String) {
        if (room.state != RoomState.PLAYING) return
        val currentRound = getCurrentRound() ?: return
        if (currentRound.phase != RoundPhase.VOTING) return
        if (currentVotes.containsKey(voterId)) return // already voted

        currentVotes[voterId] = guessedPlayerId
        val votedCount = currentVotes.size
        val totalCount = room.players.count { it.connected }

        broadcastAll(VoteCountUpdated(votedCount = votedCount, totalCount = totalCount))

        if (votedCount >= totalCount) {
            allVotedSignal.complete(Unit)
        }
    }

    // ─── Chat ─────────────────────────────────────────────────────────────────

    private suspend fun handleSendChat(senderId: String, senderName: String, text: String) {
        if (text.isBlank() || text.length > 300) return
        val rawMsg = ChatMessage(
            senderId = senderId,
            senderName = senderName,
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )
        val stored = chatRepository.append(room.joinCode, rawMsg)
        broadcastAll(ChatReceived(message = stored))
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
                    p.copy(score = 0, pendingSong = null, songLocked = false)
                }
            )
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
        scope.launch { runSubmissionPhase() }
    }

    private suspend fun handleEndRoom(playerId: String) {
        val player = room.players.find { it.id == playerId } ?: return
        if (!player.isHost) return
        endRoom()
    }

    private suspend fun handleConnectSpotify(playerId: String) {
        // The actual token exchange happens server-side via SpotifyRoutes.
        // This message just flags the player as Spotify-connected.
        mutex.withLock {
            room = room.copy(players = room.players.map { p ->
                if (p.id == playerId) p.copy(spotifyConnected = true) else p
            })
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
    }

    // ─── State Machine ────────────────────────────────────────────────────────

    private val submissionCompleteSignal = CompletableDeferred<Unit>()

    private suspend fun runSubmissionPhase() {
        val songsPerPlayer = room.settings.roundLengthPreset.songsPerPlayer
        val deadlineMs = System.currentTimeMillis() + SUBMISSION_TIMEOUT_MS

        mutex.withLock {
            room = room.copy(state = RoomState.SUBMISSION)
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))
        broadcastAll(SubmissionStarted(deadlineEpochMillis = deadlineMs))

        // Wait for all players to lock OR timer to expire
        withTimeoutOrNull(SUBMISSION_TIMEOUT_MS) {
            submissionCompleteSignal.await()
        }

        // Auto-lock any players who haven't locked in
        mutex.withLock {
            room = room.copy(players = room.players.map { p ->
                if (!p.songLocked && p.pendingSong != null) p.copy(songLocked = true)
                else p
            })
        }

        // Resolve Deezer preview URLs for all locked songs
        logger.info("[${room.joinCode}] Resolving Deezer preview URLs...")
        val resolvedEntries = mutableListOf<SongEntry>()
        room.players.filter { it.songLocked }.forEach { player ->
            val pending = player.pendingSong ?: return@forEach
            val resolved = musicService.resolveEntry(pending)
            if (resolved != null) {
                resolvedEntries.add(resolved.copy(submitterId = player.id))
            } else {
                logger.warn("[${room.joinCode}] No Deezer match for: ${pending.title} - ${pending.artist}")
                // Song is dropped from the pool (player chose an unresolvable song)
            }
        }

        if (resolvedEntries.isEmpty()) {
            logger.error("[${room.joinCode}] No songs resolved. Ending room.")
            endRoom()
            return
        }

        val shuffled = resolvedEntries.shuffled()
        mutex.withLock {
            room = room.copy(
                state = RoomState.PLAYING,
                songPool = shuffled,
                currentRoundIndex = 0
            )
            persist()
        }
        broadcastAll(RoomUpdated(room = sanitizedRoomForBroadcast()))

        runPlayingPhase()
    }

    private suspend fun runPlayingPhase() {
        for (index in room.songPool.indices) {
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
        currentVotes.clear()
        allVotedSignal = CompletableDeferred()

        val votingDeadline = System.currentTimeMillis() + room.settings.votingTimerSeconds * 1000L
        val players = room.players.map { it.copy(pendingSong = null) } // anonymized

        broadcastAll(
            VotingStarted(
                roundIndex = index,
                players = players,
                votingDeadlineEpochMillis = votingDeadline
            )
        )

        // Wait for all votes OR timer
        withTimeoutOrNull(room.settings.votingTimerSeconds * 1000L) {
            allVotedSignal.await()
        }

        // Record no-votes as empty string (automatic wrong guess)
        val finalVotes = room.players.associate { player ->
            player.id to (currentVotes[player.id] ?: "")
        }

        // ── Phase 3: REVEALING ────────────────────────────────────────────────
        val (voteResults, scoreDeltas) = ScoreEngine.computeRoundResults(song, finalVotes, room.players)

        mutex.withLock {
            room = room.copy(
                players = ScoreEngine.applyDeltas(room.players, scoreDeltas)
            )
            persist()
        }

        broadcastAll(
            RoundRevealed(
                roundIndex = index,
                songEntry = song, // includes submitterId — now safe to reveal
                submitterName = room.players.find { it.id == song.submitterId }?.displayName ?: "Unknown",
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
        val sortedPlayers = room.players.sortedByDescending { it.score }
        broadcastAll(GameResults(players = sortedPlayers))
    }

    private suspend fun endRoom() {
        mutex.withLock {
            room = room.copy(state = RoomState.ENDED)
        }
        broadcastAll(RoomEnded())
        // Close all connections
        connections.values.forEach { socket ->
            try { socket.close(CloseReason(CloseReason.Codes.NORMAL, "room ended")) }
            catch (e: Exception) { /* ignore */ }
        }
        connections.clear()
        // Delete from Redis immediately
        repository.delete(room.joinCode)
        chatRepository.delete(room.joinCode)
        scope.cancel()
        logger.info("[${room.joinCode}] Room ended and cleaned up.")
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun getCurrentRound(): Round? {
        val song = room.songPool.getOrNull(room.currentRoundIndex) ?: return null
        return Round(roundIndex = room.currentRoundIndex, songEntry = song)
    }

    /**
     * Returns a Room snapshot safe to send to a specific player.
     * Strips submitterIds from the song pool.
     */
    private fun sanitizedRoom(playerId: String): Room {
        return room.copy(
            songPool = room.songPool.map { it.copy(submitterId = "") },
            players = room.players.map { it.copy(pendingSong = null) }
        )
    }

    /**
     * Returns a Room snapshot safe to broadcast to all clients.
     * Strips submitterIds and pending songs.
     */
    private fun sanitizedRoomForBroadcast(): Room {
        return room.copy(
            songPool = room.songPool.map { it.copy(submitterId = "") },
            players = room.players.map { it.copy(pendingSong = null) }
        )
    }

    private fun persist() {
        repository.save(room)
    }

    suspend fun broadcastAll(message: ServerMessage) {
        val json = message.toJson()
        connections.entries.forEach { (_, socket) ->
            try {
                socket.send(json)
            } catch (e: Exception) {
                // Connection dropped silently — onPlayerDisconnect handles cleanup
            }
        }
    }

    private suspend fun sendToPlayer(playerId: String, message: ServerMessage) {
        val json = message.toJson()
        try {
            connections[playerId]?.send(json)
        } catch (e: Exception) {
            logger.warn("[${room.joinCode}] Failed to send to $playerId: ${e.message}")
        }
    }
}

