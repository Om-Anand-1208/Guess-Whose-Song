package com.guesswhosesong.shared.dto

import com.guesswhosesong.shared.models.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * All messages sent FROM the Android client TO the Ktor server over WebSocket.
 * Discriminated automatically by the [type] field using @SerialName.
 */
@Serializable
sealed class ClientMessage

@Serializable
@SerialName("START_GAME")
class StartGame : ClientMessage() {
    override fun equals(other: Any?): Boolean = other is StartGame
    override fun hashCode(): Int = javaClass.hashCode()
}

@Serializable
@SerialName("SUBMIT_SONG")
data class SubmitSong(
    val song: SongEntry
) : ClientMessage()

/**
 * Sent whenever the user rerolls in Surprise Me mode.
 * The pendingSong field in Player is updated server-side.
 */
@Serializable
@SerialName("UPDATE_PENDING_SONG")
data class UpdatePendingSong(
    val song: SongEntry
) : ClientMessage()

@Serializable
@SerialName("LOCK_SONG")
class LockSong : ClientMessage() {
    override fun equals(other: Any?): Boolean = other is LockSong
    override fun hashCode(): Int = javaClass.hashCode()
}

@Serializable
@SerialName("CAST_VOTE")
data class CastVote(
    val guessedPlayerId: String
) : ClientMessage()

@Serializable
@SerialName("SEND_CHAT")
data class SendChat(
    val text: String
) : ClientMessage()

@Serializable
@SerialName("UPDATE_SETTINGS")
data class UpdateSettings(
    val settings: RoomSettings
) : ClientMessage()

@Serializable
@SerialName("KICK_PLAYER")
data class KickPlayer(
    val targetPlayerId: String
) : ClientMessage()

@Serializable
@SerialName("PLAY_AGAIN")
class PlayAgain : ClientMessage() {
    override fun equals(other: Any?): Boolean = other is PlayAgain
    override fun hashCode(): Int = javaClass.hashCode()
}

@Serializable
@SerialName("END_ROOM")
class EndRoom : ClientMessage() {
    override fun equals(other: Any?): Boolean = other is EndRoom
    override fun hashCode(): Int = javaClass.hashCode()
}

@Serializable
@SerialName("CONNECT_SPOTIFY")
data class ConnectSpotify(
    /** The Spotify access token obtained client-side via PKCE OAuth */
    val accessToken: String
) : ClientMessage()
