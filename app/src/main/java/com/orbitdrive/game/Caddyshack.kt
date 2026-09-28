package com.orbitdrive.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable internal fun Caddyshack(game:GameEngine,onClose:()->Unit) {
    val c=game.caddies
    val largeText=LocalDensity.current.fontScale>1.2f
    var seconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while(true) { seconds++;delay(1000) } }
    val tick=seconds
    val canChange=game.phase!=Phase.FLYING && game.phase!=Phase.CHARGING
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding(),color=Color(0xff0c1c24),contentColor=Color(0xffeef4ff)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(if(largeText) "CADDYSHACK" else "THE CADDYSHACK",fontSize=if(largeText) 18.sp else 23.sp,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis);Text("YOUR CREW • YOUR ADVANTAGE",fontSize=10.sp,color=Color(0xff9cdac0)) }
                    TextButton(onClick=onClose) { Text("CLOSE") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Card {
                        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("A NEW FACE EVERY FOUR HOURS",fontWeight=FontWeight.Bold)
                            Text("${c.stored()}/3 recruits waiting. Duplicate cards give 125 XP. Each level takes 125 XP; your equipped caddy earns 5 XP per completed range shot.",fontSize=12.sp)
                            val minutes=(c.remaining()+59999)/60000
                            Text(if(c.stored()>0) "RECRUIT READY" else "Next recruit: ${minutes/60}h ${minutes%60}m",color=Color(0xffffd47a))
                            Button(onClick={c.recruit()},enabled=c.stored()>0 && canChange,modifier=Modifier.fillMaxWidth()) { Text("MEET A RANDOM CADDY") }
                            if(c.lastRecruit.isNotEmpty()) Text(c.lastRecruit,color=Color(0xff80ffbf),fontWeight=FontWeight.Bold)
                        }
                    }
                    Text("Equip one for its full bonus and tap assists: tap the range for a forward boost, at most five per shot, two seconds apart. Unequipped caddies contribute 25% of their bonus at level 10, 60% at 40 and 100% at 80. Maximum level: 120.",fontSize=12.sp)
                    c.roster.forEachIndexed { id,item ->
                        val level=c.level(id);val active=c.selected==id
                        Card {
                            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    Canvas(Modifier.size(68.dp)) { drawCircle(item.color.copy(alpha=.18f));drawCaddySprite(id,size.width*.5f,size.height*.94f,size.minDimension/78f,item.color) }
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name,fontWeight=FontWeight.Black,color=item.color)
                                        Text("${item.job} • ${if(level==0) "UNDISCOVERED" else "LEVEL $level"}",fontSize=11.sp)
                                        Text("${item.stat}: +${"%.2f".format(c.bonus(id)*100)}% contributing",fontSize=11.sp)
                                    }
                                }
                                Text("Full bonus at this level: +${"%.2f".format(item.perLevel*level*100)}% • ${if(active) "EQUIPPED" else "${(c.passiveFactor(id)*100).toInt()}% PASSIVE"}",fontSize=11.sp)
                                if(level>0 && level<CaddyState.MAX_LEVEL) {
                                    LinearProgressIndicator(progress={ (c.xp[id]%125)/125f },modifier=Modifier.fillMaxWidth())
                                    Text("${c.xp[id]%125}/125 XP to next level",fontSize=10.sp)
                                }
                                Button(onClick={c.equip(id)},enabled=canChange && level>0 && !active,modifier=Modifier.fillMaxWidth()) { Text(if(active) "ON YOUR BAG" else if(level==0) "FIND AT THE CADDYSHACK" else "EQUIP CADDY") }
                            }
                        }
                    }
                }
            }
        }
    }
}
internal fun DrawScope.drawCaddySprite(id:Int,x:Float,ground:Float,scale:Float,color:Color) {
    fun p(dx:Float,dy:Float)=Offset(x+dx*scale,ground+dy*scale)
    val skin=listOf(Color(0xffe8b990),Color(0xffb88160),Color(0xffd7a880),Color(0xffedc9a6))[id%4]
    val dark=Color(0xff26374a)
    drawLine(dark,p(-5f,-22f),p(-7f,0f),5f*scale);drawLine(dark,p(5f,-22f),p(9f,0f),5f*scale)
    drawRoundRect(color,p(-10f,-43f),Size(21f*scale,24f*scale))
    drawLine(Color.White.copy(alpha=.7f),p(-5f,-42f),p(7f,-22f),3f*scale)
    drawCircle(skin,10f*scale,p(0f,-55f))
    drawLine(dark,p(-12f,-62f),p(14f,-62f),4f*scale)
    drawRoundRect(color,p(-8f,-68f),Size(18f*scale,8f*scale))
    drawCircle(dark,1.5f*scale,p(-3f,-55f));drawCircle(dark,1.5f*scale,p(5f,-55f))
    if(id==2 || id==4 || id==7) drawLine(Color(0xff90ddef),p(-7f,-56f),p(8f,-56f),3f*scale)
    drawLine(skin,p(9f,-38f),p(19f,-26f),4f*scale)
    drawRoundRect(dark,p(13f,-29f),Size(15f*scale,29f*scale))
    drawLine(color,p(15f,-22f),p(26f,-22f),4f*scale)
    repeat(3) { i -> drawLine(Color(0xffbed4dc),p(16f+i*4,-29f),p(16f+i*4,-42f-i*3),2f*scale);drawCircle(color,3f*scale,p(16f+i*4,-42f-i*3)) }
}
