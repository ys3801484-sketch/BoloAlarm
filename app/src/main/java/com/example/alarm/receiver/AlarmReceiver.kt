package com.example.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.alarm.service.AlarmRingingService
import com.example.alarm.ui.AlarmRingingActivity

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.example.boloalarm.ACTION_TRIGGER_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_NAME = "extra_alarm_name"
        const val EXTRA_PERSON_NAME = "extra_person_name"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_CUSTOM_MESSAGE = "extra_custom_message"
        const val EXTRA_LANGUAGE = "extra_language"
        const val EXTRA_VOICE_NAME = "extra_voice_name"
        const val EXTRA_SPEECH_SPEED = "extra_speech_speed"
        const val EXTRA_VOICE_ENABLED = "extra_voice_enabled"
        const val EXTRA_SOUND_ENABLED = "extra_sound_enabled"
        const val EXTRA_VIBRATION_ENABLED = "extra_vibration_enabled"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_SPEECH_PITCH = "extra_speech_pitch"
        const val EXTRA_RING_DURATION_SECONDS = "extra_ring_duration_seconds"
        const val EXTRA_VIBRATION_PATTERN_TYPE = "extra_vibration_pattern_type"
        const val EXTRA_CUSTOM_VIBRATION_PATTERN = "extra_custom_vibration_pattern"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER_ALARM) {
            return
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) {
            return
        }

        // Acquire a temporary partial wake lock to guarantee background broadcast execution
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = try {
            powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "BoloAlarm:AlarmReceiverWakeLock"
            )?.apply {
                acquire(60 * 1000L) // max 1 minute
            }
        } catch (e: Exception) {
            null
        }

        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            `package` = context.packageName
            action = AlarmRingingService.ACTION_START_RINGING
            putExtras(intent)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Service startup error handled safely
        }

        // Also launch AlarmRingingActivity directly with NEW_TASK
        val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtras(intent)
        }
        try {
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            // Activity startup handled via notification full-screen intent
        }
    }
}
