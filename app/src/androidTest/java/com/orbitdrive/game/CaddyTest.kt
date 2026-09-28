package com.orbitdrive.game
import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

class CaddyTest {
    private fun isolated():Context {
        val id=UUID.randomUUID().toString()
        return object:ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getSharedPreferences(name:String,mode:Int)=baseContext.getSharedPreferences("caddy_${id}_$name",mode)
        }
    }
    @Test fun freeRecruitTimerBanksThreeAndPersists() {
        val ctx=isolated();var time=2_000_000_000_000L
        val c=CaddyState(ctx,{time},Random(1))
        assertEquals(1,c.stored());assertNotNull(c.recruit());assertEquals(0,c.stored())
        assertNull(c.recruit());assertEquals(0,CaddyState(ctx,{time}).stored())
        time+=CaddyState.PERIOD-1;assertEquals(0,c.stored())
        time++;assertEquals(1,c.stored())
        time+=CaddyState.PERIOD*10;assertEquals(3,c.stored())
        repeat(3) { assertNotNull(c.recruit()) }
        assertNull(c.recruit());assertEquals(500,c.xp.sum())
        val selected=c.selected;repeat(25) { c.recordShot() }
        assertEquals(c.xp[selected],CaddyState(ctx,{time}).xp[selected])
    }
    @Test fun passiveMilestonesDoNotDoubleCountEquippedBonus() {
        val ctx=isolated();val xp=MutableList(8){0};xp[0]=10*125;xp[1]=125
        val prefs=ctx.getSharedPreferences("orbit_caddies_v1",0)
        prefs.edit().putString("save",JSONObject().put("xp",JSONArray(xp)).put("selected",1).toString()).commit()
        var c=CaddyState(ctx);assertEquals(.005,c.bonus(0),.000001)
        c.equip(0);assertEquals(.02,c.bonus(0),.000001)
        xp[0]=80*125
        prefs.edit().putString("save",JSONObject().put("xp",JSONArray(xp)).put("selected",1).toString()).commit()
        c=CaddyState(ctx);assertEquals(.16,c.bonus(0),.000001)
        c.equip(0);assertEquals(.16,c.bonus(0),.000001)
    }
    @Test fun tapAssistIsLimitedAndGolfersAreSequential() {
        val ctx=isolated();var time=2_000_000_000_000L
        val g=GameEngine(ctx,clock={time});g.caddies.recruit()
        g.startCharge();g.tick(.5);g.release()
        val before=g.forwardSpeed;g.tapRange();assertTrue(g.forwardSpeed>before)
        val once=g.forwardSpeed;g.tapRange();assertEquals(once,g.forwardSpeed,.000001)
        repeat(4) { time+=2000;g.tapRange() }
        val five=g.forwardSpeed;time+=2000;g.tapRange();assertEquals(five,g.forwardSpeed,.000001)
        assertEquals(11,g.golfers.size)
        assertTrue(g.golfers.zipWithNext().all { (a,b) -> b.cost>a.cost && b.power>a.power })
        g.buyGolfer(10);assertEquals(0,g.selectedGolfer)
    }
}
