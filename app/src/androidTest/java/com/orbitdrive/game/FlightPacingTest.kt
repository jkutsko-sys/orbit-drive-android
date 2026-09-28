package com.orbitdrive.game

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class FlightPacingTest {
    private fun game(clock:()->Long):GameEngine {
        val id=UUID.randomUUID().toString()
        val ctx=object:ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getSharedPreferences(name:String,mode:Int)=baseContext.getSharedPreferences("pacing_${id}_$name",mode)
        }
        val nodes=listOf("GRAVITY","FRICTION","BOUNCE").flatMap { branch -> (0..23).map { "$branch-$it" } }
        ctx.getSharedPreferences("orbit_drive_v1",0).edit().putString("save",JSONObject().put("club",14).put("equippedClub",14).put("nodes",JSONArray(nodes)).toString()).commit()
        return GameEngine(ctx,clock=clock).apply { abilityProgress.recordBest(80_000_000.0);buyAbilitySkill(AbilitySkills.byId.getValue("LIGHTNING-R0"));startCharge();tick(.5);release() }
    }
    @Test fun endgameShotHasVisibleLandingAndNaturalRollout() {
        var time=2_000_000_000_000L;val g=game { time }
        var landingFrames=0
        repeat(2500) { time+=10;if(g.landingWindow) landingFrames++;g.tick(.01) }
        assertTrue("No visible combo window: $landingFrames",landingFrames>=50)
        assertTrue("No ground bounces",g.bounces>=3)
        assertEquals("Natural completion took ${g.shotElapsed}s",Phase.LANDED,g.phase)
        assertTrue(g.shotElapsed<=25.0);assertEquals(0.0,g.speed,.001)
    }
    @Test fun lightningDuringLandingTriggersThunderSkip() {
        var time=2_000_000_000_000L;val g=game { time };var struck=false
        repeat(2500) {
            time+=10
            if(!struck && g.landingWindow) { g.lightning();struck=true }
            g.tick(.01)
        }
        assertTrue(struck);assertTrue("Thunder combo was not recorded",g.engagement.discoveries[0]);assertTrue(g.bounces>0)
    }
    @Test fun activeHoverIsNotCutOffAtTwentyFiveSeconds() {
        var time=2_000_000_000_000L;val g=game { time }
        g.abilityProgress.activate(FlightAbility.AIRLIFT,60.0)
        repeat(260) { time+=100;g.tick(.1) }
        assertEquals(Phase.FLYING,g.phase)
        assertTrue(g.shotElapsed>25)
        time+=60_000
        repeat(300) { time+=100;g.tick(.1) }
        assertEquals(Phase.LANDED,g.phase)
    }
}
