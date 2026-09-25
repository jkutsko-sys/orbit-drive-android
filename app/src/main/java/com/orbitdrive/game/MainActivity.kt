package com.orbitdrive.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private val Night = Color(0xff091022)
private val Card = Color(0xff172238)
private val Mint = Color(0xff80ffbf)
private val Muted = Color(0xffa6afc3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashPrefs = getSharedPreferences("orbit_crash_report", MODE_PRIVATE)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            crashPrefs.edit().putString("last", error.stackTraceToString().take(4000)).commit()
            previousHandler?.uncaughtException(thread, error)
        }
        val game = GameEngine(this)
        setContent { OrbitApp(game, crashPrefs.getString("last", null)) { crashPrefs.edit().remove("last").apply() } }
    }
}

@Composable private fun OrbitApp(game: GameEngine, previousCrash: String?, clearCrash: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var crash by remember { mutableStateOf(previousCrash) }
    if (crash != null) AlertDialog(onDismissRequest = { crash = null; clearCrash() },
        title = { Text("Previous launch ended unexpectedly") },
        text = { Text(crash ?: "", fontSize = 10.sp) },
        confirmButton = { TextButton(onClick = { crash = null; clearCrash() }) { Text("Close") } })
    val pages = listOf("Range", "Research", "Clubs", "Ascend")
    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, surface = Card, background = Night)) {
        Scaffold(containerColor = Night, bottomBar = {
            NavigationBar(containerColor = Card) {
                pages.forEachIndexed { index, title ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index },
                        icon = { Icon(listOf(Icons.Default.SportsGolf, Icons.Default.Science, Icons.Default.ShoppingBag, Icons.Default.AutoAwesome)[index], null) },
                        label = { Text(title) })
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> RangeScreen(game, true)
                    1 -> ResearchScreen(game)
                    2 -> ClubsScreen(game)
                    else -> AscendScreen(game)
                }
            }
        }
    }
}

@Composable private fun Header(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(title, fontWeight = FontWeight.Black, fontSize = 27.sp, letterSpacing = 1.sp, color = Color.White)
        Text(subtitle, fontSize = 13.sp, color = Muted)
    }
}
@Composable private fun CardBox(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(17.dp)).padding(16.dp), content = content)
}
@Composable private fun Pill(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.background(Color.White.copy(alpha = .07f), RoundedCornerShape(9.dp)).padding(9.dp), color = Color.White, fontSize = 12.sp)
}

