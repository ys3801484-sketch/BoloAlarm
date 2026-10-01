package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AlarmDao
import com.example.data.dao.BedtimeDao
import com.example.data.dao.WorldClockDao
import com.example.data.model.AlarmEntity
import com.example.data.model.BedtimeScheduleEntity
import com.example.data.model.WorldClockCityEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AlarmEntity::class,
        WorldClockCityEntity::class,
        BedtimeScheduleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun worldClockDao(): WorldClockDao
    abstract fun bedtimeDao(): BedtimeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bolo_alarm_database"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val alarmDao = database.alarmDao()
                // Initial starter alarms matching user examples:
                alarmDao.insertAlarm(
                    AlarmEntity(
                        hour = 8,
                        minute = 37,
                        name = "Breakfast",
                        personName = "Yaseen",
                        category = "Breakfast",
                        customMessage = "",
                        language = "hi",
                        repeatDays = 127, // Every day
                        isEnabled = true,
                        isVoiceEnabled = true,
                        isSoundEnabled = true,
                        isVibrationEnabled = true,
                        snoozeDurationMinutes = 10,
                        ringDurationSeconds = 300
                    )
                )
                alarmDao.insertAlarm(
                    AlarmEntity(
                        hour = 12,
                        minute = 30,
                        name = "Lunch",
                        personName = "Yaseen",
                        category = "Lunch",
                        customMessage = "",
                        language = "hi",
                        repeatDays = 31, // Mon Tue Wed Thu Fri
                        isEnabled = true,
                        isVoiceEnabled = true,
                        isSoundEnabled = true,
                        isVibrationEnabled = true,
                        snoozeDurationMinutes = 10,
                        ringDurationSeconds = 300
                    )
                )
                alarmDao.insertAlarm(
                    AlarmEntity(
                        hour = 19,
                        minute = 0,
                        name = "Study Time",
                        personName = "Yaseen",
                        category = "Study",
                        customMessage = "Yaseen, padhai ka time ho gaya hai. Ab thoda focus karke padhai shuru karo.",
                        language = "hi",
                        repeatDays = 31, // Mon Tue Wed Thu Fri
                        isEnabled = false,
                        isVoiceEnabled = true,
                        isSoundEnabled = true,
                        isVibrationEnabled = true,
                        snoozeDurationMinutes = 10,
                        ringDurationSeconds = 300
                    )
                )

                // Populate popular World Clock cities
                val worldClockDao = database.worldClockDao()
                val cities = listOf(
                    WorldClockCityEntity(cityName = "New Delhi", countryName = "India", timeZoneId = "Asia/Kolkata", orderIndex = 0),
                    WorldClockCityEntity(cityName = "Dubai", countryName = "United Arab Emirates", timeZoneId = "Asia/Dubai", orderIndex = 1),
                    WorldClockCityEntity(cityName = "London", countryName = "United Kingdom", timeZoneId = "Europe/London", orderIndex = 2),
                    WorldClockCityEntity(cityName = "New York", countryName = "United States", timeZoneId = "America/New_York", orderIndex = 3),
                    WorldClockCityEntity(cityName = "Tokyo", countryName = "Japan", timeZoneId = "Asia/Tokyo", orderIndex = 4),
                    WorldClockCityEntity(cityName = "Singapore", countryName = "Singapore", timeZoneId = "Asia/Singapore", orderIndex = 5),
                    WorldClockCityEntity(cityName = "Sydney", countryName = "Australia", timeZoneId = "Australia/Sydney", orderIndex = 6)
                )
                worldClockDao.insertAll(cities)

                // Populate default bedtime schedule
                val bedtimeDao = database.bedtimeDao()
                bedtimeDao.insertOrUpdate(
                    BedtimeScheduleEntity(
                        id = 1,
                        bedHour = 22,
                        bedMinute = 30,
                        wakeHour = 6,
                        wakeMinute = 30,
                        repeatDays = 127,
                        isEnabled = true,
                        personName = "Yaseen",
                        isVoiceEnabled = true,
                        isSoundEnabled = true,
                        isVibrationEnabled = true,
                        language = "hi"
                    )
                )
            }
        }
    }
}
