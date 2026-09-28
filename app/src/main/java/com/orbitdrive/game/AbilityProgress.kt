package com.orbitdrive.game

import android.content.Context
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject

internal data class AbilitySkill(val id: String, val ability: FlightAbility, val tier: Int, val name: String, val effect: String, val icon: String) {
    val row get() = if(tier==0) 0 else (tier-1)/3+1
    val lane get() = if(tier==0) 1 else (tier-1)%3
    val keystone get() = row==3
    val maxRank get() = if(tier==0) 1 else if(keystone) 5 else 10
    val requiredLevel get() = when(row) { 2 -> 8; 3 -> 18; else -> 0 }
    val parent get() = if(tier==0) null else "${AbilityResearch.prefix(ability)}-R${if(row==1) 0 else tier-3}"
    val parentRank get() = if(row>1) 3 else 1
    fun costAt(rank: Int) = if(tier==0) 1 else if(keystone) 3+rank/2 else 1+rank/3
}
internal object AbilitySkills {
    val milestones = listOf(100.0,300.0,1000.0,3000.0,8000.0,20000.0,45000.0,100000.0,300000.0,800000.0,2000000.0,5000000.0,12000000.0,30000000.0,80000000.0)
    val rewards = listOf(2,2,2,3,3,3,4,4,4,5,5,5,6,6,6)
    private val specs = listOf(
        listOf(
            Triple("Stormcaller","Unlock Lightning: two stored charges, 45-second recharge.","storm"),
            Triple("Arc Dynamo","+12% strike impulse per rank.","bolt"),
            Triple("Stormglass","Strikes electrify the ball for 1 + 0.6 seconds per rank, reducing friction.","orb"),
            Triple("Storm Vault","2% shorter recharge per rank; +1 stored charge at ranks 3 and 7.","battery"),
            Triple("Chain Reaction","A delayed echo adds 20% + 5% per rank of the first strike's impulse after 0.45 seconds.","chain"),
            Triple("Groundbreaker","Electrification destroys obstacles. Each destruction grants +2% forward speed and +20 cash per rank.","shatter"),
            Triple("Cloud Harvest","+10 cash per rank per strike; shots using Lightning earn +2% cash per rank.","coins"),
            Triple("Thunderheart","Each preceding strike this shot adds +15% impulse per rank, counting up to three earlier strikes.","heart"),
            Triple("Living Circuit","The first three bounces while electrified each grant +3% forward speed per rank.","bounce"),
            Triple("Rolling Thunder","Every third strike this shot advances Lightning recharge by 10% of its interval per rank.","clock")
        ),
        listOf(
            Triple("Sky Dribble","Unlock Air Dribble: 60 seconds across shots, 10-minute cooldown.","cloud"),
            Triple("Sky Juggler","+2 altitude per tap per rank.","bounce"),
            Triple("Breathkeeper","+4 seconds active duration per rank.","clock"),
            Triple("Feather Rhythm","Every third tap adds 4 + 3 altitude per rank, and +1 forward speed per rank.","rhythm"),
            Triple("Cloudwalker","8% less gravity per rank during Air Dribble, up to 80%.","cloud"),
            Triple("Aerial Acrobat","Every fifth tap adds an 8 m altitude burst per rank.","wings"),
            Triple("Wind Purse","Every third tap grants +5 cash per rank. Shots ending during Air Dribble earn +2% cash per rank.","coins"),
            Triple("Endless Stair","Every fifth tap extends the window by 0.25s per rank, capped at 2 bonus seconds per rank per activation.","stairs"),
            Triple("Sky Spring","The first three ground bounces during Air Dribble gain a minimum of 25 + 10 upward speed per rank.","spring"),
            Triple("Tailwind Ballet","Every tenth tap adds +1.5% forward speed per rank, up to five bursts per shot.","wind")
        ),
        listOf(
            Triple("Ignition Key","Unlock Rocket: one stored charge, 120-second recharge and 8-second burn.","rocket"),
            Triple("Pressure Chamber","+12% ignition impulse per rank.","flame"),
            Triple("Long Burn","+1 second of afterburner per rank.","clock"),
            Triple("Fuel Depot","2% shorter recharge per rank; +1 stored charge at ranks 3 and 7.","battery"),
            Triple("Pilot Coupling","Ignition during aircraft carry gains another +10% impulse per rank on top of the normal 35% timing bonus.","wings"),
            Triple("Ion Trail","Ignition electrifies for 0.7s per rank; afterburner ignores 8% of air drag per rank.","orb"),
            Triple("High Staging","Ignition above 100 m gains +5% impulse per rank. Shots using Rocket earn +2% cash per rank.","stairs"),
            Triple("Star Engine","Afterburner thrust rises by 40% per rank.","star"),
            Triple("Sky Harpoon","Ignition converts 10% + 8% per rank of falling speed into forward speed, capped at 200 per rank.","hook"),
            Triple("Booster Separation","When a burn ends in flight, gain +8% forward speed per rank and at least 20 upward speed per rank.","shatter")
        ),
        listOf(
            Triple("Gravity Switch","Unlock Gravity: two stored pulses, 90-second recharge, 6-second duration.","orbit"),
            Triple("Orbit Field","+0.6 seconds of pulse duration per rank.","clock"),
            Triple("Tidal Turn","Activation grants +4 altitude per rank and at least 20 + 4 upward speed per rank.","wind"),
            Triple("Moon Reservoir","2% shorter recharge per rank; +1 stored charge at ranks 3 and 7.","battery"),
            Triple("Vacuum Sail","10% less air drag per rank during Gravity, reaching zero drag at rank 10.","wings"),
            Triple("Sling Arc","Activation converts 8% of falling velocity per rank to forward speed, capped at 100 per rank.","hook"),
            Triple("Bounty Orbit","Planet bounties collected during Gravity pay +6% per rank.","coins"),
            Triple("Zero Point","Gravity becomes zero during a pulse; +0.5 seconds of duration per rank.","orb"),
            Triple("Event Horizon","When a pulse ends in flight, forward speed increases by 5% per rank.","star"),
            Triple("Moonwalk","Ground friction becomes zero during Gravity. The first three ground bounces gain at least 15 upward speed per rank.","bounce")
        )
    )
    val nodes = FlightAbility.entries.flatMap { a -> specs[a.ordinal].mapIndexed { i,s -> AbilitySkill("${AbilityResearch.prefix(a)}-R$i",a,i,s.first,s.second,s.third) } }
    val byId = nodes.associateBy { it.id }
    fun lanes(a: FlightAbility) = when(a) {
        FlightAbility.LIGHTNING -> listOf("Thunder", "Stormglass", "Stormcraft")
        FlightAbility.AIRLIFT -> listOf("Cloudwalker", "Acrobat", "Tailwind")
        FlightAbility.ROCKET -> listOf("Thrust", "Ion flight", "Staging")
        FlightAbility.GRAVITY -> listOf("Vacuum", "Slingshot", "Moonwalk")
    }
}

