package com.nosnooze.alarm.alarm

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*
import androidx.core.app.ServiceCompat
import com.nosnooze.alarm.app
import com.nosnooze.alarm.data.VibrationMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AlarmRingingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopAlarm(); return START_NOT_STICKY }
        val id = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        if (id < 0) return START_NOT_STICKY
        scope.launch {
            val alarm = withContext(Dispatchers.IO) { applicationContext.app.database.alarms().get(id) }
                ?: return@launch stopSelf()
            ServiceCompat.startForeground(
                this@AlarmRingingService,
                AlarmNotifications.NOTIFICATION_ID,
                AlarmNotifications.ringing(this@AlarmRingingService, id, alarm.label),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
            startSoundAndVibration()
        }
        return START_STICKY
    }

    private suspend fun startSoundAndVibration() {
        if (player != null) return
        val settings = applicationContext.app.settings.values.first()
        val uri = settings.soundUri?.let { android.net.Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            setDataSource(this@AlarmRingingService, uri)
            isLooping = true
            prepare()
            val initial = if (settings.gradualVolume) .15f else settings.volume
            setVolume(initial, initial)
            start()
        }
        if (settings.gradualVolume) scope.launch {
            repeat(9) { step ->
                delay(2_000)
                val level = (.15f + (settings.volume - .15f) * ((step + 1) / 9f)).coerceIn(0f, 1f)
                player?.setVolume(level, level)
            }
        }
        vibrator = getSystemService(Vibrator::class.java)
        val pattern = when (settings.vibration) {
            VibrationMode.OFF -> null
            VibrationMode.STEADY -> longArrayOf(0, 900, 350)
            VibrationMode.INTENSE -> longArrayOf(0, 450, 120, 450, 120, 900, 300)
        }
        pattern?.let { vibrator?.vibrate(VibrationEffect.createWaveform(it, 0)) }
    }

    private fun stopAlarm() {
        player?.runCatching { stop() }
        player?.release(); player = null
        vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() { stopAlarm(); scope.cancel(); super.onDestroy() }

    companion object {
        const val ACTION_START = "com.nosnooze.START_ALARM"
        const val ACTION_STOP = "com.nosnooze.COMPLETE_ALARM"
        fun complete(context: android.content.Context) {
            context.startService(Intent(context, AlarmRingingService::class.java).setAction(ACTION_STOP))
        }
    }
}
