package com.orbitdrive.game

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID

class AbilityProgressTest {
    private fun isolated(): Context {
        val key = UUID.randomUUID().toString()
        return object: ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getSharedPreferences(name:String,mode:Int) = baseContext.getSharedPreferences("ability_${key}_$name",mode)
        }
    }
    @Test fun milestonesAreOnceOnlyAndKeystonesAreExclusive() {
        val ctx=isolated(); val p=AbilityProgress(ctx)
        p.recordBest(99.0); assertEquals(0,p.earned)
        p.recordBest(100.0); assertEquals(2,p.earned)
        p.recordBest(100.0); assertEquals(2,p.earned)
        p.recordBest(80_000_000.0); assertEquals(60,p.earned)
        assertEquals(96,AbilitySkills.nodes.size)
        assertEquals(96,AbilitySkills.nodes.map { it.id }.toSet().size)
        assertTrue(AbilitySkills.nodes.all { it.parent == null || it.parent in AbilitySkills.byId })
        (0..7).forEach { p.buy(AbilitySkills.byId.getValue("LIGHTNING-$it")) }
        (8..14).forEach { p.buy(AbilitySkills.byId.getValue("LIGHTNING-$it")) }
        assertTrue(p.owns("LIGHTNING-7"))
        assertFalse(p.canBuy(AbilitySkills.byId.getValue("LIGHTNING-15")))
        val before=p.available
        p.buy(AbilitySkills.byId.getValue("LIGHTNING-15"));assertEquals(before,p.available)
        assertEquals(p.spent,AbilityProgress(ctx).spent)
        p.respec();assertEquals(60,p.available)
    }
    @Test fun chargesRechargeSequentiallyPersistAndRespecDoesNotRefill() {
        val ctx=isolated();var time=2_000_000_000_000L
        val p=AbilityProgress(ctx) { time }
        assertTrue(p.consume(FlightAbility.LIGHTNING,2,45.0))
        assertTrue(p.consume(FlightAbility.LIGHTNING,2,45.0))
        assertFalse(p.consume(FlightAbility.LIGHTNING,2,45.0))
        p.respec();assertEquals(0,p.charges(FlightAbility.LIGHTNING,2))
        var restored=AbilityProgress(ctx) { time }
        assertEquals(0,restored.charges(FlightAbility.LIGHTNING,2))
        time += 45_000
        assertEquals(1,restored.charges(FlightAbility.LIGHTNING,2))
        time += 45_000
        assertEquals(2,restored.charges(FlightAbility.LIGHTNING,2))
        p.activate(FlightAbility.AIRLIFT,60.0)
        time += 10_000;restored=AbilityProgress(ctx) { time }
        assertEquals(50.0,restored.active(FlightAbility.AIRLIFT),.001)
        time += 50_000;assertEquals(0.0,restored.active(FlightAbility.AIRLIFT),.001)
    }
    @Test fun migrationRefundsOnlyOnceAndAscensionCurrencyGrantsNoPower() {
        val ctx=isolated();val prefs=ctx.getSharedPreferences("orbit_drive_v1",0)
        val ids=listOf("ABILITY-HUB","LIGHTNING-0","LIGHTNING-1")
        prefs.edit().putString("save",JSONObject().put("cash",10).put("relics",999).put("best",45_000.0)
            .put("nodes",JSONArray(ids)).toString()).commit()
        val game=GameEngine(ctx)
        val refunded=ResearchTree.legacyAbilities.filter { it.id in ids }.sumOf { it.cost }
        assertEquals(10+refunded,game.cash,.01)
        assertEquals(1.0,game.multiplier,.0001)
        assertEquals(19,game.abilityProgress.earned)
        assertEquals(game.cash,GameEngine(ctx).cash,.01)
        assertEquals(34,game.ascensionRelics.size)
        assertEquals(34,game.ascensionRelics.map { it.name }.toSet().size)
    }
    @Test fun electricBuildUsesChargeAndTeeSceneResetsAfterFlight() {
        val ctx=isolated();var time=2_000_000_000_000L
        ctx.getSharedPreferences("orbit_drive_v1",0).edit().putString("save",JSONObject().put("club",14).put("equippedClub",14).toString()).commit()
        val game=GameEngine(ctx,clock={time})
        game.abilityProgress.recordBest(80_000_000.0)
        game.buyAbilitySkill(AbilitySkills.byId.getValue("LIGHTNING-0"))
        (8..15).forEach { game.buyAbilitySkill(AbilitySkills.byId.getValue("LIGHTNING-$it")) }
        game.startCharge();game.tick(.5);game.release()
        game.lightning();assertTrue(game.electrifiedFor>0)
        val charges=game.abilityCharges(FlightAbility.LIGHTNING)
        repeat(490) { time+=50;game.tick(.05) }
        assertEquals(Phase.LANDED,game.phase)
        assertTrue(game.distance>0)
        assertEquals(0.0,game.rangeSceneDistance,0.0)
        game.startCharge();assertEquals(0.0,game.rangeSceneDistance,0.0)
        game.release();assertEquals(charges,game.abilityCharges(FlightAbility.LIGHTNING))
    }
    @Test fun relicBonusesAreAppliedAndCapped() {
        val ctx=isolated()
        val levels=MutableList(34){0};levels[11]=999;levels[12]=6;levels[17]=10;levels[18]=10;levels[24]=6;levels[28]=10;levels[30]=6
        ctx.getSharedPreferences("orbit_drive_v1",0).edit().putString("save",JSONObject().put("relicLevels",JSONArray(levels)).toString()).commit()
        val game=GameEngine(ctx)
        assertEquals(10,game.relicLevel(11))
        assertEquals(36.0,game.abilityCooldown(FlightAbility.LIGHTNING),.001)
        assertEquals(4,game.abilityCapacity(FlightAbility.LIGHTNING))
        assertEquals(70.0,game.airliftDuration,.001)
        assertEquals(480.0,game.abilityCooldown(FlightAbility.AIRLIFT),.001)
        assertEquals(3,game.abilityCapacity(FlightAbility.ROCKET))
        assertEquals(4,game.abilityCapacity(FlightAbility.GRAVITY))
        assertEquals(8.0,game.abilityDuration(FlightAbility.GRAVITY),.001)
    }
}
