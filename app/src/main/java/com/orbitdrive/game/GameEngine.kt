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
internal data class Golfer(val name: String, val cost: Double, val power: Double, val cashBonus: Double, val look: Color, val description: String)
internal data class Apparel(val id: String, val name: String, val price: Int, val bonus: Double, val icon: String, val description: String)
internal data class BallRelic(val name: String, val relicCost: Int, val power: Double, val tint: Color,
    val stripe: Color, val mark: String, val description: String)
internal data class Obstacle(val name: String, val at: Double, val height: Double, val speedLoss: Double)
internal data class Planet(val name: String, val distance: Double, val color: Color)
internal data class AscensionRelic(val name: String, val icon: String, val description: String, val maxRank: Int = 30)
internal enum class Tech(val title: String, val description: String, val baseCost: Double, val max: Int) {
    POWER("Swingcraft", "Speed and impact", 20.0, 24),
    GRAVITY("Atmosphere", "Flight and gravity", 90.0, 24),
    BOUNCE("Rebound", "Ground strikes", 60.0, 24),
    FRICTION("Surface", "Roll and terrain", 45.0, 24),
    LIGHTNING("Abilities", "Four specialized ability paths", 180.0, 97),
    PLANES("Flightpath", "Aircraft carries", 260.0, 24),
    ORBIT("Milestone Mapping", "Distance milestones and bounties", 900.0, 24),
    ASTRAL("Astral Forge", "Deep space rewards", 2000.0, 24)
}
internal data class ResearchNode(val id: String, val branch: Tech, val tier: Int, val name: String,
    val effect: String, val cost: Double, val requires: List<String>, val ability: FlightAbility? = null)
internal object ResearchTree {
    private val names = listOf(
        listOf("Grip Tape", "Tempo Drill", "Weighted Shaft", "Hip Rotation", "Explosive Release", "Champion's Rhythm", "Railgun Swing", "Singularity Strike", "Sweet Spot Map", "Quick Hands", "Long Lever", "Power Coil", "Whip Crack", "Heavy Finish", "Launch Window", "Second Swing", "Deep Flex", "Arc Timing", "Torque Chamber", "Hammer Drop", "Slingshot Stance", "Skyline Release", "Titan Grip", "Overdrive Form"),
        listOf("High Arc", "Thin Air", "Wind Tunnel", "Thermal Lift", "Moon Draft", "Float Field", "Zero G Pocket", "Event Horizon", "Rising Column", "Cloud Ladder", "Jet Stream", "Airfoil Dimples", "Tailwind Shelf", "Vacuum Pocket", "Gravity Lens", "Inversion Pulse", "Thermal Pocket", "Sky Current", "Slipstream", "Night Air", "Orbital Draft", "Aurora Lift", "Zero Point", "Skyhook"),
        listOf("Spring Core", "Rubber Shell", "Kinetic Return", "Skip Shot", "Trampoline Turf", "Meteor Rebound", "Infinite Hop", "Comet Ricochet", "Double Skip", "Elastic Skin", "Impact Spring", "Stone Hopper", "Rebound Bank", "Rolling Kick", "Shockwave Landing", "Seismic Pulse", "Soft Landing", "Springboard", "Ground Loop", "Trick Bounce", "Bumper Shell", "Recoil Spin", "Moon Pogo", "Endless Rebound"),
        listOf("Polished Dimples", "Waxed Fairway", "Low Drag Coat", "Ice Slick", "Downhill Run", "Magnetic Glide", "Vacuum Roll", "Frictionless Wake", "Slick Contact", "Grass Cutter", "Fast Turf", "Dust Skater", "Skim Surface", "Smooth Orbit", "Low Resistance", "Rolling Grace", "Silent Bearings", "Glass Fairway", "Surface Slip", "Glide Rail", "Low Spin", "Coastline", "Vacuum Lanes", "Perpetual Glide"),
        listOf("Static Charge", "Storm Cell", "Double Tap", "Arc Conductor", "Thunderhead", "Chain Lightning", "Ion Lance", "Tempest Engine", "Charged Dimples", "Volt Reservoir", "Flash Step", "Cloud Ground", "Arc Jump", "Spark Trail", "Conductive Shell", "Stormglass Core", "Live Wire", "Ball Lightning", "Corona Field", "Electric Wake", "Thunder Rail", "Static Shield", "Sky Circuit", "Supercell"),
        listOf("Paper Glider", "Tailwind Taxi", "Cargo Sling", "Jetstream", "Biplane Boost", "Rocket Tow", "Sky Convoy", "Orbital Carrier", "Propeller Assist", "Pilot Signal", "Long Tow", "Wing Lift", "Air Relay", "Flight Crew", "Second Approach", "Twin Escort", "High Altitude", "Turbine Pass", "Cloud Runway", "Afterburner", "Sky Caravan", "Launch Ramp", "Star Pilot", "Orbital Armada"),
        listOf("Lunar Survey", "Rangefinder", "Crater Cache", "Gravity Route", "Star Compass", "Planet Atlas", "First Contact", "Cosmic Charter", "Orbit Beacons", "Bounty Trail", "Waypoint Echo", "Moon Quarry", "Ring Navigator", "Meteor Ledger", "Deep Chart", "Slingshot Corridor", "Astral Signpost", "Comet Tally", "Celestial Route", "Solar Wayfinder", "Milestone Echo", "Red Planet Route", "Starfall Ledger", "Infinite Horizon"),
        listOf("Stardust Purse", "Relic Lens", "Nebula Vault", "Cosmic Dividend", "Nova Furnace", "Celestial Bank", "Galaxy Mint", "Infinity Engine", "Star Ledger", "Meteor Market", "Nebula Interest", "Orbit Trade", "Planet Jackpot", "Relic Collector", "Astral Dividend", "Relic Alchemy", "Supernova Bank", "Cosmic Vault", "Golden Trail", "Starlight Fund", "Galaxy Ledger", "Treasure Comet", "Infinite Yield", "Astral Crown")
    )
    private val effects = listOf(
        listOf("+swing speed", "+perfect timing window", "+launch speed", "+planet damage", "+launch speed", "+cash per meter", "+launch speed", "KEYSTONE: perfect strike adds a burst", "+10% perfect shot payout", "+swing power", "+smash efficiency", "+launch power", "+impact force", "+swing power", "+perfect timing", "KEYSTONE: second push when rolling ends", "+club flexibility", "+smash efficiency", "+impact force", "+launch power", "+swing power", "+smash efficiency", "+impact force", "KEYSTONE: perfect timing overdrive"),
        listOf("+loft", "reduced gravity", "reduced drag", "+midflight lift", "reduced gravity", "+flight time", "reduced gravity", "KEYSTONE: deep-space lift", "thermal lift on descent", "reduced gravity", "reduced air drag", "+glide lift", "longer hang time", "reduced gravity", "+midflight lift", "KEYSTONE: lightning reverses fall briefly", "+glide lift", "reduced gravity", "reduced air drag", "longer hang time", "+glide lift", "reduced gravity", "reduced air drag", "KEYSTONE: skyhook lift at apex"),
        listOf("+bounce height", "+rebound speed", "+bounce height", "+ground skip", "+bounce height", "+impact speed", "+bounce height", "KEYSTONE: comet ricochet", "first landing gets an extra skip", "+bounce height", "+rebound speed", "+bounce height", "+rebound speed", "+bounce height", "+impact speed", "KEYSTONE: landing shockwave adds cash", "+bounce height", "+rebound speed", "+bounce height", "+rebound speed", "+bounce height", "+rebound speed", "+bounce height", "KEYSTONE: pogo rebounds last longer"),
        listOf("reduced drag", "+rolling distance", "reduced drag", "+ground speed", "+rolling distance", "+speed retention", "reduced drag", "KEYSTONE: glide wake", "+roll speed on first contact", "reduced drag", "+roll speed", "reduced drag", "+rolling distance", "reduced drag", "+roll speed", "KEYSTONE: roll coasts farther", "reduced drag", "+roll speed", "reduced drag", "+rolling distance", "reduced drag", "+roll speed", "reduced drag", "KEYSTONE: near-frictionless final roll"),
        listOf("unlock Lightning", "+lightning impulse", "+one lightning charge", "+lightning impulse", "+lightning lift", "+one lightning charge", "+lightning impulse", "KEYSTONE: lightning arcs again", "+charged ball duration", "+lightning impulse", "+charged ball duration", "+lightning lift", "+lightning impulse", "+charged ball duration", "+lightning impulse", "KEYSTONE: electrify ball, melt friction and shatter obstacles", "+charged ball duration", "+lightning impulse", "+charged ball duration", "+lightning lift", "+charged ball duration", "+lightning impulse", "+charged ball duration", "KEYSTONE: supercell chain burst"),
        listOf("unlock aircraft", "+carry speed", "+carry altitude", "+carry speed", "+carry altitude", "+carry speed", "+carry speed", "KEYSTONE: orbital carrier", "+first plane tow duration", "+carry speed", "+carry altitude", "+carry speed", "+carry altitude", "+carry speed", "+aircraft signal", "KEYSTONE: second plane carry", "+carry speed", "+carry altitude", "+carry speed", "+carry speed", "+carry altitude", "+carry speed", "+carry altitude", "KEYSTONE: third escort carry"),
        listOf("+checkpoint bounty", "+checkpoint bounty", "+distance reward", "+checkpoint bounty", "+checkpoint bounty", "+checkpoint bounty", "+distance reward", "KEYSTONE: charted bounties surge", "+checkpoint bounty", "+checkpoint bounty", "+distance reward", "+checkpoint bounty", "+checkpoint bounty", "+checkpoint bounty", "+distance reward", "KEYSTONE: checkpoint slingshot", "+checkpoint bounty", "+checkpoint bounty", "+distance reward", "+checkpoint bounty", "+checkpoint bounty", "+checkpoint bounty", "+distance reward", "KEYSTONE: horizon bonus and comet boost"),
        listOf("+distance cash", "+relic power", "+planet bounty", "+distance cash", "+relic power", "+planet bounty", "+distance cash", "KEYSTONE: infinite yield", "+distance cash", "+planet bounty", "+distance cash", "+planet bounty", "+distance cash", "+planet bounty", "+distance cash", "KEYSTONE: extra ascension relic", "+distance cash", "+planet bounty", "+distance cash", "+planet bounty", "+distance cash", "+planet bounty", "+distance cash", "KEYSTONE: combo cash crescendo")
    )
    private val catalog: List<ResearchNode> = Tech.entries.flatMap { branch ->
        (0..23).map { tier ->
            val requires = when (tier) {
                0 -> emptyList()
                1, 2 -> listOf("${branch.name}-0")
                3 -> listOf("${branch.name}-1")
                4 -> listOf("${branch.name}-2")
                5 -> listOf("${branch.name}-3", "${branch.name}-4")
                6, 7 -> listOf("${branch.name}-${tier - 1}")
                8, 9 -> listOf("${branch.name}-7")
                10 -> listOf("${branch.name}-8")
                11 -> listOf("${branch.name}-9")
                12 -> listOf("${branch.name}-10", "${branch.name}-11")
                13, 14 -> listOf("${branch.name}-12")
                15 -> listOf("${branch.name}-13", "${branch.name}-14")
                16, 17 -> listOf("${branch.name}-15")
                18 -> listOf("${branch.name}-16")
                19 -> listOf("${branch.name}-17")
                20 -> listOf("${branch.name}-18", "${branch.name}-19")
                21, 22 -> listOf("${branch.name}-20")
                else -> listOf("${branch.name}-21", "${branch.name}-22")
            }
            ResearchNode("${branch.name}-$tier", branch, tier, names[branch.ordinal][tier], effects[branch.ordinal][tier],
                branch.baseCost * 2.15.pow(tier), if (branch == Tech.LIGHTNING && tier == 0) listOf(AbilityResearch.HUB) else requires, if (branch == Tech.LIGHTNING) FlightAbility.LIGHTNING else null)
        }
    }
    .plus(AbilityResearch.extraNodes())
    val legacyAbilities = catalog.filter { it.branch == Tech.LIGHTNING }
    val all = catalog.filter { it.branch != Tech.LIGHTNING }
    val byId = all.associateBy { it.id }
    fun nodes(branch: Tech) = all.filter { it.branch == branch }
}
internal enum class Phase { READY, CHARGING, FLYING, LANDED }

