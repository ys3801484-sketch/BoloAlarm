package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.dao.AlarmDao
import com.example.data.dao.BedtimeDao
import com.example.data.dao.WorldClockDao
import com.example.data.model.AlarmEntity
import com.example.data.model.AppPreferences
import com.example.data.model.BedtimeScheduleEntity
import com.example.data.model.WorldClockCityEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BoloAlarmRepository(
    private val alarmDao: AlarmDao,
    private val worldClockDao: WorldClockDao,
    private val bedtimeDao: BedtimeDao,
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bolo_alarm_prefs", Context.MODE_PRIVATE)

    private val _preferencesFlow = MutableStateFlow(loadPreferences())
    val preferencesFlow: StateFlow<AppPreferences> = _preferencesFlow.asStateFlow()

    // Alarm Operations
    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    suspend fun getEnabledAlarms(): List<AlarmEntity> = alarmDao.getEnabledAlarms()

    suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    suspend fun insertAlarm(alarm: AlarmEntity): Long = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: AlarmEntity) = alarmDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: AlarmEntity) = alarmDao.deleteAlarm(alarm)

    suspend fun deleteAlarmById(id: Long) = alarmDao.deleteAlarmById(id)

    suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean) =
        alarmDao.setAlarmEnabled(id, isEnabled)

    // World Clock Operations
    val allCities: Flow<List<WorldClockCityEntity>> = worldClockDao.getAllCities()

    suspend fun insertCity(city: WorldClockCityEntity): Long = worldClockDao.insertCity(city)

    suspend fun deleteCity(city: WorldClockCityEntity) = worldClockDao.deleteCity(city)

    suspend fun deleteCityById(id: Long) = worldClockDao.deleteCityById(id)

    // Bedtime Operations
    val bedtimeSchedule: Flow<BedtimeScheduleEntity?> = bedtimeDao.getBedtimeScheduleFlow()

    suspend fun getBedtimeSchedule(): BedtimeScheduleEntity? = bedtimeDao.getBedtimeSchedule()

    suspend fun saveBedtimeSchedule(schedule: BedtimeScheduleEntity) =
        bedtimeDao.insertOrUpdate(schedule)

    // Preferences Operations
    private fun loadPreferences(): AppPreferences {
        return AppPreferences(
            use24HourFormat = prefs.getBoolean("use24HourFormat", false),
            themeMode = prefs.getString("themeMode", "system") ?: "system",
            defaultPersonName = prefs.getString("defaultPersonName", "Yaseen") ?: "Yaseen",
            defaultLanguage = prefs.getString("defaultLanguage", "hi") ?: "hi",
            defaultSpeechSpeed = prefs.getFloat("defaultSpeechSpeed", 1.0f),
            defaultSpeechPitch = prefs.getFloat("defaultSpeechPitch", 1.0f),
            globalSoundVolume = prefs.getInt("globalSoundVolume", 90),
            globalVibrationEnabled = prefs.getBoolean("globalVibrationEnabled", true),
            globalVoiceEnabled = prefs.getBoolean("globalVoiceEnabled", true),
            defaultVibrationPattern = prefs.getString("defaultVibrationPattern", "normal") ?: "normal",
            defaultSnoozeMinutes = prefs.getInt("defaultSnoozeMinutes", 10),
            defaultRingDurationSeconds = prefs.getInt("defaultRingDurationSeconds", 300),
            defaultRepeatMode = prefs.getString("defaultRepeatMode", "every_day") ?: "every_day",
            alarmType = prefs.getString("alarmType", "sound_and_vibration") ?: "sound_and_vibration"
        )
    }

    fun updatePreferences(update: (AppPreferences) -> AppPreferences) {
        val newPrefs = update(_preferencesFlow.value)
        prefs.edit().apply {
            putBoolean("use24HourFormat", newPrefs.use24HourFormat)
            putString("themeMode", newPrefs.themeMode)
            putString("defaultPersonName", newPrefs.defaultPersonName)
            putString("defaultLanguage", newPrefs.defaultLanguage)
            putFloat("defaultSpeechSpeed", newPrefs.defaultSpeechSpeed)
            putFloat("defaultSpeechPitch", newPrefs.defaultSpeechPitch)
            putInt("globalSoundVolume", newPrefs.globalSoundVolume)
            putBoolean("globalVibrationEnabled", newPrefs.globalVibrationEnabled)
            putBoolean("globalVoiceEnabled", newPrefs.globalVoiceEnabled)
            putString("defaultVibrationPattern", newPrefs.defaultVibrationPattern)
            putInt("defaultSnoozeMinutes", newPrefs.defaultSnoozeMinutes)
            putInt("defaultRingDurationSeconds", newPrefs.defaultRingDurationSeconds)
            putString("defaultRepeatMode", newPrefs.defaultRepeatMode)
            putString("alarmType", newPrefs.alarmType)
            apply()
        }
        _preferencesFlow.value = newPrefs
    }
}
