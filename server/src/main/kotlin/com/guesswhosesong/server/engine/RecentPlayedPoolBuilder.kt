package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.GameConstants
import com.guesswhosesong.shared.models.RoundCountRules
import com.guesswhosesong.shared.models.SongEntry
import java.util.Locale

internal data class RecentPlayedPool(
    val songs: List<SongEntry>,
    val eligibleOwnerIds: List<String>
)

/** Filters either Spotify mode into a playable pool with unique titles and artists. */
internal object RecentPlayedPoolBuilder {
    fun build(
        resolvedSongs: List<SongEntry>,
        roundCount: Int,
        decoySongs: List<SongEntry> = emptyList()
    ): RecentPlayedPool? {
        val candidates = resolvedSongs.filter {
            it.submitterId.isNotBlank() &&
                it.submitterId != GameConstants.DECOY_ID &&
                it.songId.isNotBlank() &&
                it.previewUrl.isNotBlank()
        }

        // Catalog resolution can map distinct Spotify records to the same preview.
        // Drop that preview if it now points at more than one player as well.
        val safeSongs = candidates
            .groupBy { it.songId }
            .values
            .filter { songs -> songs.map { it.submitterId }.distinct().size == 1 }
            .map { songs -> songs.first() }
        val ownerIds = safeSongs.map { it.submitterId }.distinct()
        if (ownerIds.size < 2) return null

        // The same recording can have different catalog IDs and release titles.
        // Take songs in fair passes, checking both title and artist separately.
        val byOwner = ownerIds.associateWith { ownerId ->
            safeSongs.filter { it.submitterId == ownerId }
        }
        val offsets = ownerIds.associateWith { 0 }.toMutableMap()
        val usedIds = mutableSetOf<String>()
        val usedTitles = mutableSetOf<String>()
        val usedArtists = mutableSetOf<String>()
        val distinctSongs = mutableListOf<SongEntry>()
        val limit = roundCount.coerceIn(RoundCountRules.MIN_ROUNDS, RoundCountRules.MAX_ROUNDS)
        while (distinctSongs.size < limit) {
            var addedThisPass = false
            for (ownerId in ownerIds) {
                val songs = byOwner.getValue(ownerId)
                while (offsets.getValue(ownerId) < songs.size) {
                    val song = songs[offsets.getValue(ownerId)]
                    offsets[ownerId] = offsets.getValue(ownerId) + 1
                    val title = normalize(song.title)
                    val artist = normalize(song.artist)
                    if (title.isEmpty() || artist.isEmpty() || song.songId in usedIds ||
                        title in usedTitles || artist in usedArtists) continue
                    distinctSongs += song
                    usedIds += song.songId
                    usedTitles += title
                    usedArtists += artist
                    addedThisPass = true
                    break
                }
                if (distinctSongs.size == limit) break
            }
            if (!addedThisPass) break
        }

        val decoyIds = usedIds.toMutableSet()
        val decoyTitles = usedTitles.toMutableSet()
        val decoyArtists = usedArtists.toMutableSet()
        val uniqueDecoys = decoySongs.filter { decoy ->
            val title = normalize(decoy.title)
            val artist = normalize(decoy.artist)
            if (decoy.submitterId != GameConstants.DECOY_ID || decoy.songId.isBlank() ||
                decoy.previewUrl.isBlank() || title.isBlank() || artist.isBlank() ||
                decoy.songId in decoyIds || title in decoyTitles || artist in decoyArtists
            ) return@filter false
            decoyIds += decoy.songId
            decoyTitles += title
            decoyArtists += artist
            true
        }

        val pool = RoundPoolBuilder.buildGenreMatched(
            playerSongs = distinctSongs,
            decoySongs = uniqueDecoys,
            playerIds = ownerIds,
            roundCount = roundCount
        )
        val selectedOwnerIds = pool.map { it.submitterId }
            .filter { it != GameConstants.DECOY_ID }.distinct()
        if (selectedOwnerIds.size < 2) return null
        return RecentPlayedPool(pool, selectedOwnerIds)
    }

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)
}
