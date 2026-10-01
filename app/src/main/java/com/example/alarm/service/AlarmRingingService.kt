package com.example.alarm.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.alarm.receiver.AlarmReceiver
import com.example.alarm.scheduler.AlarmScheduler
import com.example.alarm.ui.AlarmRingingActivity
import com.example.notification.NotificationHelper
import com.example.tts.BoloTTSManager
import com.example.tts.PersonalizedMessageEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlarmRingingService : Service() {

    companion object {
        const val ACTION_START_RINGING = "com.example.boloalarm.ACTION_START_RINGING"
        const val ACTION_STOP_RINGING = "com.example.boloalarm.ACTION_STOP_RINGING"
        const val ACTION_SNOOZE_RINGING = "com.example.boloalarm.ACTION_SNOOZE_RINGING"

        var isRinging = false
            private set
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var ttsManager: BoloTTSManager? = null
    private var speechLoopJob: Job? = null
    private var ringTimeoutJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private var currentAlarmId: Long = 0
    private var currentAlarmName: String = "Alarm"
    private var currentPersonName: String = "User"
    private var currentCategory: String = "Wake Up"
    private var currentCustomMessage: String = ""
    private var currentLanguage: String = "hi"
    private var currentVoiceName: String = ""
    private var currentSpeechSpeed: Float = 1.0f
    private var currentSpeechPitch: Float = 1.0f
    private var isVoiceEnabled: Boolean = true
    private var isSoundEnabled: Boolean = true
    private var isVibrationEnabled: Boolean = true
    private var vibrationPatternType: String = "normal"
    private var customVibrationPattern: String = "0,800,400,800"
    private var soundUriString: String = "default"
    private var snoozeMinutes: Int = 10
    private var ringDurationSeconds: Int = 300

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BoloAlarm:RingingServiceWakeLock"
        )
        wakeLock?.acquire(30 * 60 * 1000L) // 30 minutes max safe timeout

        ttsManager = BoloTTSManager(this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_RINGING

        when (action) {
            ACTION_STOP_RINGING -> {
                stopAlarmAndFinish()
            }
            ACTION_SNOOZE_RINGING -> {
                handleSnooze()
            }
            ACTION_START_RINGING -> {
                extractIntentData(intent)
                startForegroundRinging()
            }
        }
        return START_NOT_STICKY
    }

    private fun extractIntentData(intent: Intent?) {
        if (intent == null) return
        currentAlarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0)
        currentAlarmName = (intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_NAME) ?: "Alarm").take(100)
        currentPersonName = (intent.getStringExtra(AlarmReceiver.EXTRA_PERSON_NAME) ?: "User").take(50)
        currentCategory = (intent.getStringExtra(AlarmReceiver.EXTRA_CATEGORY) ?: "Wake Up").take(50)
        currentCustomMessage = (intent.getStringExtra(AlarmReceiver.EXTRA_CUSTOM_MESSAGE) ?: "").take(1000)
        currentLanguage = (intent.getStringExtra(AlarmReceiver.EXTRA_LANGUAGE) ?: "hi").take(10)
        currentVoiceName = (intent.getStringExtra(AlarmReceiver.EXTRA_VOICE_NAME) ?: "").take(100)
        currentSpeechSpeed = intent.getFloatExtra(AlarmReceiver.EXTRA_SPEECH_SPEED, 1.0f).coerceIn(0.5f, 2.0f)
        currentSpeechPitch = intent.getFloatExtra(AlarmReceiver.EXTRA_SPEECH_PITCH, 1.0f).coerceIn(0.5f, 2.0f)
        isVoiceEnabled = intent.getBooleanExtra(AlarmReceiver.EXTRA_VOICE_ENABLED, true)
        isSoundEnabled = intent.getBooleanExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, true)
        isVibrationEnabled = intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, true)
        vibrationPatternType = (intent.getStringExtra(AlarmReceiver.EXTRA_VIBRATION_PATTERN_TYPE) ?: "normal").take(30)
        customVibrationPattern = (intent.getStringExtra(AlarmReceiver.EXTRA_CUSTOM_VIBRATION_PATTERN) ?: "0,800,400,800").take(100)
        soundUriString = (intent.getStringExtra(AlarmReceiver.EXTRA_SOUND_URI) ?: "default").take(500)
        snoozeMinutes = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 10).coerceIn(1, 120)
        ringDurationSeconds = intent.getIntExtra(AlarmReceiver.EXTRA_RING_DURATION_SECONDS, 300).coerceIn(10, 3600)
    }

    private fun startForegroundRinging() {
        isRinging = true

        val speechText = PersonalizedMessageEngine.generateMessage(
            personName = currentPersonName,
            category = currentCategory,
            customMessage = currentCustomMessage,
            language = currentLanguage
        )

        // Notification builder with full-screen intent
        val notification = buildForegroundNotification(speechText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ALARM_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ALARM_ID, notification)
        }

        // 1. Play Alarm Sound
        if (isSoundEnabled) {
            playAlarmSound()
        }

        // 2. Start Vibration
        if (isVibrationEnabled) {
            startVibration()
        }

        // 3. Start TTS Personalized Speech
        if (isVoiceEnabled) {
            startVoiceAnnouncement(speechText)
        }

        // 4. Handle Ring Duration Timeout
        ringTimeoutJob?.cancel()
        if (ringDurationSeconds > 0) {
            ringTimeoutJob = serviceScope.launch {
                delay(ringDurationSeconds * 1000L)
                if (isRinging) {
                    stopAlarmAndFinish()
                }
            }
        }
    }

    private fun playAlarmSound() {
        try {
            mediaPlayer?.release()
            var alertUri: Uri? = null

            if (soundUriString != "default" && soundUriString.isNotBlank()) {
                try {
                    alertUri = soundUriString.toUri()
                } catch (e: Exception) {
                    alertUri = null
                }
            }

            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingingService, alertUri!!)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Failed to start media player", e)
        }
    }

    private fun startVibration() {
        val pattern = when (vibrationPatternType.lowercase()) {
            "short" -> longArrayOf(0, 300, 300, 300)
            "long" -> longArrayOf(0, 1500, 500, 1500)
            "heartbeat" -> longArrayOf(0, 200, 150, 350, 700)
            "custom" -> {
                try {
                    customVibrationPattern.split(",")
                        .map { it.trim().toLong() }
                        .toLongArray()
                } catch (e: Exception) {
                    longArrayOf(0, 800, 400, 800)
                }
            }
            else -> longArrayOf(0, 800, 400, 800, 400, 800) // normal
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Failed to vibrate", e)
        }
    }

    private fun startVoiceAnnouncement(speechText: String) {
        speechLoopJob?.cancel()
        speechLoopJob = serviceScope.launch {
            // Wait for TTS engine to be ready
            var attempts = 0
            while (ttsManager?.isReady?.value != true && attempts < 25) {
                delay(200)
                attempts++
            }

            while (isActive && isRinging) {
                // Lower media volume slightly while speaking so voice is crystal clear
                mediaPlayer?.setVolume(0.2f, 0.2f)
                ttsManager?.speak(
                    text = speechText,
                    language = currentLanguage,
                    speechSpeed = currentSpeechSpeed,
                    speechPitch = currentSpeechPitch,
                    voiceName = currentVoiceName
                )

                // Wait for speech duration + interval before speaking again
                delay(6000)
                mediaPlayer?.setVolume(1.0f, 1.0f)
                delay(5000)
            }
        }
    }

    private fun buildForegroundNotification(speechText: String): Notification {
        NotificationHelper.createNotificationChannels(this)

        // Full-screen intent pointing to AlarmRingingActivity
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            `package` = packageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, currentAlarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_NAME, currentAlarmName)
            putExtra(AlarmReceiver.EXTRA_PERSON_NAME, currentPersonName)
            putExtra(AlarmReceiver.EXTRA_CATEGORY, currentCategory)
            putExtra(AlarmReceiver.EXTRA_CUSTOM_MESSAGE, currentCustomMessage)
            putExtra(AlarmReceiver.EXTRA_LANGUAGE, currentLanguage)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            currentAlarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Intent
        val stopIntent = Intent(this, AlarmRingingService::class.java).apply {
            `package` = packageName
            action = ACTION_STOP_RINGING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Intent
        val snoozeIntent = Intent(this, AlarmRingingService::class.java).apply {
            `package` = packageName
            action = ACTION_SNOOZE_RINGING
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            102,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ALARM_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("BoloAlarm: $currentAlarmName")
            .setContentText(speechText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(speechText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_media_pause, "Snooze ($snoozeMinutes m)", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun handleSnooze() {
        AlarmScheduler(this).scheduleSnooze(
            alarmId = currentAlarmId,
            alarmName = currentAlarmName,
            personName = currentPersonName,
            category = currentCategory,
            customMessage = currentCustomMessage,
            language = currentLanguage,
            snoozeMinutes = snoozeMinutes,
            isVoiceEnabled = isVoiceEnabled,
            isSoundEnabled = isSoundEnabled,
            isVibrationEnabled = isVibrationEnabled,
            speechSpeed = currentSpeechSpeed,
            speechPitch = currentSpeechPitch,
            voiceName = currentVoiceName,
            soundUri = soundUriString,
            vibrationPatternType = vibrationPatternType,
            ringDurationSeconds = ringDurationSeconds
        )
        stopAlarmAndFinish()
    }

    private fun stopAlarmAndFinish() {
        isRinging = false
        speechLoopJob?.cancel()
        ringTimeoutJob?.cancel()

        try {
            ttsManager?.stop()
            ttsManager?.shutdown()
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Error stopping TTS", e)
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Error stopping player", e)
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Error stopping vibrator", e)
        }

        wakeLock?.let {
            if (it.isHeld) it.release()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarmAndFinish()
        super.onDestroy()
    }
}
