package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.GameConstants
import com.guesswhosesong.shared.models.SongEntry

/** Builds a bounded game pool while representing each player before adding their extra picks. */
object RoundPoolBuilder {
    fun build(
        playerSongs: List<SongEntry>,
        decoySongs: List<SongEntry>,
        playerIds: List<String>,
        roundCount: Int
    ): List<SongEntry> {
        val limit = roundCount.coerceIn(2, 20)
        val ids = playerIds.distinct()
        val byPlayer = playerSongs
            .filter { it.submitterId != GameConstants.DECOY_ID && it.previewUrl.isNotBlank() }
            .groupBy { it.submitterId }

        val firstPicks = ids.mapNotNull { byPlayer[it]?.firstOrNull() }.take(limit)
        if (firstPicks.size >= limit) return firstPicks.shuffled()

        val slots = limit - firstPicks.size
        val decoys = decoySongs
            .filter { it.submitterId == GameConstants.DECOY_ID && it.previewUrl.isNotBlank() }
            .distinctBy { it.songId }
            .take(2)
        val remainingByPlayer = ids.associateWith { id ->
            byPlayer[id].orEmpty().drop(1)
        }
        val offsets = ids.associateWith { 0 }.toMutableMap()
        val extras = mutableListOf<SongEntry>()
        val fairOrder = ids.shuffled()

        while (true) {
            var addedThisPass = false
            for (id in fairOrder) {
                val index = offsets.getValue(id)
                val next = remainingByPlayer.getValue(id).getOrNull(index) ?: continue
                extras += next
                offsets[id] = index + 1
                addedThisPass = true
            }
            if (!addedThisPass) break
        }

        // Interleave decoys with the fair round-robin extra sequence so decoys cannot
        // crowd out a player's additional submission when only a few slots remain.
        val selectedExtras = mutableListOf<SongEntry>()
        val selectedDecoys = mutableListOf<SongEntry>()
        var extraIndex = 0
        var decoyIndex = 0
        while (selectedExtras.size + selectedDecoys.size < slots) {
            if (extraIndex < extras.size) {
                selectedExtras += extras[extraIndex++]
                if (selectedExtras.size + selectedDecoys.size == slots) break
            }
            if (decoyIndex < decoys.size) {
                selectedDecoys += decoys[decoyIndex++]
            }
            if (extraIndex >= extras.size && decoyIndex >= decoys.size) break
        }

        return (firstPicks + selectedExtras + selectedDecoys).shuffled()
    }
}
