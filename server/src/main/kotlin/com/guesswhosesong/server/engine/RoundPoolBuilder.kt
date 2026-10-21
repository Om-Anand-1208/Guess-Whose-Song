package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.GameConstants
import com.guesswhosesong.shared.models.RoundCountRules
import com.guesswhosesong.shared.models.SongEntry

/** Builds a bounded game pool while representing each player before adding their extra picks. */
object RoundPoolBuilder {
    /** Manual mode: represent every player, then reserve a genre decoy when rounds allow. */
    fun buildGenreMatched(
        playerSongs: List<SongEntry>,
        decoySongs: List<SongEntry>,
        playerIds: List<String>,
        roundCount: Int
    ): List<SongEntry> {
        val limit = roundCount.coerceIn(RoundCountRules.MIN_ROUNDS, RoundCountRules.MAX_ROUNDS)
        val ids = playerIds.distinct()
        val byPlayer = ids.associateWith { id ->
            playerSongs.filter { it.submitterId == id && it.previewUrl.isNotBlank() }.shuffled()
        }
        val firstPicks = ids.mapNotNull { byPlayer[it]?.firstOrNull() }.take(limit)
        if (firstPicks.size >= limit) return firstPicks.shuffled()

        val remainingSlots = limit - firstPicks.size
        val decoys = decoySongs
            .takeIf { ids.size == 2 }.orEmpty()
            .filter { it.submitterId == GameConstants.DECOY_ID && it.previewUrl.isNotBlank() }
            .distinctBy { it.songId }
            .shuffled()
        val decoyCount = minOf(decoys.size, remainingSlots, 2, (limit / 3).coerceAtLeast(1))
        val selectedDecoys = decoys.take(decoyCount)

        val extras = mutableListOf<SongEntry>()
        val fairOrder = ids.shuffled()
        var nextIndex = 1
        while (extras.size < remainingSlots - decoyCount) {
            val nextPass = fairOrder.mapNotNull { byPlayer[it]?.getOrNull(nextIndex) }
            if (nextPass.isEmpty()) break
            extras += nextPass
            nextIndex++
        }
        return (firstPicks + selectedDecoys + extras.take(remainingSlots - decoyCount)).shuffled()
    }

    fun build(
        playerSongs: List<SongEntry>,
        decoySongs: List<SongEntry>,
        playerIds: List<String>,
        roundCount: Int
    ): List<SongEntry> {
        val limit = roundCount.coerceIn(RoundCountRules.MIN_ROUNDS, RoundCountRules.MAX_ROUNDS)
        val ids = playerIds.distinct()
        val byPlayer = playerSongs
            .filter { it.submitterId != GameConstants.DECOY_ID && it.previewUrl.isNotBlank() }
            .groupBy { it.submitterId }

        val firstPicks = ids.mapNotNull { byPlayer[it]?.firstOrNull() }.take(limit)
        if (firstPicks.size >= limit) return firstPicks.shuffled()

        val slots = limit - firstPicks.size
        val decoys = decoySongs
            .takeIf { ids.size == 2 }.orEmpty()
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
