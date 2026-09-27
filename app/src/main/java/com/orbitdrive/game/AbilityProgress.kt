package com.orbitdrive.game

import android.content.Context
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject

internal data class AbilitySkill(val id: String, val ability: FlightAbility, val tier: Int, val name: String, val effect: String, val cost: Int, val parent: String?) {
    val keystone get() = tier in listOf(7,15,23)
    val lane get() = when (tier) { in 1..7 -> 0; in 8..15 -> 1; else -> 2 }
}
internal object AbilitySkills {
    val milestones = listOf(100.0,300.0,1000.0,3000.0,8000.0,20000.0,45000.0,100000.0,300000.0,800000.0,2000000.0,5000000.0,12000000.0,30000000.0,80000000.0)
    val rewards = listOf(2,2,2,3,3,3,4,4,4,5,5,5,6,6,6) // 60 points at Next Star; one full specialization plus supporting paths.
    val nodes: List<AbilitySkill> = ResearchTree.legacyAbilities.filter { it.tier >= 0 }.map { old ->
        val a = old.ability!!; val t = old.tier
        val prefix = AbilityResearch.prefix(a)
        val parent = if (t == 0) null else "$prefix-${if (t in listOf(1,8,16)) 0 else t - 1}"
        val depth = if (t == 0) 0 else if (t < 8) t else if (t < 16) t - 7 else t - 15
        val effect = when {
            t == 0 -> when (a) {
                FlightAbility.LIGHTNING -> "Unlock Lightning: 2 stored charges; one recharges every 45 seconds."
                FlightAbility.AIRLIFT -> "Unlock Air Dribble: tap the range for 60 seconds across shots. 10 minute cooldown."
                FlightAbility.ROCKET -> "Unlock Rocket: 1 charge every 2 minutes; 8 seconds of afterburner."
                FlightAbility.GRAVITY -> "Unlock Gravity: 2 stored charges; one recharges every 90 seconds. 6 second pulse."
            }
            a == FlightAbility.AIRLIFT && t in listOf(2,5,10,14,18,22) -> "+5 seconds to the active window"
            a == FlightAbility.AIRLIFT && t == 12 -> "First activation adds 20 m altitude"
            a == FlightAbility.AIRLIFT && t == 23 -> "Every fifth tap extends the active window by 0.25 seconds, up to 10 bonus seconds per activation"
            a == FlightAbility.ROCKET && t == 7 -> "Afterburner thrust doubles and lasts 4 seconds longer"
            a == FlightAbility.LIGHTNING && t == 23 -> "Each strike doubles its impulse"
            else -> old.effect.replace("flight seconds","real seconds").replace("per shot","maximum stored charges")
        }
        AbilitySkill(old.id,a,t,old.name.removePrefix("KEYSTONE: "),effect,if(t==0) 1 else if(t in listOf(7,15,23)) 4 else if(depth >= 5) 2 else 1,parent)
    }
    val byId = nodes.associateBy { it.id }
    fun lanes(a: FlightAbility) = when(a) {
        FlightAbility.LIGHTNING -> listOf("Thunder", "Stormglass", "Supercell")
        FlightAbility.AIRLIFT -> listOf("Cloudwalker", "Sky Ladder", "Endless Stair")
        FlightAbility.ROCKET -> listOf("Afterburner", "Orbital Injection", "Star Engine")
        FlightAbility.GRAVITY -> listOf("Zero Point", "Orbit Sling", "Singularity Exit")
    }
}

