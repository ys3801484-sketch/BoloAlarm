package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.receiver.AlarmReceiver
import com.example.alarm.scheduler.AlarmScheduler
import com.example.data.AppDatabase
import com.example.data.model.AlarmEntity
import com.example.data.model.AppPreferences
import com.example.data.model.BedtimeScheduleEntity
import com.example.data.model.WorldClockCityEntity
import com.example.data.repository.BoloAlarmRepository
import com.example.tts.BoloTTSManager
import com.example.tts.PersonalizedMessageEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BoloViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = BoloAlarmRepository(
        alarmDao = database.alarmDao(),
        worldClockDao = database.worldClockDao(),
        bedtimeDao = database.bedtimeDao(),
        context = application
    )

    val alarmScheduler = AlarmScheduler(application)
    val ttsManager = BoloTTSManager(application)

    val allAlarms: StateFlow<List<AlarmEntity>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldClockCities: StateFlow<List<WorldClockCityEntity>> = repository.allCities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bedtimeSchedule: StateFlow<BedtimeScheduleEntity?> = repository.bedtimeSchedule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val preferences: StateFlow<AppPreferences> = repository.preferencesFlow

    // Stopwatch State
    private val _stopwatchElapsedMillis = MutableStateFlow(0L)
    val stopwatchElapsedMillis: StateFlow<Long> = _stopwatchElapsedMillis.asStateFlow()

    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning: StateFlow<Boolean> = _isStopwatchRunning.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow<List<LapTime>>(emptyList())
    val stopwatchLaps: StateFlow<List<LapTime>> = _stopwatchLaps.asStateFlow()

    private var stopwatchJob: Job? = null
    private var stopwatchStartTime: Long = 0L
    private var stopwatchAccumulatedTime: Long = 0L

    data class LapTime(
        val lapNumber: Int,
        val lapSplitMillis: Long,
        val totalElapsedMillis: Long
    )

    // Alarm Operations
    fun createAlarm(
        hour: Int,
        minute: Int,
        name: String,
        repeatDays: Int = 0,
        personName: String = "User",
        category: String = "Custom",
        customMessage: String = "",
        language: String = "hi",
        isVoiceEnabled: Boolean = true,
        isSoundEnabled: Boolean = true,
        isVibrationEnabled: Boolean = true,
        snoozeDurationMinutes: Int = 10,
        ringDurationSeconds: Int = 300
    ) {
        val newAlarm = AlarmEntity(
            hour = hour,
            minute = minute,
            name = name,
            repeatDays = repeatDays,
            personName = personName,
            category = category,
            customMessage = customMessage,
            language = language,
            isVoiceEnabled = isVoiceEnabled,
            isSoundEnabled = isSoundEnabled,
            isVibrationEnabled = isVibrationEnabled,
            snoozeDurationMinutes = snoozeDurationMinutes,
            ringDurationSeconds = ringDurationSeconds,
            isEnabled = true
        )
        saveAlarm(newAlarm)
    }

    suspend fun getAlarmById(id: Long): AlarmEntity? {
        return repository.getAlarmById(id)
    }

    fun saveAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            if (alarm.id == 0L) {
                val newId = repository.insertAlarm(alarm)
                val insertedAlarm = alarm.copy(id = newId)
                if (insertedAlarm.isEnabled) {
                    alarmScheduler.scheduleAlarm(insertedAlarm)
                }
            } else {
                alarmScheduler.cancelAlarm(alarm.id)
                repository.updateAlarm(alarm)
                if (alarm.isEnabled) {
                    alarmScheduler.scheduleAlarm(alarm)
                }
            }
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updated)
            if (updated.isEnabled) {
                alarmScheduler.scheduleAlarm(updated)
            } else {
                alarmScheduler.cancelAlarm(updated.id)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(alarm.id)
            repository.deleteAlarm(alarm)
        }
    }

    fun duplicateAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val duplicated = alarm.copy(
                id = 0,
                name = "${alarm.name} (Copy)",
                createdAt = System.currentTimeMillis()
            )
            val newId = repository.insertAlarm(duplicated)
            if (duplicated.isEnabled) {
                alarmScheduler.scheduleAlarm(duplicated.copy(id = newId))
            }
        }
    }

    fun testAlarmVoice(
        personName: String,
        category: String,
        customMessage: String,
        language: String,
        speechSpeed: Float,
        speechPitch: Float = 1.0f,
        voiceName: String = ""
    ) {
        val message = PersonalizedMessageEngine.generateMessage(
            personName = personName,
            category = category,
            customMessage = customMessage,
            language = language
        )
        ttsManager.speak(
            text = message,
            language = language,
            speechSpeed = speechSpeed,
            speechPitch = speechPitch,
            voiceName = voiceName
        )
    }

    fun quickTestAlarmRinging(alarm: AlarmEntity) {
        // Immediately fires the alarm service & activity for immediate testing
        val context = getApplication<Application>()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
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
        context.sendBroadcast(intent)
    }

    // World Clock Operations
    fun addCity(cityName: String, countryName: String, timeZoneId: String) {
        viewModelScope.launch {
            val city = WorldClockCityEntity(
                cityName = cityName,
                countryName = countryName,
                timeZoneId = timeZoneId,
                orderIndex = (worldClockCities.value.size)
            )
            repository.insertCity(city)
        }
    }

    fun deleteCity(city: WorldClockCityEntity) {
        viewModelScope.launch {
            repository.deleteCity(city)
        }
    }

    // Bedtime Operations
    fun saveBedtimeSchedule(schedule: BedtimeScheduleEntity) {
        viewModelScope.launch {
            repository.saveBedtimeSchedule(schedule)
            if (schedule.isEnabled) {
                val wakeAlarm = AlarmEntity(
                    id = 999999,
                    hour = schedule.wakeHour,
                    minute = schedule.wakeMinute,
                    name = "Wake-Up Alarm",
                    personName = schedule.personName,
                    category = "Wake Up",
                    language = schedule.language,
                    repeatDays = schedule.repeatDays,
                    isEnabled = true,
                    isVoiceEnabled = schedule.isVoiceEnabled,
                    isSoundEnabled = schedule.isSoundEnabled,
                    isVibrationEnabled = schedule.isVibrationEnabled
                )
                alarmScheduler.scheduleAlarm(wakeAlarm)
            } else {
                alarmScheduler.cancelAlarm(999999)
            }
        }
    }

    // Preferences Operations
    fun updatePreferences(update: (AppPreferences) -> AppPreferences) {
        repository.updatePreferences(update)
    }

    // Stopwatch Controls
    fun startStopwatch() {
        if (_isStopwatchRunning.value) return
        _isStopwatchRunning.value = true
        stopwatchStartTime = System.currentTimeMillis() - stopwatchAccumulatedTime
        stopwatchJob = viewModelScope.launch {
            while (isActive && _isStopwatchRunning.value) {
                _stopwatchElapsedMillis.value = System.currentTimeMillis() - stopwatchStartTime
                delay(16) // ~60fps
            }
        }
    }

    fun pauseStopwatch() {
        if (!_isStopwatchRunning.value) return
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        stopwatchAccumulatedTime = _stopwatchElapsedMillis.value
    }

    fun resetStopwatch() {
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        _stopwatchElapsedMillis.value = 0L
        stopwatchAccumulatedTime = 0L
        _stopwatchLaps.value = emptyList()
    }

    fun lapStopwatch() {
        if (!_isStopwatchRunning.value) return
        val currentTotal = _stopwatchElapsedMillis.value
        val previousTotal = _stopwatchLaps.value.firstOrNull()?.totalElapsedMillis ?: 0L
        val split = currentTotal - previousTotal
        val newLap = LapTime(
            lapNumber = _stopwatchLaps.value.size + 1,
            lapSplitMillis = split,
            totalElapsedMillis = currentTotal
        )
        _stopwatchLaps.value = listOf(newLap) + _stopwatchLaps.value
    }

    // Vibration Test
    fun testVibration(patternType: String = "normal") {
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val pattern = when (patternType.lowercase()) {
            "short" -> longArrayOf(0, 300, 300, 300)
            "long" -> longArrayOf(0, 1500, 500, 1500)
            "heartbeat" -> longArrayOf(0, 200, 150, 350, 700)
            else -> longArrayOf(0, 500, 300, 500)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }

    override fun onCleared() {
        stopwatchJob?.cancel()
        ttsManager.shutdown()
        super.onCleared()
    }
}
