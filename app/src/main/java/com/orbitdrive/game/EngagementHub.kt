package com.orbitdrive.game

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay

@Composable internal fun EngagementHub(game: GameEngine, onExpedition: () -> Unit, onClose: () -> Unit) {
    val state = game.engagement
    var page by remember { mutableIntStateOf(0) }
    var timerTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(page) { while (page == 0) { delay(30_000); timerTick++ } }
    val pages = listOf("Contracts", "Abilities", "Skill Trees", "Home", "Weekly", "Combos")
    val canChange = game.phase != Phase.FLYING && game.phase != Phase.CHARGING
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = Color(0xff091022)) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("CLUBHOUSE", modifier = Modifier.testTag("clubhouseTitle"), fontSize = 25.sp, fontWeight = FontWeight.Black); Text("${state.tickets} RANGE TICKETS", color = Color(0xff80ffbf)) }
                    TextButton(onClick = onClose) { Text("CLOSE") }
                }
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    pages.forEachIndexed { i, name -> TextButton(onClick = { page = i }) { Text(if (page == i) "• $name" else name) } }
                }
                if (state.notice.isNotEmpty()) Text(state.notice, color = Color(0xffffd47a), fontSize = 12.sp)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (page) {
                        0 -> {
                            Text("Every completed shot earns a ticket. Each contract refreshes four hours after you claim it.")
                            repeat(3) { slot ->
                                val c = state.contract(slot)
                                @Suppress("UNUSED_VARIABLE") val refresh = timerTick
                                val remaining = state.contractRemaining(slot)
                                val minutes = (remaining + 59_999) / 60_000
                                HubCard {
                                    Text(c.title, fontWeight = FontWeight.Bold)
                                    Text(if (remaining > 0) "NEXT CONTRACT IN ${minutes / 60}h ${minutes % 60}m" else "${state.contractProgress[slot]} / ${c.goal}")
                                    LinearProgressIndicator(progress = { (state.contractProgress[slot].toFloat() / c.goal).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
                                    Text("Reward: ${state.contractReward(slot)} tickets + 5 weekly points", fontSize = 12.sp)
                                    Button(onClick = { state.claimContract(slot) }, enabled = remaining == 0L && state.contractProgress[slot] >= c.goal) { Text(if (remaining > 0) "REFRESHING" else "CLAIM REWARD") }
                                }
                            }
                        }
                        1 -> {
                            Text("Equip two abilities. Unlock them with distance milestone skill points. Charges recharge over real time, including while the game is closed.")
                            Button(onClick={page=2},modifier=Modifier.fillMaxWidth()) { Text("OPEN ABILITY SKILL TREES • ${game.abilityProgress.available} SP") }
                            repeat(2) { slot -> HubCard {
                                Text("SLOT ${slot + 1}: ${if (game.abilityUnlocked(state.slots[slot])) state.slots[slot].title else "LOCKED"}", fontWeight = FontWeight.Bold)
                                FlightAbility.entries.forEach { ability ->
                                    OutlinedButton(onClick = { game.equipAbility(slot, ability) }, enabled = canChange && game.abilityUnlocked(ability), modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = abilityBackground(ability), contentColor = abilityTint(ability))) {
                                        AbilityGlyph(ability, Modifier.size(36.dp)); Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) { Text("${ability.title}${if (!game.abilityUnlocked(ability)) " • SKILL POINT TO UNLOCK" else if (state.slots[slot] == ability) " • EQUIPPED" else ""}"); Text(ability.description, fontSize = 11.sp) }
                                    }
                                }
                            } }
                            if (!canChange) Text("Finish the shot to change equipment.")
                        }
                        2 -> { AbilityTrees(game) }
                        3 -> {
                            Text(state.homeTitle, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Your Pro Shop builds the tee box piece by piece. Each purchase changes its appearance and survives ascension. ${state.facilities.sum()}/96 upgrades.")
                            repeat(state.facilities.size) { id -> HubCard {
                                Text("${state.facilityNames[id]} • ${state.facilities[id]}/${EngagementState.MAX_FACILITY_LEVEL}", fontWeight = FontWeight.Bold)
                                Text(state.facilityEffects[id])
                                LinearProgressIndicator(progress = { state.facilities[id] / EngagementState.MAX_FACILITY_LEVEL.toFloat() }, modifier = Modifier.fillMaxWidth())
                                Button(onClick = { state.upgradeFacility(id) }, enabled = canChange && state.facilities[id] < EngagementState.MAX_FACILITY_LEVEL && state.tickets >= state.facilityCost(id)) {
                                    Text(if (state.facilities[id] == EngagementState.MAX_FACILITY_LEVEL) "MAX LEVEL" else "UPGRADE • ${state.facilityCost(id)} TICKETS")
                                }
                            } }
                            Text("Trophies earned: ${state.totalMedals} expedition medals")
                        }
                        4 -> {
                            Text("WEEK OF ${state.week}", fontWeight = FontWeight.Bold)
                            HubCard {
                                Text(state.expeditionTitle, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                Text(state.expeditionRules)
                                Text("A separate fixed-equipment challenge. Your main progress and upgrades stay in the main range.")
                                Text("Best: ${game.format(state.expeditionBest)} m • ${state.expeditionMedals}/3 medals")
                                Text("500 / 2K / 8K m: 15 tickets + a permanent trail per medal.", fontSize = 12.sp)
                                Button(onClick = onExpedition, enabled = canChange, modifier = Modifier.testTag("enterExpedition")) { Text("PLAY EXPEDITION") }
                            }
                            HubCard {
                                Text("WEEKLY REWARD TRACK", fontWeight = FontWeight.Bold)
                                Text("${state.weekPoints} points • every 20 points gives 12 tickets. Tiers 2, 4 and 6 unlock trails.")
                                LinearProgressIndicator(progress = { (state.weekPoints / 120f).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
                                Text("${state.trackClaimed}/6 tiers claimed. Play any day to catch up; missed days never reset progress.", fontSize = 12.sp)
                                Button(onClick = state::grantTrack, enabled = state.weekPoints / 20 > state.trackClaimed && state.trackClaimed < 6) { Text("CLAIM EARNED TIERS") }
                                Text("Earned tiers are automatically collected at weekly rollover.", fontSize = 11.sp)
                            }
                            Text("TRAIL COLLECTION", fontWeight = FontWeight.Bold)
                            listOf("Mint", "Solar Gold", "Nebula Violet", "Starlight Blue").forEachIndexed { id, title ->
                                OutlinedButton(onClick = { state.chooseTrail(id) }, enabled = id <= state.trailUnlocked) { Text("$title${if (state.trailSelected == id) " • ACTIVE" else if (id > state.trailUnlocked) " • LOCKED" else ""}") }
                            }
                        }
                        else -> {
                            Text("COMBINATION BOOK", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text("Discover combinations through timing and upgraded relic pairs. Effects activate during flight.")
                            repeat(4) { id -> HubCard {
                                Text("${if (state.discoveries[id]) "✦" else "◇"} ${state.comboNames[id]}", fontWeight = FontWeight.Bold)
                                Text(state.comboHints[id]); Text(if (state.discoveries[id]) "DISCOVERED" else "UNDISCOVERED", color = Color(0xff80ffbf), fontSize = 11.sp)
                            } }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
@Composable private fun HubCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}
