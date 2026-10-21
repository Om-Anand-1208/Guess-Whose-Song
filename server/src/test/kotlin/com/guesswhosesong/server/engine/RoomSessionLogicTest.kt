package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for the host reassignment logic and score engine,
 * exercised without needing a real Redis setup.
 */
class RoomSessionLogicTest {

    @Test
    fun `play again restores lobby and clears previous round state`() {
        val song = SongEntry("song", "Title", "Artist", previewUrl = "https://example.com/preview", submitterId = "host")
        val host = Player(
            id = "host", displayName = "Host", isHost = true, score = 3,
            pendingSong = song, pendingSongs = listOf(song), songLocked = true
        )
        val guest = Player(id = "guest", displayName = "Guest", score = 2, songLocked = true)
        val finished = Room(
            id = "ABC234", joinCode = "ABC234", hostId = host.id,
            state = RoomState.RESULTS, players = listOf(host, guest),
            songPool = listOf(song), currentRoundIndex = 1
        )

        val replay = finished.resetForReplay()

        assertEquals(RoomState.LOBBY, replay.state)
        assertTrue(replay.songPool.isEmpty())
        assertEquals(0, replay.currentRoundIndex)
        assertTrue(replay.players.all { it.score == 0 && !it.songLocked && it.pendingSong == null && it.pendingSongs.isEmpty() })
        assertTrue(replay.players.first().isHost)
    }

    @Test
    fun `join admission rejects new players after capacity or game start`() {
        val host = Player(id = "host", displayName = "Host", isHost = true)
        val guest = Player(id = "guest", displayName = "Guest")
        val lobby = Room(
            id = "ABC234",
            joinCode = "ABC234",
            hostId = host.id,
            settings = RoomSettings(playerLimit = 2),
            players = listOf(host, guest)
        )

        assertEquals(RoomJoinRejection.ROOM_FULL, lobby.joinRejectionFor("new-player"))
        assertNull(lobby.joinRejectionFor("guest"))
        assertEquals(
            RoomJoinRejection.GAME_ALREADY_STARTED,
            lobby.copy(state = RoomState.SUBMISSION, players = listOf(host)).joinRejectionFor("new-player")
        )
    }

    @Test
    fun `a stored room setting cannot admit an eleventh player`() {
        val players = (1..10).map { player("p$it", "Player $it") }
        val lobby = Room(
            id = "ABC234", joinCode = "ABC234", hostId = "p1",
            settings = RoomSettings(playerLimit = 20), players = players
        )

        assertEquals(RoomJoinRejection.ROOM_FULL, lobby.joinRejectionFor("p11"))
    }

    @Test
    fun `preview advances only after every connected human listener skips`() {
        val players = listOf(player("host", "Host"), player("guest", "Guest"),
            player("offline", "Offline", connected = false),
            player("computer", "Computer").copy(isComputer = true))

        assertFalse(allListenersSkipped(players, setOf("host")))
        assertTrue(allListenersSkipped(players, setOf("host", "guest")))
    }

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
        assertNull(deltas.find { it.playerId == "p2" })
        assertEquals(1, deltas.find { it.playerId == "p1" }?.delta)
    }

    @Test
    fun `no vote = wrong guess (not left in limbo)`() {
        val song = SongEntry("1", "Test", "Artist", previewUrl = "https://ex.com/preview.mp3", submitterId = "p1")
        val players = listOf(player("p1", "Alice"), player("p2", "Bob"))
        val votes = emptyMap<String, String>()
        val (results, deltas) = ScoreEngine.computeRoundResults(song, votes, players)
        results.forEach { assertFalse(it.correct) }
        assertEquals(1, deltas.find { it.playerId == "p1" }?.delta)
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

    // ─── Multi-song Pool Verification ──────────────────────────────────────────

    @Test
    fun `multi-song pending songs are resolved correctly`() {
        val s1 = SongEntry("1", "Song A", "Artist 1", previewUrl = "https://ex.com/1.mp3", submitterId = "p1")
        val s2 = SongEntry("2", "Song B", "Artist 1", previewUrl = "https://ex.com/2.mp3", submitterId = "p1")
        val s3 = SongEntry("3", "Song C", "Artist 2", previewUrl = "https://ex.com/3.mp3", submitterId = "p2")

        val p1 = player("p1", "Alice").copy(pendingSongs = listOf(s1, s2), songLocked = true)
        val p2 = player("p2", "Bob").copy(pendingSongs = listOf(s3), songLocked = true)

        val allSongs = listOf(p1, p2).flatMap { it.pendingSongs }
        assertEquals(3, allSongs.size)
        assertEquals(2, allSongs.count { it.submitterId == "p1" })
        assertEquals(1, allSongs.count { it.submitterId == "p2" })
    }
}
