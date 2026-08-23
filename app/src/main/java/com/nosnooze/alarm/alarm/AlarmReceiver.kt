package com.nosnooze.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nosnooze.alarm.app
import kotlinx.coroutines.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        if (id < 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val alarm = context.app.database.alarms().get(id) ?: return@launch
                if (!alarm.enabled) return@launch
                if (alarm.repeatDays == 0) context.app.database.alarms().setEnabled(id, false)
                else AlarmScheduler(context).schedule(alarm)
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmRingingService::class.java)
                        .setAction(AlarmRingingService.ACTION_START)
                        .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
                )
            } finally { pending.finish() }
        }
    }
}
