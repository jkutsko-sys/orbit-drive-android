package com.orbitdrive.game

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable internal fun AbilityTrees(game:GameEngine) {
    var ability by remember { mutableStateOf(FlightAbility.LIGHTNING) }
    var selected by remember { mutableStateOf<AbilitySkill?>(null) }
    var reset by remember { mutableStateOf(false) }
    var timer by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while(true) { delay(1000);timer++ } }
    val p=game.abilityProgress
    val refresh=p.revision+timer
    Text("ABILITY SKILL TREES",fontSize=22.sp,fontWeight=FontWeight.Black)
    Text("${p.available} SKILL POINTS • ${p.spent}/${p.earned} SPENT",color=Color(0xff80ffbf),fontWeight=FontWeight.Bold)
    if(p.buildRefunded) Text("Your previous ability points have been refunded for the new ranked trees.",color=Color(0xffffd47a),fontSize=12.sp)
    Text("Invest in the skills you want. Deeper rows need tree levels and rank 3 in the connected parent. Choose one final keystone per ability.",fontSize=12.sp)
    p.nextMilestone?.let { Text("Next: ${game.format(it)} m • best ${game.format(p.best)} m",fontSize=12.sp) }
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        FlightAbility.entries.forEach { a ->
            TextButton(onClick={ability=a}) { AbilityGlyph(a,Modifier.size(23.dp));Text(if(a==ability) "• ${a.title}" else a.title,color=abilityTint(a),fontSize=11.sp) }
        }
    }
    val tint=abilityTint(ability)
    Text("${ability.title.uppercase()} • TREE LEVEL ${p.treeLevel(ability)}",color=tint,fontWeight=FontWeight.Black)
    Text("${game.abilityCapacity(ability)} charge(s) • ${game.abilityCooldown(ability).toInt()}s recharge • ${game.abilityDuration(ability).toInt()}s active",fontSize=11.sp)
    BoxWithConstraints(Modifier.fillMaxWidth().height(512.dp).background(Brush.verticalGradient(listOf(abilityBackground(ability),Color(0xff0d1426))),RoundedCornerShape(20.dp))) {
        val cell=maxWidth/3
        val tile=(cell-8.dp).coerceAtMost(104.dp)
        AbilityGlyph(ability,Modifier.size(230.dp).align(Alignment.BottomEnd).alpha(.08f))
        Canvas(Modifier.fillMaxSize()) {
            val root=Offset(size.width*.5f,76.dp.toPx());val split=103.dp.toPx()
            drawLine(tint.copy(alpha=.7f),root,Offset(root.x,split),3.dp.toPx())
            drawLine(tint.copy(alpha=.7f),Offset(size.width/6,split),Offset(size.width*5/6,split),3.dp.toPx())
            repeat(3) { lane ->
                val x=size.width*(lane+.5f)/3
                drawLine(tint.copy(alpha=.7f),Offset(x,split),Offset(x,120.dp.toPx()),3.dp.toPx())
                repeat(2) { row -> drawLine(tint.copy(alpha=.45f),Offset(x,(218+row*135).dp.toPx()),Offset(x,(255+row*135).dp.toPx()),3.dp.toPx()) }
            }
            repeat(18) { i -> drawCircle(tint.copy(alpha=.12f),2.dp.toPx(),Offset(size.width*((i*37)%100)/100f,size.height*((i*61)%100)/100f)) }
        }
        AbilitySkills.nodes.filter { it.ability==ability }.forEach { n ->
            val top=if(n.row==0) 8.dp else (120+(n.row-1)*135).dp
            val left=cell*(n.lane+.5f)-tile/2
            SkillTile(n,p,Modifier.offset(left,top).width(tile),tint) { selected=it }
        }
        listOf(8 to 232,18 to 367).forEach { (level,top) ->
            Text("TREE LEVEL $level  ·  PARENT RANK 3",Modifier.align(Alignment.TopCenter).offset(y=top.dp).background(Color(0xff111b2d),RoundedCornerShape(7.dp)).padding(horizontal=7.dp,vertical=2.dp),color=if(p.treeLevel(ability)>=level) tint else Color(0xff8495ad),fontSize=9.sp)
        }
    }
    if(!game.canEditAbilityBuild) Text("Finish your shot and wait for active abilities to end before changing your build.",fontSize=11.sp)
    OutlinedButton(onClick={reset=true},enabled=game.canEditAbilityBuild && p.spent>0,modifier=Modifier.fillMaxWidth()) { Text("REFUND ALL SKILL POINTS") }
    selected?.let { n ->
        val rank=p.rank(n.id)
        AlertDialog(onDismissRequest={selected=null},icon={SkillIcon(n.icon,Modifier.size(48.dp),abilityTint(n.ability))},title={Text("${n.name} • $rank/${n.maxRank}")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(n.effect)
            if(n.parent!=null) Text("Requires ${AbilitySkills.byId.getValue(n.parent!!).name} rank ${n.parentRank} and tree level ${n.requiredLevel}.",fontSize=12.sp)
            if(n.keystone) Text("One final keystone per ability. This keystone supports five ranks.",color=abilityTint(n.ability))
            Text(if(rank>=n.maxRank) "MAX RANK" else "Next rank: ${rank+1} • ${p.cost(n)} skill point(s)")
        }},confirmButton={TextButton(onClick={game.buyAbilitySkill(n)},enabled=game.canEditAbilityBuild && p.canBuy(n)) { Text("UPGRADE") }},dismissButton={TextButton(onClick={selected=null}) { Text("CLOSE") }})
    }
    if(reset) AlertDialog(onDismissRequest={reset=false},title={Text("Refund ${p.spent} points?")},text={Text("All skill ranks reset. Earned points, charges and cooldowns are preserved.")},confirmButton={TextButton(onClick={game.respecAbilities();reset=false}) { Text("REFUND") }},dismissButton={TextButton(onClick={reset=false}) { Text("CANCEL") }})
}
@Composable private fun SkillTile(n:AbilitySkill,p:AbilityProgress,modifier:Modifier,tint:Color,onSelect:(AbilitySkill)->Unit) {
    val owned=p.owns(n.id);val ready=p.canBuy(n)
    Column(modifier.clickable { onSelect(n) },horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(if(n.keystone) 62.dp else 56.dp).background(Brush.linearGradient(listOf(if(owned||ready) tint.copy(alpha=.65f) else Color(0xff344151),abilityBackground(n.ability))),RoundedCornerShape(if(n.keystone) 17.dp else 10.dp))
            .border(if(owned) 2.dp else 1.dp,if(owned||ready) tint else Color(0xff657186),RoundedCornerShape(if(n.keystone) 17.dp else 10.dp)),contentAlignment=Alignment.Center) {
            SkillIcon(n.icon,Modifier.size(35.dp),if(owned||ready) Color.White else Color(0xff8a98aa))
            Text("${p.rank(n.id)}/${n.maxRank}",Modifier.align(Alignment.BottomCenter).offset(y=7.dp).background(Color(0xff0d1426),RoundedCornerShape(5.dp)).padding(horizontal=5.dp),fontSize=11.sp,fontWeight=FontWeight.Black,color=tint)
        }
        Text(n.name,Modifier.padding(top=10.dp),textAlign=TextAlign.Center,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=2)
    }
}
@Composable internal fun SkillIcon(kind:String,modifier:Modifier,tint:Color) {
    val icon=when(kind) {
        "storm"->Icons.Default.Thunderstorm;"bolt"->Icons.Default.Bolt;"battery"->Icons.Default.BatteryChargingFull
        "chain"->Icons.Default.Link;"shatter"->Icons.Default.BrokenImage;"coins"->Icons.Default.MonetizationOn
        "heart"->Icons.Default.Favorite;"bounce"->Icons.Default.SportsGolf;"clock"->Icons.Default.Schedule
        "cloud"->Icons.Default.Cloud;"rhythm"->Icons.Default.GraphicEq;"wings"->Icons.Default.Flight
        "stairs"->Icons.Default.Stairs;"spring"->Icons.Default.VerticalAlignTop;"wind"->Icons.Default.Air
        "rocket"->Icons.Default.RocketLaunch;"flame"->Icons.Default.LocalFireDepartment;"star"->Icons.Default.AutoAwesome
        "hook"->Icons.Default.Anchor;"orbit"->Icons.Default.Public;else->Icons.Default.BlurCircular
    }
    Icon(icon,null,modifier,tint=tint)
}
