package com.nosnooze.alarm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute") fun observeAll(): Flow<List<Alarm>>
    @Query("SELECT * FROM alarms WHERE id = :id") suspend fun get(id: Long): Alarm?
    @Query("SELECT * FROM alarms WHERE enabled = 1") suspend fun enabled(): List<Alarm>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(alarm: Alarm): Long
    @Delete suspend fun delete(alarm: Alarm)
    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id") suspend fun setEnabled(id: Long, enabled: Boolean)
}
