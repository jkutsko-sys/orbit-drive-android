package com.orbitdrive.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun abilityTint(a: FlightAbility) = when (a) {
    FlightAbility.LIGHTNING -> Color(0xffffd54c)
    FlightAbility.AIRLIFT -> Color(0xff7be7ee)
    FlightAbility.ROCKET -> Color(0xffffaa65)
    FlightAbility.GRAVITY -> Color(0xffcea0ff)
}
internal fun abilityBackground(a: FlightAbility) = when (a) {
    FlightAbility.LIGHTNING -> Color(0xff303d59)
    FlightAbility.AIRLIFT -> Color(0xff124954)
    FlightAbility.ROCKET -> Color(0xff593328)
    FlightAbility.GRAVITY -> Color(0xff382651)
}
@Composable internal fun AbilityGlyph(ability: FlightAbility, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val c = abilityTint(ability)
        when (ability) {
            FlightAbility.LIGHTNING -> {
                drawCircle(Color(0xffa1adc4),w*.19f,Offset(w*.32f,h*.37f))
                drawCircle(Color(0xffbbc6d8),w*.23f,Offset(w*.54f,h*.3f))
                drawCircle(Color(0xff8b9ab5),w*.18f,Offset(w*.74f,h*.4f))
                drawRoundRect(Color(0xff9caec7),Offset(w*.2f,h*.36f),Size(w*.62f,h*.22f))
                drawPath(Path().apply { moveTo(w*.51f,h*.38f); lineTo(w*.35f,h*.71f); lineTo(w*.51f,h*.66f); lineTo(w*.44f,h*.94f); lineTo(w*.72f,h*.55f); lineTo(w*.57f,h*.58f); close() },c)
            }
            FlightAbility.AIRLIFT -> {
                drawCircle(c,w*.14f,Offset(w*.5f,h*.22f))
                repeat(3) { i -> val y=h*(.48f+i*.18f); drawLine(c.copy(alpha=1-i*.2f),Offset(w*.24f,y),Offset(w*.5f,y-h*.12f),w*.07f); drawLine(c.copy(alpha=1-i*.2f),Offset(w*.5f,y-h*.12f),Offset(w*.76f,y),w*.07f) }
            }
            FlightAbility.ROCKET -> {
                drawPath(Path().apply { moveTo(w*.5f,h*.05f); lineTo(w*.73f,h*.4f); lineTo(w*.73f,h*.7f); lineTo(w*.27f,h*.7f); lineTo(w*.27f,h*.4f); close() },Color(0xffecf1ff))
                drawCircle(Color(0xff68d9ff),w*.1f,Offset(w*.5f,h*.4f))
                drawPath(Path().apply { moveTo(w*.32f,h*.7f); lineTo(w*.5f,h*.98f); lineTo(w*.68f,h*.7f); close() },c)
            }
            FlightAbility.GRAVITY -> {
                drawCircle(c.copy(alpha=.2f),w*.4f,center)
                drawCircle(c,w*.22f,center)
                drawOval(Color(0xffe8d7ff),Offset(w*.05f,h*.34f),Size(w*.9f,h*.3f),style=Stroke(w*.055f))
                drawCircle(Color.White,w*.055f,Offset(w*.83f,h*.37f))
            }
        }
    }
}
