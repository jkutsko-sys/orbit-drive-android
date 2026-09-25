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
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    private lateinit var audio: AudioDirector
    override fun onDestroy() { audio.release(); super.onDestroy() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashPrefs = getSharedPreferences("orbit_crash_report", MODE_PRIVATE)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            crashPrefs.edit().putString("last", error.stackTraceToString().take(4000)).commit()
            previousHandler?.uncaughtException(thread, error)
        }
        val game = GameEngine(this)
        audio = AudioDirector(this)
        setContent { OrbitApp(game, audio, crashPrefs.getString("last", null)) { crashPrefs.edit().remove("last").apply() } }
    }
}

@Composable private fun OrbitApp(game: GameEngine, audio: AudioDirector, previousCrash: String?, clearCrash: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var settingsOpen by remember { mutableStateOf(false) }
    LaunchedEffect(game.soundEventId) { if (game.soundEventId > 0) audio.cue(game.soundCue) }
    LaunchedEffect(game.distance) { audio.setArea(game.distance) }
    var crash by remember { mutableStateOf(previousCrash) }
    if (crash != null) AlertDialog(onDismissRequest = { crash = null; clearCrash() },
        title = { Text("Previous launch ended unexpectedly") },
        text = { Text(crash ?: "", fontSize = 10.sp) },
        confirmButton = { TextButton(onClick = { crash = null; clearCrash() }) { Text("Close") } })
    val pages = listOf("Range", "Research", "Clubs", "Golfer", "Ascend")
    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, surface = Card, background = Night)) {
        if (settingsOpen) SettingsDialog(game, audio) { settingsOpen = false }
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
                    0 -> RangeScreen(game, true) { settingsOpen = true }
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

@Composable private fun RangeScreen(game: GameEngine, visible: Boolean, openSettings: () -> Unit) {
    LaunchedEffect(game, visible) {
        var last = System.nanoTime()
        while (visible) {
            delay(16)
            val now = System.nanoTime()
            game.tick(((now - last) / 1e9).coerceAtMost(.5))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = openSettings, modifier = Modifier.testTag("settings")) {
                        Icon(Icons.Default.Settings, "Settings", tint = Mint)
                    }
                    Text("$${game.format(game.cash)}", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Mint)
                }
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
            if (game.electrifiedFor > 0) Text("⚡ ELECTRIFIED  ${"%.1f".format(game.electrifiedFor)}s",
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).background(Color(0xff12394d), RoundedCornerShape(12.dp)).padding(8.dp),
                color = Color(0xffbdf6ff), fontWeight = FontWeight.Black, fontSize = 12.sp)
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

private fun DrawScope.drawGolferSprite(x: Float, ground: Float, id: Int, shirt: Color, scale: Float) {
    fun p(dx: Float, dy: Float) = Offset(x + dx * scale, ground + dy * scale)
    val skin = if (id == 4) Color(0xffb8875b) else Color(0xffebc5a1)
    val dark = Color(0xff283449)
    drawCircle(Color.Black.copy(alpha = .18f), 15f * scale, p(0f, -1f))
    drawLine(dark, p(-5f, -22f), p(-11f, -2f), strokeWidth = 6f * scale)
    drawLine(dark, p(5f, -22f), p(12f, -2f), strokeWidth = 6f * scale)
    drawRoundRect(shirt, topLeft = p(-11f, -48f), size = Size(23f * scale, 29f * scale), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale))
    drawLine(skin, p(9f, -43f), p(17f, -34f), strokeWidth = 6f * scale)
    drawLine(skin, p(17f, -34f), p(13f, -31f), strokeWidth = 5f * scale)
    if (id == 4) {
        drawCircle(skin, 13f * scale, p(1f, -60f))
        drawCircle(skin, 4f * scale, p(-9f, -70f))
        drawCircle(skin, 4f * scale, p(10f, -70f))
        drawRoundRect(Color(0xffd1a174), topLeft = p(1f, -57f), size = Size(14f * scale, 9f * scale), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale))
        drawCircle(dark, 1.5f * scale, p(-3f, -63f)); drawCircle(dark, 1.5f * scale, p(8f, -63f))
        drawCircle(dark, 1.5f * scale, p(11f, -54f))
    } else {
        drawCircle(skin, 11f * scale, p(0f, -60f))
        drawRoundRect(dark, topLeft = p(-12f, -72f), size = Size(24f * scale, 7f * scale), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale))
        drawLine(dark, p(3f, -68f), p(18f, -68f), strokeWidth = 3f * scale)
        drawCircle(dark, 1.3f * scale, p(-4f, -61f)); drawCircle(dark, 1.3f * scale, p(5f, -61f))
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
        val drift = (game.distance * .9).toFloat()
        if (!space) {
            repeat(5) { i ->
                val x = ((i * size.width / 3 - drift * .7f) % (size.width + 160) + size.width + 160) % (size.width + 160) - 70
                val y = size.height * (.19f + (i % 3) * .12f)
                drawCircle(Color.White.copy(alpha = .45f), 22f, Offset(x, y))
                drawCircle(Color.White.copy(alpha = .45f), 16f, Offset(x + 21, y + 3))
                drawCircle(Color.White.copy(alpha = .45f), 15f, Offset(x - 19, y + 5))
            }
            repeat(4) { i ->
                val x = ((i * size.width / 2 - drift * .4f) % (size.width + 200) + size.width + 200) % (size.width + 200)
                drawLine(Color(0xff243b52), Offset(x, ground - 45), Offset(x, ground), strokeWidth = 22f)
                drawRect(Color(0xff253c54), Offset(x - 21, ground - 52), Size(42f, 8f))
            }
            repeat(3) { i ->
                val x = ((i * size.width / 2 - drift * .8f) % (size.width + 120) + size.width + 120) % (size.width + 120)
                val y = size.height * (.13f + i * .055f)
                drawLine(Color(0xff263d56), Offset(x, y), Offset(x + 8, y - 4), strokeWidth = 2f)
                drawLine(Color(0xff263d56), Offset(x + 8, y - 4), Offset(x + 16, y), strokeWidth = 2f)
            }
        }
        drawRect(if (space) Color(0xff302746) else Color(0xff1e664d), topLeft = Offset(0f, ground), size = Size(size.width, size.height - ground))
        repeat(6) { i -> drawLine(Color.White.copy(alpha = .08f), Offset(0f, ground + i * 22f), Offset(size.width, ground + i * 22f)) }
        repeat(12) { i ->
            val x = ((i * 90f - drift * 1.8f) % (size.width + 90) + size.width + 90) % (size.width + 90)
            drawLine(Color.White.copy(alpha = .17f), Offset(x, ground + 18), Offset(x + 25, ground + 18), strokeWidth = 2f)
        }
        val ballX = size.width * if (game.phase == Phase.READY || game.phase == Phase.CHARGING) .18f else .36f
        val height = (ln(1 + max(0.0, game.altitude)) / ln(200.0)).toFloat() * size.height * .55f
        val ballY = max(size.height * .15f, ground - 10 - height)
        drawCircle(Color.Black.copy(alpha = .25f), radius = 12f, center = Offset(ballX, ground))
        if (game.phase == Phase.FLYING) drawLine(Mint.copy(alpha = .65f), Offset(ballX - min(130f, (game.speed * .3).toFloat()), ballY + 20), Offset(ballX - 8, ballY + 2), strokeWidth = 6f)
        val ball = game.ball
        if (game.electrifiedFor > 0) {
            val shimmer = 3f + (game.electrifiedFor.toFloat() * 5f) % 6f
            drawCircle(Color(0xffb3f7ff).copy(alpha = .18f), radius = 20f + shimmer, center = Offset(ballX, ballY))
            drawCircle(Color(0xff70dfff), radius = 15f, center = Offset(ballX, ballY), style = Stroke(width = 2.5f))
            repeat(6) { i ->
                val angle = i * PI / 3 + game.electrifiedFor * 7
                val p1 = Offset(ballX + cos(angle).toFloat() * 16f, ballY + sin(angle).toFloat() * 16f)
                val p2 = Offset(ballX + cos(angle + .2).toFloat() * 25f, ballY + sin(angle + .2).toFloat() * 25f)
                drawLine(Color(0xffd7ffff), p1, p2, strokeWidth = 2f)
            }
        }
        if (game.twinLaunch && game.phase == Phase.FLYING) {
            val secondY = ballY + 22f + sin(game.distance.toFloat() * .04f) * 4f
            drawCircle(ball.tint.copy(alpha = .88f), 9f, Offset(ballX - 29f, secondY))
            drawLine(ball.stripe, Offset(ballX - 34f, secondY - 5f), Offset(ballX - 24f, secondY + 5f), strokeWidth = 2f)
            drawLine(Mint.copy(alpha = .4f), Offset(ballX - 85f, secondY + 7f), Offset(ballX - 39f, secondY), strokeWidth = 3f)
        }
        drawCircle(ball.tint, radius = 11f, center = Offset(ballX, ballY))
        drawCircle(ball.stripe.copy(alpha = .8f), radius = 11f, center = Offset(ballX, ballY), style = Stroke(width = 2f))
        val spin = (game.distance * .12).toFloat()
        drawLine(ball.stripe, Offset(ballX + cos(spin) * 8f, ballY + sin(spin) * 8f),
            Offset(ballX - cos(spin) * 8f, ballY - sin(spin) * 8f), strokeWidth = 2.4f)
        drawIntoCanvas { canvas ->
            val label = Paint().apply { isAntiAlias = true; color = android.graphics.Color.rgb(30, 38, 55);
                textSize = 10f; textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
            canvas.nativeCanvas.drawText(ball.mark, ballX, ballY + 3f, label)
        }
        if (game.lightningFlashFor > 0) {
            val flash = (game.lightningFlashFor / .38).toFloat().coerceIn(0f, 1f)
            drawCircle(Color(0xfff6f9b0).copy(alpha = flash * .35f), 35f + (1 - flash) * 28f, Offset(ballX, ballY))
            val bolt = androidx.compose.ui.graphics.Path().apply {
                moveTo(ballX - 32, 0f); lineTo(ballX + 13, ballY * .36f)
                lineTo(ballX - 15, ballY * .55f); lineTo(ballX + 19, ballY * .71f)
                lineTo(ballX - 6, ballY - 28); lineTo(ballX, ballY)
            }
            drawPath(bolt, Color(0xfffff0a2).copy(alpha = flash), style = Stroke(width = 6f * flash + 2f))
            drawPath(bolt, Color.White.copy(alpha = flash), style = Stroke(width = 2f))
        }
        if (game.planeSpriteFor > 0) {
            val appear = (game.planeSpriteFor / 1.8).toFloat().coerceIn(0f, 1f)
            val px = ballX - 72 + (1 - appear) * 32f; val py = ballY - 42 - (1 - appear) * 10f
            drawLine(Color.White.copy(alpha = appear * .5f), Offset(px + 30, py + 13), Offset(ballX, ballY), strokeWidth = 2f)
            drawRoundRect(Color(0xffe7e9ee), Offset(px, py), Size(65f, 15f), androidx.compose.ui.geometry.CornerRadius(8f))
            drawRect(Color(0xff5a8ed4), Offset(px + 21, py + 7), Size(27f, 12f))
            drawRect(Color(0xffd0d8e5), Offset(px + 4, py - 16), Size(7f, 18f))
            drawCircle(Color(0xff213d69), 4f, Offset(px + 55, py + 7))
            drawLine(Color.White, Offset(px + 65, py - 10), Offset(px + 65, py + 24), strokeWidth = 3f)
        }
        if (game.phase == Phase.READY || game.phase == Phase.CHARGING) {
            val gx = ballX - 38f
            drawGolferSprite(gx, ground, game.selectedGolfer, game.golfer.look, 1f)
            val clubColor = if (game.equippedClub == game.clubs.lastIndex) Color(0xffe7a6ff)
                else listOf(Color.Gray, Color(0xffc5a77e), Color.Cyan, Color(0xffb4d0d5), Color(0xffffc36a), Color(0xfff07d74), Color.Magenta)[game.equippedClub % 7]
            val sweep = if (game.phase == Phase.CHARGING) game.charge.toFloat() * 42f else 0f
            drawLine(clubColor, Offset(gx + 14, ground - 35), Offset(ballX - 5, ground - 13 - sweep), strokeWidth = 3f + game.equippedClub * .13f)
            drawLine(clubColor, Offset(ballX - 13, ground - 13 - sweep), Offset(ballX - 1, ground - 13 - sweep), strokeWidth = 7f)
            if (game.equippedClub == game.clubs.lastIndex) {
                // Prismatic orbit and floating fragments evoke a legendary endgame club without borrowed art.
                repeat(6) { i ->
                    val angle = i * PI / 3 + game.charge * 1.6
                    val px = gx + 8f + cos(angle).toFloat() * 24f
                    val py = ground - 36f + sin(angle).toFloat() * 29f
                    val shards = listOf(Color(0xff67e8f9), Color(0xffec93fa), Color(0xffffd17a))
                    drawLine(shards[i % 3], Offset(px - 4, py + 4), Offset(px + 5, py - 5), strokeWidth = 4f)
                }
                drawCircle(Color(0xffa7e9ff).copy(alpha = .35f), 32f, Offset(gx + 8, ground - 37), style = Stroke(width = 3f))
            }
        }
        if (game.phase == Phase.FLYING && game.speed > 20) {
            repeat(7) { i ->
                val x = ((i * 97f - drift * 2.5f) % size.width + size.width) % size.width
                val y = ground * (.55f + (i % 4) * .07f)
                drawLine(Color.White.copy(alpha = .09f + min(.23f, game.speed.toFloat() / 2000f)), Offset(x, y), Offset(x + 25 + min(85f, game.speed.toFloat() * .12f), y), strokeWidth = 2f)
            }
        }
        if (game.distance < 10000) {
            val baseMarker = (game.distance / 100).toInt()
            for (marker in baseMarker..baseMarker + 8) {
                val x = ballX + ((marker * 100 - game.distance) * 1.1).toFloat()
                if (x in 0f..size.width && marker > 0) {
                    drawLine(Color.White.copy(alpha = .7f), Offset(x, ground - 24), Offset(x, ground), strokeWidth = 2f)
                    drawRect(Mint.copy(alpha = .85f), Offset(x, ground - 24), Size(22f, 12f))
                }
            }
        }
        game.obstacles.forEachIndexed { obstacleIndex, obstacle ->
            val x = ballX + ((obstacle.at - game.distance) * .55).toFloat()
            if (x in -30f..(size.width + 30f) && game.distance < 7000 && !game.obstacleBroken(obstacleIndex)) {
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
        game.planetImpactID?.let { id ->
            if (game.planetEffectFor > 0) {
                val planet = game.planets[id]
                val progress = (1 - game.planetEffectFor / 1.8).toFloat().coerceIn(0f, 1f)
                val center = Offset(size.width * .59f, ground * .42f)
                val radius = 45f + id * 3f
                drawCircle(planet.color.copy(alpha = if (game.planetShattered && progress > .48f) (1 - progress) * 1.8f else 1f), radius, center)
                // Ball rushes into the core, then radial fractures and fragments fly out.
                if (progress < .46f) {
                    val incoming = Offset(ballX + (center.x - ballX) * progress / .46f, ballY + (center.y - ballY) * progress / .46f)
                    drawLine(Mint.copy(alpha = .8f), Offset(ballX - 80, ballY + 15), incoming, strokeWidth = 8f)
                    drawCircle(game.ball.tint, 12f, incoming)
                }
                repeat(7) { i ->
                    val angle = i * 2 * PI / 7
                    val from = Offset(center.x + cos(angle).toFloat() * 5f, center.y + sin(angle).toFloat() * 5f)
                    val to = Offset(center.x + cos(angle + .3).toFloat() * radius * min(1f, progress * 2),
                        center.y + sin(angle + .3).toFloat() * radius * min(1f, progress * 2))
                    drawLine(Color(0xff1c1832), from, to, strokeWidth = 3f + progress * 2f)
                    if (game.planetShattered && progress > .4f) {
                        val travel = (progress - .4f) * 155f
                        val fragment = Offset(center.x + cos(angle).toFloat() * (radius + travel),
                            center.y + sin(angle).toFloat() * (radius + travel))
                        drawCircle(planet.color.copy(alpha = 1 - progress), 8f + (i % 3) * 3f, fragment)
                    }
                }
                if (game.planetShattered && progress > .4f) {
                    drawCircle(Color.White.copy(alpha = (1 - progress) * .55f), (progress - .4f) * 150f, center, style = Stroke(width = 10f))
                    drawCircle(Color(0xffffd189).copy(alpha = (1 - progress) * .3f), (progress - .4f) * 100f, center)
                }
            }
        }

    }
}

private fun nodePosition(node: ResearchNode): Offset {
    val angle = -PI / 2 + node.branch.ordinal * PI / 4 + when (node.tier) {
        1, 3, 8, 10, 13, 16, 18, 21 -> -.075
        2, 4, 9, 11, 14, 17, 19, 22 -> .075
        else -> 0.0
    }
    val radius = 100f + node.tier * 65f
    return Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
}

@Composable private fun ResearchScreen(game: GameEngine) {
    val revision = game.revision
    var selected by remember { mutableStateOf<ResearchNode?>(null) }
    var zoom by remember { mutableFloatStateOf(.78f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val transition = rememberInfiniteTransition(label = "affordable discoveries")
    val pulse by transition.animateFloat(initialValue = .22f, targetValue = .62f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "ready glow")
    val colors = listOf(Color(0xffff9f72), Color(0xff88c7ff), Color(0xffe7b6ff), Color(0xff96f1c2),
        Color(0xffffdf69), Color(0xffa8c5ff), Color(0xffee90a0), Color(0xffc7b2ff))
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RESEARCH CONSTELLATION", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("${ResearchTree.all.count(game::owns)}/192 • $${game.format(game.cash)} • drag to explore", color = Mint, fontSize = 11.sp)
            }
            TextButton(onClick = { pan = Offset.Zero; zoom = .78f }) { Text("CENTER") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val glyphs = listOf("↗", "☁", "◆", "≈", "ϟ", "✈", "◉", "★")
            Tech.entries.forEach { branch ->
                TextButton(onClick = {
                    val point = nodePosition(ResearchTree.nodes(branch)[12]); zoom = .78f; pan = Offset(-point.x * zoom, -point.y * zoom)
                }) { Text("${glyphs[branch.ordinal]} ${branch.title}", fontSize = 11.sp) }
            }
        }
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTransformGestures { _, change, scale, _ ->
                    pan += change; zoom = (zoom * scale).coerceIn(.28f, 2.2f)
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
                for (ring in 0..23) drawCircle(Color.White.copy(alpha = .045f), (100 + ring * 65) * zoom, center, style = Stroke(width = 1f))
                ResearchTree.all.forEach { node ->
                    val end = center + nodePosition(node) * zoom
                    node.requires.forEach { id ->
                        ResearchTree.byId[id]?.let { parent ->
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
                    if (!owned && available && game.cash >= node.cost) {
                        drawCircle(color.copy(alpha = pulse * .35f), (if (node.tier in listOf(7, 15, 23)) 32f else 23f) * zoom, pos)
                        drawCircle(color.copy(alpha = pulse), (if (node.tier in listOf(7, 15, 23)) 26f else 19f) * zoom, pos, style = Stroke(width = 2f))
                    }
                    val r = if (node.tier in listOf(7, 15, 23)) 23f else 14f
                    drawCircle(if (owned) color else Color(0xff172238), (r + if (node == selected) 5f else 0f) * zoom, pos)
                    drawCircle(if (owned || available) color else Color.White.copy(alpha = .25f), r * zoom, pos, style = Stroke(width = if (node.tier in listOf(7, 15, 23)) 4f else 2f))
                }
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
                    paint.color = android.graphics.Color.rgb(9, 16, 34); paint.textSize = 22f * zoom
                    canvas.nativeCanvas.drawText("✦", center.x, center.y + 7f * zoom, paint)
                    val glyphs = listOf("↗", "☁", "◆", "≈", "ϟ", "✈", "◉", "★")
                    ResearchTree.all.forEach { node ->
                        val pos = center + nodePosition(node) * zoom
                        paint.color = if (game.owns(node)) android.graphics.Color.rgb(9, 16, 34) else android.graphics.Color.WHITE
                        paint.textSize = (if (node.tier in listOf(7, 15, 23)) 20f else 14f) * zoom
                        canvas.nativeCanvas.drawText(if (node.tier in listOf(7, 15, 23)) "✦" else glyphs[node.branch.ordinal], pos.x, pos.y + 6f * zoom, paint)
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
                TextButton(onClick = { zoom = (zoom / 1.3f).coerceAtLeast(.28f) }) { Text("−") }
                TextButton(onClick = { zoom = (zoom * 1.3f).coerceAtMost(2.2f) }) { Text("+") }
            }
            selected?.let { node ->
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp).background(Card, RoundedCornerShape(17.dp)).padding(14.dp)) {
                    Row { Text(listOf("↗", "☁", "◆", "≈", "ϟ", "✈", "◉", "★")[node.branch.ordinal], color = colors[node.branch.ordinal], fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp)); Column { Text((if (node.tier in listOf(7, 15, 23)) "KEYSTONE • " else "") + node.name, color = Color.White, fontWeight = FontWeight.Black)
                            Text("${node.branch.title} • ${node.effect}", color = Mint, fontSize = 12.sp) } }
                    if (node.requires.isNotEmpty()) {
                        val parents = node.requires.mapNotNull { id -> ResearchTree.byId[id]?.name }
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
        item { Header("THE PRO SHOP", "Buy in order, equip any owned club, and upgrade its swing.") }
        items(game.clubs.indices.toList()) { id ->
            val club = game.clubs[id]
            CardBox {
                Text(club.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(if (id == game.equippedClub) "EQUIPPED • LEVEL ${game.clubLevel(id)}" else if (id <= game.ownedClub) "OWNED • LEVEL ${game.clubLevel(id)}" else "LOCKED", color = Mint, fontSize = 10.sp)
                Text(game.clubPerks[id], color = Color(0xffffd787), fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("SWING ${game.format(club.swing * 1.15.pow(game.clubLevel(id)))}")
                    Pill("LOFT ${club.loft.toInt()}°")
                    Pill("SMASH ${"%.2f".format(club.smash)}×")
                }
                if (id == game.equippedClub) {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = game::upgradeClub, enabled = game.clubLevel(id) < 30 && game.cash >= game.clubUpgradeCost(), modifier = Modifier.fillMaxWidth()) {
                        Text("UPGRADE CLUB  •  $${game.format(game.clubUpgradeCost())}")
                    }
                } else if (id <= game.ownedClub) {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { game.equipClub(id) }, enabled = game.phase != Phase.FLYING, modifier = Modifier.fillMaxWidth()) { Text("EQUIP ${club.name.uppercase()}") }
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
                    Canvas(Modifier.size(width = 62.dp, height = 76.dp)) {
                        drawGolferSprite(size.width / 2, size.height - 3f, id, player.look, .85f)
                    }
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
        Text("RELIC COLLECTION  •  ${game.relicBank} TO SPEND", color = Mint, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("GOLF BALLS", color = Color.White, fontWeight = FontWeight.Black)
            game.balls.forEachIndexed { index, ball ->
                CardBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(42.dp)) {
                            drawCircle(ball.tint, size.minDimension * .4f)
                            drawCircle(ball.stripe, size.minDimension * .4f, style = Stroke(width = 3f))
                            drawLine(ball.stripe, Offset(size.width * .2f, size.height * .7f), Offset(size.width * .8f, size.height * .3f), strokeWidth = 3f)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column { Text(ball.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${"%.2f".format(ball.power)}× launch • ${ball.description}", color = Mint, fontSize = 11.sp) }
                    }
                    if (index == game.selectedBall) Text("IN PLAY", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    else if (game.ownsBall(index)) Button(onClick = { game.equipBall(index) }, modifier = Modifier.fillMaxWidth()) { Text("PLAY THIS BALL") }
                    else Button(onClick = { game.buyBall(index) }, enabled = index == game.nextBallUnlock && game.relicBank >= ball.relicCost, modifier = Modifier.fillMaxWidth()) {
                        Text(if (index == game.nextBallUnlock) "BUY FOR ${ball.relicCost} RELICS" else "UNLOCK PREVIOUS BALL FIRST")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("PERMANENT APPAREL", color = Color.White, fontWeight = FontWeight.Black)
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
        text = { Text("Cash, clubs, research, and planet progress reset. Golfers, golf balls, apparel, and permanent relics remain.") },
        confirmButton = { TextButton(onClick = { game.ascend(); confirm = false }) { Text("Ascend") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
}

@Composable private fun SettingsDialog(game: GameEngine, audio: AudioDirector, close: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = close, title = { Text("SETTINGS") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Music"); Switch(checked = audio.musicEnabled, onCheckedChange = audio::setMusic)
            }
            Text("Music volume")
            Slider(value = audio.musicVolume, onValueChange = audio::adjustMusicVolume)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Sound effects"); Switch(checked = audio.effectsEnabled, onCheckedChange = audio::setEffects)
            }
            Text("Effects volume")
            Slider(value = audio.effectsVolume, onValueChange = audio::adjustEffectsVolume)
            HorizontalDivider()
            Text("Preview voucher", fontWeight = FontWeight.Bold)
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Voucher code") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("voucherCode"))
            Button(onClick = { result = game.redeemVoucher(code); code = "" }, modifier = Modifier.testTag("redeem")) { Text("Redeem") }
            if (result.isNotEmpty()) Text(result, color = Mint)
        }
    }, confirmButton = { TextButton(onClick = close) { Text("Done") } })
}
