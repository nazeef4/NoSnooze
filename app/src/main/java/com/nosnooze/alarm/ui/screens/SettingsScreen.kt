package com.nosnooze.alarm.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosnooze.alarm.app
import com.nosnooze.alarm.data.*
import com.nosnooze.alarm.ui.theme.*
import kotlinx.coroutines.launch
import android.media.Ringtone
import android.os.Build

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = context.app.settings
    val current by store.values.collectAsState(initial = AlarmSettings())
    val scope = rememberCoroutineScope()
    fun update(value: AlarmSettings) { scope.launch { store.update(value) } }
    var preview by remember { mutableStateOf<Ringtone?>(null) }
    DisposableEffect(Unit) { onDispose { preview?.stop() } }
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = if (Build.VERSION.SDK_INT >= 33) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        if (result.resultCode == android.app.Activity.RESULT_OK) update(current.copy(soundUri = uri?.toString()))
    }

    Scaffold(containerColor = Paper, topBar = {
        Row(Modifier.statusBarsPadding().fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
            Text("Alarm power", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("LOUD & RELENTLESS", color = Orange, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
            Text("Tune the wake-up", fontWeight = FontWeight.Black, fontSize = 30.sp)
            Spacer(Modifier.height(24.dp))
            SettingCard {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.VolumeUp, null); Spacer(Modifier.width(12.dp)); Column { Text("Alarm volume", fontWeight = FontWeight.Bold); Text("Independent playback level", color = Muted, fontSize = 12.sp) } }
                Slider(current.volume, { update(current.copy(volume = it)) }, valueRange = .25f..1f, colors = SliderDefaults.colors(thumbColor = Orange, activeTrackColor = Orange))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        soundPicker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current.soundUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                        })
                    }) { Icon(Icons.Outlined.LibraryMusic, null); Spacer(Modifier.width(5.dp)); Text("Choose") }
                    OutlinedButton(onClick = {
                        if (preview?.isPlaying == true) { preview?.stop(); preview = null }
                        else preview = RingtoneManager.getRingtone(context, current.soundUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))?.also { if (Build.VERSION.SDK_INT >= 28) it.isLooping = false; it.play() }
                    }) { Icon(Icons.Outlined.PlayArrow, null); Text("Test") }
                }
            }
            Spacer(Modifier.height(14.dp))
            SettingCard {
                Text("Vibration", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    VibrationMode.entries.forEachIndexed { index, value ->
                        SegmentedButton(selected = current.vibration == value, onClick = { update(current.copy(vibration = value)) }, shape = SegmentedButtonDefaults.itemShape(index, VibrationMode.entries.size)) { Text(value.label) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Gradual volume", fontWeight = FontWeight.Bold); Text("Build from 15% to your chosen level", color = Muted, fontSize = 12.sp) }
                    Switch(current.gradualVolume, { update(current.copy(gradualVolume = it)) }, colors = SwitchDefaults.colors(checkedTrackColor = Orange))
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("PHONE ACCESS", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            AccessRow(Icons.Outlined.AlarmOn, "Exact alarm access", "Keeps alarms accurate and shows the alarm icon") {
                if (Build.VERSION.SDK_INT >= 31) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
            if (Build.VERSION.SDK_INT >= 34) AccessRow(Icons.Outlined.Fullscreen, "Full-screen alerts", "Opens the mission over the lock screen") {
                context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
            }
            Spacer(Modifier.height(18.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = Lavender.copy(.45f)) {
                Row(Modifier.padding(16.dp)) { Icon(Icons.Outlined.Security, null); Spacer(Modifier.width(12.dp)); Text("Your alarms, voice recognition and photos stay on this phone. No account and no cloud upload.", fontSize = 13.sp) }
            }
        }
    }
}

@Composable private fun SettingCard(content: @Composable ColumnScope.() -> Unit) = Surface(shape = RoundedCornerShape(24.dp), color = androidx.compose.ui.graphics.Color.White) { Column(Modifier.padding(18.dp), content = content) }

@Composable private fun AccessRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 12.sp) }; Icon(Icons.Outlined.ChevronRight, null) }
    }
}
