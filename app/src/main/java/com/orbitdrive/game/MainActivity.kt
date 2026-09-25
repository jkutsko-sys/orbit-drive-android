package com.orbitdrive.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.hypot
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
    val pages = listOf("Range", "Research", "Clubs", "Golfer", "Ascend")
    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, surface = Card, background = Night)) {
        Scaffold(containerColor = Night, bottomBar = {
            NavigationBar(containerColor = Card) {
                pages.forEachIndexed { index, title ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index },
                        icon = { Icon(listOf(Icons.Default.SportsGolf, Icons.Default.Science, Icons.Default.ShoppingBag, Icons.Default.Person, Icons.Default.AutoAwesome)[index], null) },
                        label = { Text(title) })
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> RangeScreen(game, true)
                    1 -> ResearchScreen(game)
                    2 -> ClubsScreen(game)
                    3 -> GolferScreen(game)
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
        val drift = (game.distance * .18).toFloat()
        if (!space) {
            repeat(5) { i ->
                val x = ((i * size.width / 3 - drift * .35f) % (size.width + 160) + size.width + 160) % (size.width + 160) - 70
                val y = size.height * (.19f + (i % 3) * .12f)
                drawCircle(Color.White.copy(alpha = .45f), 22f, Offset(x, y))
                drawCircle(Color.White.copy(alpha = .45f), 16f, Offset(x + 21, y + 3))
                drawCircle(Color.White.copy(alpha = .45f), 15f, Offset(x - 19, y + 5))
            }
            repeat(4) { i ->
                val x = ((i * size.width / 2 - drift * .12f) % (size.width + 200) + size.width + 200) % (size.width + 200)
                drawLine(Color(0xff243b52), Offset(x, ground - 45), Offset(x, ground), strokeWidth = 22f)
                drawRect(Color(0xff253c54), Offset(x - 21, ground - 52), Size(42f, 8f))
            }
            repeat(3) { i ->
                val x = ((i * size.width / 2 - drift * .45f) % (size.width + 120) + size.width + 120) % (size.width + 120)
                val y = size.height * (.13f + i * .055f)
                drawLine(Color(0xff263d56), Offset(x, y), Offset(x + 8, y - 4), strokeWidth = 2f)
                drawLine(Color(0xff263d56), Offset(x + 8, y - 4), Offset(x + 16, y), strokeWidth = 2f)
            }
        }
        drawRect(if (space) Color(0xff302746) else Color(0xff1e664d), topLeft = Offset(0f, ground), size = Size(size.width, size.height - ground))
        repeat(6) { i -> drawLine(Color.White.copy(alpha = .08f), Offset(0f, ground + i * 22f), Offset(size.width, ground + i * 22f)) }
        repeat(12) { i ->
            val x = ((i * 90f - drift * 1.2f) % (size.width + 90) + size.width + 90) % (size.width + 90)
            drawLine(Color.White.copy(alpha = .17f), Offset(x, ground + 18), Offset(x + 25, ground + 18), strokeWidth = 2f)
        }
        val ballX = size.width * if (game.phase == Phase.READY || game.phase == Phase.CHARGING) .18f else .36f
        val height = (ln(1 + max(0.0, game.altitude)) / ln(200.0)).toFloat() * size.height * .55f
        val ballY = max(size.height * .15f, ground - 10 - height)
        drawCircle(Color.Black.copy(alpha = .25f), radius = 12f, center = Offset(ballX, ground))
        if (game.phase == Phase.FLYING) drawLine(Mint.copy(alpha = .65f), Offset(ballX - min(130f, (game.speed * .3).toFloat()), ballY + 20), Offset(ballX - 8, ballY + 2), strokeWidth = 6f)
        drawCircle(Color.White, radius = 9f, center = Offset(ballX, ballY))
        val spin = (game.distance * .12).toFloat()
        drawLine(Color(0xff263c5c), Offset(ballX + cos(spin) * 7f, ballY + sin(spin) * 7f),
            Offset(ballX - cos(spin) * 7f, ballY - sin(spin) * 7f), strokeWidth = 2f)
        if (game.phase == Phase.READY || game.phase == Phase.CHARGING) {
            val gx = ballX - 38f
            drawCircle(game.golfer.look, 10f, Offset(gx, ground - 57))
            drawLine(game.golfer.look, Offset(gx, ground - 47), Offset(gx + 4, ground - 20), strokeWidth = 10f)
            drawLine(Color(0xffe2e9ef), Offset(gx + 4, ground - 20), Offset(gx - 8, ground), strokeWidth = 5f)
            drawLine(Color(0xffe2e9ef), Offset(gx + 4, ground - 20), Offset(gx + 17, ground), strokeWidth = 5f)
            val clubColor = listOf(Color.Gray, Color(0xffc5a77e), Color.Cyan, Color(0xffb4d0d5), Color(0xffffc36a), Color(0xfff07d74), Color.Magenta)[game.ownedClub % 7]
            val sweep = if (game.phase == Phase.CHARGING) game.charge.toFloat() * 42f else 0f
            drawLine(Color(0xfff4dfbe), Offset(gx + 4, ground - 38), Offset(gx + 19, ground - 38 - sweep * .25f), strokeWidth = 4f)
            drawLine(clubColor, Offset(gx + 19, ground - 38 - sweep * .25f), Offset(ballX - 5, ground - 13 - sweep), strokeWidth = 3f + game.ownedClub * .13f)
            drawLine(clubColor, Offset(ballX - 13, ground - 13 - sweep), Offset(ballX - 1, ground - 13 - sweep), strokeWidth = 7f)
        }
        game.obstacles.forEach { obstacle ->
            val x = ballX + ((obstacle.at - game.distance) * .55).toFloat()
            if (x in -30f..(size.width + 30f) && game.distance < 7000) {
                val h = (obstacle.height * 3).toFloat()
                if (obstacle.name.contains("rock")) drawCircle(Color(0xff727b84), 13f, Offset(x, ground - 5))
                else { drawRect(Color(0xff40566a), Offset(x - 12, ground - h), Size(24f, h));
                    drawRect(Color(0xff9cabc2), Offset(x - 15, ground - h), Size(30f, 5f)) }
            }
        }
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

private fun nodePosition(node: ResearchNode): Offset {
    val angle = -PI / 2 + node.branch.ordinal * PI / 4 + when (node.tier) { 1, 3 -> -.10; 2, 4 -> .10; else -> 0.0 }
    val radius = 100f + node.tier * 75f
    return Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
}

@Composable private fun ResearchScreen(game: GameEngine) {
    val revision = game.revision
    var selected by remember { mutableStateOf<ResearchNode?>(null) }
    var zoom by remember { mutableFloatStateOf(.78f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val colors = listOf(Color(0xffff9f72), Color(0xff88c7ff), Color(0xffe7b6ff), Color(0xff96f1c2),
        Color(0xffffdf69), Color(0xffa8c5ff), Color(0xffee90a0), Color(0xffc7b2ff))
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RESEARCH CONSTELLATION", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("${ResearchTree.all.count(game::owns)}/64 • $${game.format(game.cash)} • drag to explore", color = Mint, fontSize = 11.sp)
            }
            TextButton(onClick = { pan = Offset.Zero; zoom = .78f }) { Text("CENTER") }
        }
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTransformGestures { _, change, scale, _ ->
                    pan += change; zoom = (zoom * scale).coerceIn(.42f, 2.0f)
                } }
                .pointerInput(zoom, pan) { detectTapGestures { point ->
                    val center = Offset(size.width / 2f, size.height / 2f) + pan
                    selected = ResearchTree.all.minByOrNull { node ->
                        val pos = nodePosition(node); hypot(point.x - center.x - pos.x * zoom, point.y - center.y - pos.y * zoom)
                    }?.takeIf { node ->
                        val pos = nodePosition(node)
                        hypot(point.x - center.x - pos.x * zoom, point.y - center.y - pos.y * zoom) < 30f * zoom + 22f
                    }
                } }
            ) {
                val center = Offset(size.width / 2, size.height / 2) + pan
                drawRect(Brush.radialGradient(listOf(Color(0xff183051), Night), center = center, radius = size.maxDimension))
                for (ring in 0..7) drawCircle(Color.White.copy(alpha = .07f), (100 + ring * 75) * zoom, center, style = Stroke(width = 1f))
                ResearchTree.all.forEach { node ->
                    val end = center + nodePosition(node) * zoom
                    node.requires.forEach { id ->
                        ResearchTree.all.find { it.id == id }?.let { parent ->
                            val start = center + nodePosition(parent) * zoom
                            drawLine(if (game.owns(node)) colors[node.branch.ordinal].copy(alpha = .85f) else Color.White.copy(alpha = .22f), start, end, strokeWidth = if (game.owns(node)) 4f else 2f)
                        }
                    }
                    if (node.tier == 0) drawLine(colors[node.branch.ordinal].copy(alpha = .5f), center, end, strokeWidth = 3f)
                }
                drawCircle(Mint.copy(alpha = .14f), 37f * zoom, center)
                drawCircle(Mint, 22f * zoom, center)
                ResearchTree.all.forEach { node ->
                    val pos = center + nodePosition(node) * zoom
                    val owned = game.owns(node); val available = game.unlocked(node)
                    val color = colors[node.branch.ordinal]
                    drawCircle(if (owned) color else Color(0xff172238), if (node == selected) 23f * zoom else 18f * zoom, pos)
                    drawCircle(if (owned || available) color else Color.White.copy(alpha = .25f), 18f * zoom, pos, style = Stroke(width = 2.5f))
                }
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
                    paint.color = android.graphics.Color.rgb(9, 16, 34); paint.textSize = 22f * zoom
                    canvas.nativeCanvas.drawText("✦", center.x, center.y + 7f * zoom, paint)
                    val glyphs = listOf("↗", "☁", "◆", "≈", "ϟ", "✈", "◉", "★")
                    ResearchTree.all.forEach { node ->
                        val pos = center + nodePosition(node) * zoom
                        paint.color = if (game.owns(node)) android.graphics.Color.rgb(9, 16, 34) else android.graphics.Color.WHITE
                        paint.textSize = 19f * zoom
                        canvas.nativeCanvas.drawText(glyphs[node.branch.ordinal], pos.x, pos.y + 6f * zoom, paint)
                        if (zoom >= .72f && node.tier == 0) {
                            paint.color = android.graphics.Color.WHITE; paint.textSize = 12f * zoom
                            canvas.nativeCanvas.drawText(node.branch.title, pos.x, pos.y - 28f * zoom, paint)
                        }
                    }
                }
            }
            Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
                Text("PINCH TO ZOOM", color = Muted, fontSize = 10.sp)
                Text("TAP A NODE", color = Muted, fontSize = 10.sp)
            }
            Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                TextButton(onClick = { zoom = (zoom / 1.3f).coerceAtLeast(.42f) }) { Text("−") }
                TextButton(onClick = { zoom = (zoom * 1.3f).coerceAtMost(2f) }) { Text("+") }
            }
            selected?.let { node ->
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp).background(Card, RoundedCornerShape(17.dp)).padding(14.dp)) {
                    Row { Text(listOf("↗", "☁", "◆", "≈", "ϟ", "✈", "◉", "★")[node.branch.ordinal], color = colors[node.branch.ordinal], fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp)); Column { Text(node.name, color = Color.White, fontWeight = FontWeight.Black)
                            Text("${node.branch.title} • ${node.effect}", color = Mint, fontSize = 12.sp) } }
                    if (node.requires.isNotEmpty()) {
                        val parents = node.requires.mapNotNull { id -> ResearchTree.all.find { it.id == id }?.name }
                        Text("Requires ${parents.joinToString(" + ")}", color = Muted, fontSize = 11.sp)
                    }
                    if (game.owns(node)) Text("DISCOVERED", color = Mint, fontWeight = FontWeight.Bold)
                    else Button(onClick = { game.buyNode(node) }, enabled = game.unlocked(node) && game.cash >= node.cost, modifier = Modifier.fillMaxWidth()) {
                        Text(if (game.unlocked(node)) "DISCOVER • $${game.format(node.cost)}" else "LOCKED")
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

@Composable private fun GolferScreen(game: GameEngine) {
    val revision = game.revision
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("GOLFER ROSTER", "Buy golfers in order. Higher power and earnings cost more.") }
        items(game.golfers.indices.toList()) { id ->
            val player = game.golfers[id]
            val owned = game.ownsGolfer(id)
            CardBox {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, Modifier.size(44.dp), tint = player.look)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(player.description, color = Muted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("${"%.2f".format(player.power)}× launch  •  ${"%.2f".format(player.cashBonus)}× cash", color = Mint, fontSize = 13.sp)
                if (id == game.selectedGolfer) Text("CURRENT GOLFER", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Black)
                else if (owned) Button(onClick = { game.equipGolfer(id) }, modifier = Modifier.fillMaxWidth()) { Text("PLAY AS ${player.name.uppercase()}") }
                else Button(onClick = { game.buyGolfer(id) }, enabled = id == game.nextGolferUnlock && game.cash >= player.cost, modifier = Modifier.fillMaxWidth()) {
                    Text(if (id == game.nextGolferUnlock) "UNLOCK • $${game.format(player.cost)}" else "UNLOCK PREVIOUS GOLFER FIRST")
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
        Spacer(Modifier.height(14.dp))
        Text("PERMANENT RELIC APPAREL  •  ${game.relicBank} TO SPEND", color = Mint, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            game.apparel.forEach { item ->
                CardBox {
                    Text("${item.icon}  ${item.name}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(item.description, color = Mint, fontSize = 12.sp)
                    if (game.ownsApparel(item.id)) Text("OWNED • PERSISTS THROUGH ASCENSION", color = Muted, fontSize = 10.sp)
                    else Button(onClick = { game.buyApparel(item.id) }, enabled = game.relicBank >= item.price, modifier = Modifier.fillMaxWidth()) {
                        Text("BUY FOR ${item.price} RELICS")
                    }
                }
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Ascend for +${game.potentialRelics} relics?") },
        text = { Text("Cash, clubs, research, and planet progress reset. Golfers, apparel, and permanent relics remain.") },
        confirmButton = { TextButton(onClick = { game.ascend(); confirm = false }) { Text("Ascend") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
}
