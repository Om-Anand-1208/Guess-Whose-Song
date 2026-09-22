package com.guesswhosesong.shared

import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {

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
    fun testRoundTripRoundRevealed() {
        val song = SongEntry(
            songId = "123",
            title = "Blinding Lights",
            artist = "The Weeknd",
            deezerPreviewUrl = "https://example.com/preview.mp3",
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
