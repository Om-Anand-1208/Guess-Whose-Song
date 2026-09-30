package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.GameConstants
import com.guesswhosesong.shared.models.SongEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecentPlayedPoolBuilderTest {
    private fun song(
        id: String,
        owner: String,
        previewUrl: String = "https://music.test/$id.mp3"
    ) = SongEntry(id, "Song $id", "Artist", previewUrl = previewUrl, submitterId = owner)

    @Test
    fun `pool is capped and contains no decoy songs`() {
        val result = RecentPlayedPoolBuilder.build(
            resolvedSongs = listOf(
                song("p1-a", "p1"), song("p1-b", "p1"),
                song("p2-a", "p2"), song("p2-b", "p2"),
                song("decoy", GameConstants.DECOY_ID)
            ),
            roundCount = 2
        )!!

        assertEquals(2, result.songs.size)
        assertEquals(setOf("p1", "p2"), result.eligibleOwnerIds.toSet())
        assertFalse(result.songs.any { it.submitterId == GameConstants.DECOY_ID })
    }

    @Test
    fun `catalog duplicate shared by owners is removed for both owners`() {
        val result = RecentPlayedPoolBuilder.build(
            resolvedSongs = listOf(
                song("shared", "p1"), song("shared", "p2"),
                song("p1-unique", "p1"), song("p2-unique", "p2")
            ),
            roundCount = 4
        )!!

        assertFalse(result.songs.any { it.songId == "shared" })
        assertEquals(setOf("p1", "p2"), result.eligibleOwnerIds.toSet())
    }

    @Test
    fun `unplayable songs do not make a player eligible`() {
        val result = RecentPlayedPoolBuilder.build(
            resolvedSongs = listOf(
                song("p1", "p1"), song("p2", "p2", previewUrl = "")
            ),
            roundCount = 10
        )

        assertNull(result)
    }

    @Test
    fun `pool may contain fewer songs than selected round count`() {
        val result = RecentPlayedPoolBuilder.build(
            resolvedSongs = listOf(song("p1", "p1"), song("p2", "p2")),
            roundCount = 10
        )!!

        assertEquals(2, result.songs.size)
        assertTrue(result.songs.all { it.previewUrl.isNotBlank() })
    }
}