internal class GameEngine(context: Context, val expedition: Boolean = false, sharedEngagement: EngagementState? = null, private val abilitySource: GameEngine? = null, private val clock: () -> Long = System::currentTimeMillis) {
    val engagement = sharedEngagement ?: EngagementState(context)
    val caddies: CaddyState = abilitySource?.caddies ?: CaddyState(context,clock)
    private fun caddyBonus(id:Int) = if(expedition) 0.0 else caddies.bonus(id)
    val abilityProgress: AbilityProgress = abilitySource?.abilityProgress ?: AbilityProgress(context, clock)
    private val prefs = context.getSharedPreferences(if (expedition) "orbit_expedition_v1" else "orbit_drive_v1", Context.MODE_PRIVATE)
    private val previewBuild = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    fun redeemVoucher(code: String): String {
        if (!previewBuild) return "Vouchers are unavailable in this build"
        if (code.trim().equals("TICKETS", ignoreCase = true)) {
            engagement.grantTicketsForTesting(1000); cue("purchase")
            return "1,000 range tickets added for testing"
        }
        if (!code.trim().equals("ADMIN", ignoreCase = true)) return "Invalid voucher code"
        cash += 100_000.0; save(); cue("purchase")
        return "$100,000 added for testing"
    }
    val clubs = listOf(
        Club("Bent Starter", 0.0, 35.0, 31.0, 1.12),
        Club("Garage Sale Wood", 500.0, 45.0, 29.0, 1.20),
        Club("Steel Driver", 2600.0, 58.0, 25.0, 1.31),
        Club("Carbon Slice", 12000.0, 73.0, 23.0, 1.42),
        Club("Titanium Cannon", 55000.0, 92.0, 21.0, 1.52),
        Club("Mach One", 230000.0, 115.0, 20.0, 1.65),
        Club("Plasma Wood", 950000.0, 145.0, 18.0, 1.78),
        Club("Thunder Driver", 4000000.0, 185.0, 18.0, 1.92),
        Club("Moonshot Mk I", 17000000.0, 235.0, 17.0, 2.05),
        Club("Starbreaker", 72000000.0, 300.0, 16.0, 2.19),
        Club("Nebula Lance", 310000000.0, 380.0, 15.0, 2.35),
        Club("Nova Hammer", 1300000000.0, 480.0, 14.0, 2.50),
        Club("Void Bender", 5500000000.0, 620.0, 13.0, 2.68),
        Club("Galactic Crown", 23000000000.0, 800.0, 12.0, 2.85),
        Club("Zenith", 100000000000.0, 1050.0, 11.0, 3.05)
    )
    val clubPerks = listOf(
        "Starter • no special modifier", "Rebound Wood • stronger first bounce", "Steel Tempo • wider perfect timing",
        "Twin Launch • two balls, 2× distance cash, 18% slower launch", "Dust Kicker • softer obstacle collisions",
        "Mach Draft • less air drag", "Plasma Tail • bonus lightning impulse", "Storm Driver • one extra lightning charge",
        "Moonshot • lower gravity", "Starbreaker • 35% bigger checkpoint bounties", "Nebula Bank • richer checkpoint bounties",
        "Nova Hammer • lightning adds lift", "Void Bender • extra skyhook lift", "Galactic Crown • more cash per meter",
        "Zenith • every previous club perk, no Twin Launch speed penalty"
    )
    val golfers = listOf(
        Golfer("Range Rookie", 0.0, 1.0, 1.0, Color(0xff80ffbf), "A backyard beginner with a big dream"),
        Golfer("Rob Does Shorts", 2500.0, 1.04, 1.05, Color(0xffffae67), "A fun first upgrade with a small cash bonus"),
        Golfer("Brandt Horvett", 15000.0, 1.09, 1.07, Color(0xff54bdf2), "Smooth tempo and a painter's touch"),
        Golfer("Tig Wedge", 120000.0, 1.15, 1.11, Color(0xffc8f28a), "Creative shotmaker with a loyal following"),
        Golfer("Cappy Gilmore", 430000.0, 1.20, 1.13, Color(0xffb8875b), "A capybara with an unshakable putting face"),
        Golfer("Ricky Foreway", 1200000.0, 1.25, 1.16, Color(0xffffd064), "Bright gear, brighter launch monitor"),
        Golfer("Rice DeChambrix", 6500000.0, 1.36, 1.18, Color(0xffed80fa), "A physics obsessive chasing raw speed"),
        Golfer("Nelly Birdie", 35000000.0, 1.43, 1.24, Color(0xffff8dae), "Effortless swing that bends the sky"),
        Golfer("Roary McFairway", 200000000.0, 1.58, 1.31, Color(0xffa2b0ff), "An elite all-around champion for deep space"),
        Golfer("The Germane Slammer", 1200000000.0, 1.78, 1.40, Color(0xffe6a24d), "A precision powerhouse with a hammer-like finish"),
        Golfer("Klyde Birkshire", 8000000000.0, 2.0, 1.52, Color(0xffde7658), "A flowing-haired long-drive legend chasing the next star")
    )
    val apparel = listOf(
        Apparel("tee", "Martini Tee", 1, 0.04, "♧", "+4% launch power"),
        Apparel("hat", "Wood Wood Cap", 2, 0.08, "♢", "+8% launch power"),
        Apparel("glove", "Tytleist Glove", 3, 0.12, "✦", "+12% launch power"),
        Apparel("shirt", "Fairway Famous Polo", 4, 0.16, "◇", "+16% launch power"),
        Apparel("shoes", "Moonwalk Spikes", 6, 0.22, "☆", "+22% launch power"),
        Apparel("visor", "Cosmic Caddie Visor", 9, 0.32, "◈", "+32% launch power")
    )
    val balls = listOf(
        BallRelic("Doodle Noodle", 0, 1.0, Color(0xfff5ebbc), Color(0xffb45e45), "N", "A squishy range-ball original"),
        BallRelic("Birdie Biscuit", 2, 1.06, Color(0xffffe9ab), Color(0xffff906b), "B", "A little extra pop"),
        BallRelic("Call-a-Wayward", 4, 1.13, Color(0xfff4f9ff), Color(0xff72b6ff), "C", "Carries through the clouds"),
        BallRelic("Tour V-Won", 7, 1.22, Color(0xfff5f5f5), Color(0xffee6363), "V", "A serious tour-grade sphere"),
        BallRelic("Pro V-Wonder", 12, 1.34, Color(0xffffffff), Color(0xfff3a43b), "P", "The premium ball for interplanetary drives"),
        BallRelic("Cosmic V-One", 19, 1.48, Color(0xffdbeaff), Color(0xffa66aff), "✦", "The final evolution of the long drive")
    )
    val obstacles = listOf(
        Obstacle("bunker rock", 115.0, 4.0, 0.18),
        Obstacle("practice shed", 720.0, 10.0, 0.22),
        Obstacle("range tower", 2400.0, 21.0, 0.27),
        Obstacle("satellite debris", 15500.0, 42.0, 0.16)
    )
    val planets = listOf(
        Planet("Moon", 8000.0, Color(0xffbec6d1)), Planet("Mars", 45000.0, Color(0xfff98b64)),
        Planet("Jupiter", 300000.0, Color(0xffd8a87c)), Planet("Saturn", 2000000.0, Color(0xffead999)),
        Planet("Neptune", 12000000.0, Color(0xff648bff)), Planet("Next Star", 80000000.0, Color(0xff87ffc4))
    )
    var cash by mutableStateOf(0.0); private set
    var lifetimeDistance by mutableStateOf(0.0); private set
    var bestDistance by mutableStateOf(0.0); private set
    var launches by mutableStateOf(0); private set
    var ownedClub by mutableStateOf(0); private set
    var equippedClub by mutableStateOf(0); private set
    var relics by mutableStateOf(0); private set
    var ascensions by mutableStateOf(0); private set
    var relicBank by mutableStateOf(0); private set
    var selectedGolfer by mutableStateOf(0); private set
    var selectedBall by mutableStateOf(0); private set
    private val ownedBalls = mutableSetOf(0)
    private val ownedGolfers = mutableSetOf(0)
    private val ownedApparel = mutableSetOf<String>()
    private val clubLevels = MutableList(clubs.size) { 0 }
    private val purchased = mutableSetOf<String>()
    private val destroyed = MutableList(planets.size) { false } // First clears persist across ascensions.
    private val runDestroyed = MutableList(planets.size) { false }
    private val clubMilestones = MutableList(clubs.size) { 0 }
    val ascensionRelics = listOf(
        AscensionRelic("Titan Grip", "✊", "+12% launch power per rank"),
        AscensionRelic("Golden Dimples", "◉", "+18% distance cash per rank"),
        AscensionRelic("Star Cartographer", "✧", "+20% checkpoint bounty per rank"),
        AscensionRelic("Feather Field", "☁", "6% less gravity per rank"),
        AscensionRelic("Rolling Comet", "↗", "8% less ground drag per rank"),
        AscensionRelic("Storm Battery", "⚡", "+8% lightning impulse per rank"),
        AscensionRelic("Flight Beacon", "✈", "+10% aircraft carry per rank"),
        AscensionRelic("Echo of Ascension", "◇", "+15% relics earned on ascension per rank"),
        AscensionRelic("Masterwork Shaft", "◆", "+8% club power per rank"),
        AscensionRelic("Horizon Dividend", "✦", "+10% all shot cash per rank")
    ) + abilityRelics
    private val relicLevels = MutableList(ascensionRelics.size) { 0 }
    private val discoveredRelics = mutableSetOf<Int>()
    var lastDiscovery by mutableStateOf(""); private set
    fun relicLevel(id: Int) = relicLevels[id]
    fun relicDiscovered(id: Int) = id in discoveredRelics
    val discoveryCost get() = 2 + discoveredRelics.size * 2 + discoveredRelics.size * discoveredRelics.size / 4
    fun relicUpgradeCost(id: Int) = ((if (id < 10) 2.0 else 1.45).pow(relicLevels[id]) * (2 + id / (if (id < 10) 3 else 6))).toInt().coerceAtLeast(2)
    fun discoverRelic() {
        if (discoveredRelics.size == ascensionRelics.size || relicBank < discoveryCost) return
        val cost = discoveryCost
        val unknown = ascensionRelics.indices.filter { it !in discoveredRelics }
        val found = if (discoveredRelics.isEmpty()) 0 else unknown.random()
        relicBank -= cost; discoveredRelics.add(found); relicLevels[found] = max(1,relicLevels[found]); lastDiscovery = ascensionRelics[found].name
        save(); cue("purchase")
    }
    fun upgradeRelic(id: Int) {
        if (id !in discoveredRelics || relicLevels[id] >= ascensionRelics[id].maxRank || relicBank < relicUpgradeCost(id)) return
        relicBank -= relicUpgradeCost(id); relicLevels[id]++; save(); cue("purchase")
    }
    fun clubMilestoneCount(id: Int) = clubMilestones[id]
    val clubPowerBonus get() = 1.2.pow(clubMilestones[equippedClub]) * (1 + relicLevels[8] * .08)
    val ascensionReward get() = ((potentialRelics + if (nodeEffect(Tech.ASTRAL, 15)) 1 else 0) *
        (1 + relicLevels[7] * .15) * (1 + caddyBonus(6))).toInt().coerceAtLeast(1)
    val aircraftName get() = when (level(Tech.PLANES)) {
        in 0..2 -> "Paper glider"; in 3..5 -> "Biplane"; in 6..9 -> "Propeller plane"
        in 10..13 -> "Cargo jet"; in 14..18 -> "Stealth bomber"; in 19..22 -> "Heavy rocket"
        else -> "Orbital starship"
    }
    var revision by mutableStateOf(0); private set
    var soundEventId by mutableStateOf(0); private set
    var soundCue by mutableStateOf(""); private set
    private fun cue(name: String) { soundCue = name; soundEventId++ }
    var distance by mutableStateOf(0.0); private set
    var altitude by mutableStateOf(0.0); private set
    var speed by mutableStateOf(0.0); private set
    var charge by mutableStateOf(0.0); private set
    var phase by mutableStateOf(Phase.READY); private set
    var message by mutableStateOf("Hold LAUNCH, release near full power"); private set
    var lightningCharges by mutableStateOf(0); private set
    var earned by mutableStateOf(0.0); private set
    var shotElapsed by mutableStateOf(0.0); private set
    var combo by mutableStateOf(0); private set
    var electrifiedFor by mutableStateOf(0.0); private set
    var lightningFlashFor by mutableStateOf(0.0); private set
    var planeSpriteFor by mutableStateOf(0.0); private set
    var planetEffectFor by mutableStateOf(0.0); private set
    var planetImpactID by mutableStateOf<Int?>(null); private set
    var planetShattered by mutableStateOf(false); private set
    var firstMilestoneFor by mutableStateOf(0.0); private set
    var milestoneName by mutableStateOf(""); private set
    private val brokenObstacles = mutableSetOf<Int>()
    fun obstacleBroken(index: Int) = index in brokenObstacles
    private var vx = 0.0; private var vy = 0.0; private var flightTime = 0.0
    private var bounceCount = 0; private var chargeDirection = 1.0; private var planeCarries = 0
    private var secondSwingUsed = false; private var skyhookUsed = false; private var deepLiftUsed = false
    private var previousDistance = 0.0

