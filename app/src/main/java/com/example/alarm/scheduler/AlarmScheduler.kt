package com.example.alarm.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import com.example.MainActivity
import com.example.alarm.receiver.AlarmReceiver
import com.example.data.model.AlarmEntity
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    fun scheduleAlarm(alarm: AlarmEntity) {
        if (alarmManager == null || !alarm.isEnabled) return

        val triggerTime = calculateNextTriggerTime(
            hour = alarm.hour,
            minute = alarm.minute,
            second = alarm.second,
            repeatDays = alarm.repeatDays,
            specificDateMillis = alarm.specificDateMillis
        )

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            `package` = context.packageName
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_NAME, alarm.name)
            putExtra(AlarmReceiver.EXTRA_PERSON_NAME, alarm.personName)
            putExtra(AlarmReceiver.EXTRA_CATEGORY, alarm.category)
            putExtra(AlarmReceiver.EXTRA_CUSTOM_MESSAGE, alarm.customMessage)
            putExtra(AlarmReceiver.EXTRA_LANGUAGE, alarm.language)
            putExtra(AlarmReceiver.EXTRA_VOICE_NAME, alarm.voiceName)
            putExtra(AlarmReceiver.EXTRA_SPEECH_SPEED, alarm.speechSpeed)
            putExtra(AlarmReceiver.EXTRA_SPEECH_PITCH, alarm.speechPitch)
            putExtra(AlarmReceiver.EXTRA_VOICE_ENABLED, alarm.isVoiceEnabled)
            putExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, alarm.isSoundEnabled)
            putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, alarm.isVibrationEnabled)
            putExtra(AlarmReceiver.EXTRA_VIBRATION_PATTERN_TYPE, alarm.vibrationPatternType)
            putExtra(AlarmReceiver.EXTRA_CUSTOM_VIBRATION_PATTERN, alarm.customVibrationPattern)
            putExtra(AlarmReceiver.EXTRA_SOUND_URI, alarm.soundUri)
            putExtra(AlarmReceiver.EXTRA_VOLUME, alarm.volume)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, alarm.snoozeDurationMinutes)
            putExtra(AlarmReceiver.EXTRA_RING_DURATION_SECONDS, alarm.ringDurationSeconds)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                `package` = context.packageName
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("AlarmScheduler", "Alarm ${alarm.id} scheduled for timestamp $triggerTime")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling exact alarm", e)
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                Log.e("AlarmScheduler", "Failed fallback scheduling", ex)
            }
        }
    }

    fun cancelAlarm(alarmId: Long) {
        if (alarmManager == null) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            `package` = context.packageName
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Also cancel any pending snooze intent for this alarm ID
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 100000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(snoozePendingIntent)
    }

    fun scheduleSnooze(
        alarmId: Long,
        alarmName: String,
        personName: String,
        category: String,
        customMessage: String,
        language: String,
        snoozeMinutes: Int,
        isVoiceEnabled: Boolean,
        isSoundEnabled: Boolean,
        isVibrationEnabled: Boolean,
        speechSpeed: Float = 1.0f,
        speechPitch: Float = 1.0f,
        voiceName: String = "",
        soundUri: String = "default",
        vibrationPatternType: String = "normal",
        ringDurationSeconds: Int = 300
    ) {
        if (alarmManager == null) return
        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            `package` = context.packageName
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_NAME, "$alarmName (Snoozed)")
            putExtra(AlarmReceiver.EXTRA_PERSON_NAME, personName)
            putExtra(AlarmReceiver.EXTRA_CATEGORY, category)
            putExtra(AlarmReceiver.EXTRA_CUSTOM_MESSAGE, customMessage)
            putExtra(AlarmReceiver.EXTRA_LANGUAGE, language)
            putExtra(AlarmReceiver.EXTRA_VOICE_NAME, voiceName)
            putExtra(AlarmReceiver.EXTRA_SPEECH_SPEED, speechSpeed)
            putExtra(AlarmReceiver.EXTRA_SPEECH_PITCH, speechPitch)
            putExtra(AlarmReceiver.EXTRA_VOICE_ENABLED, isVoiceEnabled)
            putExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, isSoundEnabled)
            putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, isVibrationEnabled)
            putExtra(AlarmReceiver.EXTRA_VIBRATION_PATTERN_TYPE, vibrationPatternType)
            putExtra(AlarmReceiver.EXTRA_SOUND_URI, soundUri)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(AlarmReceiver.EXTRA_RING_DURATION_SECONDS, ringDurationSeconds)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 100000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            (alarmId + 100000).toInt(),
            Intent(context, MainActivity::class.java).apply {
                `package` = context.packageName
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, showIntent), pendingIntent)
        } catch (e: Exception) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    companion object {
        fun calculateNextTriggerTime(
            hour: Int,
            minute: Int,
            second: Int = 0,
            repeatDays: Int = 0,
            specificDateMillis: Long? = null
        ): Long {
            val now = Calendar.getInstance()

            // If a specific calendar date was selected:
            if (specificDateMillis != null) {
                val target = Calendar.getInstance().apply {
                    timeInMillis = specificDateMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, second)
                    set(Calendar.MILLISECOND, 0)
                }
                return target.timeInMillis
            }

            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, second)
                set(Calendar.MILLISECOND, 0)
            }

            if (repeatDays == 0) {
                // One-time alarm
                if (target.timeInMillis <= now.timeInMillis) {
                    // Passed for today, schedule for tomorrow
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                return target.timeInMillis
            }

            // Recurring alarm: check days
            // Bitmask: bit 0 = Mon, bit 1 = Tue, ..., bit 5 = Sat, bit 6 = Sun
            for (dayOffset in 0..7) {
                val checkCal = Calendar.getInstance().apply {
                    timeInMillis = target.timeInMillis
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                }
                if (dayOffset == 0 && checkCal.timeInMillis <= now.timeInMillis) {
                    continue
                }
                val calDay = checkCal.get(Calendar.DAY_OF_WEEK)
                val bitIndex = when (calDay) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                if ((repeatDays and (1 shl bitIndex)) != 0) {
                    return checkCal.timeInMillis
                }
            }

            // Fallback to next day
            target.add(Calendar.DAY_OF_YEAR, 1)
            return target.timeInMillis
        }
    }
}
