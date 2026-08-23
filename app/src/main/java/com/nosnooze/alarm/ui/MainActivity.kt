package com.nosnooze.alarm.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nosnooze.alarm.data.Alarm
import com.nosnooze.alarm.ui.screens.*
import com.nosnooze.alarm.ui.theme.NoSnoozeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoSnoozeTheme {
                val vm: MainViewModel = viewModel()
                var route by remember { mutableStateOf<Route>(Route.Home) }
                PermissionRequests()
                when (val page = route) {
                    Route.Home -> HomeScreen(vm.alarms.collectAsState().value, vm::toggle, vm::delete, { route = Route.Editor(null) }, { route = Route.Editor(it) }, { route = Route.Settings })
                    is Route.Editor -> AlarmEditorScreen(page.alarm, onBack = { route = Route.Home }, onSave = { vm.save(it) { route = Route.Home } })
                    Route.Settings -> SettingsScreen(onBack = { route = Route.Home })
                }
            }
        }
    }

    @Composable private fun PermissionRequests() {
        fun requestExactAlarmAccess() {
            val manager = getSystemService(AlarmManager::class.java)
            if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) {
                runCatching { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))) }
            }
        }
        val notification = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { requestExactAlarmAccess() }
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= 33) notification.launch(Manifest.permission.POST_NOTIFICATIONS)
            else requestExactAlarmAccess()
        }
    }
}

private sealed interface Route {
    data object Home : Route
    data class Editor(val alarm: Alarm?) : Route
    data object Settings : Route
}
