# Orbit Drive — Android prototype

Native Kotlin/Jetpack Compose companion to the iPhone prototype. Gameplay includes launch timing, flight and bounce simulation, eight branching research paths with 64 named discoveries, 15 clubs, planet HP and persistent damage, and ascension. Saves remain on device.

## Run

Open this folder in Android Studio, sync the Gradle project, and run `app` on an Android 8.0 (API 26) or newer emulator or device. The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21, JDK 17, compile SDK 35 and Gradle 8.9. If Android Studio asks for a Gradle distribution, select Gradle 8.9. A wrapper is not bundled because this environment has no Gradle installation or Android SDK to generate and verify it.

Change `applicationId` before Play Store release. The current project is a playable first prototype, not a signed release bundle. Production work remains: device playtesting, polish, original artwork/audio, accessibility, balance, store listing, privacy/support information and signing. It has no ads or purchases yet. Android and iPhone saves are separate; there is no account sync.

## Build on GitHub using only a phone

This project includes `.github/workflows/android-apk.yml`. Put the **contents** of this folder at the root of a GitHub repository (keep the hidden `.github` folder). In GitHub, open Actions → Build Android test APK → Run workflow. When green, open the run → Artifacts → OrbitDrive-Android-test-APK. Download the artifact ZIP to your Android phone, extract `app-debug.apk`, tap it, and allow installation from your browser or file manager if prompted. This is a debug APK for personal testing. It is not suitable for Google Play publication.

The first cloud build is the actual compile check. If it fails, keep the workflow log; a source-only review cannot establish the APK works.

## If a swing still crashes

Restart the app. The next launch displays the captured exception; take a screenshot and send it to the developer. The swing handler was replaced with a lifecycle-safe press gesture in this build, but only a physical-device test can confirm the original failure is gone.

## Version 0.2

The fresh-install first-swing crash was reproduced in the emulator (`org.json.JSONException: Forbidden numeric value: NaN`) and fixed by giving missing save values finite defaults. The cloud workflow now runs a launch-and-swing emulator smoke test before publishing its test APK.

## Version 0.3

The research tree is now one radial canvas centered on the golfer. Drag to pan, pinch or use +/- to zoom, tap a node for its name, prerequisite and price. Golfer roster prices follow their stat bonuses, and golfers must be bought in order. Apparel is permanent and bought with an unspent relic balance. Ground scenery scrolls with distance; four sparse obstacles can reduce speed if hit. The tee shows the selected golfer and club.

This preview uses `com.orbitdrive.game.preview` and a repository-held **test-only** signing key so future preview APKs can update in place. The key is public and must never be used for a store release or sensitive app. The prior test package has a different ID and separate local save.

## Version 0.4

Affordable research nodes pulse gently. Ball relics are permanent sequential unlocks bought with unspent relics; each has a different appearance and launch multiplier. Cappy Gilmore joins the golfer roster as a mid-tier capybara. Vector golfer portraits and tee sprites show the selected golfer, with distinct capybara features. Scenery and meter flags move faster to make ball travel visible.

## Version 0.5

The research constellation contains 192 unique purchasable nodes, with added split-and-merge routes and three large keystones per branch. The Stormglass Core keystone electrifies the ball when lightning is tapped, dramatically reducing drag and breaking struck obstacles instead of slowing. Aircraft, lightning and planet impact now have animated vector effects; a shattered planet cracks, bursts into fragments and sends out a shock ring.

## Preview 0.6

The eight research branches contain three effect keystones each. Clubs now have individual perks and previously purchased clubs can be equipped from the shop. Carbon Slice launches two visible balls, doubles shot income and reduces launch speed by 18%; the final Zenith club combines earlier club perks without that penalty. Settings include separate music and effects controls, original procedural range/orbit/deep-space arrangements, and a one-time `ADMIN` preview voucher worth $10,000. The voucher only works in a debuggable build and remains redeemed through ascension.

## Preview 0.7

Shots now accelerate through the existing physics timeline as they travel farther and finish within 24.5 seconds of active gameplay. The physics still integrates distance, drag, bounce, impacts and cash in the same simulation units; this changes playback duration rather than the club's reach. The emulator suite checks a long shot against both fine and coarse frame intervals and confirms that the next shot becomes available.

The Settings gear sits beside the cash total on the Range screen. An instrumented UI test verifies that the dialog opens and its voucher controls are accessible.

