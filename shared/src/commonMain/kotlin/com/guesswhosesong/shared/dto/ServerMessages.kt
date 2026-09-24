package com.guesswhosesong.shared.dto

import com.guesswhosesong.shared.models.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * All messages sent FROM the Ktor server TO Android clients over WebSocket.
 * Discriminated automatically by the [type] field using @SerialName.
 */
@Serializable
sealed class ServerMessage

// ─── Connection / Room ──────────────────────────────────────────────────────

/** Sent immediately on WebSocket connect; carries full room state */
@Serializable
@SerialName("ROOM_JOINED")
data class RoomJoined(
    val room: Room,
    val selfPlayerId: String
) : ServerMessage()

/** Full room snapshot broadcast whenever lobby state changes */
@Serializable
@SerialName("ROOM_UPDATED")
data class RoomUpdated(
    val room: Room
) : ServerMessage()

/** Sent to a kicked player before closing their connection */
@Serializable
@SerialName("KICKED")
class Kicked : ServerMessage() {
    override fun equals(other: Any?): Boolean = other is Kicked
    override fun hashCode(): Int = javaClass.hashCode()
}

/** Notifies clients that a new host has been assigned */
@Serializable
@SerialName("HOST_CHANGED")
data class HostChanged(
    val newHostId: String,
    val newHostName: String
) : ServerMessage()

// ─── Submission Phase ───────────────────────────────────────────────────────

/** Broadcast when submission phase starts; carries deadline epoch millis */
@Serializable
@SerialName("SUBMISSION_STARTED")
data class SubmissionStarted(
    val deadlineEpochMillis: Long
) : ServerMessage()

/** Progress: how many players have locked in their song */
@Serializable
@SerialName("SUBMISSION_PROGRESS")
data class SubmissionProgress(
    val lockedCount: Int,
    val totalCount: Int
) : ServerMessage()

// ─── Round Flow ─────────────────────────────────────────────────────────────

/** Broadcast when preview starts; clients begin playback */
@Serializable
@SerialName("ROUND_PREVIEW_STARTED")
data class RoundPreviewStarted(
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
    val roundIndex: Int,
    val players: List<Player>, // full list, anonymized (no pendingSong)
    val votingDeadlineEpochMillis: Long
) : ServerMessage()

/** Live counter: how many have voted (no actual vote data leaked) */
@Serializable
@SerialName("VOTE_COUNT_UPDATED")
data class VoteCountUpdated(
    val votedCount: Int,
    val totalCount: Int
) : ServerMessage()

/** Reveal: full breakdown + scores */
@Serializable
@SerialName("ROUND_REVEALED")
data class RoundRevealed(
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
    val players: List<Player> // sorted by score desc
) : ServerMessage()

@Serializable
@SerialName("ROOM_ENDED")
class RoomEnded : ServerMessage() {
    override fun equals(other: Any?): Boolean = other is RoomEnded
    override fun hashCode(): Int = javaClass.hashCode()
}

// ─── Chat ────────────────────────────────────────────────────────────────────

@Serializable
@SerialName("CHAT_RECEIVED")
data class ChatReceived(
    val message: ChatMessage
) : ServerMessage()

// ─── Errors ──────────────────────────────────────────────────────────────────

@Serializable
@SerialName("ERROR")
data class ErrorMessage(
    val code: String,
    val message: String
) : ServerMessage()

// ─── Ping/Pong ───────────────────────────────────────────────────────────────

@Serializable
@SerialName("PONG")
class Pong : ServerMessage() {
    override fun equals(other: Any?): Boolean = other is Pong
    override fun hashCode(): Int = javaClass.hashCode()
}
