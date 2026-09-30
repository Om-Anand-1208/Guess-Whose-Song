package com.guesswhosesong.web

import com.guesswhosesong.shared.dto.CastVote
import com.guesswhosesong.shared.dto.ClientMessage
import com.guesswhosesong.shared.dto.EndRoom
import com.guesswhosesong.shared.dto.LockSong
import com.guesswhosesong.shared.dto.PlayAgain
import com.guesswhosesong.shared.dto.SendChat
import com.guesswhosesong.shared.dto.StartGame
import com.guesswhosesong.shared.dto.UpdatePendingSongs
import com.guesswhosesong.shared.dto.UpdateSettings
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.Room
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.TrackSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WebGameStoreInteractionTest {
    @Test
    fun songPickerAddsRemovesAndEnforcesTheRoundBasedLimit() {
        val sent = mutableListOf<ClientMessage>()
        val store = fakeStore(sent)

        store.selectTrack(track("one"))
        store.selectTrack(track("two"))
        store.selectTrack(track("three"))

        assertEquals(listOf("one", "two"), store.state.value.pendingSongs.map { it.songId })
        assertEquals("You can select at most 2 songs", store.state.value.error)
        assertIs<UpdatePendingSongs>(sent.last())

        store.removeSong("one")
        assertEquals(listOf("two"), store.state.value.pendingSongs.map { it.songId })
        store.lockSongs()
        assertIs<LockSong>(sent.last())
    }

    @Test
    fun lobbyVotingChatReplayAndEndActionsSendTheirClientMessages() {
        val sent = mutableListOf<ClientMessage>()
        val store = fakeStore(sent)

        store.updateSettings(RoomSettings(roundCount = 6))
        store.startGame()
        store.castVote("p2")
        store.setChatDraft("  great song  ")
        store.sendChat()
        store.playAgain()
        store.endRoom()

        assertIs<UpdateSettings>(sent[0])
        assertIs<StartGame>(sent[1])
        assertIs<CastVote>(sent[2])
        assertEquals("great song", assertIs<SendChat>(sent[3]).text)
        assertIs<PlayAgain>(sent[4])
        assertIs<EndRoom>(sent[5])
        assertEquals("", store.state.value.chatDraft)
    }

    private fun fakeStore(sent: MutableList<ClientMessage>): WebGameStore {
        val players = listOf(
            Player(id = "p1", displayName = "Alice", isHost = true),
            Player(id = "p2", displayName = "Bob")
        )
        val room = Room(
            id = "room-id",
            joinCode = "ABC123",
            hostId = "p1",
            settings = RoomSettings(roundCount = 4),
            players = players
        )
        return WebGameStore(
            initialState = WebUiState(
                page = WebPage.SUBMISSION,
                displayName = "Alice",
                avatarId = "sunny",
                avatarCustomization = AvatarCustomization.defaultsFor("sunny"),
                joinCode = room.joinCode,
                room = room,
                selfPlayerId = "p1"
            ),
            testMessageSink = { sent += it },
            startServices = false
        )
    }

    private fun track(id: String) = TrackSearchResult(
        id = id,
        title = "Song $id",
        artist = "Artist",
        albumArtUrl = "https://music.test/$id.jpg",
        previewUrl = "https://music.test/$id.mp3"
    )
}