@Composable private fun RangeScreen(game: GameEngine, visible: Boolean) {
    LaunchedEffect(game, visible) {
        var last = System.nanoTime()
        while (visible) {
            delay(16)
            val now = System.nanoTime()
            game.tick(((now - last) / 1e9).coerceAtMost(.05))
            last = now
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("ORBIT DRIVE", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("THE INFINITE RANGE", fontSize = 10.sp, letterSpacing = 3.sp, color = Mint)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$${game.format(game.cash)}", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Mint)
                Text("BEST ${game.format(game.bestDistance)} m", fontSize = 10.sp, color = Muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("DISTANCE" to "${game.format(game.distance)} m", "BALL SPEED" to "${game.format(game.speed)} m/s", "ALTITUDE" to "${game.format(game.altitude)} m")
                .forEach { (label, value) ->
                    Column(Modifier.weight(1f).background(Card, RoundedCornerShape(10.dp)).padding(9.dp)) {
                        Text(label, fontSize = 9.sp, color = Muted)
                        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                    }
                }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Card, RoundedCornerShape(17.dp))) {
            RangeArt(game, Modifier.fillMaxSize())
            game.nextPlanet?.let { planet ->
                Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp)
                    .background(Color.Black.copy(alpha = .55f), RoundedCornerShape(11.dp)).padding(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("NEXT: ${planet.name.uppercase()}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        Text("${game.format(planet.distance)} m", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Mint)
                    }
                    LinearProgressIndicator(progress = { (game.planetHP / planet.hp).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(5.dp), color = Mint, trackColor = Color.White.copy(alpha = .2f))
                    Text("PLANET HP ${game.format(game.planetHP)} / ${game.format(planet.hp)}", fontSize = 9.sp, color = Muted)
                }
            }
        }
        Text(game.message, Modifier.fillMaxWidth(), color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(game.club.name.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${game.launches} swings • ${game.relics} relics", fontSize = 10.sp, color = Muted)
            }
            if (game.phase == Phase.FLYING) {
                Button(onClick = game::lightning, enabled = game.lightningCharges > 0, modifier = Modifier.height(60.dp)) {
                    Text("⚡ ${game.lightningCharges}", fontWeight = FontWeight.Black)
                }
            } else {
                // Press starts charging immediately; lift releases. The pointer gesture also handles short taps.
                Box(Modifier.width(170.dp).height(65.dp).background(Mint, RoundedCornerShape(15.dp)).testTag("launch")
                    .pointerInput(game) {
                        detectTapGestures(onPress = {
                            game.startCharge()
                            try { tryAwaitRelease() } finally { game.release() }
                        })
                    }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(progress = { game.charge.toFloat() }, modifier = Modifier.width(130.dp).height(5.dp), color = Color(0xfffeac5b), trackColor = Night.copy(alpha = .2f))
                        Text("HOLD & RELEASE", color = Night, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text("LAUNCH", color = Night, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable private fun RangeArt(game: GameEngine, modifier: Modifier) {
    Canvas(modifier) {
        val space = game.distance > 7000
        drawRect(brush = Brush.verticalGradient(if (space) listOf(Color(0xff08051c), Color(0xff201038)) else listOf(Color(0xff194d79), Color(0xff66b7bc))))
        repeat(42) { i ->
            val x = ((i * 173 + 41) % 997) / 997f * size.width
            val y = ((i * 313 + 19) % 991) / 991f * size.height * .68f
            drawCircle(Color.White.copy(alpha = if (space) .75f else .15f), radius = if (i % 4 == 0) 2.5f else 1f, center = Offset(x, y))
        }
        val ground = size.height * .8f
        drawRect(if (space) Color(0xff302746) else Color(0xff1e664d), topLeft = Offset(0f, ground), size = Size(size.width, size.height - ground))
        repeat(6) { i -> drawLine(Color.White.copy(alpha = .08f), Offset(0f, ground + i * 22f), Offset(size.width, ground + i * 22f)) }
        val ballX = size.width * if (game.phase == Phase.READY || game.phase == Phase.CHARGING) .18f else .36f
        val height = (ln(1 + max(0.0, game.altitude)) / ln(200.0)).toFloat() * size.height * .55f
        val ballY = max(size.height * .15f, ground - 10 - height)
        drawCircle(Color.Black.copy(alpha = .25f), radius = 12f, center = Offset(ballX, ground))
        if (game.phase == Phase.FLYING) drawLine(Mint.copy(alpha = .65f), Offset(ballX - min(130f, (game.speed * .3).toFloat()), ballY + 20), Offset(ballX - 8, ballY + 2), strokeWidth = 6f)
        drawCircle(Color.White, radius = 9f, center = Offset(ballX, ballY))
        game.nextPlanet?.let { planet ->
            if (planet.distance - game.distance < max(2500.0, planet.distance * .15)) {
                val progress = 1 - max(0.0, (planet.distance - game.distance) / max(2500.0, planet.distance * .15))
                val x = size.width * (.92f - .48f * progress.toFloat())
                val center = Offset(x, ground * .42f)
                val radius = if (planet.distance > 100000) 48f else 30f
                drawCircle(planet.color, radius, center)
                if (game.planetHP < planet.hp) {
                    drawLine(Color.Black.copy(alpha = .8f), Offset(x - 12, center.y - 15), Offset(x + 4, center.y), strokeWidth = 3f)
                    drawLine(Color.Black.copy(alpha = .8f), Offset(x + 4, center.y), Offset(x - 6, center.y + 18), strokeWidth = 3f)
                }
            }
        }
    }
}

@Composable private fun ResearchScreen(game: GameEngine) {
    val revision = game.revision
    var selected by remember { mutableStateOf(Tech.POWER) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Header("RESEARCH CONSTELLATION", "64 named discoveries across eight branching paths.")
            Text("$${game.format(game.cash)} CASH  •  ${ResearchTree.all.count(game::owns)}/64 DISCOVERED", color = Mint, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tech.entries.forEach { tech ->
                    FilterChip(selected = selected == tech, onClick = { selected = tech },
                        label = { Text("${tech.title} ${game.level(tech)}/8") })
                }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            item {
                Text(selected.description.uppercase(), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text("Buy connected discoveries with cash. Forks join at tier six.", color = Muted, fontSize = 12.sp)
            }
            items(ResearchTree.nodes(selected)) { node ->
                val owned = game.owns(node)
                val unlocked = game.unlocked(node)
                val parentNames = node.requires.mapNotNull { id -> ResearchTree.all.find { it.id == id }?.name }
                CardBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (owned) "◆" else if (unlocked) "◇" else "○", color = if (owned) Mint else Muted, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(node.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(node.effect, color = Mint, fontSize = 12.sp)
                        }
                        Text("T${node.tier + 1}", color = Muted, fontSize = 11.sp)
                    }
                    if (parentNames.isNotEmpty()) {
                        Spacer(Modifier.height(7.dp))
                        Text("↳ ${parentNames.joinToString(" + ")}", color = Muted, fontSize = 11.sp)
                    }
                    if (!owned) {
                        Spacer(Modifier.height(9.dp))
                        Button(onClick = { game.buyNode(node) }, enabled = unlocked && game.cash >= node.cost,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (unlocked) "DISCOVER  •  $${game.format(node.cost)}" else "LOCKED • REQUIRES CONNECTED NODE")
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun ClubsScreen(game: GameEngine) {
    val revision = game.revision
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Header("THE PRO SHOP", "15 drivers from junkyard to deep space. Upgrade each club’s swing.") }
        items(game.clubs.indices.toList()) { id ->
            val club = game.clubs[id]
            CardBox {
                Text(club.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(if (id == game.ownedClub) "EQUIPPED • LEVEL ${game.clubLevel(id)}" else if (id < game.ownedClub) "OWNED" else "LOCKED", color = Mint, fontSize = 10.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("SWING ${game.format(club.swing * 1.15.pow(game.clubLevel(id)))}")
                    Pill("LOFT ${club.loft.toInt()}°")
                    Pill("SMASH ${"%.2f".format(club.smash)}×")
                }
                if (id == game.ownedClub) {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = game::upgradeClub, enabled = game.clubLevel(id) < 30 && game.cash >= game.clubUpgradeCost(), modifier = Modifier.fillMaxWidth()) {
                        Text("UPGRADE CLUB  •  $${game.format(game.clubUpgradeCost())}")
                    }
                } else if (id == game.ownedClub + 1) {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { game.buyClub(id) }, enabled = game.cash >= club.cost, modifier = Modifier.fillMaxWidth()) {
                        Text("BUY CLUB  •  $${game.format(club.cost)}")
                    }
                }
            }
        }
    }
}

@Composable private fun AscendScreen(game: GameEngine) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("THE LONG DRIVE", "Leave the range behind. Keep the power you earned.")
        CardBox {
            Spacer(Modifier.height(20.dp))
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(70.dp).align(Alignment.CenterHorizontally), tint = Mint)
            Text("ASCEND", Modifier.fillMaxWidth(), fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
            Text("Restart your run and forge permanent relics. Every relic multiplies launch speed, planet damage, and earnings by 1.35×.",
                Modifier.fillMaxWidth().padding(14.dp), color = Muted, textAlign = TextAlign.Center)
            Text("${game.relics} RELICS  •  ${"%.2f".format(game.multiplier)}× POWER  •  ${game.ascensions} ASCENSIONS", Modifier.fillMaxWidth(), color = Mint, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text("Potential relics: +${game.potentialRelics}", Modifier.fillMaxWidth(), color = Mint, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Unlock at 45K m best distance. Relics depend on total distance driven this run.",
                Modifier.fillMaxWidth().padding(12.dp), color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Button(onClick = { confirm = true }, enabled = game.ascendAvailable, modifier = Modifier.fillMaxWidth()) { Text("ASCEND AND RESET RUN") }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Ascend for +${game.potentialRelics} relics?") },
        text = { Text("Cash, clubs, research, and planet progress reset. Permanent relics remain.") },
        confirmButton = { TextButton(onClick = { game.ascend(); confirm = false }) { Text("Ascend") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
}
