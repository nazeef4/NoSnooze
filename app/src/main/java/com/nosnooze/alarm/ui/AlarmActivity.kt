package com.nosnooze.alarm.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nosnooze.alarm.alarm.*
import com.nosnooze.alarm.app
import com.nosnooze.alarm.data.*
import com.nosnooze.alarm.ui.screens.*
import com.nosnooze.alarm.ui.theme.NoSnoozeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        setContent {
            NoSnoozeTheme {
                BackHandler(true) { }
                val alarm by produceState<Alarm?>(null, id) { value = withContext(Dispatchers.IO) { app.database.alarms().get(id) } }
                Surface(Modifier.fillMaxSize(), color = com.nosnooze.alarm.ui.theme.Ink) {
                    alarm?.let { item ->
                        val complete = { AlarmRingingService.complete(this); finishAndRemoveTask() }
                        when (item.challenge) {
                            ChallengeType.PHOTO_MATCH -> if (item.referenceImagePath != null) PhotoMatchChallenge(item.referenceImagePath, complete) else ReadAloudChallenge(id, complete, "Your reference photo is unavailable. Complete this backup mission.")
                            ChallengeType.READ_ALOUD -> ReadAloudChallenge(id, complete)
                            ChallengeType.AWAKE_SELFIE -> SelfieChallenge(complete)
                        }
                    }
                }
            }
        }
    }
}
