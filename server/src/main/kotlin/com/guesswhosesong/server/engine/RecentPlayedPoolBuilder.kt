package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.GameConstants
import com.guesswhosesong.shared.models.SongEntry

internal data class RecentPlayedPool(
    val songs: List<SongEntry>,
    val eligibleOwnerIds: List<String>
)

/** Filters resolved Spotify history into a playable, uniquely attributed game pool. */
internal object RecentPlayedPoolBuilder {
    fun build(resolvedSongs: List<SongEntry>, roundCount: Int): RecentPlayedPool? {
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

        val pool = RoundPoolBuilder.build(
            playerSongs = safeSongs,
            decoySongs = emptyList(),
            playerIds = ownerIds,
            roundCount = roundCount
        )
        val selectedOwnerIds = pool.map { it.submitterId }.distinct()
        if (selectedOwnerIds.size < 2) return null
        return RecentPlayedPool(pool, selectedOwnerIds)
    }
}
