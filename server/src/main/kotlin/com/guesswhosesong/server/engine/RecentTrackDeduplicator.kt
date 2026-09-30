package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.SpotifySuggestion
import java.util.Locale

internal data class PlayerRecentTrack(
    val playerId: String,
    val track: SpotifySuggestion
)

/**
 * Keeps one recent-played entry for tracks heard by exactly one player.
 *
 * Track IDs are authoritative when both entries have one. If either ID is
 * unavailable, normalized title and primary artist are used as a fallback.
 */
internal object RecentTrackDeduplicator {
    fun uniqueToOnePlayer(playedTracks: List<PlayerRecentTrack>): List<PlayerRecentTrack> {
        val entries = playedTracks.filter {
            it.playerId.isNotBlank() && it.track.title.isNotBlank() && it.track.artist.isNotBlank()
        }
        if (entries.isEmpty()) return emptyList()

        val parents = IntArray(entries.size) { it }

        fun find(index: Int): Int {
            if (parents[index] != index) parents[index] = find(parents[index])
            return parents[index]
        }

        fun union(first: Int, second: Int) {
            val firstRoot = find(first)
            val secondRoot = find(second)
            if (firstRoot != secondRoot) parents[secondRoot] = firstRoot
        }

        for (first in entries.indices) {
            for (second in first + 1 until entries.size) {
                if (sameTrack(entries[first].track, entries[second].track)) {
                    union(first, second)
                }
            }
        }

        return entries.indices
            .groupBy(::find)
            .values
            .filter { group -> group.map { entries[it].playerId }.distinct().size == 1 }
            .map { group -> entries[group.first()] }
    }

    private fun sameTrack(first: SpotifySuggestion, second: SpotifySuggestion): Boolean {
        val firstId = first.spotifyTrackId?.trim()?.takeIf(String::isNotEmpty)
        val secondId = second.spotifyTrackId?.trim()?.takeIf(String::isNotEmpty)
        if (firstId != null && secondId != null) return firstId == secondId

        return normalize(first.title) == normalize(second.title) &&
            normalize(first.artist) == normalize(second.artist)
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .filter(Char::isLetterOrDigit)
}
