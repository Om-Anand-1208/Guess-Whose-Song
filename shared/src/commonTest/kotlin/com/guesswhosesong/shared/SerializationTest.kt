package com.guesswhosesong.shared

import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString

class SerializationTest {

    @Test
    fun `room chat wire message round trips`() {
        val message = RoomChatMessage("msg-1", "player-1", "Alice", "Hello room", 1234L)
        val json = GWSJson.encodeToString(RoomChatMessage.serializer(), message)
        assertEquals(message, GWSJson.decodeFromString<RoomChatMessage>(json))
    }

    @Test
    fun `round count defaults old saved settings to ten`() {
        val decoded = GWSJson.decodeFromString<RoomSettings>("""{"roundLengthPreset":"STANDARD"}""")
        assertEquals(10, decoded.roundCount)
        assertEquals(10, RoomSettings().roundCount)
        assertEquals(GameMode.MANUAL, decoded.gameMode)
    }

    @Test
    fun `top tracks mode survives room settings serialization`() {
        val settings = RoomSettings(gameMode = GameMode.SPOTIFY_TOP)
        val decoded = GWSJson.decodeFromString<RoomSettings>(GWSJson.encodeToString(RoomSettings.serializer(), settings))

        assertEquals(GameMode.SPOTIFY_TOP, decoded.gameMode)
        assertTrue(decoded.gameMode.isAutomaticSpotifyMode)
        assertTrue(GameMode.SPOTIFY_RECENT.isAutomaticSpotifyMode)
        assertFalse(GameMode.MANUAL.isAutomaticSpotifyMode)
    }

    @Test
    fun `voting message defaults eligible owners for manual rooms`() {
        val decoded = GWSJson.decodeFromString<VotingStarted>(
            """{"type":"VOTING_STARTED","roundIndex":0,"players":[],"votingDeadlineEpochMillis":1234}"""
        )
        assertEquals(emptyList<String>(), decoded.eligibleOwnerIds)
    }

    @Test
    fun `round count rules bound settings and song submissions`() {
        assertFalse(RoundCountRules.isValid(2, 2))
        assertTrue(RoundCountRules.isValid(3, 2))
        assertEquals(3, RoundCountRules.minimumForPlayerCount(2))
        assertTrue(RoundCountRules.isValid(20, 20))
        assertTrue(RoundCountRules.isValid(30, 10))
        assertFalse(RoundCountRules.isValid(1, 2))
        assertFalse(RoundCountRules.isValid(31, 2))
        assertFalse(RoundCountRules.isValid(4, 5))
        assertEquals(4, RoundCountRules.maxSongsPerPlayer(10, 3))
        assertEquals(1, RoundCountRules.maxSongsPerPlayer(10, 20))
    }

    @Test
    fun testRoundTripRoomJoined() {
        val msg: ServerMessage = RoomJoined(
            room = Room(
                id = "room1",
                joinCode = "ABC123",
                hostId = "player1"
            ),
            selfPlayerId = "player1"
        )
        val json = msg.toJson()
        val decoded = json.toServerMessage()
        assertEquals(msg, decoded)
    }

    @Test
    fun testRoundTripCastVote() {
        val msg: ClientMessage = CastVote(guessedPlayerId = "player2")
        val json = msg.toJson()
        val decoded = json.toClientMessage()
        assertEquals(msg, decoded)
    }

    @Test
    fun `skip preview round trips with its round index`() {
        val message: ClientMessage = SkipPreview(roundIndex = 4)
        assertEquals(message, message.toJson().toClientMessage())
    }

    @Test
    fun testRoundTripRoundRevealed() {
        val song = SongEntry(
            songId = "123",
            title = "Blinding Lights",
            artist = "The Weeknd",
            previewUrl = "https://example.com/preview.mp3",
            submitterId = "player1"
        )
        val msg: ServerMessage = RoundRevealed(
            roundIndex = 0,
            songEntry = song,
            submitterName = "Alice",
            voteResults = listOf(
                VoteResult(
                    voterId = "player2",
                    voterName = "Bob",
                    guessedPlayerId = "player1",
                    guessedPlayerName = "Alice",
                    correct = true
                )
            ),
            scoreDeltas = listOf(
                ScoreDelta(playerId = "player2", playerName = "Bob", delta = 1, newTotal = 1)
            )
        )
        val json = msg.toJson()
        val decoded = json.toServerMessage()
        assertEquals(msg, decoded)
    }
}
