package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.GameConstants
import com.guesswhosesong.shared.models.SongEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoundPoolBuilderTest {
    private fun song(id: String, submitter: String, previewUrl: String = "https://music.test/$id.mp3") =
        SongEntry(id, "Song $id", "Artist $submitter", previewUrl = previewUrl, submitterId = submitter)

    @Test
    fun `first picks represent every player before extra submissions`() {
        val ids = listOf("p1", "p2", "p3")
        val submitted = ids.flatMap { playerId -> (1..4).map { song("$playerId-$it", playerId) } }

        val pool = RoundPoolBuilder.build(submitted, emptyList(), ids, roundCount = 8)
        val counts = pool.groupingBy { it.submitterId }.eachCount().values

        assertEquals(8, pool.size)
        assertTrue(ids.all { id -> pool.any { it.submitterId == id } })
        assertTrue(counts.max() - counts.min() <= 1, "Extra picks should be distributed evenly")
    }

    @Test
    fun `pool never exceeds selected rounds and caps decoys at two`() {
        val players = listOf(song("p1-1", "p1"), song("p2-1", "p2"))
        val decoys = (1..6).map { song("d$it", GameConstants.DECOY_ID) }

        val pool = RoundPoolBuilder.build(players, decoys, listOf("p1", "p2"), roundCount = 5)

        assertEquals(4, pool.size)
        assertEquals(2, pool.count { it.submitterId == GameConstants.DECOY_ID })
        assertTrue(pool.any { it.submitterId == "p1" })
        assertTrue(pool.any { it.submitterId == "p2" })
    }

    @Test
    fun `extra songs get fair slots before decoys are interleaved`() {
        val players = listOf(
            song("p1-1", "p1"), song("p1-2", "p1"),
            song("p2-1", "p2"), song("p2-2", "p2")
        )
        val decoys = listOf(
            song("d1", GameConstants.DECOY_ID),
            song("d2", GameConstants.DECOY_ID)
        )

        val pool = RoundPoolBuilder.build(players, decoys, listOf("p1", "p2"), roundCount = 5)

        assertEquals(5, pool.size)
        assertEquals(1, pool.count { it.submitterId == GameConstants.DECOY_ID })
        assertTrue(pool.any { it.songId == "p1-2" })
        assertTrue(pool.any { it.songId == "p2-2" })
    }

    @Test
    fun `unresolved tracks are skipped and available tracks form a shorter pool`() {
        val playerSongs = listOf(
            song("good", "p1"),
            song("unresolved", "p2", previewUrl = "")
        )
        val decoys = listOf(song("decoy-good", GameConstants.DECOY_ID))

        val pool = RoundPoolBuilder.build(playerSongs, decoys, listOf("p1", "p2"), roundCount = 10)

        assertEquals(2, pool.size)
        assertFalse(pool.any { it.previewUrl.isBlank() })
        assertTrue(pool.any { it.submitterId == "p1" })
        assertFalse(pool.any { it.submitterId == "p2" })
    }

    @Test
    fun `pool is empty when no submitted or decoy track can play`() {
        val pool = RoundPoolBuilder.build(
            playerSongs = listOf(song("bad", "p1", previewUrl = "")),
            decoySongs = listOf(song("bad-decoy", GameConstants.DECOY_ID, previewUrl = "")),
            playerIds = listOf("p1"),
            roundCount = 10
        )

        assertTrue(pool.isEmpty())
    }
}
