package com.guesswhosesong.shared.dto

import com.guesswhosesong.shared.models.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * All messages sent FROM the Ktor server TO Android clients over WebSocket.
 * Discriminated by the [type] field.
 */
@Serializable
sealed class ServerMessage {
    abstract val type: String
}

// ─── Connection / Room ──────────────────────────────────────────────────────

/** Sent immediately on WebSocket connect; carries full room state */
@Serializable
@SerialName("ROOM_JOINED")
data class RoomJoined(
    override val type: String = "ROOM_JOINED",
    val room: Room,
    val selfPlayerId: String
) : ServerMessage()

/** Full room snapshot broadcast whenever lobby state changes */
@Serializable
@SerialName("ROOM_UPDATED")
data class RoomUpdated(
    override val type: String = "ROOM_UPDATED",
    val room: Room
) : ServerMessage()

/** Sent to a kicked player before closing their connection */
@Serializable
@SerialName("KICKED")
data class Kicked(
    override val type: String = "KICKED"
) : ServerMessage()

/** Notifies clients that a new host has been assigned */
@Serializable
@SerialName("HOST_CHANGED")
data class HostChanged(
    override val type: String = "HOST_CHANGED",
    val newHostId: String,
    val newHostName: String
) : ServerMessage()

// ─── Submission Phase ───────────────────────────────────────────────────────

/** Broadcast when submission phase starts; carries deadline epoch millis */
@Serializable
@SerialName("SUBMISSION_STARTED")
data class SubmissionStarted(
    override val type: String = "SUBMISSION_STARTED",
    val deadlineEpochMillis: Long
) : ServerMessage()

/** Progress: how many players have locked in their song */
@Serializable
@SerialName("SUBMISSION_PROGRESS")
data class SubmissionProgress(
    override val type: String = "SUBMISSION_PROGRESS",
    val lockedCount: Int,
    val totalCount: Int
) : ServerMessage()

// ─── Round Flow ─────────────────────────────────────────────────────────────

/** Broadcast when preview starts; clients begin playback */
@Serializable
@SerialName("ROUND_PREVIEW_STARTED")
data class RoundPreviewStarted(
    override val type: String = "ROUND_PREVIEW_STARTED",
    val roundIndex: Int,
    val totalRounds: Int,
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val previewUrl: String,
    val previewDurationMs: Long = 30_000L
) : ServerMessage()

/** Broadcast when voting phase opens */
@Serializable
@SerialName("VOTING_STARTED")
data class VotingStarted(
    override val type: String = "VOTING_STARTED",
    val roundIndex: Int,
    val players: List<Player>, // full list, anonymized (no pendingSong)
    val votingDeadlineEpochMillis: Long
) : ServerMessage()

/** Live counter: how many have voted (no actual vote data leaked) */
@Serializable
@SerialName("VOTE_COUNT_UPDATED")
data class VoteCountUpdated(
    override val type: String = "VOTE_COUNT_UPDATED",
    val votedCount: Int,
    val totalCount: Int
) : ServerMessage()

/** Reveal: full breakdown + scores */
@Serializable
@SerialName("ROUND_REVEALED")
data class RoundRevealed(
    override val type: String = "ROUND_REVEALED",
    val roundIndex: Int,
    val songEntry: SongEntry, // now includes submitterId
    val submitterName: String,
    val voteResults: List<VoteResult>,
    val scoreDeltas: List<ScoreDelta>
) : ServerMessage()

// ─── Game End ────────────────────────────────────────────────────────────────

@Serializable
@SerialName("GAME_RESULTS")
data class GameResults(
    override val type: String = "GAME_RESULTS",
    val players: List<Player> // sorted by score desc
) : ServerMessage()

@Serializable
@SerialName("ROOM_ENDED")
data class RoomEnded(
    override val type: String = "ROOM_ENDED"
) : ServerMessage()

// ─── Chat ────────────────────────────────────────────────────────────────────

@Serializable
@SerialName("CHAT_RECEIVED")
data class ChatReceived(
    override val type: String = "CHAT_RECEIVED",
    val message: ChatMessage
) : ServerMessage()

// ─── Errors ──────────────────────────────────────────────────────────────────

@Serializable
@SerialName("ERROR")
data class ErrorMessage(
    override val type: String = "ERROR",
    val code: String,
    val message: String
) : ServerMessage()

// ─── Ping/Pong ───────────────────────────────────────────────────────────────

@Serializable
@SerialName("PONG")
data class Pong(
    override val type: String = "PONG"
) : ServerMessage()
