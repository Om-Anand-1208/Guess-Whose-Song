package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for the host reassignment logic and score engine,
 * exercised without needing a real Redis/WebSocket setup.
 */
class RoomSessionLogicTest {

    // Helper builders
    private fun player(
        id: String,
        name: String,
        connected: Boolean = true,
        isHost: Boolean = false,
        score: Int = 0,
        joinedAt: Long = System.currentTimeMillis()
    ) = Player(id = id, displayName = name, connected = connected, isHost = isHost,
        score = score, joinedAt = joinedAt)

    // ─── Score Engine ─────────────────────────────────────────────────────────

    @Test
    fun `correct vote awards 1 point`() {
        val song = SongEntry("1", "Test", "Artist", previewUrl = "https://ex.com/preview.mp3", submitterId = "p1")
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = mapOf("p2" to "p1")
        val (_, deltas) = ScoreEngine.computeRoundResults(song, votes, players)
        assertEquals(1, deltas.size)
        assertEquals("p2", deltas[0].playerId)
        assertEquals(1, deltas[0].delta)
    }

    @Test
    fun `incorrect vote awards 0 points`() {
        val song = SongEntry("1", "Test", "Artist", previewUrl = "https://ex.com/preview.mp3", submitterId = "p1")
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = mapOf("p2" to "p2") // self-vote = wrong
        val (_, deltas) = ScoreEngine.computeRoundResults(song, votes, players)
        assertTrue(deltas.isEmpty())
    }

    @Test
    fun `no vote = wrong guess (not left in limbo)`() {
        val song = SongEntry("1", "Test", "Artist", previewUrl = "https://ex.com/preview.mp3", submitterId = "p1")
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = emptyMap<String, String>()
        val (results, deltas) = ScoreEngine.computeRoundResults(song, votes, players)
        assertTrue(deltas.isEmpty())
        results.forEach { assertFalse(it.correct) }
    }

    @Test
    fun `multiple correct guessers each earn 1 point`() {
        val song = SongEntry("1", "Test", "Artist", previewUrl = "https://ex.com/preview.mp3", submitterId = "p1")
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"), player("p3", "Carol"))
        val votes = mapOf("p2" to "p1", "p3" to "p1")
        val (_, deltas) = ScoreEngine.computeRoundResults(song, votes, players)
        assertEquals(2, deltas.size)
        assertTrue(deltas.all { it.delta == 1 })
    }

    @Test
    fun `applyDeltas accumulates on existing scores`() {
        val players = listOf(player("p1", "Alice", score = 3), player("p2", "Bob", score = 1))
        val deltas = listOf(ScoreDelta("p2", "Bob", 1, 2))
        val updated = ScoreEngine.applyDeltas(players, deltas)
        assertEquals(3, updated.find { it.id == "p1" }!!.score)
        assertEquals(2, updated.find { it.id == "p2" }!!.score)
    }

    // ─── Host Reassignment ────────────────────────────────────────────────────

    @Test
    fun `reassignment picks earliest-joined connected non-host`() {
        val players = listOf(
            player("p1", "Alice", isHost = true, connected = false, joinedAt = 100),
            player("p2", "Bob", connected = true, joinedAt = 300),
            player("p3", "Carol", connected = true, joinedAt = 200)
        )
        val next = HostReassignment.findNextHost(players, "p1")
        assertEquals("p3", next?.id) // Carol joined at 200, earlier than Bob's 300
    }

    @Test
    fun `reassignment skips disconnected players`() {
        val players = listOf(
            player("p1", "Alice", isHost = true, connected = false, joinedAt = 100),
            player("p2", "Bob", connected = false, joinedAt = 150),
            player("p3", "Carol", connected = true, joinedAt = 200)
        )
        val next = HostReassignment.findNextHost(players, "p1")
        assertEquals("p3", next?.id)
    }

    @Test
    fun `reassignment returns null when no connected players`() {
        val players = listOf(
            player("p1", "Alice", isHost = true, connected = false),
            player("p2", "Bob", connected = false)
        )
        val next = HostReassignment.findNextHost(players, "p1")
        assertNull(next)
    }

    @Test
    fun `applyHostChange sets flags correctly`() {
        val players = listOf(
            player("p1", "Alice", isHost = true),
            player("p2", "Bob", isHost = false),
            player("p3", "Carol", isHost = false)
        )
        val updated = HostReassignment.applyHostChange(players, "p1", "p2")
        assertFalse(updated.find { it.id == "p1" }!!.isHost)
        assertTrue(updated.find { it.id == "p2" }!!.isHost)
        assertFalse(updated.find { it.id == "p3" }!!.isHost)
    }
}

