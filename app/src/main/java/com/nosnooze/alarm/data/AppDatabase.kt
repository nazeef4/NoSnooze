package com.nosnooze.alarm.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromChallenge(value: ChallengeType) = value.name
    @TypeConverter fun toChallenge(value: String) = ChallengeType.valueOf(value)
}

@Database(entities = [Alarm::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
}