    var gravityPulseFor by mutableStateOf(0.0); private set
    var rocketFlashFor by mutableStateOf(0.0); private set
    var comboMessage by mutableStateOf(""); private set
    var comboFlashFor by mutableStateOf(0.0); private set
    var ghostTarget by mutableStateOf(0.0); private set
    private val abilityUses = androidx.compose.runtime.mutableStateListOf(0, 0, 0, 0)
    var airliftFor by mutableStateOf(0.0); private set
    private var airTapCooldown = 0.0
    private var airTaps = 0
    private var airExtensions = 0.0
    private var afterburnerFor = 0.0
    private var strikeCount=0
    private var echoFor=0.0
    private var echoImpulse=0.0
    private var caddyTapAt=0L
    private var caddyTaps=0
    var caddyFlashFor by mutableStateOf(0.0); private set
    val airliftDuration get() = 60.0 + 4 * skillRank(FlightAbility.AIRLIFT,2) + abilityRelic(17)
    val forwardSpeed get() = vx
    val rangeSceneDistance get() = if (phase == Phase.FLYING) distance else 0.0
    private fun abilityRelic(id: Int) = if (expedition) 0 else relicLevels[id]
    fun skillRank(ability:FlightAbility,index:Int) = if(expedition) 0 else abilityProgress.rank(ability,index)
    fun abilityNode(ability:FlightAbility,tier:Int) = skillRank(ability,tier)>0
    fun abilityUnlocked(ability: FlightAbility) = abilityProgress.unlocked(ability)
    val canEditAbilityBuild get() = phase != Phase.FLYING && phase != Phase.CHARGING && !abilityProgress.activeAny()
    fun buyAbilitySkill(node: AbilitySkill) { if (canEditAbilityBuild) { abilityProgress.buy(node); cue("purchase") } }
    fun respecAbilities() { if (canEditAbilityBuild) abilityProgress.respec() }
    fun abilityCapacity(a: FlightAbility): Int {
        val relic = when(a) { FlightAbility.LIGHTNING -> 12; FlightAbility.ROCKET -> 24; FlightAbility.GRAVITY -> 30; else -> -1 }
        val extra = if (relic < 0) 0 else (if (abilityRelic(relic) >= 1) 1 else 0) + (if (abilityRelic(relic) >= 6) 1 else 0)
        return when(a) {
            FlightAbility.AIRLIFT -> 1
            FlightAbility.LIGHTNING -> 2 + (if(skillRank(a,3)>=3) 1 else 0) + (if(skillRank(a,3)>=7) 1 else 0) + (if(hasClubPerk(7)) 1 else 0) + extra
            else -> (if(a == FlightAbility.GRAVITY) 2 else 1) + (if(skillRank(a,3)>=3) 1 else 0) + (if(skillRank(a,3)>=7) 1 else 0) + extra
        }
    }
    fun abilityCooldown(a: FlightAbility): Double {
        val base = when(a) { FlightAbility.LIGHTNING -> 45.0; FlightAbility.AIRLIFT -> 600.0; FlightAbility.ROCKET -> 120.0; FlightAbility.GRAVITY -> 90.0 }
        val relic = when(a) { FlightAbility.LIGHTNING -> 11; FlightAbility.AIRLIFT -> 18; FlightAbility.ROCKET -> 23; FlightAbility.GRAVITY -> 29 }
        return base * (1 - abilityRelic(relic) * .02 - (if(a==FlightAbility.AIRLIFT) 0.0 else skillRank(a,3)*.02) - caddyBonus(7)).coerceAtLeast(.5)
    }
    fun abilityDuration(a: FlightAbility): Double = when(a) {
        FlightAbility.AIRLIFT -> airliftDuration
        FlightAbility.ROCKET -> 8.0 + skillRank(a,2) + abilityRelic(25) * .3
        FlightAbility.GRAVITY -> (6.0 + skillRank(a,1)*.6 + skillRank(a,7)*.5 + abilityRelic(28) * .2)*(1+caddyBonus(5))
        FlightAbility.LIGHTNING -> 0.0
    }
    fun abilityActive(a: FlightAbility) = abilityProgress.active(a)
    fun abilityReadyIn(a: FlightAbility) = abilityProgress.readyIn(a)
    fun abilityReady(a: FlightAbility) = abilityCharges(a) > 0 && (a == FlightAbility.AIRLIFT || a == FlightAbility.LIGHTNING || abilityActive(a) <= 0)
    private fun electrify(seconds: Double) {
        if(seconds > 0) abilityProgress.activate(FlightAbility.LIGHTNING, max(seconds,abilityProgress.active(FlightAbility.LIGHTNING)))
        electrifiedFor = abilityProgress.active(FlightAbility.LIGHTNING)
    }
    fun equipAbility(slot: Int, ability: FlightAbility) {
        if (slot in 0..1 && phase != Phase.FLYING && phase != Phase.CHARGING && abilityUnlocked(ability)) engagement.select(slot,ability)
    }
    private fun abilityCash(value: Double) { if (!expedition) { cash += value; earned += value } }
    private var bounceArmed = false
    private var thunderArmed = false
    private var usedLightning = false
    private var shotClears = 0
    private var cargoBall = false
    private var shotPerfect = false
    fun abilityCharges(ability: FlightAbility): Int {
        if (!abilityUnlocked(ability) || (expedition && engagement.weekRule == 0 && ability == FlightAbility.LIGHTNING)) return 0
        if (ability == FlightAbility.AIRLIFT && abilityProgress.active(ability) > 0) return 1
        return abilityProgress.charges(ability,abilityCapacity(ability))
    }
    private fun showCombo(id: Int) {
        comboMessage = engagement.comboNames[id]; comboFlashFor = 2.2
        engagement.discoverCombo(id); cue("purchase")
    }
    fun tapRange() {
        if(phase!=Phase.FLYING) return
        if(abilityActive(FlightAbility.AIRLIFT)>0 && FlightAbility.AIRLIFT in engagement.slots) useAbility(FlightAbility.AIRLIFT)
        if(!expedition && caddies.selected>=0 && caddyTaps<5 && clock()>=caddyTapAt) {
            caddyTaps++;caddyTapAt=clock()+2000
            vx += (2+caddies.level(caddies.selected)*.2)*multiplier
            speed=hypot(vx,vy);caddyFlashFor=.5
        }
    }
    fun useAbility(ability: FlightAbility) {
        if(phase!=Phase.FLYING || ability !in engagement.slots || !abilityReady(ability)) return
        if(ability==FlightAbility.LIGHTNING) { lightning();return }
        fun r(i:Int)=skillRank(ability,i)
        if(ability==FlightAbility.AIRLIFT) {
            airliftFor=abilityProgress.active(ability)
            if(airliftFor<=0) {
                if(!abilityProgress.consume(ability,abilityCapacity(ability),abilityCooldown(ability))) return
                abilityProgress.activate(ability,airliftDuration);airliftFor=abilityProgress.active(ability);airTaps=0
            }
            if(airTapCooldown>0) return
            airTapCooldown=.07*(1-abilityRelic(19)*.01);airTaps++
            var lift=(6.0+2*r(1))*(1+abilityRelic(16)*.02)*(1+caddyBonus(3))
            if(airTaps%3==0 && r(3)>0) { lift+=4+3*r(3);vx+=r(3) }
            if(airTaps%5==0) lift+=8*r(5)
            altitude+=lift;vy=max(0.0,vy)+12+abilityRelic(21)
            if(airTaps%5==0 && r(7)>0) { abilityProgress.extend(ability,.25*r(7),2.0*r(7));airliftFor=abilityProgress.active(ability) }
            if(airTaps%10==0 && airTaps<=50) vx*=1+.015*r(9)
            if(airTaps%3==0) abilityCash(abilityRelic(20)+5.0*r(6))
            message="AIR DRIBBLE • $airTaps taps • ${"%.1f".format(airliftFor)}s"
        } else {
            if(!abilityProgress.consume(ability,abilityCapacity(ability),abilityCooldown(ability))) return
            abilityProgress.activate(ability,abilityDuration(ability));abilityUses[ability.ordinal]++
            if(ability==FlightAbility.ROCKET) {
                val timed=planeSpriteFor>0
                var impulse=85.0*(1+.12*r(1))*(1+abilityRelic(22)*.03)*(1+caddyBonus(4))
                if(altitude>100) impulse*=1+.05*r(6)
                if(vy<0 && r(8)>0) vx+=min(200.0*r(8),-vy*(.1+.08*r(8)))
                vx+=impulse*multiplier*(if(expedition) 1.0 else 1+engagement.facilities[3]*.03)*(if(timed) 1.35+.10*r(4) else 1.0)
                vy+=18;afterburnerFor=abilityProgress.active(ability)
                electrify(.7*r(5)+abilityRelic(26)*.3);abilityCash(abilityRelic(27)*5.0)
                rocketFlashFor=1.2;cue("plane");if(timed) showCombo(1) else message="Rocket ignition!"
            } else {
                if(vy<0) vx+=min(100.0*r(5),-vy*.08*r(5))
                gravityPulseFor=abilityProgress.active(ability)
                vy=max(vy,20.0+4*r(2));altitude+=4*r(2)+abilityRelic(31)
                abilityCash(abilityRelic(33)*5.0);message="Gravity pulse!"
                if(!expedition && relicLevels[3]>0 && relicLevels[4]>0) { electrify(4.0);showCombo(3) }
            }
        }
        speed=hypot(vx,vy)
    }
    init { if (!expedition) restore() }
    val club get() = clubs[if (expedition) 3 else equippedClub]
    fun hasClubPerk(id: Int) = !expedition && (id == equippedClub || (equippedClub == clubs.lastIndex && id < clubs.lastIndex))
    val twinLaunch get() = hasClubPerk(3) || cargoBall
    val golfer get() = golfers[selectedGolfer]
    val ball get() = balls[selectedBall]
    fun ownsBall(id: Int) = id in ownedBalls
    val nextBallUnlock get() = (ownedBalls.maxOrNull() ?: 0) + 1
    fun ownsGolfer(id: Int) = id in ownedGolfers
    val nextGolferUnlock get() = (ownedGolfers.maxOrNull() ?: 0) + 1
    fun ownsApparel(id: String) = id in ownedApparel
    val apparelPower get() = 1.0 + apparel.filter { it.id in ownedApparel }.sumOf { it.bonus }
    val multiplier get() = if (expedition) 1.0 else (1 + relicLevels[0] * .12) * (if (nodeEffect(Tech.ASTRAL, 1)) 1.08 else 1.0) * (if (nodeEffect(Tech.ASTRAL, 4)) 1.12 else 1.0)
    val nextPlanetIndex get() = planets.indices.firstOrNull { !runDestroyed[it] }
    val nextPlanet get() = nextPlanetIndex?.let { planets[it] }
    val potentialRelics get() = max(0, (log10(max(1.0, bestDistance / 10000)) * 3 + log10(max(1.0, lifetimeDistance / 10000)) * 2).toInt())
    val ascendAvailable get() = bestDistance >= 45000 && potentialRelics > 0 && phase != Phase.FLYING
    fun level(tech: Tech): Int {
        if (!expedition && tech == Tech.LIGHTNING) return AbilitySkills.nodes.count { it.ability == FlightAbility.LIGHTNING && abilityProgress.owns(it.id) }
        if (!expedition) return ResearchTree.nodes(tech).count { it.id in purchased && (tech != Tech.LIGHTNING || it.ability == FlightAbility.LIGHTNING) }
        return when (engagement.weekRule) {
            0 -> if (tech == Tech.GRAVITY) 18 else 0
            1 -> if (tech == Tech.BOUNCE || tech == Tech.FRICTION) 12 else 0
            else -> if (tech == Tech.PLANES) 8 else if (tech == Tech.LIGHTNING) 5 else 0
        }
    }
    fun owns(node: ResearchNode) = node.id in purchased
    fun unlocked(node: ResearchNode) = node.requires.all { it in purchased }
    fun nodeEffect(tech: Tech, tier: Int) = if (tech == Tech.LIGHTNING) abilityNode(FlightAbility.LIGHTNING,tier) else !expedition && "${tech.name}-$tier" in purchased
    fun clubLevel(id: Int) = clubLevels[id]
    fun isDestroyed(id: Int) = destroyed[id]

