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
        assertNull(deltas.find { it.playerId == "p2" })
        // Alice gets 1 point for stumping the room
        assertEquals(1, deltas.find { it.playerId == "p1" }?.delta)
    }

    @Test
    fun `no vote counted as wrong guess`() {
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = emptyMap<String, String>() // nobody voted
        val (results, deltas) = ScoreEngine.computeRoundResults(song("p1"), votes, players)

        results.forEach { assertFalse(it.correct) }
        // Alice gets 1 point for stumping the room
        assertEquals(1, deltas.find { it.playerId == "p1" }?.delta)
    }

    @Test
    fun `submitter receives 0 stump bonus if someone guesses them`() {
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"), player("p3", "Carol"))
        val votes = mapOf("p2" to "p1", "p3" to "p2") // Bob guessed Alice, Carol guessed Bob
        val (results, deltas) = ScoreEngine.computeRoundResults(song("p1"), votes, players)

        // Bob got it right
        assertTrue(results.find { it.voterId == "p2" }!!.correct)
        assertEquals(1, deltas.find { it.playerId == "p2" }?.delta)

        // Alice was guessed by Bob, so Alice does not get a stump bonus
        assertNull(deltas.find { it.playerId == "p1" })

        // Carol got it wrong
        assertFalse(results.find { it.voterId == "p3" }!!.correct)
    }

    @Test
    fun `correctly guessing decoy track awards 1 point`() {
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = mapOf("p1" to ScoreEngine.DECOY_ID, "p2" to "p1")
        val (results, deltas) = ScoreEngine.computeRoundResults(song(ScoreEngine.DECOY_ID), votes, players)

        // Alice correctly guessed decoy
        val aliceResult = results.find { it.voterId == "p1" }!!
        assertTrue(aliceResult.correct)
        assertEquals(1, deltas.find { it.playerId == "p1" }?.delta)

        // Bob guessed Alice, which is wrong for a decoy track
        val bobResult = results.find { it.voterId == "p2" }!!
        assertFalse(bobResult.correct)
        assertNull(deltas.find { it.playerId == "p2" })
    }

    @Test
    fun `submitter voting for themselves earns 0 points`() {
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = mapOf("p1" to "p1", "p2" to "p2") // Alice self-voted, Bob self-voted
        val (results, deltas) = ScoreEngine.computeRoundResults(song("p1"), votes, players)

        val aliceResult = results.find { it.voterId == "p1" }!!
        assertFalse(aliceResult.correct) // self-vote is never correct

        // Bob didn't guess Alice, so Alice gets +1 stump bonus
        assertEquals(1, deltas.find { it.playerId == "p1" }?.delta)
    }

    @Test
    fun `recent mode self guess earns one point without stacking the submitter bonus`() {
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = mapOf("p1" to "p1", "p2" to "p2")
        val (results, deltas) = ScoreEngine.computeRoundResults(
            song("p1"), votes, players, allowSubmitterSelfGuess = true
        )

        assertTrue(results.find { it.voterId == "p1" }!!.correct)
        assertEquals(1, deltas.find { it.playerId == "p1" }?.delta)
        assertNull(deltas.find { it.playerId == "p2" })
    }

    @Test
    fun `recent mode submitter keeps hidden bonus when they do not self guess`() {
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = mapOf("p1" to "p2", "p2" to "p2")
        val (results, deltas) = ScoreEngine.computeRoundResults(
            song("p1"), votes, players, allowSubmitterSelfGuess = true
        )

        assertFalse(results.find { it.voterId == "p1" }!!.correct)
        assertEquals(1, deltas.find { it.playerId == "p1" }?.delta)
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