## Preview 0.8

Planets are distance checkpoints rather than health encounters: crossing one gives a bounty on each shot, while the first clear plays a longer cinematic and remains recorded across ascensions. The former Orbital Science branch is now Milestone Mapping; existing node IDs and purchases migrate intact. Flightpath aircraft progress from paper glider through propeller aircraft, jets, stealth bomber, rocket, and starship. Club purchase prices are spread farther apart and levels 10, 20, and 30 permanently grant a 20% power step for that club, even when its levels reset at ascension. Base shot income and relic power progression are rebalanced; ten randomly discoverable relic powers can be ranked up with increasingly expensive relic costs, including a bonus to relics earned on ascension. The preview `ADMIN` voucher can be redeemed repeatedly for $10,000 each time.

## Preview 0.9

The range camera is 18% closer to the world around the ball. Golfer sprites and roster portraits are larger with clearer clothing and facial details. Rocks, sheds and towers have larger silhouettes and added facets, roofs, doors and windows. Planets now have shaded surfaces, unique crater/band/ring details and bigger approach sprites; the artwork remains resolution-independent vector drawing. Physics, collision positions and rewards are unchanged.

## Preview 0.10

The enlarged range scene is clipped to its card, keeping the distance, speed and altitude HUD visible. The launch charge tracker shows projected total swing power in m/s, calculated from the same launch formula used when the ball is struck. In debuggable previews, each `ADMIN` voucher redemption grants $100,000 and can be used again for testing.

## Preview 0.11 — Orbit Drive Labs

This update uses a separate Android application ID (`com.orbitdrive.game.preview.labs`) and appears as **Orbit Drive Labs**. It starts with a fresh save and installs alongside v0.10. Keep the original Orbit Drive installed to retain and return to its existing progress. The complete v0.10 source is pinned on the `rollback/v0.10` branch at commit `f2766a987c029ca5ac64767b8da2d1f212233364`.

Open **CONTRACTS • HOME** on the Range screen for:

- Two selectable flight abilities from Lightning, Bounce, Rocket and Gravity, with one use per shot (research grants extra lightning charges).
- Three rotating contracts, range tickets, and permanent Clubhouse, Workshop, Trophy Room and Launch Pad upgrades.
- A combination book covering Thunder Skip, Slingshot Launch, Double Cargo and Magnetic Coast. The latter two require upgraded relic pairs.
- A weekly expedition with fixed Carbon Slice equipment and one of three repeating rule sets: low gravity without lightning, high rebound with low friction, or aircraft relay with extra lightning. Expedition shots do not alter the main range's cash, research or best distance.
- A six-tier weekly reward track. All play counts; no daily streak is required. Earned unclaimed tiers are automatically paid at rollover. Weekly expeditions rotate Monday at 00:00 UTC, use the device date offline, and have no competitive leaderboard.
- Three collectible ball trails and persistent expedition medals. Range buildings evolve across four eras, and a gold personal-best marker shows the distance to beat.

Existing 24.5-second active-flight pacing continues. The ADMIN testing voucher remains repeatable for $100,000 in debuggable builds. No analytics, online account or Google Play cloud save was added in this update.

## Labs 0.12 — Ability Academy

Research now contains 265 purchasable nodes. Abilities replaces Stormcalling as the parent branch, with an Academy gate and four independent 24-node paths, each with three keystones. Existing purchased Lightning nodes migrate intact and unlock the Academy. An ability must be researched before it can be equipped or used; expeditions share the main range's unlocks but retain fixed equipment and no ability research bonuses.

Bounce's slot becomes **Airlift**: activate it, then tap repeatedly for five real gameplay seconds to raise the ball without sacrificing horizontal velocity to the taps. Its tree extends the window, rewards tap rhythms, reduces gravity and adds altitude bursts. Rocket research adds extra tanks, sustained afterburner and orbital ignition bonuses. Gravity research adds zero gravity, additional charges, falling-velocity conversion and a pulse-end slingshot. Upgrades are described individually in Research.

Contracts and abilities have colored, illustrated buttons. Launch controls are taller with their labels shifted upward. This remains the separate Labs app and updates v0.11's save; the original v0.10 app and rollback branch are preserved.

## Labs 0.13 — Pro Shop range

