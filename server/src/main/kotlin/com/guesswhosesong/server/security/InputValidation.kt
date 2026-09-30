package com.guesswhosesong.server.security

import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.RoundCountRules
import com.guesswhosesong.shared.models.SongEntry

object InputValidation {
    fun displayName(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isNotEmpty() && trimmed.length <= 24 && trimmed.none { it.isISOControl() }
    }

    fun joinCode(value: String): Boolean =
        value.length == 6 && value.all { it in "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" }

    fun searchTerm(value: String): Boolean =
        value.trim().isNotEmpty() && value.length <= 200 && value.none { it.isISOControl() }

    fun settings(value: RoomSettings): Boolean =
        value.roundCount in RoundCountRules.MIN_ROUNDS..RoundCountRules.MAX_ROUNDS &&
            value.playerLimit in 2..20 && value.votingTimerSeconds in setOf(10, 15, 20, 30)

    fun playlistId(value: String): Boolean = Regex("[A-Za-z0-9]{22}").matches(value)

    fun song(value: SongEntry): Boolean {
        val bounded = { text: String, max: Int ->
            text.length <= max && text.none { it.isISOControl() }
        }
        val validWebUrl = { text: String ->
            text.isBlank() || runCatching {
                val uri = java.net.URI(text)
                uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
            }.getOrDefault(false)
        }
        return value.songId.matches(Regex("[A-Za-z0-9:_-]{1,200}")) &&
            value.title.trim().length in 1..200 && bounded(value.title, 200) &&
            value.artist.trim().length in 1..120 && bounded(value.artist, 120) &&
            bounded(value.albumArtUrl, 2_000) && validWebUrl(value.albumArtUrl) &&
            bounded(value.previewUrl, 2_000) && validWebUrl(value.previewUrl) &&
            bounded(value.deezerPreviewUrl, 2_000) && validWebUrl(value.deezerPreviewUrl)
    }
}
