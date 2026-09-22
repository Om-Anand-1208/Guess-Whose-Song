package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScoreEngineTest {

    private fun player(id: String, name: String, score: Int = 0) = Player(
        id = id, displayName = name, score = score, joinedAt = 0L
    )

    private fun song(submitterId: String) = SongEntry(
        songId = "1", title = "Test Song", artist = "Test Artist",
        previewUrl = "https://example.com/preview.mp3",
        submitterId = submitterId
    )

    @Test
    fun `correct vote earns 1 point`() {
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = mapOf("p2" to "p1") // Bob correctly guessed Alice submitted
        val (results, deltas) = ScoreEngine.computeRoundResults(song("p1"), votes, players)

        val bobResult = results.find { it.voterId == "p2" }!!
        assertTrue(bobResult.correct)
        assertEquals(1, deltas.find { it.playerId == "p2" }?.delta)
    }

    @Test
    fun `wrong vote earns 0 points`() {
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = mapOf("p2" to "p2") // Bob guessed himself — wrong
        val (results, deltas) = ScoreEngine.computeRoundResults(song("p1"), votes, players)

        val bobResult = results.find { it.voterId == "p2" }!!
        assertFalse(bobResult.correct)
        assertTrue(deltas.isEmpty())
    }

    @Test
    fun `no vote counted as wrong guess`() {
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = emptyMap<String, String>() // nobody voted
        val (results, deltas) = ScoreEngine.computeRoundResults(song("p1"), votes, players)

        results.forEach { assertFalse(it.correct) }
        assertTrue(deltas.isEmpty())
    }

    @Test
    fun `apply deltas updates scores correctly`() {
        val players = listOf(player("p1", "Alice", score = 2), player("p2", "Bob", score = 1))
        val deltas = listOf(ScoreDelta("p2", "Bob", delta = 1, newTotal = 2))
        val updated = ScoreEngine.applyDeltas(players, deltas)

        assertEquals(2, updated.find { it.id == "p1" }?.score)
        assertEquals(2, updated.find { it.id == "p2" }?.score)
    }

    @Test
    fun `host reassignment picks earliest joined connected player`() {
        val players = listOf(
            Player("p1", "Alice", isHost = true, connected = false, joinedAt = 1000L),
            Player("p2", "Bob", connected = true, joinedAt = 2000L),
            Player("p3", "Carol", connected = true, joinedAt = 1500L)
        )
        val next = HostReassignment.findNextHost(players, "p1")
        assertEquals("p3", next?.id) // Carol joined earlier than Bob
    }

    @Test
    fun `host reassignment skips disconnected players`() {
        val players = listOf(
            Player("p1", "Alice", isHost = true, connected = false, joinedAt = 1000L),
            Player("p2", "Bob", connected = false, joinedAt = 1500L), // also disconnected
            Player("p3", "Carol", connected = true, joinedAt = 2000L)
        )
        val next = HostReassignment.findNextHost(players, "p1")
        assertEquals("p3", next?.id)
    }
}
