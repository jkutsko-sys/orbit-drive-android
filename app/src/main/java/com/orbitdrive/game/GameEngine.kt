package com.orbitdrive.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

internal data class Club(val name: String, val cost: Double, val swing: Double, val loft: Double, val smash: Double)
internal data class Planet(val name: String, val distance: Double, val hp: Double, val color: Color)
internal enum class Tech(val title: String, val description: String, val baseCost: Double, val max: Int) {
    POWER("Power", "Faster launches and greater planet damage", 20.0, 15),
    GRAVITY("Low Gravity", "More hang time", 90.0, 15),
    BOUNCE("Bounce", "Higher rebounds", 60.0, 15),
    FRICTION("Low Friction", "Roll farther", 45.0, 15),
    LIGHTNING("Lightning", "Tap during flight for a velocity surge", 180.0, 15),
    PLANES("Aircraft", "One midair carry per shot", 260.0, 8)
}
internal enum class Phase { READY, CHARGING, FLYING, LANDED }

internal class GameEngine(context: Context) {
    private val prefs = context.getSharedPreferences("orbit_drive_v1", Context.MODE_PRIVATE)
    val clubs = listOf(
        Club("Bent Starter", 0.0, 35.0, 31.0, 1.12), Club("Steel Driver", 350.0, 55.0, 25.0, 1.31),
        Club("Titanium Cannon", 3000.0, 82.0, 21.0, 1.48), Club("Plasma Wood", 35000.0, 130.0, 18.0, 1.72),
        Club("Starbreaker", 500000.0, 210.0, 15.0, 2.05)
    )
    val planets = listOf(
        Planet("Moon", 8000.0, 160.0, Color(0xffbec6d1)), Planet("Mars", 45000.0, 450.0, Color(0xfff98b64)),
        Planet("Jupiter", 300000.0, 1400.0, Color(0xffd8a87c)), Planet("Saturn", 2000000.0, 5000.0, Color(0xffead999)),
        Planet("Neptune", 12000000.0, 18000.0, Color(0xff648bff)), Planet("Next Star", 80000000.0, 65000.0, Color(0xff87ffc4))
    )
    var cash by mutableStateOf(0.0); private set
    var lifetimeDistance by mutableStateOf(0.0); private set
    var bestDistance by mutableStateOf(0.0); private set
    var launches by mutableStateOf(0); private set
    var ownedClub by mutableStateOf(0); private set
    var relics by mutableStateOf(0); private set
    var ascensions by mutableStateOf(0); private set
    private val clubLevels = MutableList(clubs.size) { 0 }
    private val techLevels = MutableList(Tech.entries.size) { 0 }
    private val damage = MutableList(planets.size) { 0.0 }
    private val destroyed = MutableList(planets.size) { false }
    var revision by mutableStateOf(0); private set
    var distance by mutableStateOf(0.0); private set
    var altitude by mutableStateOf(0.0); private set
    var speed by mutableStateOf(0.0); private set
    var charge by mutableStateOf(0.0); private set
    var phase by mutableStateOf(Phase.READY); private set
    var message by mutableStateOf("Hold LAUNCH, release near full power"); private set
    var lightningCharges by mutableStateOf(0); private set
    var planetHP by mutableStateOf(0.0); private set
    var earned by mutableStateOf(0.0); private set
    var combo by mutableStateOf(0); private set
    private var vx = 0.0; private var vy = 0.0; private var flightTime = 0.0
    private var bounceCount = 0; private var chargeDirection = 1.0; private var planeUsed = false
    private var previousDistance = 0.0

