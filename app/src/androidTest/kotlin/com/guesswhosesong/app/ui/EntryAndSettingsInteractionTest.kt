package com.guesswhosesong.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.guesswhosesong.app.ui.screens.join.SavePlayerPage
import com.guesswhosesong.app.ui.screens.join.WelcomePage
import com.guesswhosesong.shared.models.AvatarCustomization
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EntryAndSettingsInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeCreateJoinAndSettingsActionsUseTheSuppliedFakeService() {
        val fake = FakeEntryActions()
        val joinCode = mutableStateOf("")
        composeRule.setContent {
            MaterialTheme {
                WelcomePage(
                    joinCode = joinCode.value,
                    isLoading = false,
                    error = null,
                    onJoinCodeChange = { joinCode.value = it.uppercase() },
                    onCreateRoom = { fake.createdRooms++ },
                    onJoinRoom = { fake.joinedRooms++ },
                    onSettings = { fake.openedSettings++ }
                )
            }
        }

        composeRule.onNodeWithText("Create a room").performClick()
        composeRule.onNodeWithText("Room code").performTextInput("ab12cd")
        composeRule.onNodeWithText("Join room", substring = true).performClick()
        composeRule.onAllNodesWithContentDescription("Scan room QR code").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Appearance and player settings").performClick()

        composeRule.runOnIdle {
            assertEquals(1, fake.createdRooms)
            assertEquals(1, fake.joinedRooms)
            assertEquals(1, fake.openedSettings)
            assertEquals("AB12CD", joinCode.value)
        }
    }

    @Test
    fun roomCodeQrScannerIsNotAvailableInDarkMode() {
        composeRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                WelcomePage(
                    joinCode = "",
                    isLoading = false,
                    error = null,
                    onJoinCodeChange = {},
                    onCreateRoom = {},
                    onJoinRoom = {},
                    onSettings = {}
                )
            }
        }

        composeRule.onAllNodesWithContentDescription("Scan room QR code").assertCountEquals(0)
    }

    @Test
    fun homeLoadingAndErrorStatesAreVisibleAndDisableRoomActions() {
        val fake = FakeEntryActions()
        composeRule.setContent {
            MaterialTheme {
                WelcomePage(
                    joinCode = "ABC123",
                    isLoading = true,
                    error = "Room service is unavailable",
                    onJoinCodeChange = {},
                    onCreateRoom = { fake.createdRooms++ },
                    onJoinRoom = { fake.joinedRooms++ },
                    onSettings = { fake.openedSettings++ }
                )
            }
        }

        composeRule.onNodeWithText("Create a room").assertIsNotEnabled()
        composeRule.onNodeWithText("Join room", substring = true).assertIsNotEnabled()
        composeRule.onNodeWithText("Room service is unavailable").assertExists()
    }

    @Test
    fun profileSettingsUseInitialsAutosaveIndicatorAndSingleConnectionActions() {
        val fake = FakeEntryActions()
        var draftName = "Guest"
        composeRule.setContent {
            MaterialTheme {
                SavePlayerPage(
                    draftName = draftName,
                    draftAvatar = AvatarCustomization.defaultsFor("sunny"),
                    nameSaved = true,
                    nameError = null,
                    onNameChange = { draftName = it },
                    isSpotifyConnected = false,
                    isAccountLinked = false,
                    statusMessage = null,
                    statusIsError = false,
                    onLinkGoogle = { fake.linkedGoogle++ },
                    onDisconnectGoogle = {},
                    onConnectSpotify = { fake.connectedSpotify++ },
                    onDisconnectSpotify = { fake.disconnectedSpotify++ },
                    onRefreshSpotify = { fake.refreshedSpotify++ },
                    onBackToGame = { fake.returnedFromSettings++ }
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("Guest Remix")
        composeRule.onAllNodesWithText("Your player").assertCountEquals(0)
        composeRule.onAllNodesWithText("Edit character").assertCountEquals(0)
        composeRule.onAllNodesWithText("Save settings").assertCountEquals(0)
        composeRule.onAllNodesWithText("Cancel").assertCountEquals(0)
        composeRule.onAllNodesWithText("Connections").assertCountEquals(0)
        composeRule.onNodeWithText("Player settings").assertExists()
        composeRule.onAllNodesWithText("Connect")[0].performClick()
        composeRule.onAllNodesWithText("Connect")[1].performClick()
        composeRule.onNodeWithContentDescription("Sync Spotify status").performClick()
        composeRule.onNodeWithText("Done").performClick()

        composeRule.runOnIdle {
            assertEquals(1, fake.connectedSpotify)
            assertEquals(1, fake.linkedGoogle)
            assertEquals(1, fake.refreshedSpotify)
            assertEquals(1, fake.returnedFromSettings)
            assertEquals("Guest Remix", draftName)
        }
    }

    @Test
    fun connectedProfileCanDisconnectGoogleAndSpotify() {
        val fake = FakeEntryActions()
        composeRule.setContent {
            MaterialTheme {
                SavePlayerPage(
                    draftName = "Guest",
                    draftAvatar = AvatarCustomization.defaultsFor("sunny"),
                    nameSaved = true,
                    nameError = null,
                    onNameChange = {},
                    isSpotifyConnected = true,
                    isAccountLinked = true,
                    statusMessage = "Connected",
                    statusIsError = false,
                    onLinkGoogle = { fake.linkedGoogle++ },
                    onDisconnectGoogle = { fake.disconnectedGoogle++ },
                    onConnectSpotify = { fake.connectedSpotify++ },
                    onDisconnectSpotify = { fake.disconnectedSpotify++ },
                    onRefreshSpotify = { fake.refreshedSpotify++ },
                    onBackToGame = { fake.returnedFromSettings++ }
                )
            }
        }

        composeRule.onAllNodesWithText("Disconnect")[1].performClick()
        composeRule.onAllNodesWithText("Disconnect")[0].performClick()
        composeRule.onNodeWithContentDescription("Sync Spotify status").performClick()
        composeRule.runOnIdle {
            assertEquals(1, fake.disconnectedSpotify)
            assertEquals(1, fake.disconnectedGoogle)
            assertEquals(0, fake.linkedGoogle)
            assertEquals(1, fake.refreshedSpotify)
        }
    }

    private class FakeEntryActions {
        var createdRooms = 0
        var joinedRooms = 0
        var openedSettings = 0
        var connectedSpotify = 0
        var disconnectedSpotify = 0
        var disconnectedGoogle = 0
        var linkedGoogle = 0
        var refreshedSpotify = 0
        var returnedFromSettings = 0
    }
}
