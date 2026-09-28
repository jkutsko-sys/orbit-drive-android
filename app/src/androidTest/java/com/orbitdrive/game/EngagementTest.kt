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
        var time=2_000_000_000_000L
        val main = GameEngine(ctx,clock={time})
        main.redeemVoucher("ADMIN")
        val state = main.engagement
        main.abilityProgress.recordBest(100.0)
        main.buyAbilitySkill(AbilitySkills.byId.getValue("ROCKET-R0"))
        main.equipAbility(0,FlightAbility.ROCKET)
        val mainCash = main.cash
        val exp = GameEngine(ctx,true,state,main,clock={time})
        exp.startCharge(); exp.tick(.5); exp.release()
        val before = exp.speed
        exp.useAbility(FlightAbility.ROCKET)
        assertTrue(exp.speed > before)
        val fired = exp.speed
        exp.useAbility(FlightAbility.ROCKET)
        assertEquals(fired,exp.speed,0.0001)
        repeat(500) { time+=50;exp.tick(.05) }
        assertEquals(Phase.LANDED,exp.phase)
        assertEquals(mainCash,GameEngine(ctx).cash,.01)
        assertEquals(0,GameEngine(ctx).launches)
    }
    @Test fun abilityUnlocksAndAirliftWindow() {
        val ctx = isolated()
        var time = 2_000_000_000_000L
        val game = GameEngine(ctx, clock = { time })
        game.equipAbility(0,FlightAbility.ROCKET)
        assertFalse(game.abilityUnlocked(FlightAbility.ROCKET))
        assertNotEquals(FlightAbility.ROCKET,game.engagement.slots[0])
        game.buyAbilitySkill(AbilitySkills.byId.getValue("AIRLIFT-R0"))
        assertFalse(game.abilityUnlocked(FlightAbility.AIRLIFT))
        game.abilityProgress.recordBest(100.0)
        game.buyAbilitySkill(AbilitySkills.byId.getValue("AIRLIFT-R0"))
        game.equipAbility(0,FlightAbility.AIRLIFT)
        game.startCharge(); game.tick(.5); game.release()
        val forward = game.forwardSpeed
        val altitude = game.altitude
        game.useAbility(FlightAbility.AIRLIFT)
        assertEquals(60.0,game.airliftFor,.001)
        assertEquals(forward,game.forwardSpeed,.001)
        assertTrue(game.altitude > altitude)
        repeat(49) { time += 100; game.tick(.1); game.useAbility(FlightAbility.AIRLIFT) }
        assertTrue(game.airliftFor > 55)
        time += 60_000
        game.tick(.1)
        assertEquals(0.0,game.airliftFor,.001)
        assertEquals(0,game.abilityCharges(FlightAbility.AIRLIFT))
        game.useAbility(FlightAbility.AIRLIFT)
        assertEquals(0.0,game.airliftFor,.001)
        val restored = GameEngine(ctx,clock = { time })
        assertTrue(restored.abilityUnlocked(FlightAbility.AIRLIFT))
        assertEquals(0,restored.abilityCharges(FlightAbility.AIRLIFT))
    }
    @Test fun contractsWaitFourHoursAndShopLevelsPersist() {
        val ctx = isolated()
        var clock = 2_000_000_000_000L
        val state = EngagementState(ctx, now = { clock })
        repeat(5) { state.recordShot(150.0,false,0,false,0,false) }
        state.claimContract(0)
        assertEquals(EngagementState.CONTRACT_COOLDOWN_MS,state.contractRemaining(0))
        repeat(5) { state.recordShot(150.0,false,0,false,0,false) }
        assertEquals(0,state.contractProgress[0])
        clock += EngagementState.CONTRACT_COOLDOWN_MS - 1
        state.claimContract(0)
        assertEquals(1,state.contractRounds[0])
        clock++
        repeat(3) { state.recordShot(150.0,true,0,false,0,false) }
        assertEquals(3,state.contractProgress[0])
        state.claimContract(0)
        assertEquals(2,state.contractRounds[0])
        state.grantTicketsForTesting(1000)
        assertTrue(state.tickets >= 1000)
        repeat(5) { state.upgradeFacility(4) }
        val restored = EngagementState(ctx, now = { clock })
        assertEquals(5,restored.facilities[4])
        assertEquals(EngagementState.CONTRACT_COOLDOWN_MS,restored.contractRemaining(0))
        assertEquals(8,restored.facilities.size)
    }
    @Test fun ticketsVoucherIsRepeatableInPreview() {
        val game = GameEngine(isolated())
        val before = game.engagement.tickets
        assertTrue(game.redeemVoucher("tickets").contains("1,000"))
        game.redeemVoucher("TICKETS")
        assertEquals(before + 2000,game.engagement.tickets)
    }

}
