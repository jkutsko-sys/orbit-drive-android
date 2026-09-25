package com.orbitdrive.game

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class LaunchSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun swingDoesNotCrash() {
        rule.onNodeWithTag("launch").assertExists().performTouchInput {
            down(center)
            advanceEventTime(1200)
            up()
        }
        rule.waitForIdle()
        rule.onNodeWithText("ORBIT DRIVE").assertExists()
    }

    @Test fun treeAndGolferRosterOpen() {
        rule.onNodeWithText("Research").performClick()
        rule.onNodeWithText("RESEARCH CONSTELLATION").assertExists()
        rule.onNodeWithText("Golfer").performClick()
        rule.onNodeWithText("GOLFER ROSTER").assertExists()
        rule.onNodeWithText("Ascend").performClick()
        rule.onNodeWithText("GOLF BALLS").assertExists()
    }
}
