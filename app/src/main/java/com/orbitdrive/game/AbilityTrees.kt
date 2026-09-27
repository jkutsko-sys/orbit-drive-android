package com.orbitdrive.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable internal fun AbilityTrees(game: GameEngine) {
    var ability by remember { mutableStateOf(FlightAbility.LIGHTNING) }
    var selected by remember { mutableStateOf<AbilitySkill?>(null) }
    var reset by remember { mutableStateOf(false) }
    var timer by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while(true) { delay(1000); timer++ } }
    val progress = game.abilityProgress
    val revision = progress.revision + timer
    Text("ABILITY SKILL TREES",fontSize=22.sp,fontWeight=FontWeight.Black)
    Text("${progress.available} POINTS AVAILABLE • ${progress.spent}/${progress.earned} ALLOCATED",color=Color(0xff80ffbf),fontWeight=FontWeight.Bold)
    Text("Earn permanent points once at distance milestones. Choose one final keystone per ability. Refund points for free between shots when active effects have ended.",fontSize=12.sp)
    progress.nextMilestone?.let { next ->
        val reward = AbilitySkills.rewards[AbilitySkills.milestones.indexOf(next)]
        Text("Next: ${game.format(next)} m → +$reward points • best ${game.format(progress.best)} m",fontSize=12.sp)
        LinearProgressIndicator(progress={ (progress.best / next).toFloat().coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
    }
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        FlightAbility.entries.forEach { a ->
            TextButton(onClick={ability=a}) { AbilityGlyph(a,Modifier.size(24.dp)); Text(if(ability==a) "• ${a.title}" else a.title,fontSize=11.sp,color=abilityTint(a)) }
        }
    }
    val tint = abilityTint(ability)
    Text("${game.abilityCapacity(ability)} stored charge(s) • ${game.abilityCooldown(ability).toInt()}s recharge • ${game.abilityDuration(ability).toInt()}s duration",color=tint,fontSize=12.sp)
    Text("${game.abilityCharges(ability)} ready • next charge in ${game.abilityReadyIn(ability).toInt()}s",fontSize=11.sp)
    val nodes = AbilitySkills.nodes.filter { it.ability == ability }
    Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
        SkillTile(nodes.first { it.tier==0 },progress,Modifier.fillMaxWidth(.6f),tint) { selected=it }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
        repeat(3) { Box(Modifier.width(2.dp).height(15.dp).background(tint.copy(alpha=.6f))) }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        repeat(3) { lane ->
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(AbilitySkills.lanes(ability)[lane],fontSize=10.sp,color=tint,fontWeight=FontWeight.Bold)
                nodes.filter { it.tier>0 && it.lane==lane }.forEach { n ->
                    Box(Modifier.width(2.dp).height(10.dp).background(tint.copy(alpha=.4f)))
                    SkillTile(n,progress,Modifier.fillMaxWidth(),tint) { selected=it }
                }
            }
        }
    }
    if (!game.canEditAbilityBuild) Text("Build changes unlock after your shot and active ability durations end.",fontSize=11.sp)
    OutlinedButton(onClick={reset=true},enabled=game.canEditAbilityBuild && progress.spent>0,modifier=Modifier.fillMaxWidth()) { Text("REFUND ALL ABILITY POINTS") }
    Text("Milestones: " + AbilitySkills.milestones.indices.joinToString(" • ") { "${game.format(AbilitySkills.milestones[it])}m (+${AbilitySkills.rewards[it]})" },fontSize=10.sp)
    selected?.let { n ->
        AlertDialog(onDismissRequest={selected=null},title={Text(n.name)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(n.effect)
            if(n.keystone) Text("KEYSTONE: choose only one of the three final keystones for this ability.",color=tint)
            Text("Cost: ${n.cost} skill point(s)")
            n.parent?.let { Text("Requires: ${AbilitySkills.byId.getValue(it).name}") }
            if(progress.owns(n.id)) Text("ALLOCATED",color=tint)
        }},confirmButton={TextButton(onClick={game.buyAbilitySkill(n);selected=null},enabled=game.canEditAbilityBuild && progress.canBuy(n)) { Text("ALLOCATE") }},dismissButton={TextButton(onClick={selected=null}) { Text("CLOSE") }})
    }
    if(reset) AlertDialog(onDismissRequest={reset=false},title={Text("Refund ${progress.spent} points?")},text={Text("Choose a different build. Distance milestones remain earned. Cooldowns and spent charges are not reset.")},confirmButton={TextButton(onClick={game.respecAbilities();reset=false}) { Text("REFUND") }},dismissButton={TextButton(onClick={reset=false}) { Text("CANCEL") }})
}
@Composable private fun SkillTile(node: AbilitySkill,progress: AbilityProgress,modifier: Modifier,tint: Color,onSelect:(AbilitySkill)->Unit) {
    val owned=progress.owns(node.id)
    val ready=progress.canBuy(node)
    Column(modifier.background(if(owned) tint.copy(alpha=.35f) else if(ready) tint.copy(alpha=.15f) else Color(0xff172238),RoundedCornerShape(if(node.keystone) 18.dp else 9.dp))
        .clickable { onSelect(node) }.padding(7.dp).heightIn(min=68.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Text(if(node.keystone) "✦" else if(owned) "✓" else "◇",color=if(owned||ready) tint else Color.Gray,fontSize=16.sp)
        Text(node.name,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=3)
        Text(if(owned) "ALLOCATED" else "${node.cost} SP",fontSize=9.sp,color=tint)
    }
}
