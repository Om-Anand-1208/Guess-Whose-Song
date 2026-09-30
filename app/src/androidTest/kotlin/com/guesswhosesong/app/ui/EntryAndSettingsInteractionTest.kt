package com.guesswhosesong.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
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
        composeRule.onNodeWithText("Join a room").performClick()
        composeRule.onNodeWithContentDescription("Settings").performClick()

        composeRule.runOnIdle {
            assertEquals(1, fake.createdRooms)
            assertEquals(1, fake.joinedRooms)
            assertEquals(1, fake.openedSettings)
            assertEquals("AB12CD", joinCode.value)
        }
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
        composeRule.onNodeWithText("Join a room").assertIsNotEnabled()
        composeRule.onNodeWithText("Room service is unavailable").assertExists()
    }

    @Test
    fun profileSettingsExerciseAvatarAndConnectionActions() {
        val fake = FakeEntryActions()
        var savedName = ""
        var savedAvatar = AvatarCustomization.defaultsFor("sunny")
        composeRule.setContent {
            MaterialTheme {
                SavePlayerPage(
                    displayName = "Guest",
                    avatarCustomization = AvatarCustomization.defaultsFor("sunny"),
                    isSpotifyConnected = false,
                    isAccountLinked = false,
                    statusMessage = null,
                    statusIsError = false,
                    error = null,
                    onLinkGoogle = { fake.linkedGoogle++ },
                    onRecoverGoogle = { fake.recoveredGoogle++ },
                    onConnectSpotify = { fake.connectedSpotify++ },
                    onDisconnectSpotify = { fake.disconnectedSpotify++ },
                    onSaveProfile = { name, avatar -> savedName = name; savedAvatar = avatar },
                    onBackToGame = { fake.returnedFromSettings++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Lime avatar preset").performClick()
        composeRule.onNodeWithText("Spotify").performClick()
        composeRule.onNodeWithText("Link Google account").performClick()
        composeRule.onNodeWithText("Already linked? Recover your player").performClick()
        composeRule.onNodeWithText("← Back").performClick()
        composeRule.onNodeWithText("Save settings").performClick()

        composeRule.runOnIdle {
            assertEquals(1, fake.connectedSpotify)
            assertEquals(1, fake.linkedGoogle)
            assertEquals(1, fake.recoveredGoogle)
            assertEquals(1, fake.returnedFromSettings)
            assertEquals("Guest", savedName)
            assertEquals("lime", savedAvatar.shapeId)
        }
    }

    @Test
    fun connectedProfileCanDisconnectSpotifyAndLinkedGoogleCannotBeLinkedAgain() {
        val fake = FakeEntryActions()
        composeRule.setContent {
            MaterialTheme {
                SavePlayerPage(
                    displayName = "Guest",
                    avatarCustomization = AvatarCustomization.defaultsFor("sunny"),
                    isSpotifyConnected = true,
                    isAccountLinked = true,
                    statusMessage = "Connected",
                    statusIsError = false,
                    error = null,
                    onLinkGoogle = { fake.linkedGoogle++ },
                    onRecoverGoogle = { fake.recoveredGoogle++ },
                    onConnectSpotify = { fake.connectedSpotify++ },
                    onDisconnectSpotify = { fake.disconnectedSpotify++ },
                    onSaveProfile = { _, _ -> },
                    onBackToGame = { fake.returnedFromSettings++ }
                )
            }
        }

        composeRule.onNodeWithText("Spotify connected").performClick()
        composeRule.onNodeWithText("Google account linked").assertIsNotEnabled()
        composeRule.runOnIdle {
            assertEquals(1, fake.disconnectedSpotify)
            assertEquals(0, fake.linkedGoogle)
        }
    }

    private class FakeEntryActions {
        var createdRooms = 0
        var joinedRooms = 0
        var openedSettings = 0
        var connectedSpotify = 0
        var disconnectedSpotify = 0
        var linkedGoogle = 0
        var recoveredGoogle = 0
        var returnedFromSettings = 0
    }
}
