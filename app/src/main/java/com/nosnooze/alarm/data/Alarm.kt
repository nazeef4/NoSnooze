package com.nosnooze.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Wake up",
    val enabled: Boolean = true,
    /** Sunday bit 0 through Saturday bit 6. Zero means ring once. */
    val repeatDays: Int = 0,
    val challenge: ChallengeType = ChallengeType.PHOTO_MATCH,
    val referenceImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ChallengeType(val title: String, val subtitle: String) {
    PHOTO_MATCH("Photo mission", "Match any place or object"),
    READ_ALOUD("Read out loud", "Say two lines clearly"),
    AWAKE_SELFIE("Awake selfie", "Prove your eyes are open")
}
