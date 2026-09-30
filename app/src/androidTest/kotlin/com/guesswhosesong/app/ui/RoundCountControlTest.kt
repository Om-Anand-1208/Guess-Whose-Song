package com.guesswhosesong.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.guesswhosesong.app.ui.screens.lobby.RoundCountControl
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RoundCountControlTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun roundCountCanIncreaseButNeverDropBelowRosterSize() {
        val selectedRounds = mutableIntStateOf(3)
        composeRule.setContent {
            MaterialTheme {
                RoundCountControl(roundCount = selectedRounds.intValue, currentPlayerCount = 4) {
                    selectedRounds.intValue = it
                }
            }
        }

        composeRule.onNodeWithText("3 rounds").assertTextEquals("3 rounds")
        composeRule.onNodeWithContentDescription("Decrease rounds").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Increase rounds").performClick()
        composeRule.runOnIdle { assertEquals(4, selectedRounds.intValue) }
        composeRule.onNodeWithContentDescription("Decrease rounds").assertIsNotEnabled()
    }

    @Test
    fun roundCountIsCappedAtTwenty() {
        val selectedRounds = mutableIntStateOf(20)
        composeRule.setContent {
            MaterialTheme {
                RoundCountControl(roundCount = selectedRounds.intValue, currentPlayerCount = 4) {
                    selectedRounds.intValue = it
                }
            }
        }

        composeRule.onNodeWithContentDescription("Increase rounds").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Decrease rounds").performClick()
        composeRule.runOnIdle { assertEquals(19, selectedRounds.intValue) }
    }
}
