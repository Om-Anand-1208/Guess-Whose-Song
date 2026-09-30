package com.guesswhosesong.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.guesswhosesong.app.ui.screens.game.ChatDock
import com.guesswhosesong.app.ui.screens.game.VotingSection
import com.guesswhosesong.app.ui.screens.lobby.LobbySettingsSheet
import com.guesswhosesong.app.ui.screens.lobby.PlayerListItem
import com.guesswhosesong.app.ui.screens.results.ResultsActions
import com.guesswhosesong.app.ui.screens.submission.SearchSongsField
import com.guesswhosesong.app.ui.screens.submission.SelectedSongCard
import com.guesswhosesong.app.ui.screens.submission.SpotifyConnectCard
import com.guesswhosesong.app.ui.screens.submission.TrackListItem
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.ChatMessage
import com.guesswhosesong.shared.models.GameMode
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.TrackSearchResult
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FlowControlInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lobbySettingsAndPlayerActionsUpdateFakeRoomState() {
        val fake = FakeFlowActions()
        val initialSettings = RoomSettings(roundCount = 10)
        composeRule.setContent {
            MaterialTheme {
                LobbySettingsSheet(
                    displayName = "Host",
                    avatarCustomization = AvatarCustomization.defaultsFor("sunny"),
                    isSpotifyConnected = false,
                    isAccountLinked = false,
                    accountStatus = null,
                    accountStatusIsError = false,
                    roomSettings = initialSettings,
                    currentPlayerCount = 3,
                    isHost = true,
                    profileError = null,
                    onConnectSpotify = { fake.connectedSpotify++ },
                    onDisconnectSpotify = { fake.disconnectedSpotify++ },
                    onLinkGoogle = { fake.linkedGoogle++ },
                    onDismiss = { fake.dismissedSettings++ },
                    onSave = { name, avatar, settings ->
                        fake.savedName = name
                        fake.savedAvatar = avatar
                        fake.savedSettings = settings
                    }
                )
            }
        }

        composeRule.onNodeWithText("Display name").performTextClearance()
        composeRule.onNodeWithText("Display name").performTextInput("Host Remix")
        composeRule.onNodeWithContentDescription("Lime avatar preset").performClick()
        composeRule.onNodeWithText("Connect Spotify").performClick()
        composeRule.onNodeWithText("Link Google account").performClick()
        composeRule.onNodeWithContentDescription("Increase rounds").performClick()
        listOf("10s", "15s", "20s", "30s").forEach { choice ->
            composeRule.onNodeWithText(choice).performClick()
        }
        composeRule.onNodeWithText("Recently Played").performClick()
        composeRule.onNodeWithText("Save changes").performClick()

        composeRule.runOnIdle {
            assertEquals(1, fake.connectedSpotify)
            assertEquals(1, fake.linkedGoogle)
            assertEquals("Host Remix", fake.savedName)
            assertEquals("lime", fake.savedAvatar?.shapeId)
            assertEquals(11, fake.savedSettings?.roundCount)
            assertEquals(30, fake.savedSettings?.votingTimerSeconds)
            assertEquals(GameMode.SPOTIFY_RECENT, fake.savedSettings?.gameMode)
        }
    }

    @Test
    fun lobbyKickAndSubmissionSearchSelectionAndRemovalWork() {
        val fake = FakeFlowActions()
        val query = mutableStateOf("")
        val track = TrackSearchResult("track-1", "Night Train", "Artist", "", "https://music.test/preview.mp3")
        val song = SongEntry("track-1", "Blue Hours", "Artist", previewUrl = "https://music.test/preview.mp3")
        composeRule.setContent {
            MaterialTheme {
                Column {
                    PlayerListItem(
                        player = Player(id = "p2", displayName = "Guest"),
                        isSelf = false,
                        onKick = { fake.kickedPlayers++ }
                    )
                    SearchSongsField(
                        query = query.value,
                        songCount = 0,
                        onQueryChange = { query.value = it },
                        onSearch = { fake.searches++ }
                    )
                    TrackListItem(track = track, isAdded = false, isFull = false) { fake.selectedTracks++ }
                    SelectedSongCard(index = 1, song = song, locked = false) { fake.removedSongs++ }
                    SpotifyConnectCard { fake.connectedSpotify++ }
                }
            }
        }

        composeRule.onNodeWithText("Kick").performClick()
        composeRule.onNodeWithText("Search for a song…").performTextInput("Moonlight")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.onNodeWithContentDescription("Clear search").performClick()
        composeRule.onNodeWithText("Night Train").performClick()
        composeRule.onNodeWithContentDescription("Remove song").performClick()
        composeRule.onNodeWithText("Spotify connected?").performClick()

        composeRule.runOnIdle {
            assertEquals(1, fake.kickedPlayers)
            assertEquals(1, fake.searches)
            assertEquals(1, fake.selectedTracks)
            assertEquals(1, fake.removedSongs)
            assertEquals(1, fake.connectedSpotify)
            assertEquals("", query.value)
        }
    }

    @Test
    fun votingChatAndResultsActionsReachTheFakeGameService() {
        val fake = FakeFlowActions()
        val chatDraft = mutableStateOf("")
        val chatExpanded = mutableStateOf(false)
        val players = listOf(
            Player(id = "p1", displayName = "Alice"),
            Player(id = "p2", displayName = "Bob")
        )
        composeRule.setContent {
            MaterialTheme {
                Column {
                    VotingSection(
                        players = players,
                        selfPlayerId = "p1",
                        votedPlayerId = null,
                        votedCount = 0,
                        totalCount = 2,
                        deadlineMs = System.currentTimeMillis() + 30_000L,
                        onConfirmVote = { fake.vote = it }
                    )
                    ChatDock(
                        messages = listOf(ChatMessage("p2", "Bob", "Nice pick")),
                        expanded = chatExpanded.value,
                        onExpandToggle = { chatExpanded.value = !chatExpanded.value },
                        input = chatDraft.value,
                        onInputChange = { chatDraft.value = it },
                        onSend = { fake.sentMessage = chatDraft.value; chatDraft.value = "" }
                    )
                    ResultsActions(
                        isHost = true,
                        onPlayAgain = { fake.playAgain++ },
                        onEndGame = { fake.endedGame++ }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Choose a player to vote").assertIsNotEnabled()
        composeRule.onNodeWithText("Bob").performClick()
        composeRule.onNodeWithText("Vote for Bob").performClick()
        composeRule.onNodeWithText("Show").performClick()
        composeRule.onNodeWithText("Hide").performClick()
        composeRule.onNodeWithText("Show").performClick()
        composeRule.onNodeWithText("Say something…").performTextInput("Good game")
        composeRule.onNodeWithContentDescription("Send message").performClick()
        composeRule.onNodeWithText("Play Again 🎵").performClick()
        composeRule.onNodeWithText("End Game").performClick()

        composeRule.runOnIdle {
            assertEquals("p2", fake.vote)
            assertEquals("Good game", fake.sentMessage)
            assertEquals(1, fake.playAgain)
            assertEquals(1, fake.endedGame)
            assertEquals("", chatDraft.value)
        }
    }

    @Test
    fun fullSongSelectionAndSelfVoteStayDisabled() {
        val fake = FakeFlowActions()
        val track = TrackSearchResult("track-1", "Night Train", "Artist", "", "https://music.test/preview.mp3")
        composeRule.setContent {
            MaterialTheme {
                Column {
                    TrackListItem(track = track, isAdded = false, isFull = true) { fake.selectedTracks++ }
                    VotingSection(
                        players = listOf(Player(id = "p1", displayName = "Alice"), Player(id = "p2", displayName = "Bob")),
                        selfPlayerId = "p1",
                        votedPlayerId = null,
                        votedCount = 0,
                        totalCount = 2,
                        deadlineMs = System.currentTimeMillis() + 30_000L,
                        onConfirmVote = { fake.vote = it }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Night Train").assertIsNotEnabled()
        composeRule.onNodeWithText("You").assertIsNotEnabled()
        composeRule.runOnIdle {
            assertEquals(0, fake.selectedTracks)
            assertEquals(null, fake.vote)
        }
    }

    @Test
    fun recentlyPlayedVotingShowsOnlyEligibleOwnersAndAllowsSelfVote() {
        val fake = FakeFlowActions()
        composeRule.setContent {
            MaterialTheme {
                VotingSection(
                    players = listOf(Player(id = "p1", displayName = "Alice"), Player(id = "p2", displayName = "Bob")),
                    eligibleOwnerIds = listOf("p1"),
                    selfPlayerId = "p1",
                    votedPlayerId = null,
                    votedCount = 0,
                    totalCount = 2,
                    deadlineMs = System.currentTimeMillis() + 30_000L,
                    allowSelfVote = true,
                    onConfirmVote = { fake.vote = it }
                )
            }
        }

        composeRule.onNodeWithText("0/2 voted", substring = true).assertExists()
        composeRule.onNodeWithText("Bob").assertDoesNotExist()
        composeRule.onNodeWithText("Nobody / Decoy").assertDoesNotExist()
        composeRule.onNodeWithText("Alice").performClick()
        composeRule.onNodeWithText("Vote for Alice").performClick()

        composeRule.runOnIdle { assertEquals("p1", fake.vote) }
    }

    @Test
    fun guestResultsWaitForHostWithoutHostOnlyActions() {
        composeRule.setContent {
            MaterialTheme {
                ResultsActions(isHost = false, onPlayAgain = {}, onEndGame = {})
            }
        }

        composeRule.onNodeWithText("Waiting for host to continue…").assertExists()
        composeRule.onNodeWithText("Play Again 🎵").assertDoesNotExist()
        composeRule.onNodeWithText("End Game").assertDoesNotExist()
    }

    private class FakeFlowActions {
        var connectedSpotify = 0
        var disconnectedSpotify = 0
        var linkedGoogle = 0
        var dismissedSettings = 0
        var savedName: String? = null
        var savedAvatar: AvatarCustomization? = null
        var savedSettings: RoomSettings? = null
        var kickedPlayers = 0
        var searches = 0
        var selectedTracks = 0
        var removedSongs = 0
        var vote: String? = null
        var sentMessage: String? = null
        var playAgain = 0
        var endedGame = 0
    }
}
