package com.example.data.model

data class AppPreferences(
    val use24HourFormat: Boolean = false,
    val themeMode: String = "system", // "system", "light", "dark"
    val defaultPersonName: String = "Yaseen",
    val defaultLanguage: String = "hi", // "hi", "en", "default"
    val defaultSpeechSpeed: Float = 1.0f,
    val defaultSpeechPitch: Float = 1.0f,
    val globalSoundVolume: Int = 90,
    val globalVibrationEnabled: Boolean = true,
    val globalVoiceEnabled: Boolean = true,
    val defaultVibrationPattern: String = "normal",
    val defaultSnoozeMinutes: Int = 10,
    val defaultRingDurationSeconds: Int = 300,
    val defaultRepeatMode: String = "every_day", // "once", "every_day", "weekdays", "weekends"
    val alarmType: String = "sound_and_vibration"
)