/** Wall-clock timers continue across flights, tabs, app restarts and offline time. */
internal class AbilityProgress(context: Context, private val now: () -> Long = System::currentTimeMillis) {
    private val prefs = context.getSharedPreferences("orbit_abilities_v2", Context.MODE_PRIVATE)
    private val ranks = mutableStateMapOf<String,Int>()
    var buildRefunded by mutableStateOf(false); private set
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
        if(j.optInt("schema") >= 3) {
            val saved = j.optJSONObject("ranks") ?: JSONObject()
            AbilitySkills.nodes.forEach { ranks[it.id] = saved.optInt(it.id).coerceIn(0,it.maxRank) }
        } else buildRefunded = (j.optJSONArray("nodes")?.length() ?: 0) > 0
        repeat(4) { i ->
            until[i] = j.optJSONArray("until")?.optLong(i) ?: 0L
            extension[i] = j.optJSONArray("extension")?.optDouble(i,0.0) ?: 0.0
            j.optJSONArray("recharge")?.optJSONArray(i)?.let { a -> repeat(a.length()) { recharge[i].add(a.optLong(it)) } }
        }
    }
    val earned get() = AbilitySkills.milestones.indices.filter { best >= AbilitySkills.milestones[it] }.sumOf { AbilitySkills.rewards[it] }
    val spent get() = AbilitySkills.nodes.sumOf { n -> (0 until rank(n.id)).sumOf(n::costAt) }
    val available get() = (earned - spent).coerceAtLeast(0)
    val nextMilestone get() = AbilitySkills.milestones.firstOrNull { best < it }
    fun rank(id: String) = ranks[id] ?: 0
    fun rank(a: FlightAbility, index: Int) = rank("${AbilityResearch.prefix(a)}-R$index")
    fun treeLevel(a: FlightAbility) = AbilitySkills.nodes.filter { it.ability==a }.sumOf { rank(it.id) }
    fun owns(id: String) = rank(id)>0
    fun unlocked(a: FlightAbility) = rank(a,0)>0
    fun cost(n: AbilitySkill) = n.costAt(rank(n.id))
    fun canBuy(n: AbilitySkill) = n.id in AbilitySkills.byId && rank(n.id)<n.maxRank && available>=cost(n) && treeLevel(n.ability)>=n.requiredLevel &&
        (n.parent==null || rank(n.parent!!)>=n.parentRank) &&
        (!n.keystone || AbilitySkills.nodes.none { it.ability==n.ability && it.keystone && it.id!=n.id && owns(it.id) })
    fun buy(n: AbilitySkill) { if(canBuy(n)) { ranks[n.id]=rank(n.id)+1;buildRefunded=false;save() } }
    fun respec() { ranks.clear();save() }
    fun advanceRecharge(a: FlightAbility,seconds: Double) { val q=recharge[a.ordinal];q.indices.forEach { q[it]-=(seconds*1000).toLong() };prune(a);save() }
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
            put("schema",3);put("best",best); put("migrated",migrated); put("ranks",JSONObject(ranks.toMap()))
            put("until",JSONArray(until)); put("extension",JSONArray(extension)); put("recharge",JSONArray(recharge.map { JSONArray(it) }))
        }.toString()).apply(); revision++
    }
}