The Pro Shop contains eight facilities with 12 levels each. Upgrade costs rise with level; every purchase adds a visible piece to the tee platform, clubhouse or skyline, and the facilities survive ascension. Existing facility levels and tickets carry forward. Contracts open a new goal four hours after each claim; the timer persists through closing the app and shots during cooldown do not advance that contract. The `TICKETS` voucher grants 1,000 tickets per use in debuggable previews. Activate Airlift from its ability button, then tap anywhere on the range scene for repeated lifts during its active window.


## Labs 0.14 — Ability builds and relic progression

The camera returns to the home tee after every shot, with the full Pro Shop campus scaled to fit the range. Last-shot distance remains in the HUD.

Abilities now live in Clubhouse → Skill Trees, separate from the 168 cash research nodes. Their 96 skills form three routes per ability; only one final keystone can be selected per ability. Fifteen first-time distance milestones (100 m to 80M m) grant 60 permanent skill points in total. Free full respecs are available between shots once active effects end, without resetting cooldowns. Points and builds survive ascension. Old cash ability purchases are refunded once; current best and previously destroyed distance milestones seed earned points.

Base timing (real time, including while offline): Lightning stores 2 charges and recharges one every 45s; Gravity stores 2 charges, recharges one every 90s and lasts 6s; Rocket stores 1 charge, recharges every 120s and burns for 8s; Air Dribble lasts 60s across shots with a 600s cooldown beginning at activation. Charges recharge sequentially. Research and relics modify capacities and durations; cooldown relic reductions are capped at 20%. Air Dribble taps are made on the range or ability button. Timers are saved locally and use the device clock.

The exponential power multiplier from lifetime ascension currency is removed. Ascension itself grants no stats. There are now 34 relics, including 24 ability modifiers, with the new relics capped at 6 or 10 ranks. Existing original relic ranks are retained. The first new collection discovery guarantees Titan Grip; subsequent discoveries are random. A discovery now grants rank 1 immediately, and the larger collection uses a gentler discovery cost curve. Existing low-distance saves can feel weaker after the removal of the automatic multiplier; ability builds and relic upgrades supply the replacement progression.

The app remains Orbit Drive Labs with the same signing key and application ID. This APK updates the existing Labs save; the original preview app stays separate.


## Labs 0.15 — Ranked trees and the Caddyshack

Ability trees now use 40 illustrated nodes: an unlock and nine focused skills per ability. Main skills support 10 ranks; final keystones support five, with one final keystone per ability. The second and third rows require tree levels 8 and 18 plus rank 3 in the connected parent. Each rank counts as one tree level. Higher ranks cost more points. Existing milestone points are preserved and prior ability allocations are refunded automatically, while cooldowns and durations remain saved.

The Caddyshack is a separate popup on the Range screen. Eight original caddies provide an equipped bonus and tap assists (five per shot, two seconds apart). The first recruit is free immediately; subsequent random recruits arrive every four hours, with up to three banked. Cards grant 125 XP and the equipped caddy earns 5 XP per normal range shot; each level needs 125 XP, capped at level 120. Unequipped passive contribution is 25% at level 10, 60% at 40, and 100% at 80. Equipped and passive contributions never double count. Caddies and XP survive ascension.

Music uses quieter original soft-key melodies and warm sine-wave chords at approximately 77 BPM, with no square-wave lead or kick. Existing volume preferences are retained. The Germane Slammer (1.2B cash, 1.78x launch, 1.40x cash) and Klyde Birkshire (8B cash, 2x launch, 1.52x cash) extend the sequential golfer roster with new tee portraits.


## Labs 0.16 — Responsive layouts and complete flights
Range controls reflow to available width; cramped/large-text layouts scroll instead of clipping the canvas and launch controls. Menu bodies, settings, ascension, and research details remain scrollable. Ability graphs expand with font scale and pan horizontally when needed; club stats wrap. Orientation is no longer locked.

Flight pacing now compresses long airborne arcs, gives the first three landings readable combo windows, and accelerates the late bounces and natural roll-out. Physics completion requires ground speed below the stop threshold, never an elapsed-time cutoff. Ordinary shots target roughly 25 seconds; active sustained abilities can extend them. The previous 240-simulation-second truncation is removed, so powerful shots can earn additional distance from their formerly missing bounces/roll. Cooldowns and durations continue to use real time.

Regression coverage includes high-power landing/combo/rollout, no timer cutoff during an active ability, frame-step consistency, and UI screenshots at default, enlarged text/display, and tablet/landscape dimensions.
