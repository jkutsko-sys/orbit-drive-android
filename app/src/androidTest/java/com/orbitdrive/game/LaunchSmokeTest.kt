package com.orbitdrive.game

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.json.JSONArray

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
    @Test fun settingsVoucherIsOneTime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("orbit_drive_v1", android.content.Context.MODE_PRIVATE)
        val prior = prefs.getString("save", null)
        try {
            prefs.edit().remove("save").commit()
            val engine = GameEngine(context)
            assertEquals("Invalid voucher code", engine.redeemVoucher("wrong"))
            assertEquals("$10,000 added for testing", engine.redeemVoucher("admin"))
            assertEquals(10_000.0, engine.cash, 0.01)
            assertEquals("ADMIN voucher already redeemed", GameEngine(context).redeemVoucher("ADMIN"))
        } finally {
            if (prior == null) prefs.edit().remove("save").commit()
            else prefs.edit().putString("save", prior).commit()
        }
    }

    @Test fun expandedTreeAndElectricKeystone() {
        assertEquals(192, ResearchTree.all.size)
        assertEquals(192, ResearchTree.all.map { it.id }.toSet().size)
        assertEquals(192, ResearchTree.all.map { it.name }.toSet().size)
        assertTrue(ResearchTree.all.all { node -> node.requires.all { it in ResearchTree.byId } })
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("orbit_drive_v1", android.content.Context.MODE_PRIVATE)
        val prior = prefs.getString("save", null)
        try {
            val nodes = JSONArray((0..15).map { "LIGHTNING-$it" })
            prefs.edit().putString("save", JSONObject().put("nodes", nodes).toString()).commit()
            val engine = GameEngine(context)
            engine.startCharge(); engine.tick(.05); engine.release(); engine.lightning()
            assertTrue(engine.electrifiedFor > 0.0)
            assertTrue(engine.lightningFlashFor > 0.0)
        } finally {
            if (prior == null) prefs.edit().remove("save").commit()
            else prefs.edit().putString("save", prior).commit()
        }
    }
}
