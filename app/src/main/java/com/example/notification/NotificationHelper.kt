package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object NotificationHelper {

    const val CHANNEL_ALARM_ID = "bolo_alarm_high_priority"
    const val CHANNEL_TIMER_ID = "bolo_timer_channel"
    const val CHANNEL_BEDTIME_ID = "bolo_bedtime_channel"
    const val CHANNEL_GENERAL_ID = "bolo_general_channel"

    const val NOTIFICATION_ALARM_ID = 1001
    const val NOTIFICATION_TIMER_ID = 2001
    const val NOTIFICATION_BEDTIME_ID = 3001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            // Alarm Channel
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "Alarms & Voice Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority full-screen alarms with personalized voice reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
                setSound(alarmSound, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            // Timer Channel
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ID,
                "Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when timers complete"
                enableVibration(true)
            }

            // Bedtime Channel
            val bedtimeChannel = NotificationChannel(
                CHANNEL_BEDTIME_ID,
                "Bedtime Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Wind-down and bedtime schedule alerts"
            }

            // General Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL_ID,
                "General Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General BoloAlarm notifications"
            }

            notificationManager.createNotificationChannels(
                listOf(alarmChannel, timerChannel, bedtimeChannel, generalChannel)
            )
        }
    }
}
