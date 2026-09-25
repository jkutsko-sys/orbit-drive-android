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
    POWER("Swingcraft", "Speed and impact", 20.0, 8),
    GRAVITY("Atmosphere", "Flight and gravity", 90.0, 8),
    BOUNCE("Rebound", "Ground strikes", 60.0, 8),
    FRICTION("Surface", "Roll and terrain", 45.0, 8),
    LIGHTNING("Stormcalling", "Active lightning", 180.0, 8),
    PLANES("Flightpath", "Aircraft carries", 260.0, 8),
    ORBIT("Orbital Science", "Planet encounters", 900.0, 8),
    ASTRAL("Astral Forge", "Deep space rewards", 2000.0, 8)
}
internal data class ResearchNode(val id: String, val branch: Tech, val tier: Int, val name: String,
    val effect: String, val cost: Double, val requires: List<String>)
internal object ResearchTree {
    private val names = listOf(
        listOf("Grip Tape", "Tempo Drill", "Weighted Shaft", "Hip Rotation", "Explosive Release", "Champion's Rhythm", "Railgun Swing", "Singularity Strike"),
        listOf("High Arc", "Thin Air", "Wind Tunnel", "Thermal Lift", "Moon Draft", "Float Field", "Zero G Pocket", "Event Horizon"),
        listOf("Spring Core", "Rubber Shell", "Kinetic Return", "Skip Shot", "Trampoline Turf", "Meteor Rebound", "Infinite Hop", "Comet Ricochet"),
        listOf("Polished Dimples", "Waxed Fairway", "Low Drag Coat", "Ice Slick", "Downhill Run", "Magnetic Glide", "Vacuum Roll", "Frictionless Wake"),
        listOf("Static Charge", "Storm Cell", "Double Tap", "Arc Conductor", "Thunderhead", "Chain Lightning", "Ion Lance", "Tempest Engine"),
        listOf("Paper Glider", "Tailwind Taxi", "Cargo Sling", "Jetstream", "Biplane Boost", "Rocket Tow", "Sky Convoy", "Orbital Carrier"),
        listOf("Lunar Survey", "Impact Scanner", "Crater Bonus", "Gravity Slingshot", "Armor Piercer", "Planetbreaker", "Core Detonation", "Star Chart"),
        listOf("Stardust Purse", "Relic Lens", "Nebula Vault", "Cosmic Dividend", "Nova Furnace", "Celestial Bank", "Galaxy Mint", "Infinity Engine")
    )
    private val effects = listOf(
        listOf("+swing speed", "+perfect timing window", "+launch speed", "+planet damage", "+launch speed", "+cash per meter", "+launch speed", "+impact burst"),
        listOf("+loft", "reduced gravity", "reduced drag", "+midflight lift", "reduced gravity", "+flight time", "reduced gravity", "+deep space range"),
        listOf("+bounce height", "+rebound speed", "+bounce height", "+ground skip", "+bounce height", "+impact speed", "+bounce height", "+comet ricochet"),
        listOf("reduced drag", "+rolling distance", "reduced drag", "+ground speed", "+rolling distance", "+speed retention", "reduced drag", "+wake speed"),
        listOf("unlock lightning tap", "+lightning impulse", "+one lightning charge", "+lightning impulse", "+lightning lift", "+one lightning charge", "+lightning impulse", "+storm power"),
        listOf("unlock aircraft", "+carry speed", "+carry altitude", "+carry speed", "+carry altitude", "+carry speed", "+carry speed", "+orbital carry"),
        listOf("reveal planetary HP", "+impact damage", "+planet bounty", "+impact speed", "+impact damage", "+planet bounty", "+impact damage", "+slingshot speed"),
        listOf("+distance cash", "+relic power", "+planet bounty", "+distance cash", "+relic power", "+planet bounty", "+distance cash", "+late game speed")
    )
    val all: List<ResearchNode> = Tech.entries.flatMap { branch ->
        (0..7).map { tier ->
            val index = branch.ordinal
            val requires = when (tier) {
                0 -> emptyList()
                1, 2 -> listOf("${branch.name}-0")
                3 -> listOf("${branch.name}-1")
                4 -> listOf("${branch.name}-2")
                5 -> listOf("${branch.name}-3", "${branch.name}-4")
                6 -> listOf("${branch.name}-5")
                else -> listOf("${branch.name}-6")
            }
            ResearchNode("${branch.name}-$tier", branch, tier, names[index][tier], effects[index][tier],
                branch.baseCost * 2.15.pow(tier), requires)
        }
    }
    fun nodes(branch: Tech) = all.filter { it.branch == branch }
}
internal enum class Phase { READY, CHARGING, FLYING, LANDED }

