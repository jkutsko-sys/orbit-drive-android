package com.orbitdrive.game

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class ResponsiveLayoutTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private fun visible(node:SemanticsNodeInteraction):SemanticsNodeInteraction {
        try { node.assertIsDisplayed() } catch(e:AssertionError) { node.performScrollTo() }
        repeat(8) {
            try { return node.assertIsDisplayed() } catch(e:AssertionError) {
                if(rule.onAllNodesWithTag("hubBody").fetchSemanticsNodes().isNotEmpty()) {
                    rule.onNodeWithTag("hubBody").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f,150f) }
                } else {
                    shot("failure")
                    throw AssertionError("Not visible: ${node.fetchSemanticsNode().config}",e)
                }
            }
        }
        shot("failure")
        throw AssertionError("Not visible after scrolling: ${node.fetchSemanticsNode().config}")
    }
    private fun shot(name:String) {
        rule.waitForIdle()
        val i=InstrumentationRegistry.getInstrumentation()
        val c=rule.activity.resources.configuration
        fun shell(cmd:String) { android.os.ParcelFileDescriptor.AutoCloseInputStream(i.uiAutomation.executeShellCommand(cmd)).use { it.readBytes() } }
        shell("mkdir -p /sdcard/Download/orbit-ui-checks")
        shell("screencap -p /sdcard/Download/orbit-ui-checks/$name-${c.screenWidthDp}x${c.screenHeightDp}-${c.fontScale}.png")
    }
    @Test fun controlsAndMenusRemainReachable() {
        visible(rule.onNodeWithTag("launch"));visible(rule.onNodeWithTag("swingPower"));shot("range-controls")
        visible(rule.onNodeWithTag("settings")).performClick()
        visible(rule.onNodeWithTag("voucherCode"));shot("settings")
        rule.onNodeWithText("Done").performClick()
        visible(rule.onNodeWithTag("caddyshack")).performClick()
        visible(rule.onNodeWithText("MEET A RANDOM CADDY"));shot("caddies-responsive")
        rule.onNodeWithText("CLOSE").performClick()
        visible(rule.onNodeWithTag("clubhouse")).performClick()
        visible(rule.onNodeWithText("Skill Trees")).performClick()
        visible(rule.onNodeWithText("Stormcaller")).performClick()
        visible(rule.onNodeWithText("UPGRADE"));shot("skill-dialog")
        rule.onAllNodesWithText("CLOSE").onLast().performClick()
        rule.onNodeWithText("CLOSE").performClick()
        visible(rule.onNodeWithText("Clubs")).performClick();shot("clubs-responsive")
        visible(rule.onNodeWithText("Golfer")).performClick();shot("golfers-responsive")
        visible(rule.onNodeWithText("Ascend")).performClick()
        visible(rule.onNodeWithTag("discoverRelic"));shot("ascend-responsive")
    }
}
