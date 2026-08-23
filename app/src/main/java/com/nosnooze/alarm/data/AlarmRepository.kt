package com.nosnooze.alarm.data

import android.content.Context
import com.nosnooze.alarm.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class AlarmRepository(context: Context) {
    private val dao = context.applicationContext.let { (it as com.nosnooze.alarm.NoSnoozeApp).database.alarms() }
    private val scheduler = AlarmScheduler(context)
    fun observe(): Flow<List<Alarm>> = dao.observeAll()
    suspend fun get(id: Long) = dao.get(id)
    suspend fun save(alarm: Alarm): Long {
        val id = dao.save(alarm)
        val saved = alarm.copy(id = if (alarm.id == 0L) id else alarm.id)
        if (saved.enabled) scheduler.schedule(saved) else scheduler.cancel(saved.id)
        return id
    }
    suspend fun toggle(alarm: Alarm, enabled: Boolean) {
        dao.setEnabled(alarm.id, enabled)
        if (enabled) scheduler.schedule(alarm.copy(enabled = true)) else scheduler.cancel(alarm.id)
    }
    suspend fun delete(alarm: Alarm) { scheduler.cancel(alarm.id); dao.delete(alarm) }
}