/** Wall-clock timers continue across flights, tabs, app restarts and offline time. */
internal class AbilityProgress(context: Context, private val now: () -> Long = System::currentTimeMillis) {
    private val prefs = context.getSharedPreferences("orbit_abilities_v2", Context.MODE_PRIVATE)
    private val bought = mutableSetOf<String>()
    private val recharge = List(4) { mutableListOf<Long>() }
    private val until = MutableList(4) { 0L }
    private val extension = MutableList(4) { 0.0 }
    var best by mutableDoubleStateOf(0.0); private set
    var migrated by mutableStateOf(false); private set
    var revision by mutableIntStateOf(0); private set
    init {
        val j = runCatching { JSONObject(prefs.getString("save","{}")!!) }.getOrDefault(JSONObject())
        best = j.optDouble("best",0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0
        migrated = j.optBoolean("migrated")
        j.optJSONArray("nodes")?.let { a -> repeat(a.length()) { a.optString(it).takeIf(AbilitySkills.byId::containsKey)?.let(bought::add) } }
        repeat(4) { i ->
            until[i] = j.optJSONArray("until")?.optLong(i) ?: 0L
            extension[i] = j.optJSONArray("extension")?.optDouble(i,0.0) ?: 0.0
            j.optJSONArray("recharge")?.optJSONArray(i)?.let { a -> repeat(a.length()) { recharge[i].add(a.optLong(it)) } }
        }
    }
    val earned get() = AbilitySkills.milestones.indices.filter { best >= AbilitySkills.milestones[it] }.sumOf { AbilitySkills.rewards[it] }
    val spent get() = bought.sumOf { AbilitySkills.byId[it]?.cost ?: 0 }
    val available get() = (earned - spent).coerceAtLeast(0)
    val nextMilestone get() = AbilitySkills.milestones.firstOrNull { best < it }
    fun owns(id: String): Boolean { revision; return id in bought }
    fun unlocked(a: FlightAbility) = owns("${AbilityResearch.prefix(a)}-0")
    fun canBuy(n: AbilitySkill) = !owns(n.id) && available >= n.cost && (n.parent == null || owns(n.parent)) &&
        (!n.keystone || AbilitySkills.nodes.none { it.ability == n.ability && it.keystone && owns(it.id) })
    fun buy(n: AbilitySkill) { if (canBuy(n)) { bought.add(n.id); save() } }
    fun respec() { bought.clear(); save() } // Timers and recharge queues are deliberately retained.
    fun recordBest(value: Double) { if(value.isFinite() && value > best) { best = value; save() } }
    fun markMigrated() { migrated = true; save() }
    fun active(a: FlightAbility) = ((until[a.ordinal] - now()).coerceAtLeast(0L) / 1000.0)
    fun activeAny() = FlightAbility.entries.any { active(it) > 0 }
    fun activate(a: FlightAbility, seconds: Double) { until[a.ordinal] = now() + (seconds * 1000).toLong(); extension[a.ordinal] = 0.0; save() }
    fun extend(a: FlightAbility, seconds: Double, maxBonus: Double) {
        val i = a.ordinal; val add = minOf(seconds, (maxBonus - extension[i]).coerceAtLeast(0.0))
        if (active(a) > 0 && add > 0) { until[i] += (add * 1000).toLong(); extension[i] += add; save() }
    }
    private fun prune(a: FlightAbility) { recharge[a.ordinal].removeAll { it <= now() } }
    fun charges(a: FlightAbility, capacity: Int): Int { revision; prune(a); return (capacity - recharge[a.ordinal].size).coerceAtLeast(0) }
    fun readyIn(a: FlightAbility): Double { prune(a); return ((recharge[a.ordinal].firstOrNull() ?: now()) - now()).coerceAtLeast(0L) / 1000.0 }
    fun consume(a: FlightAbility, capacity: Int, cooldown: Double): Boolean {
        if (charges(a,capacity) <= 0) return false
        val q = recharge[a.ordinal]
        q.add(maxOf(now(),q.lastOrNull() ?: 0L) + (cooldown * 1000).toLong()); save(); return true
    }
    private fun save() {
        prefs.edit().putString("save", JSONObject().apply {
            put("best",best); put("migrated",migrated); put("nodes",JSONArray(bought.toList()))
            put("until",JSONArray(until)); put("extension",JSONArray(extension)); put("recharge",JSONArray(recharge.map { JSONArray(it) }))
        }.toString()).apply(); revision++
    }
}