    init { restore() }
    val club get() = clubs[ownedClub]
    val multiplier get() = 1.35.pow(relics)
    val nextPlanetIndex get() = planets.indices.firstOrNull { !destroyed[it] }
    val nextPlanet get() = nextPlanetIndex?.let { planets[it] }
    val potentialRelics get() = max(0, log10(max(1.0, lifetimeDistance / 10000)).toInt())
    val ascendAvailable get() = bestDistance >= 45000 && potentialRelics > 0 && phase != Phase.FLYING
    fun level(tech: Tech) = techLevels[tech.ordinal]
    fun clubLevel(id: Int) = clubLevels[id]
    fun isDestroyed(id: Int) = destroyed[id]
    fun techCost(tech: Tech) = tech.baseCost * 1.65.pow(level(tech))
    fun clubUpgradeCost() = (40 + club.cost * 0.28) * 1.6.pow(clubLevels[ownedClub])
    fun format(value: Double): String = when {
        value >= 1e9 -> "%.2fB".format(value / 1e9)
        value >= 1e6 -> "%.2fM".format(value / 1e6)
        value >= 1000 -> "%.1fK".format(value / 1000)
        else -> "%.0f".format(value)
    }
    fun startCharge() {
        if (phase != Phase.READY && phase != Phase.LANDED) return
        phase = Phase.CHARGING; charge = 0.0; chargeDirection = 1.0; message = "Release to swing"
    }
    fun release() {
        if (phase != Phase.CHARGING) return
        phase = Phase.FLYING; distance = 0.0; previousDistance = 0.0; altitude = 1.0; flightTime = 0.0
        bounceCount = 0; combo = 0; earned = 0.0; planeUsed = false
        lightningCharges = if (level(Tech.LIGHTNING) > 0) 1 + level(Tech.LIGHTNING) / 5 else 0
        planetHP = nextPlanetIndex?.let { (planets[it].hp - damage[it]).coerceAtLeast(0.0) } ?: 0.0
        val launchSpeed = (club.swing * 1.15.pow(clubLevels[ownedClub]) + level(Tech.POWER) * 8) * club.smash * (0.28 + 0.72 * charge) * multiplier
        val angle = Math.toRadians(club.loft + level(Tech.GRAVITY) * 0.4)
        vx = launchSpeed * cos(angle); vy = launchSpeed * sin(angle); speed = launchSpeed
        launches++; save(); message = if (charge > 0.85) "PERFECT STRIKE!" else "Ball away!"
    }
    fun lightning() {
        if (phase != Phase.FLYING || lightningCharges <= 0) return
        lightningCharges--
        val impulse = 35 + level(Tech.LIGHTNING) * 18
        vx += impulse * multiplier; vy += impulse * 0.4
        message = "⚡ LIGHTNING BOOST +$impulse"
    }
    fun tick(step: Double) {
        val dt = step.coerceIn(0.0, 0.05)
        if (phase == Phase.CHARGING) {
            charge += chargeDirection * dt * 0.78
            if (charge >= 1) { charge = 1.0; chargeDirection = -1.0 }
            if (charge <= 0) { charge = 0.0; chargeDirection = 1.0 }
            return
        }
        if (phase != Phase.FLYING) return
        repeat(4) {
            if (phase != Phase.FLYING) return@repeat
            val h = dt / 4
            flightTime += h; previousDistance = distance
            vy -= 45 / (1 + level(Tech.GRAVITY) * 0.24) * h
            vx *= (1 - max(0.00005, 0.0016 - level(Tech.FRICTION) * 0.000075) * h).coerceAtLeast(0.0)
            distance += vx * h; altitude += vy * h
            val index = nextPlanetIndex
            if (index != null && previousDistance < planets[index].distance && distance >= planets[index].distance) {
                val planet = planets[index]
                val hit = max(1.0, speed * (1 + level(Tech.POWER) * 0.18) * multiplier)
                damage[index] = (damage[index] + hit).coerceAtMost(planet.hp)
                planetHP = planet.hp - damage[index]
                if (planetHP <= 0) {
                    destroyed[index] = true; combo++
                    val bounty = planet.hp * 3 * multiplier
                    earned += bounty; cash += bounty
                    message = "${planet.name} shattered! +$${format(bounty)}"
                    planetHP = nextPlanetIndex?.let { planets[it].hp - damage[it] } ?: 0.0
                } else { message = "${planet.name} impact! ${format(planetHP)} HP left"; vx *= 0.83 }
                save()
            }
            if (!planeUsed && level(Tech.PLANES) > 0 && flightTime > 2.5 && altitude > 5) {
                planeUsed = true; vx += (70 + level(Tech.PLANES) * 30) * multiplier
                vy = max(vy, 35.0 + level(Tech.PLANES) * 8); message = "✈ Aircraft carry!"
            }
            if (altitude <= 0) {
                altitude = 0.0; bounceCount++
                val restitution = min(0.87, 0.34 + level(Tech.BOUNCE) * 0.035)
                if (abs(vy) > 11 && bounceCount < 28) {
                    vy = abs(vy) * restitution
                    vx *= max(0.6, 0.82 + level(Tech.FRICTION) * 0.012)
                } else {
                    vy = 0.0; vx *= (1 - (0.7 / (1 + level(Tech.FRICTION) * 0.35)) * h).coerceAtLeast(0.0)
                    if (vx < 2 || flightTime > 240) finish()
                }
            }
            speed = hypot(vx, vy)
            if (flightTime > 240) finish()
        }
    }
    private fun finish() {
        if (phase != Phase.FLYING) return
        phase = Phase.LANDED; speed = 0.0; altitude = 0.0
        val payout = max(1.0, distance * (0.13 + combo * 0.03) * multiplier)
        earned += payout; cash += payout; lifetimeDistance += distance; bestDistance = max(bestDistance, distance)
        message = "${format(distance)} m • +$${format(earned)}"; save()
    }
    fun buyTech(tech: Tech) {
        val price = techCost(tech)
        if (level(tech) >= tech.max || cash < price) return
        cash -= price; techLevels[tech.ordinal]++; save()
    }
    fun buyClub(id: Int) {
        if (id != ownedClub + 1 || id !in clubs.indices || cash < clubs[id].cost) return
        cash -= clubs[id].cost; ownedClub = id; save()
    }
    fun upgradeClub() {
        val price = clubUpgradeCost()
        if (cash < price || clubLevels[ownedClub] >= 30) return
        cash -= price; clubLevels[ownedClub]++; save()
    }
    fun ascend() {
        if (!ascendAvailable) return
        val gained = potentialRelics; relics += gained; ascensions++
        cash = 0.0; lifetimeDistance = 0.0; bestDistance = 0.0; launches = 0; ownedClub = 0
        clubLevels.indices.forEach { clubLevels[it] = 0 }; techLevels.indices.forEach { techLevels[it] = 0 }
        damage.indices.forEach { damage[it] = 0.0; destroyed[it] = false }
        phase = Phase.READY; message = "Ascended! +$gained permanent relics"; save()
    }
    private fun save() {
        val json = JSONObject().apply {
            put("cash", cash); put("lifetime", lifetimeDistance); put("best", bestDistance); put("launches", launches)
            put("club", ownedClub); put("relics", relics); put("ascensions", ascensions)
            put("clubLevels", JSONArray(clubLevels)); put("techLevels", JSONArray(techLevels))
            put("damage", JSONArray(damage)); put("destroyed", JSONArray(destroyed))
        }
        prefs.edit().putString("save", json.toString()).apply(); revision++
    }
    private fun restore() {
        val json = runCatching { JSONObject(prefs.getString("save", "{}") ?: "{}") }.getOrDefault(JSONObject())
        cash = json.optDouble("cash").coerceAtLeast(0.0); lifetimeDistance = json.optDouble("lifetime").coerceAtLeast(0.0)
        bestDistance = json.optDouble("best").coerceAtLeast(0.0); launches = json.optInt("launches").coerceAtLeast(0)
        ownedClub = json.optInt("club").coerceIn(clubs.indices)
        relics = json.optInt("relics").coerceAtLeast(0); ascensions = json.optInt("ascensions").coerceAtLeast(0)
        val levels = json.optJSONArray("clubLevels") ?: JSONArray()
        val research = json.optJSONArray("techLevels") ?: JSONArray()
        val hits = json.optJSONArray("damage") ?: JSONArray()
        val dead = json.optJSONArray("destroyed") ?: JSONArray()
        clubLevels.indices.forEach { clubLevels[it] = levels.optInt(it).coerceIn(0, 30) }
        techLevels.indices.forEach { techLevels[it] = research.optInt(it).coerceIn(0, Tech.entries[it].max) }
        damage.indices.forEach { damage[it] = hits.optDouble(it).coerceIn(0.0, planets[it].hp); destroyed[it] = dead.optBoolean(it) }
        revision++
    }
}
