package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bedtime_schedule")
data class BedtimeScheduleEntity(
    @PrimaryKey
    val id: Long = 1,
    val bedHour: Int = 22, // 10:30 PM
    val bedMinute: Int = 30,
    val wakeHour: Int = 6, // 6:30 AM
    val wakeMinute: Int = 30,
    val repeatDays: Int = 127, // Every day by default
    val isEnabled: Boolean = true,
    val personName: String = "User",
    val isVoiceEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val language: String = "hi",
    val reminderBeforeMinutes: Int = 15 // Reminder 15 mins before bed
) {
    fun calculateSleepDurationHours(): Double {
        var bedMinutes = bedHour * 60 + bedMinute
        var wakeMinutes = wakeHour * 60 + wakeMinute
        if (wakeMinutes <= bedMinutes) {
            wakeMinutes += 24 * 60
        }
        val diff = wakeMinutes - bedMinutes
        return diff / 60.0
    }
}