    fun clubUpgradeCost() = (40 + club.cost * 0.28) * 1.6.pow(clubLevels[equippedClub])
    val projectedSwingPower get() = launchPowerFor(if (phase == Phase.CHARGING) charge else 1.0)
    private fun launchPowerFor(strike: Double): Double {
        val perfect = strike > (if (hasClubPerk(2)) .76 else .85)
        val strikeBonus = if (perfect && nodeEffect(Tech.POWER, 23)) 1.50 else if (perfect && nodeEffect(Tech.POWER, 7)) 1.12 else 1.0
        return (club.swing * 1.15.pow(clubLevels[equippedClub]) + level(Tech.POWER) * 11) * club.smash *
            (0.28 + 0.72 * (if (nodeEffect(Tech.POWER, 1)) max(strike, 0.55) else strike)) *
            multiplier * golfer.power * (1+caddyBonus(0)) * apparelPower * ball.power * clubPowerBonus * strikeBonus * (if (expedition) 1.0 else 1 + engagement.facilities[1] * .02 + engagement.facilities[7] * .01) *
            (if (hasClubPerk(3) && equippedClub != clubs.lastIndex) .82 else 1.0)
    }
    fun format(value: Double): String = when {
        value >= 1e9 -> "%.2fB".format(value / 1e9)
        value >= 1e6 -> "%.2fM".format(value / 1e6)
        value >= 1000 -> "%.1fK".format(value / 1000)
        else -> "%.0f".format(value)
    }
    fun startCharge() {
        if (phase != Phase.READY && phase != Phase.LANDED) return
        engagement.refreshWeek()
        phase = Phase.CHARGING; charge = 0.0; chargeDirection = 1.0; message = "Release to swing"
    }
    fun release() {
        if (phase != Phase.CHARGING) return
        phase = Phase.FLYING; distance = 0.0; previousDistance = 0.0; altitude = 1.0; flightTime = 0.0; shotElapsed = 0.0
        bounceCount = 0; combo = 0; earned = 0.0; planeCarries = 0; secondSwingUsed = false; skyhookUsed = false; deepLiftUsed = false
        ghostTarget = if (expedition) engagement.expeditionBest else bestDistance
        for (i in abilityUses.indices) abilityUses[i] = 0
        strikeCount=0;echoFor=0.0;echoImpulse=0.0;caddyTaps=0;airTaps=0
        airliftFor = abilityProgress.active(FlightAbility.AIRLIFT); airTapCooldown = 0.0; afterburnerFor = abilityProgress.active(FlightAbility.ROCKET)
        usedLightning = false; shotClears = 0; cargoBall = false; bounceArmed = false; thunderArmed = false
        gravityPulseFor = abilityProgress.active(FlightAbility.GRAVITY); rocketFlashFor = 0.0; comboFlashFor = 0.0
        runDestroyed.indices.forEach { runDestroyed[it] = false }
        brokenObstacles.clear(); electrifiedFor = abilityProgress.active(FlightAbility.LIGHTNING); lightningFlashFor = 0.0; planeSpriteFor = 0.0; planetEffectFor = 0.0; planetImpactID = null
        lightningCharges = abilityCharges(FlightAbility.LIGHTNING)
        val perfect = charge > (if (hasClubPerk(2)) .76 else .85)
        shotPerfect = perfect
        val launchSpeed = launchPowerFor(charge)
        val angle = Math.toRadians(club.loft + level(Tech.GRAVITY) * 0.4)
        vx = launchSpeed * cos(angle); vy = launchSpeed * sin(angle); speed = launchSpeed
        launches++; save(); cue("swing"); message = if (perfect) "PERFECT STRIKE!" else "Ball away!"
    }
    fun lightning() {
        val a=FlightAbility.LIGHTNING
        if(phase!=Phase.FLYING || !abilityUnlocked(a) || abilityCharges(a)<=0) return
        if(!abilityProgress.consume(a,abilityCapacity(a),abilityCooldown(a))) return
        fun r(i:Int)=skillRank(a,i)
        usedLightning=true;if(vy<0 && altitude<35) thunderArmed=true
        lightningCharges=abilityCharges(a)
        val impulse=(60+(if(hasClubPerk(6)) 32 else 0))*(1+.12*r(1))*(1+abilityRelic(10)*.03)*(1+caddyBonus(2))*(1+min(3,strikeCount)*.15*r(7))
        vx+=impulse*multiplier*(1+relicLevels[5]*.08);vy+=impulse*.4
        if(nodeEffect(Tech.GRAVITY,15)) vy=max(vy,65.0)
        if(hasClubPerk(11)) vy+=35
        strikeCount++
        if(r(4)>0) { echoFor=.45;echoImpulse+=impulse*multiplier*(.20+.05*r(4)) }
        if(r(2)>0) electrify(1+.6*r(2)+abilityRelic(13)*.25)
        if(strikeCount%3==0 && r(9)>0) abilityProgress.advanceRecharge(a,abilityCooldown(a)*.1*r(9))
        abilityCash(abilityRelic(14)*2.0+10*r(6));lightningFlashFor=.38;cue("lightning")
        speed=hypot(vx,vy);message=if(electrifiedFor>0) "ELECTRIFIED BALL" else "LIGHTNING BOOST"
    }
    fun tick(step: Double) {
        val dt = step.coerceIn(0.0, 0.5)
        val wasBurning=afterburnerFor>0
        val wasPulsing = gravityPulseFor > 0
        airliftFor = abilityProgress.active(FlightAbility.AIRLIFT)
        gravityPulseFor = abilityProgress.active(FlightAbility.GRAVITY)
        afterburnerFor = abilityProgress.active(FlightAbility.ROCKET)
        electrifiedFor = abilityProgress.active(FlightAbility.LIGHTNING)
        lightningCharges = abilityCharges(FlightAbility.LIGHTNING)
        airTapCooldown = (airTapCooldown - dt).coerceAtLeast(0.0)
        if (phase == Phase.FLYING) {
            if(wasPulsing && gravityPulseFor==0.0) vx*=1+.05*skillRank(FlightAbility.GRAVITY,8)
            if(wasBurning && afterburnerFor==0.0 && skillRank(FlightAbility.ROCKET,9)>0) {
                val rank=skillRank(FlightAbility.ROCKET,9);vx*=1+.08*rank;vy=max(vy,20.0*rank)
            }
            if(afterburnerFor>0) vx+=35*dt*multiplier*(1+.4*skillRank(FlightAbility.ROCKET,7))
            if(echoFor>0) { echoFor-=dt;if(echoFor<=0) { vx+=echoImpulse;echoImpulse=0.0;lightningFlashFor=.38;cue("lightning") } }
        }
        caddyFlashFor=(caddyFlashFor-dt).coerceAtLeast(0.0)
        rocketFlashFor = (rocketFlashFor - dt).coerceAtLeast(0.0)
        comboFlashFor = (comboFlashFor - dt).coerceAtLeast(0.0)
        lightningFlashFor = (lightningFlashFor - dt).coerceAtLeast(0.0)
        planeSpriteFor = (planeSpriteFor - dt).coerceAtLeast(0.0)
        planetEffectFor = (planetEffectFor - dt).coerceAtLeast(0.0)
        firstMilestoneFor = (firstMilestoneFor - dt).coerceAtLeast(0.0)
        if (phase == Phase.CHARGING) {
            charge += chargeDirection * dt * 0.78
            if (charge >= 1) { charge = 1.0; chargeDirection = -1.0 }
            if (charge <= 0) { charge = 0.0; chargeDirection = 1.0 }
            return
        }
        if (phase != Phase.FLYING) return
        // Advance the original 240-second physics trajectory within 24.5 real seconds.
        // Distance, collision speeds and rewards remain in simulation units; only playback time changes.
        shotElapsed = (shotElapsed + dt).coerceAtMost(24.5)
        val progress = shotElapsed / 24.5
        val targetFlightTime = shotElapsed + (240.0 - 24.5) * progress * progress
        while (phase == Phase.FLYING && flightTime + 1e-9 < targetFlightTime) {
            val h = min(0.0125, targetFlightTime - flightTime)
            flightTime += h; previousDistance = distance
            vy -= 45 / (1 + level(Tech.GRAVITY) * 0.24 + (if (hasClubPerk(8)) .35 else 0.0) + relicLevels[3] * .06) * h * (if (gravityPulseFor > 0) { if (skillRank(FlightAbility.GRAVITY,7)>0) 0.0 else .2 } else if(airliftFor>0) 1-.08*skillRank(FlightAbility.AIRLIFT,4) else 1.0)
            if (nodeEffect(Tech.GRAVITY, 3) && flightTime in 1.0..2.0) vy += 3.0 * h
            if (nodeEffect(Tech.GRAVITY, 8) && vy < 0 && altitude > 0) vy += 4.0 * h
            if (!deepLiftUsed && nodeEffect(Tech.GRAVITY, 7) && distance > 7000 && vy < 0) {
                vy += 55.0; deepLiftUsed = true; message = "☁ Deep-space lift!"
            }
            if (!skyhookUsed && (nodeEffect(Tech.GRAVITY, 23) || hasClubPerk(12)) && vy < 0 && altitude > 30) {
                vy = 90.0; skyhookUsed = true; message = "☁ Skyhook lifts the ball!"
            }
            val airDrag = max(0.00005, 0.0016 - level(Tech.FRICTION) * 0.000075) * (if (hasClubPerk(5)) .7 else 1.0)
            val gravityDrag=if(gravityPulseFor>0) (1-.1*skillRank(FlightAbility.GRAVITY,4))*(1-abilityRelic(32)*.02) else 1.0
            val rocketDrag=if(afterburnerFor>0) 1-.08*skillRank(FlightAbility.ROCKET,5) else 1.0
            vx *= (1-(if(airliftFor>0) 0.0 else airDrag*gravityDrag*rocketDrag)*h*(if(electrifiedFor>0) .08 else 1.0)).coerceAtLeast(0.0)
            distance += vx * h; altitude += vy * h
            obstacles.forEachIndexed { obstacleIndex, obstacle ->
                if (previousDistance < obstacle.at && distance >= obstacle.at && altitude >= obstacle.height) shotClears++
                if (previousDistance < obstacle.at && distance >= obstacle.at && altitude < obstacle.height) {
                    if (electrifiedFor > 0 && skillRank(FlightAbility.LIGHTNING,5)>0) {
                        brokenObstacles.add(obstacleIndex); shotClears++; abilityCash(abilityRelic(15) * 10.0+20*skillRank(FlightAbility.LIGHTNING,5))
                        vx *= 1+.02*skillRank(FlightAbility.LIGHTNING,5)
                        message = "⚡ ${obstacle.name} shattered!"
                    } else {
                        vx *= 1 - obstacle.speedLoss * (if (hasClubPerk(4)) .5 else 1.0)
                        vy = max(vy, 8.0)
                        message = "Hit ${obstacle.name}! Speed -${(obstacle.speedLoss * 100).toInt()}%"
                    }
                }
            }
            planets.indices.filter { !runDestroyed[it] && previousDistance < planets[it].distance && distance >= planets[it].distance }.forEach { index ->
                val planet = planets[index]
                val firstClear = !destroyed[index]
                runDestroyed[index] = true; destroyed[index] = true; combo++
                planetImpactID = index; planetShattered = true
                planetEffectFor = if (firstClear) 3.0 else 1.8
                if (firstClear) { firstMilestoneFor = 3.0; milestoneName = planet.name }
                if (nodeEffect(Tech.ORBIT, 15)) { vx *= 1.25; vy = max(vy, 45.0) }
                if (nodeEffect(Tech.ORBIT, 23)) { vx *= 1.4; vy = max(vy, 55.0) }
                val bounty = planet.distance * .4 * (1 + level(Tech.ORBIT) * .12 + level(Tech.ASTRAL) * .08) *
                    (if (nodeEffect(Tech.ORBIT, 7)) 1.35 else 1.0) * (if (nodeEffect(Tech.ORBIT, 23)) 1.5 else 1.0) *
                    (1 + relicLevels[2] * .2) * (1 + relicLevels[9] * .1) *
                    (if (hasClubPerk(10)) 1.4 else 1.0) * (if (hasClubPerk(9)) 1.35 else 1.0) *
                    (if (twinLaunch) 2.0 else 1.0) * multiplier * (if(gravityPulseFor>0) 1+.06*skillRank(FlightAbility.GRAVITY,6) else 1.0)
                earned += bounty; cash += bounty
                message = "${planet.name} milestone! +$${format(bounty)}"; cue("planet")
                save()
            }
            val maxCarries = 1 + (if (nodeEffect(Tech.PLANES, 15)) 1 else 0) + (if (nodeEffect(Tech.PLANES, 23)) 1 else 0)
            if (level(Tech.PLANES) > 0 && planeCarries < maxCarries && flightTime > 2.5 + planeCarries * 5.0 && altitude > 5) {
                if (!expedition && relicLevels[5] > 0 && relicLevels[6] > 0 && !cargoBall) { cargoBall = true; showCombo(2) }
                planeCarries++; planeSpriteFor = if (nodeEffect(Tech.PLANES, 7)) 2.5 else 1.8; cue("plane")
                vx += (70 + level(Tech.PLANES) * 30) * multiplier * (1 + relicLevels[6] * .10) * (if (nodeEffect(Tech.PLANES, 7)) 1.28 else 1.0) * (if (nodeEffect(Tech.PLANES, 8)) 1.15 else 1.0)
                vy = max(vy, 35.0 + level(Tech.PLANES) * 8); message = "✈ $aircraftName carry #$planeCarries!"
            }
            if (altitude <= 0) {
                altitude = 0.0
                val landedImpact = vy < -1.0
                if (landedImpact) {
                    bounceCount++
                    if(electrifiedFor>0 && bounceCount<=3) vx*=1+.03*skillRank(FlightAbility.LIGHTNING,8)
                    if (bounceArmed || thunderArmed) {
                        vy = -max(abs(vy) * 1.25, 35.0); vx *= 1.15
                        if (thunderArmed) { showCombo(0); electrify(2.0) }
                        bounceArmed = false; thunderArmed = false
                    }
                }
                val restitution = min(0.9, 0.34 + level(Tech.BOUNCE) * 0.035 + (if (hasClubPerk(1) && bounceCount == 1) .12 else 0.0) + (if (nodeEffect(Tech.BOUNCE, 7)) 0.06 else 0.0))
                if (landedImpact && nodeEffect(Tech.BOUNCE, 15)) { val shock = min(10000.0, abs(vy) * 2); earned += shock; cash += shock }
                if (landedImpact && nodeEffect(Tech.BOUNCE, 8) && bounceCount == 1) vy *= 1.28
                if (landedImpact && nodeEffect(Tech.FRICTION, 7) && bounceCount == 1) vx *= 1.16
                if (landedImpact && nodeEffect(Tech.FRICTION, 8) && bounceCount == 1) vx *= 1.08
                val specialBounce=landedImpact && bounceCount<=3 && ((airliftFor>0 && skillRank(FlightAbility.AIRLIFT,8)>0) || (gravityPulseFor>0 && skillRank(FlightAbility.GRAVITY,9)>0))
                if ((abs(vy) > 11 || specialBounce) && bounceCount < (if (nodeEffect(Tech.BOUNCE, 23)) 60 else 28)) {
                    vy = abs(vy) * restitution
                    if(bounceCount<=3 && airliftFor>0 && skillRank(FlightAbility.AIRLIFT,8)>0) vy=max(vy,25.0+10*skillRank(FlightAbility.AIRLIFT,8))
                    if(bounceCount<=3 && gravityPulseFor>0 && skillRank(FlightAbility.GRAVITY,9)>0) vy=max(vy,15.0*skillRank(FlightAbility.GRAVITY,9))
                    vx *= if (airliftFor > 0) 1.0 else max(0.6, 0.82 + level(Tech.FRICTION) * 0.012 + (if (nodeEffect(Tech.BOUNCE, 3)) 0.03 else 0.0))
                } else {
                    vy = 0.0
                    val groundDrag = (0.7 / (1 + level(Tech.FRICTION) * 0.35)) *
                        (if (nodeEffect(Tech.FRICTION, 23)) .25 else if (nodeEffect(Tech.FRICTION, 15)) .55 else 1.0) *
                        (if (electrifiedFor > 0) .08 else 1.0) / (1 + relicLevels[4] * .08)
                    vx *= (1 - (if(airliftFor>0 || (gravityPulseFor>0 && skillRank(FlightAbility.GRAVITY,9)>0)) 0.0 else groundDrag) * h).coerceAtLeast(0.0)
                    if (vx < 2 && nodeEffect(Tech.POWER, 15) && !secondSwingUsed) {
                        secondSwingUsed = true; vx = max(30.0, club.swing * .5); vy = 15.0; message = "↗ Second Swing!"
                    } else if (vx < 2 || flightTime >= 240) finish()
                }
            }
            speed = hypot(vx, vy)
            if (flightTime >= 240) finish()
        }
        if (shotElapsed >= 24.5 && phase == Phase.FLYING) finish()
    }
    private fun finish() {
        if (phase != Phase.FLYING) return
        phase = Phase.LANDED; speed = 0.0; altitude = 0.0
        val buildCash=(1+caddyBonus(1))*(1+(if(usedLightning) .02*skillRank(FlightAbility.LIGHTNING,6) else 0.0)+(if(airliftFor>0) .02*skillRank(FlightAbility.AIRLIFT,6) else 0.0)+(if(abilityUses[FlightAbility.ROCKET.ordinal]>0) .02*skillRank(FlightAbility.ROCKET,6) else 0.0))
        val payout = buildCash * (if (expedition) 1.0 else 1 + (engagement.facilities[0] + engagement.facilities[6]) * .02) * max(1.0, (20 + distance * (0.06 + combo * 0.015)) * golfer.cashBonus * (1 + relicLevels[1] * .18) * (1 + relicLevels[9] * .10) * (if (twinLaunch) 2.0 else 1.0) * (if (hasClubPerk(13)) 1.25 else 1.0) * (if (nodeEffect(Tech.ASTRAL, 7)) 1.2 else 1.0) * (1 + level(Tech.ASTRAL) * 0.18 + (if (nodeEffect(Tech.POWER, 5)) 0.15 else 0.0) + (if (nodeEffect(Tech.POWER, 8) && charge > .85) .10 else 0.0) + (if (nodeEffect(Tech.ASTRAL, 23)) combo * .12 else 0.0)) * multiplier)
        earned += payout; cash += payout; lifetimeDistance += distance; bestDistance = max(bestDistance, distance)
        if (!expedition) { abilityProgress.recordBest(distance);caddies.recordShot() }
        engagement.recordShot(distance, shotPerfect, shotClears, usedLightning, combo, expedition)
        message = if (expedition) "Expedition: ${format(distance)} m • medals ${engagement.expeditionMedals}/3" else "${format(distance)} m • +$${format(earned)}"
        save(); cue("land")
    }
    fun buyNode(node: ResearchNode) {
        if (node.id !in ResearchTree.byId || owns(node) || !unlocked(node) || cash < node.cost) return
        cash -= node.cost; purchased.add(node.id)
        if (node.ability != null && node.tier == 0) {
            val slot = engagement.slots.indexOfFirst { !abilityUnlocked(it) }
            if (slot >= 0) engagement.select(slot,node.ability)
        }
        save(); cue("purchase")
    }
    fun buyClub(id: Int) {
        if (id != ownedClub + 1 || id !in clubs.indices || cash < clubs[id].cost) return
        cash -= clubs[id].cost; ownedClub = id; equippedClub = id; save(); cue("purchase")
    }
    fun equipClub(id: Int) {
        if (id !in clubs.indices || id > ownedClub || phase == Phase.FLYING) return
        equippedClub = id; save()
    }
    fun upgradeClub() {
        val price = clubUpgradeCost()
        if (cash < price || clubLevels[equippedClub] >= 30) return
        cash -= price; clubLevels[equippedClub]++
        if (clubLevels[equippedClub] % 10 == 0) clubMilestones[equippedClub] =
            max(clubMilestones[equippedClub], clubLevels[equippedClub] / 10)
        save(); cue("purchase")
    }
    fun buyGolfer(id: Int) {
        if (id !in golfers.indices || id != nextGolferUnlock || cash < golfers[id].cost) return
        cash -= golfers[id].cost; ownedGolfers.add(id); selectedGolfer = id; save()
    }
    fun equipGolfer(id: Int) {
        if (id !in ownedGolfers) return
        selectedGolfer = id; save()
    }
    fun buyBall(id: Int) {
        if (id !in balls.indices || id != nextBallUnlock || relicBank < balls[id].relicCost) return
        relicBank -= balls[id].relicCost; ownedBalls.add(id); selectedBall = id; save()
    }
    fun equipBall(id: Int) {
        if (id !in ownedBalls) return
        selectedBall = id; save()
    }
    fun buyApparel(id: String) {
        val item = apparel.firstOrNull { it.id == id } ?: return
        if (id in ownedApparel || relicBank < item.price) return
        relicBank -= item.price; ownedApparel.add(id); save()
    }
    fun ascend() {
        if (!ascendAvailable) return
        val gained = ascensionReward
        relics += gained; relicBank += gained; ascensions++
        cash = 0.0; lifetimeDistance = 0.0; bestDistance = 0.0; launches = 0; ownedClub = 0; equippedClub = 0
        clubLevels.indices.forEach { clubLevels[it] = 0 }; purchased.clear()
        runDestroyed.indices.forEach { runDestroyed[it] = false }
        phase = Phase.READY; message = "Ascended! +$gained permanent relics"; save()
    }
    private fun save() {
        if (expedition) return
        val json = JSONObject().apply {
            put("cash", cash); put("lifetime", lifetimeDistance); put("best", bestDistance); put("launches", launches)
            put("club", ownedClub); put("equippedClub", equippedClub); put("relics", relics); put("ascensions", ascensions)
            put("rosterVersion", 2); put("relicBank", relicBank); put("selectedGolfer", selectedGolfer)
            put("selectedBall", selectedBall); put("ownedBalls", JSONArray(ownedBalls.toList()))
            put("ownedGolfers", JSONArray(ownedGolfers.toList())); put("ownedApparel", JSONArray(ownedApparel.toList()))
            put("clubLevels", JSONArray(clubLevels)); put("clubMilestones", JSONArray(clubMilestones)); put("nodes", JSONArray(purchased.toList()))
            put("discoveredRelics", JSONArray(discoveredRelics.toList())); put("relicLevels", JSONArray(relicLevels))
            put("destroyed", JSONArray(destroyed))
        }
        prefs.edit().putString("save", json.toString()).apply(); revision++
    }
    private fun restore() {
        val json = runCatching { JSONObject(prefs.getString("save", "{}") ?: "{}") }.getOrDefault(JSONObject())
        cash = json.optDouble("cash", 0.0).takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0; lifetimeDistance = json.optDouble("lifetime", 0.0).takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0
        bestDistance = json.optDouble("best", 0.0).takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0; launches = json.optInt("launches").coerceAtLeast(0)
        ownedClub = json.optInt("club").coerceIn(clubs.indices)
        equippedClub = json.optInt("equippedClub", ownedClub).coerceIn(0, ownedClub)
        relics = json.optInt("relics").coerceAtLeast(0); ascensions = json.optInt("ascensions").coerceAtLeast(0)
        relicBank = json.optInt("relicBank", relics).coerceAtLeast(0)
        val oldRoster = json.optInt("rosterVersion", 1) < 2
        fun rosterIndex(index: Int) = if (oldRoster && index >= 4) index + 1 else index
        json.optJSONArray("ownedGolfers")?.let { list -> repeat(list.length()) { rosterIndex(list.optInt(it)).takeIf { n -> n in golfers.indices }?.let(ownedGolfers::add) } }
        selectedGolfer = rosterIndex(json.optInt("selectedGolfer")).coerceIn(golfers.indices).takeIf { it in ownedGolfers } ?: 0
        json.optJSONArray("ownedBalls")?.let { list -> repeat(list.length()) { list.optInt(it).takeIf { n -> n in balls.indices }?.let(ownedBalls::add) } }
        selectedBall = json.optInt("selectedBall").coerceIn(balls.indices).takeIf { it in ownedBalls } ?: 0
        json.optJSONArray("ownedApparel")?.let { list -> repeat(list.length()) { list.optString(it).takeIf { id -> apparel.any { a -> a.id == id } }?.let(ownedApparel::add) } }
        val levels = json.optJSONArray("clubLevels") ?: JSONArray()
        val nodes = json.optJSONArray("nodes")
        if (nodes != null) { repeat(nodes.length()) { nodes.optString(it).takeIf { id -> ResearchTree.all.any { n -> n.id == id } }?.let(purchased::add) } }
        else { // Carry forward older research saves into the new named tree.
            val old = json.optJSONArray("techLevels") ?: JSONArray()
            (0 until min(6, old.length())).forEach { branch ->
                (0 until min(8, old.optInt(branch))).forEach { tier -> purchased.add("${Tech.entries[branch].name}-$tier") }
            }
        }
        if (!abilityProgress.migrated && !expedition) {
            val oldIds = (0 until (nodes?.length() ?: 0)).map { nodes!!.optString(it) }.toSet()
            cash += ResearchTree.legacyAbilities.filter { it.id in oldIds }.sumOf { it.cost }
            val historical = planets.indices.filter { json.optJSONArray("destroyed")?.optBoolean(it) == true }.maxOfOrNull { planets[it].distance } ?: 0.0
            abilityProgress.recordBest(max(bestDistance,historical))
            // Commit the refund before this constructor exits, so opening again cannot duplicate it.
            prefs.edit().putString("save",json.put("cash",cash).put("nodes",JSONArray(purchased.toList())).toString()).commit()
            abilityProgress.markMigrated()
        }
        val dead = json.optJSONArray("destroyed") ?: JSONArray()
        val milestones = json.optJSONArray("clubMilestones") ?: JSONArray()
        clubLevels.indices.forEach {
            clubLevels[it] = levels.optInt(it).coerceIn(0, 30)
            clubMilestones[it] = max(milestones.optInt(it), clubLevels[it] / 10).coerceIn(0, 3)
        }
        val discovered = json.optJSONArray("discoveredRelics") ?: JSONArray()
        repeat(discovered.length()) { discovered.optInt(it).takeIf { id -> id in ascensionRelics.indices }?.let(discoveredRelics::add) }
        val ranks = json.optJSONArray("relicLevels") ?: JSONArray()
        relicLevels.indices.forEach { relicLevels[it] = ranks.optInt(it).coerceIn(0, ascensionRelics[it].maxRank) }

        destroyed.indices.forEach { destroyed[it] = dead.optBoolean(it) }
        revision++
    }
}
