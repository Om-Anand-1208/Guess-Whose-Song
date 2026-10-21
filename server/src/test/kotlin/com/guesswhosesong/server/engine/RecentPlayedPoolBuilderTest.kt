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
        title: String = "Song $id",
        artist: String = "Artist $id",
        previewUrl: String = "https://music.test/$id.mp3"
    ) = SongEntry(id, title, artist, previewUrl = previewUrl, submitterId = owner)

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
    fun `two player Spotify modes use one song per player and a distinct decoy in three rounds`() {
        val result = RecentPlayedPoolBuilder.build(
            resolvedSongs = listOf(
                song("p1-a", "p1"), song("p1-b", "p1"),
                song("p2-a", "p2"), song("p2-b", "p2")
            ),
            roundCount = 3,
            decoySongs = listOf(song("decoy", GameConstants.DECOY_ID))
        )!!

        assertEquals(3, result.songs.size)
        assertEquals(setOf("p1", "p2", GameConstants.DECOY_ID), result.songs.map { it.submitterId }.toSet())
        assertEquals(setOf("p1", "p2"), result.eligibleOwnerIds.toSet())
    }

    @Test
    fun `three player Spotify pool leaves out decoys`() {
        val players = listOf("p1", "p2", "p3")
        val result = RecentPlayedPoolBuilder.build(
            resolvedSongs = players.map { song(it, it) },
            roundCount = 3,
            decoySongs = listOf(song("decoy", GameConstants.DECOY_ID))
        )!!

        assertEquals(players.toSet(), result.songs.map { it.submitterId }.toSet())
    }

    @Test
    fun `decoy must have a new title and artist`() {
        val result = RecentPlayedPoolBuilder.build(
            resolvedSongs = listOf(song("p1", "p1"), song("p2", "p2")),
            roundCount = 3,
            decoySongs = listOf(
                song("same-title", GameConstants.DECOY_ID, title = "Song p1"),
                song("same-artist", GameConstants.DECOY_ID, artist = "Artist p2"),
                song("unique", GameConstants.DECOY_ID)
            )
        )!!

        assertEquals("unique", result.songs.single { it.submitterId == GameConstants.DECOY_ID }.songId)
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

    @Test
    fun `different releases by the same artist cannot both enter either Spotify pool`() {
        val result = RecentPlayedPoolBuilder.build(
            resolvedSongs = listOf(
                song("single", "p1", title = "Somewhere", artist = "The Singer"),
                song("album", "p2", title = "Somewhere (From the Movie)", artist = "the singer"),
                song("other", "p2", title = "Midnight", artist = "Another Singer")
            ),
            roundCount = 3
        )!!

        assertEquals(setOf("single", "other"), result.songs.map { it.songId }.toSet())
        assertEquals(setOf("p1", "p2"), result.eligibleOwnerIds.toSet())
    }

    @Test
    fun `same title with different artists is included once`() {
        val result = RecentPlayedPoolBuilder.build(
            resolvedSongs = listOf(
                song("first", "p1", title = "Hello!", artist = "Singer A"),
                song("second", "p2", title = "hello", artist = "Singer B"),
                song("alternative", "p2", title = "Goodbye", artist = "Singer C")
            ),
            roundCount = 3
        )!!

        assertEquals(setOf("first", "alternative"), result.songs.map { it.songId }.toSet())
    }
}
