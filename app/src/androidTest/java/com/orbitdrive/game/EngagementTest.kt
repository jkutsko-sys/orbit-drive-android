package com.orbitdrive.game

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.util.UUID

class EngagementTest {
    private fun isolated(): Context {
        val id = UUID.randomUUID().toString()
        return object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getSharedPreferences(name: String, mode: Int) = baseContext.getSharedPreferences("test_${id}_$name", mode)
        }
    }
    @Test fun contractsClaimOnceAndFacilitiesPersist() {
        val ctx = isolated()
        val state = EngagementState(ctx)
        repeat(5) { state.recordShot(150.0,false,1,false,0,false) }
        val before = state.tickets
        state.claimContract(0)
        assertEquals(before + 8, state.tickets)
        state.claimContract(0)
        assertEquals(before + 8, state.tickets)
        repeat(20) { state.recordShot(100.0,false,0,false,0,false) }
        state.upgradeFacility(1)
        assertEquals(1, EngagementState(ctx).facilities[1])
        state.select(0, FlightAbility.AIRLIFT)
        assertNotEquals(state.slots[0], state.slots[1])
    }
    @Test fun weeklyCatchupAutoClaimsAndExpeditionRewardsDoNotDuplicate() {
        val ctx = isolated()
        var date = LocalDate.of(2026,9,26)
        val state = EngagementState(ctx) { date }
        repeat(20) { state.recordShot(100.0,false,0,false,0,false) }
        val before = state.tickets
        date = date.plusDays(7); state.refreshWeek()
        assertEquals(before + 24, state.tickets)
        assertEquals(0,state.weekPoints)
        state.recordShot(8100.0,false,0,false,0,true)
        assertEquals(3,state.expeditionMedals)
        val after = state.tickets
        state.recordShot(8100.0,false,0,false,0,true)
        assertEquals(after + 1,state.tickets)
        assertEquals(3,EngagementState(ctx) { date }.trailUnlocked)
    }
    @Test fun rocketHasOneUseAndExpeditionLeavesMainSaveAlone() {
        val ctx = isolated()
        val main = GameEngine(ctx)
        main.redeemVoucher("ADMIN")
        val state = main.engagement
        main.buyNode(ResearchTree.byId.getValue(AbilityResearch.HUB))
        main.buyNode(ResearchTree.byId.getValue("ROCKET-0"))
        main.equipAbility(0,FlightAbility.ROCKET)
        val mainCash = main.cash
        val exp = GameEngine(ctx,true,state,main)
        exp.startCharge(); exp.tick(.5); exp.release()
        val before = exp.speed
        exp.useAbility(FlightAbility.ROCKET)
        assertTrue(exp.speed > before)
        val fired = exp.speed
        exp.useAbility(FlightAbility.ROCKET)
        assertEquals(fired,exp.speed,0.0001)
        repeat(490) { exp.tick(.05) }
        assertEquals(Phase.LANDED,exp.phase)
        assertEquals(mainCash,GameEngine(ctx).cash,.01)
        assertEquals(0,GameEngine(ctx).launches)
    }
    @Test fun abilityUnlocksAndAirliftWindow() {
        val ctx = isolated()
        val game = GameEngine(ctx)
        game.redeemVoucher("ADMIN")
        game.equipAbility(0,FlightAbility.ROCKET)
        assertFalse(game.abilityUnlocked(FlightAbility.ROCKET))
        assertNotEquals(FlightAbility.ROCKET,game.engagement.slots[0])
        game.buyNode(ResearchTree.byId.getValue("AIRLIFT-0"))
        assertFalse(game.abilityUnlocked(FlightAbility.AIRLIFT))
        game.buyNode(ResearchTree.byId.getValue(AbilityResearch.HUB))
        game.buyNode(ResearchTree.byId.getValue("AIRLIFT-0"))
        assertTrue(game.abilityUnlocked(FlightAbility.AIRLIFT))
        game.equipAbility(0,FlightAbility.AIRLIFT)
        game.startCharge(); game.tick(.5); game.release()
        val forward = game.forwardSpeed
        val altitude = game.altitude
        game.useAbility(FlightAbility.AIRLIFT)
        assertEquals(5.0,game.airliftFor,.001)
        assertEquals(forward,game.forwardSpeed,.001)
        assertTrue(game.altitude > altitude)
        repeat(49) { game.tick(.1); game.useAbility(FlightAbility.AIRLIFT) }
        assertTrue(game.airliftFor > 0)
        game.tick(.11)
        assertEquals(0.0,game.airliftFor,.001)
        assertEquals(0,game.abilityCharges(FlightAbility.AIRLIFT))
        game.useAbility(FlightAbility.AIRLIFT)
        assertEquals(0.0,game.airliftFor,.001)
        assertTrue(GameEngine(ctx).abilityUnlocked(FlightAbility.AIRLIFT))
    }

}
