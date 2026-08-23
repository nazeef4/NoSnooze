package com.nosnooze.alarm.alarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nosnooze.alarm.data.Alarm
import com.nosnooze.alarm.ui.MainActivity
import java.time.*

class AlarmScheduler(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)

    fun schedule(alarm: Alarm): Long {
        val triggerAt = nextOccurrence(alarm)
        val operation = PendingIntent.getBroadcast(
            context, alarm.id.toInt(),
            Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarm.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val show = PendingIntent.getActivity(
            context, alarm.id.toInt(), Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), operation)
        } catch (_: SecurityException) {
            // The editor sends the user to exact-alarm access. This fallback still preserves the alarm.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
        return triggerAt
    }

    fun cancel(id: Long) {
        val operation = PendingIntent.getBroadcast(
            context, id.toInt(), Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        operation?.let(manager::cancel)
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        fun nextOccurrence(alarm: Alarm, now: ZonedDateTime = ZonedDateTime.now()): Long {
            var candidate = now.withHour(alarm.hour).withMinute(alarm.minute).withSecond(0).withNano(0)
            if (alarm.repeatDays == 0) {
                if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
            } else {
                for (offset in 0..7) {
                    val day = candidate.plusDays(offset.toLong())
                    val bit = day.dayOfWeek.value % 7 // java Sunday=7 -> bit 0
                    if ((alarm.repeatDays and (1 shl bit)) != 0 && day.isAfter(now)) {
                        candidate = day
                        break
                    }
                }
            }
            return candidate.toInstant().toEpochMilli()
        }
    }
}
