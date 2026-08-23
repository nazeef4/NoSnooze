package com.nosnooze.alarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosnooze.alarm.alarm.AlarmScheduler
import com.nosnooze.alarm.data.*
import com.nosnooze.alarm.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    alarms: List<Alarm>,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: (Alarm) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Alarm) -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        containerColor = Paper,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd, containerColor = Orange, contentColor = Color.White, shape = RoundedCornerShape(20.dp)) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("New alarm", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NO SNOOZE", color = Orange, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 12.sp)
                    Text("Your wake-up plan", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
                }
                IconButton(onClick = onSettings, modifier = Modifier.clip(CircleShape).background(Color.White)) { Icon(Icons.Outlined.Tune, "Alarm settings") }
            }
            Spacer(Modifier.height(24.dp))
            WakeCard(alarms.filter { it.enabled }.minByOrNull { AlarmScheduler.nextOccurrence(it) })
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ALARMS", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                Text("${alarms.size} total", color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
            if (alarms.isEmpty()) EmptyState(onAdd) else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                alarms.forEach { AlarmCard(it, onToggle, onEdit, onDelete) }
            }
        }
    }
}

@Composable private fun WakeCard(next: Alarm?) {
    val date = remember(next) {
        next?.let {
            Instant.ofEpochMilli(AlarmScheduler.nextOccurrence(it)).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
        }
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Ink).padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Orange), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Alarm, null, tint = Color.White) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(if (next == null) "YOU'RE ALL CLEAR" else "NEXT WAKE-UP", color = Color.White.copy(.55f), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(if (next == null) "No alarm set" else String.format(Locale.getDefault(), "%02d:%02d", next.hour, next.minute), color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
        }
        date?.let { Text(it, color = Color.White.copy(.75f), fontSize = 12.sp) }
    }
}

@Composable private fun AlarmCard(alarm: Alarm, onToggle: (Alarm, Boolean) -> Unit, onEdit: (Alarm) -> Unit, onDelete: (Alarm) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val tint = when (alarm.challenge) { ChallengeType.PHOTO_MATCH -> Mint; ChallengeType.READ_ALOUD -> Lavender; ChallengeType.AWAKE_SELFIE -> Sky }
    Surface(onClick = { onEdit(alarm) }, shape = RoundedCornerShape(24.dp), color = Color.White, tonalElevation = 0.dp) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tint), contentAlignment = Alignment.Center) {
                Icon(when (alarm.challenge) { ChallengeType.PHOTO_MATCH -> Icons.Outlined.PhotoCamera; ChallengeType.READ_ALOUD -> Icons.Outlined.RecordVoiceOver; ChallengeType.AWAKE_SELFIE -> Icons.Outlined.Face }, null)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute), fontWeight = FontWeight.Black, fontSize = 26.sp)
                Text("${alarm.label}  ·  ${repeatLabel(alarm.repeatDays)}", color = Muted, fontSize = 12.sp)
                Text(alarm.challenge.title, color = Orange, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm, it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Orange))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; onDelete(alarm) })
                }
            }
        }
    }
}

@Composable private fun EmptyState(onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.NightsStay, null, Modifier.size(48.dp), tint = Muted)
        Spacer(Modifier.height(12.dp)); Text("No alarms yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Create one. Tomorrow-you will thank you.", color = Muted)
        TextButton(onClick = onAdd) { Text("Set your first alarm", color = Orange) }
    }
}

private fun repeatLabel(mask: Int): String {
    if (mask == 0) return "Once"
    if (mask == 0b1111111) return "Every day"
    val names = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    return names.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.joinToString(" ")
}
