package com.orbitdrive.game

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

internal data class Caddy(val name:String,val job:String,val stat:String,val perLevel:Double,val color:Color)
internal class CaddyState(context:Context,private val now:()->Long=System::currentTimeMillis,private val random:Random=Random.Default) {
    companion object { const val PERIOD=4*60*60*1000L; const val XP_PER_LEVEL=125;const val MAX_LEVEL=120 }
    val roster=listOf(
        Caddy("Milo Mulligan","Range veteran","Launch power",.002,Color(0xffe7b877)),
        Caddy("Penny Putts","Club accountant","Shot cash",.003,Color(0xffeaaaac)),
        Caddy("Nimbus Ned","Storm spotter","Lightning impulse",.003,Color(0xffb2bde7)),
        Caddy("Skye Hopper","Air show coach","Air Dribble lift",.0025,Color(0xff8ee6dc)),
        Caddy("Jet Jensen","Rocket mechanic","Rocket impulse",.003,Color(0xffedac73)),
        Caddy("Luna Looper","Orbit navigator","Gravity duration",.0015,Color(0xffc4a0ee)),
        Caddy("Dusty Diggs","Relic hunter","Ascension relic yield",.0015,Color(0xffb3d59b)),
        Caddy("Cal Clockwell","Timing coach","Ability recharge reduction",.0008,Color(0xff8ac7e3))
    )
    private val prefs=context.getSharedPreferences("orbit_caddies_v1",Context.MODE_PRIVATE)
    val xp=mutableStateListOf<Int>().apply { repeat(roster.size) { add(0) } }
    var selected by mutableIntStateOf(-1);private set
    var nextAt by mutableLongStateOf(now());private set
    var lastRecruit by mutableStateOf("");private set
    init {
        val j=runCatching { JSONObject(prefs.getString("save","{}")!!) }.getOrDefault(JSONObject())
        xp.indices.forEach { xp[it]=(j.optJSONArray("xp")?.optInt(it) ?: 0).coerceIn(0,MAX_LEVEL*XP_PER_LEVEL) }
        selected=j.optInt("selected",-1).takeIf { it in xp.indices && xp[it]>=XP_PER_LEVEL } ?: -1
        nextAt=j.optLong("nextAt",now())
    }
    fun level(id:Int) = if(id in xp.indices) xp[id]/XP_PER_LEVEL else 0
    fun passiveFactor(id:Int) = when(level(id)) { in 0..9 -> 0.0;in 10..39 -> .25;in 40..79 -> .6;else -> 1.0 }
    fun bonus(id:Int):Double = if(id !in xp.indices) 0.0 else roster[id].perLevel*level(id)*(if(selected==id) 1.0 else passiveFactor(id))
    fun stored():Int = if(now()<nextAt) 0 else (1+(now()-nextAt)/PERIOD).coerceAtMost(3).toInt()
    fun remaining():Long = (nextAt-now()).coerceAtLeast(0L)
    fun recruit():Int? {
        if(stored()==0) return null
        val time=now()
        // Bank at most three recruits, preserving progress toward the next four-hour refill.
        nextAt=maxOf(nextAt,time-2*PERIOD-(time-nextAt)%PERIOD)+PERIOD
        val id=random.nextInt(roster.size)
        val wasOwned=level(id)>0
        xp[id]=(xp[id]+XP_PER_LEVEL).coerceAtMost(MAX_LEVEL*XP_PER_LEVEL)
        if(selected<0) selected=id
        lastRecruit=if(wasOwned) "${roster[id].name}: +125 XP" else "New caddy: ${roster[id].name}!"
        save();return id
    }
    fun equip(id:Int) { if(level(id)>0) { selected=id;save() } }
    fun recordShot() { if(selected>=0) { xp[selected]=(xp[selected]+5).coerceAtMost(MAX_LEVEL*XP_PER_LEVEL);save() } }
    private fun save() { prefs.edit().putString("save",JSONObject().put("xp",JSONArray(xp.toList())).put("selected",selected).put("nextAt",nextAt).toString()).apply() }
}
