package com.orbitdrive.game

import android.content.Context
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

internal enum class FlightAbility(val title: String, val icon: String, val description: String) {
    LIGHTNING("Lightning", "ϟ", "Stores 2 charges; regenerates one every 45 seconds. Time strikes before landing for Thunder Skip."),
    AIRLIFT("Air Dribble", "↑", "Tap the range during a 60-second active window. 10-minute cooldown; spans multiple shots."),
    ROCKET("Rocket", "↑", "One ignition charge per 2 minutes; 8-second afterburner. Aircraft timing gives Slingshot Launch."),
    GRAVITY("Gravity", "☁", "Stores 2 pulses; regenerates one every 90 seconds. Each pulse lasts 6 seconds.")
}
internal data class Contract(val title: String, val goal: Int, val kind: Int)
internal class EngagementState(context: Context, private val now: () -> Long = System::currentTimeMillis, private val today: () -> LocalDate = { LocalDate.now(ZoneOffset.UTC) }) {
    companion object { const val CONTRACT_COOLDOWN_MS = 4 * 60 * 60 * 1000L; const val MAX_FACILITY_LEVEL = 12 }
    private val prefs = context.getSharedPreferences("orbit_engagement_v1", Context.MODE_PRIVATE)
    var tickets by mutableIntStateOf(0); private set
    val facilities = mutableStateListOf(0, 0, 0, 0, 0, 0, 0, 0)
    val facilityNames = listOf("Clubhouse", "Workshop", "Trophy Room", "Launch Pad", "Tee Pavilion", "Range Lights", "Vault", "Flight Tower")
    val facilityEffects = listOf("+2% shot cash per level", "+2% launch power per level", "+1 ticket per contract per level", "+3% rocket impulse per level", "+1 bonus ticket every three shots per level", "+1 weekly point every four shots per level", "+2% shot cash per level", "+1% launch power per level")
    val slots = mutableStateListOf(FlightAbility.LIGHTNING, FlightAbility.AIRLIFT)
    val contractRounds = mutableStateListOf(0, 0, 0)
    val contractProgress = mutableStateListOf(0, 0, 0)
    val contractReadyAt = mutableStateListOf(0L, 0L, 0L)
    var shotCount by mutableIntStateOf(0); private set
    val discoveries = mutableStateListOf(false, false, false, false)
    val comboNames = listOf("Thunder Skip", "Slingshot Launch", "Double Cargo", "Magnetic Coast")
    val comboHints = listOf("Lightning during the highlighted landing window: a charged rebound.", "Rocket during a plane carry: +35% ignition.", "Rank Storm Battery + Flight Beacon: aircraft adds a second payout ball.", "Rank Feather Field + Rolling Comet: Gravity pulse also removes ground drag.")
    var week by mutableStateOf(""); private set
    var weekPoints by mutableIntStateOf(0); private set
    var trackClaimed by mutableIntStateOf(0); private set
    var expeditionBest by mutableDoubleStateOf(0.0); private set
    var expeditionMedals by mutableIntStateOf(0); private set
    var totalMedals by mutableIntStateOf(0); private set
    var trailUnlocked by mutableIntStateOf(0); private set
    var trailSelected by mutableIntStateOf(0); private set
    var notice by mutableStateOf(""); private set
    val weekRule get() = Math.floorMod(LocalDate.parse(week).toEpochDay() / 7, 3).toInt()
    val expeditionTitle get() = listOf("Lunar Silence", "Spring Circuit", "Storm Runway")[weekRule]
    val expeditionRules get() = listOf("Carbon Slice • low gravity • no lightning", "Carbon Slice • high rebound • slippery surface", "Carbon Slice • aircraft relay • extra lightning")[weekRule]
    val homeTitle get() = when (facilities.sum()) { in 0..11 -> "Backyard Range"; in 12..35 -> "Grandstand Stadium"; in 36..71 -> "Orbital Facility"; else -> "Interstellar Academy" }
    init {
        val j = runCatching { JSONObject(prefs.getString("save", "{}")!!) }.getOrDefault(JSONObject())
        tickets = j.optInt("tickets").coerceAtLeast(0)
        week = j.optString("week", "")
        weekPoints = j.optInt("points").coerceAtLeast(0); trackClaimed = j.optInt("claimed").coerceIn(0, 6)
        expeditionBest = j.optDouble("expBest", 0.0); expeditionMedals = j.optInt("medals").coerceIn(0,3)
        totalMedals = j.optInt("totalMedals").coerceAtLeast(0)
        trailUnlocked = j.optInt("trailUnlocked").coerceIn(0,3); trailSelected = j.optInt("trailSelected").coerceIn(0,trailUnlocked)
        facilities.indices.forEach { facilities[it] = j.optJSONArray("facilities")?.optInt(it)?.coerceIn(0,MAX_FACILITY_LEVEL) ?: 0 }
        for (i in 0..3) discoveries[i] = j.optJSONArray("combos")?.optBoolean(i) ?: false
        shotCount = j.optInt("shotCount").coerceAtLeast(0)
        for (i in 0..2) {
            contractRounds[i] = j.optJSONArray("rounds")?.optInt(i)?.coerceAtLeast(0) ?: 0
            contractProgress[i] = j.optJSONArray("progress")?.optInt(i)?.coerceAtLeast(0) ?: 0
            contractReadyAt[i] = j.optJSONArray("contractReadyAt")?.optLong(i)?.coerceAtLeast(0L) ?: 0L
        }
        j.optJSONArray("slots")?.let { a -> for (i in 0..1) slots[i] = FlightAbility.entries[a.optInt(i).coerceIn(0,3)] }
        if (slots[0] == slots[1]) slots[1] = FlightAbility.entries[(slots[0].ordinal + 1) % 4]
        refreshWeek()
    }
    fun refreshWeek() {
        val key = today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        if (key == week) return
        if (week.isNotEmpty()) grantTrack() // Earned, unclaimed rewards are never lost at rollover.
        week = key; weekPoints = 0; trackClaimed = 0; expeditionBest = 0.0; expeditionMedals = 0
        save()
    }
    fun contract(slot: Int): Contract = when (slot) {
        0 -> if (contractRounds[slot] % 2 == 0) Contract("Complete five shots",5,0) else Contract("Land three perfect strikes",3,1)
        1 -> if (contractRounds[slot] % 2 == 0) Contract("Clear three obstacles",3,2) else Contract("Finish four shots without lightning",4,3)
        else -> if (contractRounds[slot] % 2 == 0) Contract("Drive 5,000 total meters",5000,4) else Contract("Collect three planet bounties",3,5)
    }
    fun claimContract(slot: Int) {
        if (slot !in 0..2 || contractRemaining(slot) > 0) return
        val c = contract(slot)
        if (contractProgress[slot] < c.goal) return
        tickets += contractReward(slot)
        contractRounds[slot]++; contractProgress[slot] = 0; weekPoints += 5
        contractReadyAt[slot] = now() + CONTRACT_COOLDOWN_MS
        notice = "Contract paid! New goal opens in four hours."; save()
    }
    fun contractRemaining(slot: Int) = (contractReadyAt[slot] - now()).coerceAtLeast(0L)
    fun contractReward(slot: Int) = 8 + slot * 4 + facilities[2] + (contractRounds[slot] / 2).coerceAtMost(12)
    fun grantTicketsForTesting(amount: Int) { tickets += amount; notice = "+$amount test tickets"; save() }
    fun recordShot(distance: Double, perfect: Boolean, clears: Int, usedLightning: Boolean, planets: Int, expedition: Boolean) {
        // The active week is refreshed before a shot starts, so a shot crossing midnight stays consistent.
        shotCount++; tickets += 1 + if (shotCount % 3 == 0) facilities[4] else 0
        weekPoints += (if (perfect) 3 else 2) + if (shotCount % 4 == 0) facilities[5] else 0
        if (expedition) {
            expeditionBest = maxOf(expeditionBest, distance)
            val medals = listOf(500.0, 2000.0, 8000.0).count { expeditionBest >= it }
            if (medals > expeditionMedals) {
                tickets += (medals - expeditionMedals) * 15; totalMedals += medals - expeditionMedals
                expeditionMedals = medals; trailUnlocked = maxOf(trailUnlocked, medals)
                notice = "Expedition medal! New trail available."
            }
        } else for (i in 0..2) {
            if (contractRemaining(i) > 0) continue
            val c = contract(i)
            val add = when (c.kind) { 0 -> 1; 1 -> if (perfect) 1 else 0; 2 -> clears; 3 -> if (!usedLightning) 1 else 0; 4 -> distance.coerceAtMost(c.goal.toDouble()).toInt(); else -> planets }
            contractProgress[i] = (contractProgress[i] + add).coerceAtMost(c.goal)
        }
        save()
    }
    fun grantTrack() {
        val earned = (weekPoints / 20).coerceAtMost(6)
        if (earned > trackClaimed) {
            tickets += (earned - trackClaimed) * 12
            trailUnlocked = maxOf(trailUnlocked, earned / 2)
            trackClaimed = earned; notice = "Weekly rewards collected!"; save()
        }
    }
    fun facilityCost(id: Int): Int { val level = facilities[id]; return 20 + level * 12 + level * level * 5 }
    fun upgradeFacility(id: Int) {
        if (id !in facilities.indices || facilities[id] >= MAX_FACILITY_LEVEL || tickets < facilityCost(id)) return
        tickets -= facilityCost(id); facilities[id]++; save()
    }
    fun select(slot: Int, ability: FlightAbility) {
        val other = 1 - slot
        if (slots[other] == ability) slots[other] = slots[slot]
        slots[slot] = ability; save()
    }
    fun discoverCombo(id: Int) { discoveries[id] = true; save() }
    fun chooseTrail(id: Int) { if (id in 0..trailUnlocked) { trailSelected = id; save() } }
    private fun save() {
        prefs.edit().putString("save", JSONObject().apply {
            put("tickets",tickets); put("facilities",JSONArray(facilities.toList())); put("slots",JSONArray(slots.map { it.ordinal }))
            put("rounds",JSONArray(contractRounds.toList())); put("progress",JSONArray(contractProgress.toList())); put("contractReadyAt",JSONArray(contractReadyAt.toList()))
            put("shotCount",shotCount); put("combos",JSONArray(discoveries.toList()))
            put("week",week); put("points",weekPoints); put("claimed",trackClaimed); put("expBest",expeditionBest); put("medals",expeditionMedals)
            put("totalMedals",totalMedals); put("trailUnlocked",trailUnlocked); put("trailSelected",trailSelected)
        }.toString()).apply()
    }
}