internal class GameEngine(context: Context) {
    private val prefs = context.getSharedPreferences("orbit_drive_v1", Context.MODE_PRIVATE)
    val clubs = listOf(
        Club("Bent Starter", 0.0, 35.0, 31.0, 1.12),
        Club("Garage Sale Wood", 160.0, 45.0, 29.0, 1.20),
        Club("Steel Driver", 600.0, 58.0, 25.0, 1.31),
        Club("Carbon Slice", 2200.0, 73.0, 23.0, 1.42),
        Club("Titanium Cannon", 8000.0, 92.0, 21.0, 1.52),
        Club("Mach One", 28000.0, 115.0, 20.0, 1.65),
        Club("Plasma Wood", 95000.0, 145.0, 18.0, 1.78),
        Club("Thunder Driver", 320000.0, 185.0, 18.0, 1.92),
        Club("Moonshot Mk I", 1100000.0, 235.0, 17.0, 2.05),
        Club("Starbreaker", 3800000.0, 300.0, 16.0, 2.19),
        Club("Nebula Lance", 13000000.0, 380.0, 15.0, 2.35),
        Club("Nova Hammer", 45000000.0, 480.0, 14.0, 2.50),
        Club("Void Bender", 155000000.0, 620.0, 13.0, 2.68),
        Club("Galactic Crown", 530000000.0, 800.0, 12.0, 2.85),
        Club("Infinity Driver", 1800000000.0, 1050.0, 11.0, 3.05)
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
    private val purchased = mutableSetOf<String>()
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
    val multiplier get() = 1.35.pow(relics) * (if (nodeEffect(Tech.ASTRAL, 1)) 1.08 else 1.0) * (if (nodeEffect(Tech.ASTRAL, 4)) 1.12 else 1.0)
    val nextPlanetIndex get() = planets.indices.firstOrNull { !destroyed[it] }
    val nextPlanet get() = nextPlanetIndex?.let { planets[it] }
    val potentialRelics get() = max(0, log10(max(1.0, lifetimeDistance / 10000)).toInt())
    val ascendAvailable get() = bestDistance >= 45000 && potentialRelics > 0 && phase != Phase.FLYING
    fun level(tech: Tech) = ResearchTree.nodes(tech).count { it.id in purchased }
    fun owns(node: ResearchNode) = node.id in purchased
    fun unlocked(node: ResearchNode) = node.requires.all { it in purchased }
    fun nodeEffect(tech: Tech, tier: Int) = "${tech.name}-$tier" in purchased
    fun clubLevel(id: Int) = clubLevels[id]
    fun isDestroyed(id: Int) = destroyed[id]

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
        lightningCharges = if (level(Tech.LIGHTNING) > 0) 1 + (if (nodeEffect(Tech.LIGHTNING, 2)) 1 else 0) + (if (nodeEffect(Tech.LIGHTNING, 5)) 1 else 0) else 0
        planetHP = nextPlanetIndex?.let { (planets[it].hp - damage[it]).coerceAtLeast(0.0) } ?: 0.0
        val launchSpeed = (club.swing * 1.15.pow(clubLevels[ownedClub]) + level(Tech.POWER) * 11) * club.smash * (0.28 + 0.72 * (if (nodeEffect(Tech.POWER, 1)) max(charge, 0.55) else charge)) * multiplier
        val angle = Math.toRadians(club.loft + level(Tech.GRAVITY) * 0.4)
        vx = launchSpeed * cos(angle); vy = launchSpeed * sin(angle); speed = launchSpeed
        launches++; save(); message = if (charge > 0.85) "PERFECT STRIKE!" else "Ball away!"
    }
    fun lightning() {
        if (phase != Phase.FLYING || lightningCharges <= 0) return
        lightningCharges--
        val impulse = 35 + level(Tech.LIGHTNING) * 18
        vx += impulse * multiplier * (if (nodeEffect(Tech.LIGHTNING, 7)) 1.4 else 1.0); vy += impulse * (if (nodeEffect(Tech.LIGHTNING, 4)) 0.8 else 0.4)
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
            if (nodeEffect(Tech.GRAVITY, 3) && flightTime in 1.0..2.0) vy += 3.0 * h
            vx *= (1 - max(0.00005, 0.0016 - level(Tech.FRICTION) * 0.000075) * h).coerceAtLeast(0.0)
            distance += vx * h; altitude += vy * h
            val index = nextPlanetIndex
            if (index != null && previousDistance < planets[index].distance && distance >= planets[index].distance) {
                val planet = planets[index]
                val hit = max(1.0, speed * (1 + level(Tech.POWER) * 0.18 + level(Tech.ORBIT) * 0.24) * multiplier)
                damage[index] = (damage[index] + hit).coerceAtMost(planet.hp)
                planetHP = planet.hp - damage[index]
                if (planetHP <= 0) {
                    destroyed[index] = true; combo++
                    val bounty = planet.hp * 3 * (1 + level(Tech.ORBIT) * 0.18 + level(Tech.ASTRAL) * 0.14) * multiplier
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
                val restitution = min(0.9, 0.34 + level(Tech.BOUNCE) * 0.035 + (if (nodeEffect(Tech.BOUNCE, 7)) 0.06 else 0.0))
                if (abs(vy) > 11 && bounceCount < 28) {
                    vy = abs(vy) * restitution
                    vx *= max(0.6, 0.82 + level(Tech.FRICTION) * 0.012 + (if (nodeEffect(Tech.BOUNCE, 3)) 0.03 else 0.0))
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
        val payout = max(1.0, distance * (0.13 + combo * 0.03) * (1 + level(Tech.ASTRAL) * 0.18 + (if (nodeEffect(Tech.POWER, 5)) 0.15 else 0.0)) * multiplier)
        earned += payout; cash += payout; lifetimeDistance += distance; bestDistance = max(bestDistance, distance)
        message = "${format(distance)} m • +$${format(earned)}"; save()
    }
    fun buyNode(node: ResearchNode) {
        if (owns(node) || !unlocked(node) || cash < node.cost) return
        cash -= node.cost; purchased.add(node.id); save()
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
        clubLevels.indices.forEach { clubLevels[it] = 0 }; purchased.clear()
        damage.indices.forEach { damage[it] = 0.0; destroyed[it] = false }
        phase = Phase.READY; message = "Ascended! +$gained permanent relics"; save()
    }
    private fun save() {
        val json = JSONObject().apply {
            put("cash", cash); put("lifetime", lifetimeDistance); put("best", bestDistance); put("launches", launches)
            put("club", ownedClub); put("relics", relics); put("ascensions", ascensions)
            put("clubLevels", JSONArray(clubLevels)); put("nodes", JSONArray(purchased.toList()))
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
        val nodes = json.optJSONArray("nodes")
        if (nodes != null) { repeat(nodes.length()) { nodes.optString(it).takeIf { id -> ResearchTree.all.any { n -> n.id == id } }?.let(purchased::add) } }
        else { // Carry forward older research saves into the new named tree.
            val old = json.optJSONArray("techLevels") ?: JSONArray()
            (0 until min(6, old.length())).forEach { branch ->
                (0 until min(8, old.optInt(branch))).forEach { tier -> purchased.add("${Tech.entries[branch].name}-$tier") }
            }
        }
        val hits = json.optJSONArray("damage") ?: JSONArray()
        val dead = json.optJSONArray("destroyed") ?: JSONArray()
        clubLevels.indices.forEach { clubLevels[it] = levels.optInt(it).coerceIn(0, 30) }

        damage.indices.forEach { damage[it] = hits.optDouble(it).coerceIn(0.0, planets[it].hp); destroyed[it] = dead.optBoolean(it) }
        revision++
    }
}
