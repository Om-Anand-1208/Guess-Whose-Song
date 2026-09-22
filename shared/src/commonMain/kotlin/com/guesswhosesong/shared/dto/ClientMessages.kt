package com.guesswhosesong.shared.dto

import com.guesswhosesong.shared.models.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * All messages sent FROM the Android client TO the Ktor server over WebSocket.
 * Discriminated by the [type] field.
 */
@Serializable
sealed class ClientMessage {
    abstract val type: String
}

@Serializable
@SerialName("START_GAME")
data class StartGame(
    override val type: String = "START_GAME"
) : ClientMessage()

@Serializable
@SerialName("SUBMIT_SONG")
data class SubmitSong(
    override val type: String = "SUBMIT_SONG",
    val song: SongEntry
) : ClientMessage()

/**
 * Sent whenever the user rerolls in Surprise Me mode.
 * The pendingSong field in Player is updated server-side.
 */
@Serializable
@SerialName("UPDATE_PENDING_SONG")
data class UpdatePendingSong(
    override val type: String = "UPDATE_PENDING_SONG",
    val song: SongEntry
) : ClientMessage()

@Serializable
@SerialName("LOCK_SONG")
data class LockSong(
    override val type: String = "LOCK_SONG"
) : ClientMessage()

@Serializable
@SerialName("CAST_VOTE")
data class CastVote(
    override val type: String = "CAST_VOTE",
    val guessedPlayerId: String
) : ClientMessage()

@Serializable
@SerialName("SEND_CHAT")
data class SendChat(
    override val type: String = "SEND_CHAT",
    val text: String
) : ClientMessage()

@Serializable
@SerialName("UPDATE_SETTINGS")
data class UpdateSettings(
    override val type: String = "UPDATE_SETTINGS",
    val settings: RoomSettings
) : ClientMessage()

@Serializable
@SerialName("KICK_PLAYER")
data class KickPlayer(
    override val type: String = "KICK_PLAYER",
    val targetPlayerId: String
) : ClientMessage()

@Serializable
@SerialName("PLAY_AGAIN")
data class PlayAgain(
    override val type: String = "PLAY_AGAIN"
) : ClientMessage()

@Serializable
@SerialName("END_ROOM")
data class EndRoom(
    override val type: String = "END_ROOM"
) : ClientMessage()

@Serializable
@SerialName("CONNECT_SPOTIFY")
data class ConnectSpotify(
    override val type: String = "CONNECT_SPOTIFY",
    /** The Spotify access token obtained client-side via PKCE OAuth */
    val accessToken: String
) : ClientMessage()
