package com.orbitdrive.game

internal object AbilityResearch {
    const val HUB = "ABILITY-HUB"
    fun prefix(ability: FlightAbility) = when (ability) {
        FlightAbility.LIGHTNING -> "LIGHTNING"
        FlightAbility.AIRLIFT -> "AIRLIFT"
        FlightAbility.ROCKET -> "ROCKET"
        FlightAbility.GRAVITY -> "PULSE"
    }
    private val lift = listOf(
        "Sky Dribble" to "Unlock Airlift: tap repeatedly for 5 real seconds; each tap raises altitude without reducing forward speed.",
        "Cloud Steps" to "+2 altitude per tap", "Long Breath" to "+1 second duration", "Updraft Palm" to "+5 vertical impulse per tap",
        "Feather Rhythm" to "Every third tap gives double altitude", "Air Pocket" to "+1 second duration", "Rising Tempo" to "+2 altitude per tap",
        "KEYSTONE: Cloudwalker" to "While active, gravity is reduced by 75%", "Quick Soles" to "Tap recovery reduced to 0.05 seconds",
        "Lofty Ambition" to "+5 vertical impulse per tap", "Skyward Steps" to "+1 second duration", "Aerial Coins" to "+5 cash per tap",
        "Cloud Cushion" to "First activation cancels falling velocity", "Upper Atmosphere" to "+3 altitude per tap", "Breath Reserve" to "+1 second duration",
        "KEYSTONE: Sky Ladder" to "Every fifth tap adds a 30 m altitude burst", "Dancing Dimples" to "+5 vertical impulse per tap",
        "Airborne Dividend" to "Tap cash doubles", "Marathon Breath" to "+1 second duration", "Heavenward" to "+3 altitude per tap",
        "Gentle Landing" to "Airlift ends with an upward kick", "Rapid Ascent" to "Every third tap also boosts forward speed 2%",
        "Final Breath" to "+1 second duration", "KEYSTONE: Endless Stair" to "Every fifth tap extends duration by 0.25 seconds, up to 3 bonus seconds"
    )
    private val rocket = listOf(
        "Ignition Key" to "Unlock Rocket: one forward ignition per shot", "Fuel Mix" to "+15 ignition impulse", "Vector Nozzle" to "+10 upward impulse",
        "Hot Exhaust" to "Ignition electrifies the ball for 2 flight seconds", "Second Tank" to "+1 rocket use per shot", "Air Launch" to "+20% impulse when above 100 m",
        "Rich Burn" to "+15 ignition impulse", "KEYSTONE: Afterburner" to "Ignition gives 2 flight seconds of sustained forward thrust",
        "Long Flame" to "+1 second of afterburner", "Blue Propellant" to "+20 ignition impulse", "Vertical Stage" to "+15 upward impulse",
        "Salvage Jet" to "+50 cash per ignition", "Clean Burn" to "Ignore air drag during afterburner", "Pilot Coupling" to "Aircraft timing bonus rises to 60%",
        "Reserve Tank" to "+1 rocket use per shot", "KEYSTONE: Orbital Injection" to "Ignition above 1K m gives 40% more impulse",
        "Heavy Fuel" to "+20 ignition impulse", "Long Booster" to "+1 second afterburner", "Rocket Dividend" to "Ignition cash doubles",
        "Third Stage" to "+20 upward impulse", "Ion Exhaust" to "+2 flight seconds electrified after ignition", "Launch Window" to "+10% impulse while rising",
        "Hyper Fuel" to "+25 ignition impulse", "KEYSTONE: Star Engine" to "Each later ignition this shot is 25% stronger"
    )
    private val gravity = listOf(
        "Gravity Switch" to "Unlock Gravity: reduce gravity and lift the ball for 4 flight seconds", "Long Orbit" to "+1 second duration",
        "Soft Field" to "Gravity is reduced to 10% during the pulse", "Lift Reversal" to "Activation gives at least 35 upward velocity",
        "Second Pulse" to "+1 pulse use per shot", "Orbital Cash" to "+40 cash per pulse", "Extended Orbit" to "+1 second duration",
        "KEYSTONE: Zero Point" to "No gravity while pulse is active", "Vacuum Bubble" to "No air drag while pulse is active",
        "Gentle Ascent" to "+10 upward velocity on activation", "Long Eclipse" to "+1 second duration", "Magnetic Shell" to "Activation electrifies ball for 2 flight seconds",
        "Tidal Lift" to "+20 altitude on activation", "Moon Dividend" to "Pulse cash doubles", "Reserve Field" to "+1 pulse use per shot",
        "KEYSTONE: Orbit Sling" to "Activation converts part of falling velocity into forward speed", "Deep Orbit" to "+1 second duration",
        "Stellar Lift" to "+15 upward velocity on activation", "Planet Magnet" to "+25% planet bounties while pulse is active",
        "Wide Field" to "+30 altitude on activation", "Final Orbit" to "+1 second duration", "Star Dividend" to "+80 cash per pulse",
        "Aurora Shell" to "+2 flight seconds electrified on activation", "KEYSTONE: Singularity Exit" to "When the pulse ends, forward speed increases 15%"
    )
    private fun parents(prefix: String, tier: Int): List<String> = when (tier) {
        0 -> listOf(HUB)
        1, 2 -> listOf("$prefix-0")
        3 -> listOf("$prefix-1"); 4 -> listOf("$prefix-2")
        5 -> listOf("$prefix-3", "$prefix-4")
        6, 7 -> listOf("$prefix-${tier-1}")
        8, 9 -> listOf("$prefix-7"); 10 -> listOf("$prefix-8"); 11 -> listOf("$prefix-9")
        12 -> listOf("$prefix-10", "$prefix-11"); 13, 14 -> listOf("$prefix-12")
        15 -> listOf("$prefix-13", "$prefix-14"); 16, 17 -> listOf("$prefix-15")
        18 -> listOf("$prefix-16"); 19 -> listOf("$prefix-17"); 20 -> listOf("$prefix-18", "$prefix-19")
        21, 22 -> listOf("$prefix-20"); else -> listOf("$prefix-21", "$prefix-22")
    }
    fun extraNodes(): List<ResearchNode> = listOf(ResearchNode(HUB, Tech.LIGHTNING,-1,"Ability Academy","Open four specialized ability paths",75.0,emptyList())) +
        listOf(FlightAbility.AIRLIFT, FlightAbility.ROCKET, FlightAbility.GRAVITY).flatMap { ability ->
            val specs = when (ability) { FlightAbility.AIRLIFT -> lift; FlightAbility.ROCKET -> rocket; else -> gravity }
            val prefix = prefix(ability)
            specs.mapIndexed { tier, (name,effect) -> ResearchNode("$prefix-$tier",Tech.LIGHTNING,tier,name,effect,
                (if (ability == FlightAbility.AIRLIFT) 240.0 else 400.0) * Math.pow(1.85,tier.toDouble()),parents(prefix,tier),ability) }
        }
}
