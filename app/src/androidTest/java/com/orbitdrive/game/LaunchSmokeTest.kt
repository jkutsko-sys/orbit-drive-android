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
    @Test fun settingsOpensFromCashHeader() {
        rule.onNodeWithTag("settings").assertExists().performClick()
        rule.onNodeWithText("SETTINGS").assertExists()
        rule.onNodeWithTag("voucherCode").assertExists()
        rule.onNodeWithTag("redeem").assertExists()
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

    @Test fun longShotCompletesWithinTwentyFiveSecondsAtConsistentDistance() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("orbit_drive_v1", android.content.Context.MODE_PRIVATE)
        val prior = prefs.getString("save", null)
        try {
            val setup = JSONObject().put("club", 14).put("equippedClub", 14).put("relics", 4)
                .put("nodes", JSONArray((0..23).map { "GRAVITY-$it" })).toString()
            prefs.edit().putString("save", setup).commit()
            val fine = GameEngine(context)
            prefs.edit().putString("save", setup).commit()
            val coarse = GameEngine(context)
            listOf(fine, coarse).forEach { game -> game.startCharge(); game.tick(.5); game.release() }
            repeat(489) { fine.tick(.05) }
            assertEquals(Phase.FLYING, fine.phase)
            fine.tick(.05)
            repeat(49) { coarse.tick(.5) }
            assertEquals(Phase.LANDED, fine.phase)
            assertEquals(Phase.LANDED, coarse.phase)
            assertTrue(fine.shotElapsed <= 24.5)
            assertTrue(fine.distance > 7000)
            assertEquals(fine.distance, coarse.distance, fine.distance * .02)
            fine.startCharge()
            assertEquals(Phase.CHARGING, fine.phase)
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
