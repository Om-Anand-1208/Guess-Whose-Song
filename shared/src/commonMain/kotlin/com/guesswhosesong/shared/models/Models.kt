package com.guesswhosesong.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class RoomSettings(
    val roundLengthPreset: RoundLengthPreset = RoundLengthPreset.STANDARD,
    val playerLimit: Int = 10,
    val votingTimerSeconds: Int = 20
)

@Serializable
data class SongEntry(
    val songId: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String = "",
    val deezerPreviewUrl: String,
    val previewUrl: String,
    /** NEVER sent to clients until reveal phase */
    val submitterId: String = ""
)

@Serializable
data class Player(
    val id: String,
    val displayName: String,
    val isHost: Boolean = false,
    val connected: Boolean = true,
    val score: Int = 0,
    val spotifyConnected: Boolean = false,
    /** Continuously overwritten during submission; auto-locks on timer expiry */
    val pendingSong: SongEntry? = null,
    val songLocked: Boolean = false,
    val joinedAt: Long = 0L // epoch millis, used for host reassignment ordering
)

@Serializable
data class Round(
    val roundIndex: Int,
    val songEntry: SongEntry,
    val phase: RoundPhase = RoundPhase.PLAYING_PREVIEW,
    /** voterId -> guessedPlayerId. Withheld from clients until REVEALING */
    val votes: Map<String, String> = emptyMap(),
    val votingDeadline: Long = 0L // epoch millis
)

@Serializable
data class Room(
    val id: String,
    val joinCode: String,
    val hostId: String,
    val state: RoomState = RoomState.LOBBY,
    val settings: RoomSettings = RoomSettings(),
    val createdAt: Long = 0L,
    val players: List<Player> = emptyList(),
    val songPool: List<SongEntry> = emptyList(),
    val currentRoundIndex: Int = 0
)

@Serializable
data class ChatMessage(
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = 0L
)

/** Sent to clients during reveal — vote breakdown per player */
@Serializable
data class VoteResult(
    val voterId: String,
    val voterName: String,
    val guessedPlayerId: String,
    val guessedPlayerName: String,
    val correct: Boolean
)

/** Per-round score delta shown on reveal screen */
@Serializable
data class ScoreDelta(
    val playerId: String,
    val playerName: String,
    val delta: Int,
    val newTotal: Int
)

/** Spotify suggestion (title + artist only, no playback) */
@Serializable
data class SpotifySuggestion(
    val title: String,
    val artist: String,
    val albumArtUrl: String = "",
    val category: SpotifyCategory
)

@Serializable
enum class SpotifyCategory {
    TOP_TRACKS,
    RECENTLY_PLAYED,
    PLAYLIST
}

/** Deezer search result returned by backend proxy */
/** Search result returned by backend proxy (from iTunes or fallback) */
@Serializable
data class DeezerTrack(
    val id: Long,
data class TrackSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val previewUrl: String
)
