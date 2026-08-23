package com.nosnooze.alarm.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("alarm_settings")

data class AlarmSettings(
    val volume: Float = 1f,
    val vibration: VibrationMode = VibrationMode.INTENSE,
    val gradualVolume: Boolean = false,
    val soundUri: String? = null
)

enum class VibrationMode(val label: String) { OFF("Off"), STEADY("Steady"), INTENSE("Intense") }

class SettingsStore(private val context: Context) {
    private object Keys {
        val volume = floatPreferencesKey("volume")
        val vibration = stringPreferencesKey("vibration")
        val gradual = booleanPreferencesKey("gradual")
        val soundUri = stringPreferencesKey("sound_uri")
    }
    val values: Flow<AlarmSettings> = context.dataStore.data.map { p ->
        AlarmSettings(
            volume = p[Keys.volume] ?: 1f,
            vibration = runCatching { VibrationMode.valueOf(p[Keys.vibration] ?: "INTENSE") }.getOrDefault(VibrationMode.INTENSE),
            gradualVolume = p[Keys.gradual] ?: false,
            soundUri = p[Keys.soundUri]
        )
    }
    suspend fun update(settings: AlarmSettings) = context.dataStore.edit { p ->
        p[Keys.volume] = settings.volume
        p[Keys.vibration] = settings.vibration.name
        p[Keys.gradual] = settings.gradualVolume
        if (settings.soundUri == null) p.remove(Keys.soundUri) else p[Keys.soundUri] = settings.soundUri
    }
}
