package com.nosnooze.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nosnooze.alarm.app
import kotlinx.coroutines.*

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { context.app.database.alarms().enabled().forEach { AlarmScheduler(context).schedule(it) } }
            finally { pending.finish() }
        }
    }
}
