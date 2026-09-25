package com.orbitdrive.game

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
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
}
