package com.nosnooze.alarm

import android.app.Application
import androidx.room.Room
import com.nosnooze.alarm.alarm.AlarmNotifications
import com.nosnooze.alarm.data.AppDatabase
import com.nosnooze.alarm.data.SettingsStore

class NoSnoozeApp : Application() {
    lateinit var database: AppDatabase
    lateinit var settings: SettingsStore

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "nosnooze.db").build()
        settings = SettingsStore(this)
        AlarmNotifications.createChannel(this)
    }
}

val android.content.Context.app: NoSnoozeApp
    get() = applicationContext as NoSnoozeApp
