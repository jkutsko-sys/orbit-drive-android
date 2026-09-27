package com.orbitdrive.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

/** Each purchased level adds a visible part to the home tee and its skyline. */
internal fun DrawScope.drawProShop(state: EngagementState, ground: Float, distance: Float, width: Float, height: Float) {
    val f = state.facilities
    val shift = distance * .55f
    val house = width * .66f - shift
    val tee = width * .18f - shift
    val era = (f.sum() / 12).coerceIn(0, 7)
    val accent = listOf(Color(0xff8b6954),Color(0xff698b91),Color(0xff5b829d),Color(0xff6e82ad),Color(0xff757cb8),Color(0xff8973bf),Color(0xffa77bc7),Color(0xffdfb675))[era]
    val bronze = Color(0xffffd47a)
    // The starting patch becomes a tiled, lit platform with each pavilion level.
    val paved = f[4]
    drawRoundRect(if (paved == 0) Color(0xff4d8a65) else accent, Offset(tee - 47f, ground - 8f), Size(95f + paved * 4f, 10f))
    repeat(paved) { level ->
        val x = tee - 43f + level * 12f
        drawLine(if (level % 3 == 2) bronze else Color(0xffd4e6e9), Offset(x, ground - 8f), Offset(x + 5f, ground - 8f), strokeWidth = 2f)
    }
    if (paved > 2) {
        val awning = ground - 76f - paved * 2f
        drawLine(Color(0xffd4e6e9), Offset(tee - 59f, ground), Offset(tee - 59f, awning), strokeWidth = 3f)
        drawLine(Color(0xffd4e6e9), Offset(tee + 51f, ground), Offset(tee + 51f, awning), strokeWidth = 3f)
        drawRoundRect(accent, Offset(tee - 65f, awning - 7f), Size(122f, 10f))
        repeat((paved - 2).coerceAtMost(9)) { i -> drawCircle(bronze, 2.2f, Offset(tee - 51f + i * 12f, awning - 2f)) }
    }
    // Clubhouse adds height, wings and an individual window with each level.
    val club = f[0]
    val roof = ground - 30f - club * 5f
    drawRoundRect(accent, Offset(house, roof), Size(88f + club * 2f, ground - roof))
    drawLine(bronze, Offset(house - 3f, roof), Offset(house + 91f + club * 2f, roof), strokeWidth = 5f)
    drawRect(Color(0xff3a4055), Offset(house + 37f, ground - 22f), Size(16f, 22f))
    repeat(club) { i ->
        val row = i / 4; val col = i % 4
        drawRect(Color(0xffa9ebf4), Offset(house + 7f + col * 20f, roof + 7f + row * 15f), Size(9f, 9f))
    }
    if (club >= 6) drawCircle(bronze, 7f, Offset(house + 45f, roof - 9f), style = Stroke(2f))
    // Workshop grows a hangar, tools and glowing roof panels.
    if (f[1] > 0) {
        val x = house - 48f; val h = 19f + f[1] * 2.5f
        drawRect(Color(0xff465c70), Offset(x, ground - h), Size(42f, h))
        drawLine(Color(0xffcbdcea), Offset(x - 3f, ground - h), Offset(x + 44f, ground - h), strokeWidth = 4f)
        repeat(f[1]) { i -> drawRect(if (i % 3 == 0) bronze else Color(0xff8dd5eb), Offset(x + 3f + i % 6 * 6f, ground - h + 5f + i / 6 * 9f), Size(4f, 5f)) }
    }
    // Trophy Room displays one new gold trophy for each rank.
    if (f[2] > 0) {
        val x = house + 98f
        drawRoundRect(Color(0xff454360), Offset(x, ground - 33f), Size(55f, 33f))
        repeat(f[2]) { i ->
            val tx = x + 7f + i % 6 * 8f; val ty = ground - 9f - i / 6 * 15f
            drawLine(bronze, Offset(tx, ty), Offset(tx, ty - 6f), strokeWidth = 2f)
            drawCircle(bronze, 3f, Offset(tx, ty - 7f))
        }
    }
    // Rocket pad gains a taller gantry and a brighter launch marker per level.
    if (f[3] > 0) {
        val x = house + 167f; val top = ground - 37f - f[3] * 4f
        drawLine(Color(0xffc4d0df), Offset(x, ground), Offset(x, top), strokeWidth = 7f)
        repeat(f[3]) { i -> drawLine(Color(0xff78e8f2), Offset(x - 9f, ground - 9f - i * 7f), Offset(x + 9f, ground - 9f - i * 7f), strokeWidth = 2f) }
        drawCircle(bronze, 5f + f[3] * .35f, Offset(x, top))
    }
    // Range Lights, Vault and Flight Tower form the later skyline.
    if (f[5] > 0) repeat(min(4, (f[5] + 2) / 3)) { i ->
        val x = house - 100f - i * 24f; val top = ground - 36f - f[5] * 2f
        drawLine(Color(0xff9fb6c8), Offset(x, ground), Offset(x, top), strokeWidth = 3f)
        drawCircle(Color(0xfff4eeab), 4f + f[5] * .22f, Offset(x, top))
        repeat(f[5] / 4) { beam -> drawLine(Color(0xfff4eeab).copy(alpha = .35f), Offset(x, top), Offset(x - 9f - beam * 8f, top + 16f), strokeWidth = 1f) }
    }
    if (f[6] > 0) {
        val x = house + 159f; val top = ground - 16f - f[6] * 2f
        drawRoundRect(Color(0xff776457), Offset(x, top), Size(28f + f[6] * 2f, ground - top))
        repeat(f[6]) { i -> drawRect(bronze, Offset(x + 4f + i % 6 * 7f, top + 5f + i / 6 * 11f), Size(4f, 4f)) }
    }
    if (f[7] > 0) {
        val x = house + 210f; val top = ground - 33f - f[7] * 5f
        drawRoundRect(Color(0xff4d6180), Offset(x, top), Size(30f + f[7], ground - top))
        repeat(f[7]) { i -> drawRect(Color(0xff8eefff), Offset(x + 4f + i % 3 * 9f, top + 6f + i / 3 * 12f), Size(5f, 6f)) }
        if (f[7] >= 4) drawLine(bronze, Offset(x + 16f, top), Offset(x + 16f, top - 15f), strokeWidth = 3f)
        if (f[7] >= 8) drawCircle(bronze, 8f, Offset(x + 16f, top - 17f), style = Stroke(2f))
    }
    if (state.totalMedals > 0) {
        val x = house + 44f; val y = roof - 13f
        val flag = Path().apply { moveTo(x,y); lineTo(x + 16f,y + 5f); lineTo(x,y + 10f); close() }
        drawLine(bronze, Offset(x,y), Offset(x,roof), strokeWidth = 2f)
        drawPath(flag, bronze)
    }
}
