package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.RoundCountRules
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.SongEntry
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubmissionRulesTest {
    private fun song(id: String) = SongEntry(
        songId = id,
        title = "Song $id",
        artist = "Artist",
        previewUrl = "https://music.test/$id.mp3"
    )

    @Test
    fun `requires at least one valid song`() {
        assertFalse(SubmissionRules.isValid(emptyList(), roundCount = 10, playerCount = 3))
        assertTrue(SubmissionRules.isValid(listOf(song("one")), roundCount = 10, playerCount = 3))
    }

    @Test
    fun `identifies players who have not submitted a song`() {
        val players = listOf(
            Player(id = "p1", displayName = "One", pendingSongs = listOf(song("one"))),
            Player(id = "p2", displayName = "Two")
        )

        assertTrue(SubmissionRules.playersMissingSongs(players) == listOf("p2"))
    }

    @Test
    fun `caps extra submissions at ceiling of rounds divided by roster`() {
        val fourSongs = (1..4).map { song("song$it") }
        assertTrue(SubmissionRules.isValid(fourSongs, roundCount = 10, playerCount = 3))
        assertFalse(SubmissionRules.isValid(fourSongs + song("song5"), roundCount = 10, playerCount = 3))
        assertTrue(SubmissionRules.isValid(listOf(song("one")), roundCount = 20, playerCount = 20))
        assertFalse(SubmissionRules.isValid(listOf(song("one"), song("two")), roundCount = 20, playerCount = 20))
    }

    @Test
    fun `selected rounds must cover the current player count`() {
        assertTrue(RoundCountRules.isValid(4, playerCount = 4))
        assertFalse(RoundCountRules.isValid(3, playerCount = 4))
    }

    @Test
    fun `rejects duplicate IDs and duplicate title artist pairs`() {
        assertFalse(SubmissionRules.isValid(listOf(song("one"), song("one")), 10, 2))
        val sameTitle = listOf(song("one"), song("two").copy(title = "Song one", artist = "Artist"))
        assertFalse(SubmissionRules.isValid(sameTitle, 10, 2))
    }
}
