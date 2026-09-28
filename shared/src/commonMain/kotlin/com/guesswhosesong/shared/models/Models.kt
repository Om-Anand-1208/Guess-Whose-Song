package com.guesswhosesong.shared.models

import kotlinx.serialization.Serializable

object GameConstants {
    const val DECOY_ID = "decoy"
    const val DECOY_NAME = "Nobody / Decoy ❓"
}

/** Stable IDs for the little illustrated player characters shown across clients. */
object AvatarCatalog {
    const val DEFAULT_ID = "sunny"
    val ids = listOf("sunny", "lime", "violet", "tangerine", "cloud", "star", "berry", "mint")

    fun isValid(id: String): Boolean = id in ids
    fun normalize(id: String): String = id.takeIf(::isValid) ?: DEFAULT_ID
}

/** The independently selected parts of a player's illustrated avatar. */
@Serializable
data class AvatarCustomization(
    val shapeId: String,
    val colorId: String,
    val eyesId: String,
    val mouthId: String,
    val accessoryId: String
) {
    companion object {
        /** Build the legacy-compatible appearance for a shape, falling back safely. */
        fun defaultsFor(shapeId: String): AvatarCustomization {
            val safeShapeId = shapeId.takeIf { it in AvatarCustomizationCatalog.shapeIds }
                ?: AvatarCatalog.DEFAULT_ID
            return AvatarCustomization(
                shapeId = safeShapeId,
                colorId = safeShapeId,
                eyesId = "dots",
                mouthId = "smile",
                accessoryId = "none"
            )
        }

        /** Replace missing or unknown parts with safe defaults for the requested shape. */
        fun normalize(config: AvatarCustomization?, fallbackShapeId: String): AvatarCustomization {
            val fallback = defaultsFor(fallbackShapeId)
            val safeShapeId = config?.shapeId?.takeIf { it in AvatarCustomizationCatalog.shapeIds }
                ?: fallback.shapeId
            val shapeDefaults = defaultsFor(safeShapeId)
            return AvatarCustomization(
                shapeId = safeShapeId,
                colorId = config?.colorId?.takeIf { it in AvatarCustomizationCatalog.colorIds }
                    ?: shapeDefaults.colorId,
                eyesId = config?.eyesId?.takeIf { it in AvatarCustomizationCatalog.eyesIds }
                    ?: shapeDefaults.eyesId,
                mouthId = config?.mouthId?.takeIf { it in AvatarCustomizationCatalog.mouthIds }
                    ?: shapeDefaults.mouthId,
                accessoryId = config?.accessoryId?.takeIf { it in AvatarCustomizationCatalog.accessoryIds }
                    ?: shapeDefaults.accessoryId
            )
        }

        /** True only when every selected part belongs to its fixed catalog. */
        fun isValid(config: AvatarCustomization): Boolean =
            config.shapeId in AvatarCustomizationCatalog.shapeIds &&
                config.colorId in AvatarCustomizationCatalog.colorIds &&
                config.eyesId in AvatarCustomizationCatalog.eyesIds &&
                config.mouthId in AvatarCustomizationCatalog.mouthIds &&
                config.accessoryId in AvatarCustomizationCatalog.accessoryIds
    }
}

/** Fixed, cross-client identifiers accepted for avatar customization. */
object AvatarCustomizationCatalog {
    val shapeIds = AvatarCatalog.ids
    val colorIds = listOf("sunny", "lime", "violet", "tangerine", "cloud", "star", "berry", "mint")
    val eyesIds = listOf("dots", "happy", "sleepy", "wink", "sunglasses")
    val mouthIds = listOf("smile", "grin", "open", "tongue")
    val accessoryIds = listOf("none", "headphones", "glasses", "cap", "bow", "flower")
}

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
    val deezerPreviewUrl: String = "",
    val previewUrl: String,
    /** NEVER sent to clients until reveal phase */
    val submitterId: String = ""
)

@Serializable
data class Player(
    val id: String,
    val displayName: String,
    val avatarId: String = AvatarCatalog.DEFAULT_ID,
    val isHost: Boolean = false,
    val connected: Boolean = true,
    val score: Int = 0,
    val spotifyConnected: Boolean = false,
    /** Continuously overwritten during submission; auto-locks on timer expiry */
    val pendingSong: SongEntry? = null,
    val pendingSongs: List<SongEntry> = emptyList(),
    val songLocked: Boolean = false,
    val joinedAt: Long = 0L, // epoch millis, used for host reassignment ordering
    val avatarCustomization: AvatarCustomization? = null
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

/** Search result returned by backend proxy (from iTunes or fallback) */
@Serializable
data class TrackSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val previewUrl: String
)
