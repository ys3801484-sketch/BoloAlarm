package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int, // 0 - 23
    val minute: Int, // 0 - 59
    val second: Int = 0, // 0 - 59
    val name: String, // e.g. "Breakfast Reminder"
    val personName: String, // e.g. "Yaseen"
    val category: String = "Custom", // e.g. "Breakfast", "Lunch", "Medicine", etc.
    val customMessage: String = "",
    val language: String = "hi", // "hi", "en", "default"
    val voiceName: String = "",
    val speechSpeed: Float = 1.0f, // 0.5f - 2.0f
    val speechPitch: Float = 1.0f, // 0.5f - 2.0f
    val repeatDays: Int = 0, // Bitmask: bit 0=Mon, 1=Tue, ..., 6=Sun. 0 = Once, 127 = Every day
    val specificDateMillis: Long? = null, // If set, triggers only on this specific calendar date
    val isEnabled: Boolean = true,
    val isVoiceEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val vibrationPatternType: String = "normal", // "short", "normal", "long", "heartbeat", "custom"
    val customVibrationPattern: String = "0,800,400,800",
    val soundUri: String = "default",
    val volume: Int = 90,
    val snoozeDurationMinutes: Int = 10, // Completely custom minutes: 1..120
    val ringDurationSeconds: Int = 300, // 30, 60, 120, 300, 600, 900, or -1 (until stopped)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isRepeating(): Boolean = repeatDays > 0 && specificDateMillis == null

    fun isSpecificDate(): Boolean = specificDateMillis != null

    fun isDaySelected(dayIndex: Int): Boolean { // dayIndex: 0 = Mon, 1 = Tue, ..., 6 = Sun
        val bit = 1 shl dayIndex
        return (repeatDays and bit) != 0
    }

    fun getScheduleSummary(): String {
        if (specificDateMillis != null) {
            val format = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
            return format.format(Date(specificDateMillis))
        }
        if (repeatDays == 0) return "Once"
        if (repeatDays == 127) return "Every day"
        if (repeatDays == 31) return "Weekdays (Mon-Fri)"
        if (repeatDays == 96) return "Weekends (Sat-Sun)"

        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val active = mutableListOf<String>()
        for (i in 0..6) {
            if ((repeatDays and (1 shl i)) != 0) {
                active.add(days[i])
            }
        }
        return active.joinToString(" ")
    }

    fun getRepeatSummary(): String = getScheduleSummary()
}
