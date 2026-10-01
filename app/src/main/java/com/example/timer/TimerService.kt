package com.example.timer

import java.util.Locale

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.notification.NotificationHelper
import com.example.tts.BoloTTSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : Service() {

    companion object {
        const val ACTION_START_TIMER = "com.example.boloalarm.ACTION_START_TIMER"
        const val ACTION_PAUSE_TIMER = "com.example.boloalarm.ACTION_PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "com.example.boloalarm.ACTION_RESUME_TIMER"
        const val ACTION_RESET_TIMER = "com.example.boloalarm.ACTION_RESET_TIMER"
        const val ACTION_STOP_TIMER_ALERT = "com.example.boloalarm.ACTION_STOP_TIMER_ALERT"

        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_TIMER_TITLE = "extra_timer_title"

        private val _remainingSeconds = MutableStateFlow(0L)
        val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

        private val _totalDurationSeconds = MutableStateFlow(0L)
        val totalDurationSeconds: StateFlow<Long> = _totalDurationSeconds.asStateFlow()

        private val _timerTitle = MutableStateFlow("Timer")
        val timerTitle: StateFlow<String> = _timerTitle.asStateFlow()

        private val _timerState = MutableStateFlow(TimerState.IDLE)
        val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    }

    enum class TimerState {
        IDLE, RUNNING, PAUSED, FINISHED
    }

    private var timerJob: Job? = null
    private var endTimestamp: Long = 0
    private var pausedRemainingSeconds: Long = 0
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var ttsManager: BoloTTSManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
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
        val action = intent?.action ?: ACTION_START_TIMER
        when (action) {
            ACTION_START_TIMER -> {
                val seconds = intent?.getLongExtra(EXTRA_DURATION_SECONDS, 60L) ?: 60L
                val title = intent?.getStringExtra(EXTRA_TIMER_TITLE) ?: "Timer"
                startTimer(seconds, title)
            }
            ACTION_PAUSE_TIMER -> {
                pauseTimer()
            }
            ACTION_RESUME_TIMER -> {
                resumeTimer()
            }
            ACTION_RESET_TIMER -> {
                resetTimer()
            }
            ACTION_STOP_TIMER_ALERT -> {
                stopAlertAndFinish()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Long, title: String) {
        val safeSeconds = seconds.coerceIn(1L, 864000L)
        val safeTitle = (title.ifBlank { "Timer" }).take(100)
        _timerTitle.value = safeTitle
        _totalDurationSeconds.value = safeSeconds
        _remainingSeconds.value = safeSeconds
        pausedRemainingSeconds = safeSeconds
        endTimestamp = System.currentTimeMillis() + (safeSeconds * 1000L)
        _timerState.value = TimerState.RUNNING

        val notification = buildTimerNotification(safeSeconds)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_TIMER_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_TIMER_ID, notification)
        }
        runTimerLoop()
    }

    private fun pauseTimer() {
        if (_timerState.value != TimerState.RUNNING) return
        timerJob?.cancel()
        pausedRemainingSeconds = ((endTimestamp - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        _remainingSeconds.value = pausedRemainingSeconds
        _timerState.value = TimerState.PAUSED
        updateNotification(pausedRemainingSeconds)
    }

    private fun resumeTimer() {
        if (_timerState.value != TimerState.PAUSED) return
        endTimestamp = System.currentTimeMillis() + (pausedRemainingSeconds * 1000L)
        _timerState.value = TimerState.RUNNING
        runTimerLoop()
    }

    private fun resetTimer() {
        timerJob?.cancel()
        stopAlert()
        _timerState.value = TimerState.IDLE
        _remainingSeconds.value = 0L
        _totalDurationSeconds.value = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _timerState.value == TimerState.RUNNING) {
                val now = System.currentTimeMillis()
                val left = ((endTimestamp - now + 999) / 1000L).coerceAtLeast(0L)
                _remainingSeconds.value = left
                updateNotification(left)

                if (left <= 0) {
                    onTimerFinished()
                    break
                }
                delay(1000)
            }
        }
    }

    private fun onTimerFinished() {
        _timerState.value = TimerState.FINISHED
        playTimerSound()
        vibrate()

        val label = _timerTitle.value
        serviceScope.launch {
            delay(500)
            val speech = if (label.equals("Timer", ignoreCase = true)) {
                "Time is up! Your timer has completed."
            } else {
                "Time is up! Your $label timer has completed."
            }
            ttsManager?.speak(
                text = speech,
                language = "en",
                speechSpeed = 1.0f
            )
        }

        updateCompletionNotification()
    }

    private fun playTimerSound() {
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@TimerService, soundUri)
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
            e.printStackTrace()
        }
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 500, 300, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlert() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            vibrator?.cancel()
            ttsManager?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlertAndFinish() {
        stopAlert()
        resetTimer()
    }

    private fun formatTime(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    private fun buildTimerNotification(secondsLeft: Long): Notification {
        NotificationHelper.createNotificationChannels(this)
        val title = _timerTitle.value

        val openIntent = Intent(this, MainActivity::class.java).apply {
            `package` = packageName
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = Intent(this, TimerService::class.java).apply {
            `package` = packageName
            action = ACTION_RESET_TIMER
        }
        val pendingReset = PendingIntent.getService(
            this,
            202,
            resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_TIMER_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("BoloAlarm: $title")
            .setContentText("Remaining: ${formatTime(secondsLeft)}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", pendingReset)
            .build()
    }

    private fun updateNotification(secondsLeft: Long) {
        val notification = buildTimerNotification(secondsLeft)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        manager?.notify(NotificationHelper.NOTIFICATION_TIMER_ID, notification)
    }

    private fun updateCompletionNotification() {
        val title = _timerTitle.value
        val openIntent = Intent(this, MainActivity::class.java).apply {
            `package` = packageName
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopAlertIntent = Intent(this, TimerService::class.java).apply {
            `package` = packageName
            action = ACTION_STOP_TIMER_ALERT
        }
        val pendingStopAlert = PendingIntent.getService(
            this,
            203,
            stopAlertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_TIMER_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("BoloAlarm: $title finished!")
            .setContentText("Time's up! Tap to stop alert.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStopAlert)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        manager?.notify(NotificationHelper.NOTIFICATION_TIMER_ID, notification)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        stopAlert()
        ttsManager?.shutdown()
        super.onDestroy()
    }
}
