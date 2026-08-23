package com.nosnooze.alarm.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosnooze.alarm.data.*
import com.nosnooze.alarm.ui.theme.*
import java.io.File
import java.time.LocalTime
import java.util.Locale

@Composable
fun AlarmEditorScreen(existing: Alarm?, onBack: () -> Unit, onSave: (Alarm) -> Unit) {
    val context = LocalContext.current
    val now = LocalTime.now().plusMinutes(1)
    var hour by remember { mutableIntStateOf(existing?.hour ?: now.hour) }
    var minute by remember { mutableIntStateOf(existing?.minute ?: now.minute) }
    var label by remember { mutableStateOf(existing?.label ?: "Wake up") }
    var repeatDays by remember { mutableIntStateOf(existing?.repeatDays ?: 0) }
    var challenge by remember { mutableStateOf(existing?.challenge ?: ChallengeType.PHOTO_MATCH) }
    var reference by remember { mutableStateOf(existing?.referenceImagePath) }
    var cameraOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (cameraOpen) {
        val file = remember { File(context.filesDir, "photo_reference/reference_${System.currentTimeMillis()}.jpg") }
        Box(Modifier.fillMaxSize()) {
            CameraCapture(file, false, "Save reference photo", onCaptured = { reference = it.absolutePath; cameraOpen = false }, onError = { error = it })
            IconButton(onClick = { cameraOpen = false }, modifier = Modifier.statusBarsPadding().padding(12.dp).background(Color.Black.copy(.5f), CircleShape)) { Icon(Icons.Outlined.Close, "Close", tint = Color.White) }
        }
        return
    }

    Scaffold(containerColor = Paper, topBar = {
        Row(Modifier.statusBarsPadding().fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
            Text(if (existing == null) "New alarm" else "Edit alarm", fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                if (challenge == ChallengeType.PHOTO_MATCH && reference == null) error = "Add a reference photo first"
                else onSave(Alarm(existing?.id ?: 0, hour, minute, label.ifBlank { "Wake up" }, true, repeatDays, challenge, reference, existing?.createdAt ?: System.currentTimeMillis()))
            }) { Text("SAVE", color = Orange, fontWeight = FontWeight.Black) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            TextButton(onClick = { TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(String.format(Locale.getDefault(), "%02d:%02d", hour, minute), fontSize = 68.sp, fontWeight = FontWeight.Black, color = Ink)
            }
            Text("TAP TO CHANGE", Modifier.align(Alignment.CenterHorizontally), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(26.dp))
            SectionTitle("ALARM NAME")
            OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), leadingIcon = { Icon(Icons.Outlined.Label, null) })
            Spacer(Modifier.height(22.dp))
            SectionTitle("REPEAT")
            DayPicker(repeatDays) { repeatDays = it }
            Spacer(Modifier.height(26.dp))
            SectionTitle("MISSION TO DISMISS")
            Text("No dismiss button. Finish the mission and the alarm stops.", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            ChallengeType.entries.forEach { type ->
                ChallengeChoice(type, challenge == type) { challenge = type }
                Spacer(Modifier.height(10.dp))
            }
            if (challenge == ChallengeType.PHOTO_MATCH) {
                Surface(shape = RoundedCornerShape(20.dp), color = Mint.copy(.48f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null); Spacer(Modifier.width(10.dp)); Text("Choose any fixed place or object away from bed — for example your balcony, sink, or coffee machine. Day/night lighting changes are supported, but the target must still be visible.", fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { cameraOpen = true }, shape = RoundedCornerShape(14.dp)) {
                            Icon(if (reference == null) Icons.Outlined.AddAPhoto else Icons.Outlined.CheckCircle, null)
                            Spacer(Modifier.width(8.dp)); Text(if (reference == null) "Add reference photo" else "Reference ready · Retake")
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp)) }
            Spacer(Modifier.height(110.dp))
        }
    }
}

@Composable private fun SectionTitle(value: String) = Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))

@Composable private fun DayPicker(mask: Int, onChange: (Int) -> Unit) {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            val selected = mask and (1 shl index) != 0
            Box(Modifier.size(42.dp).clip(CircleShape).background(if (selected) Ink else Color.White).clickable { onChange(mask xor (1 shl index)) }, contentAlignment = Alignment.Center) {
                Text(day, color = if (selected) Color.White else Ink, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable private fun ChallengeChoice(type: ChallengeType, selected: Boolean, onSelect: () -> Unit) {
    val icon = when (type) { ChallengeType.PHOTO_MATCH -> Icons.Outlined.PhotoCamera; ChallengeType.READ_ALOUD -> Icons.Outlined.RecordVoiceOver; ChallengeType.AWAKE_SELFIE -> Icons.Outlined.Face }
    val color = when (type) { ChallengeType.PHOTO_MATCH -> Mint; ChallengeType.READ_ALOUD -> Lavender; ChallengeType.AWAKE_SELFIE -> Sky }
    Surface(onClick = onSelect, shape = RoundedCornerShape(22.dp), border = if (selected) BorderStroke(2.dp, Orange) else null, color = Color.White) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(color), contentAlignment = Alignment.Center) { Icon(icon, null) }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(type.title, fontWeight = FontWeight.Bold); Text(type.subtitle, color = Muted, fontSize = 12.sp) }
            RadioButton(selected, onClick = onSelect, colors = RadioButtonDefaults.colors(selectedColor = Orange))
        }
    }
}
