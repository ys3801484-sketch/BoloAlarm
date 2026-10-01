package com.example

import com.example.alarm.scheduler.AlarmScheduler
import com.example.data.model.AlarmEntity
import com.example.tts.PersonalizedMessageEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun personalizedMessageEngine_hindiBreakfastFormat() {
        val message = PersonalizedMessageEngine.generateMessage(
            personName = "Yaseen",
            category = "Breakfast",
            customMessage = "",
            language = "hi"
        )
        assertEquals("Yaseen, aapka breakfast time ho gaya hai. Kripya breakfast kar lijiye.", message)
    }

    @Test
    fun personalizedMessageEngine_englishBreakfastFormat() {
        val message = PersonalizedMessageEngine.generateMessage(
            personName = "Yaseen",
            category = "Breakfast",
            customMessage = "",
            language = "en"
        )
        assertEquals("Yaseen, it is breakfast time. Please have your breakfast.", message)
    }

    @Test
    fun personalizedMessageEngine_customMessageTakesPrecedence() {
        val custom = "Yaseen, 7:43 baj gaye hain. Breakfast karne ka time ho gaya hai."
        val message = PersonalizedMessageEngine.generateMessage(
            personName = "Yaseen",
            category = "Breakfast",
            customMessage = custom,
            language = "hi"
        )
        assertEquals(custom, message)
    }

    @Test
    fun alarmEntity_scheduleSummary() {
        val onceAlarm = AlarmEntity(hour = 8, minute = 30, name = "Test", personName = "User", repeatDays = 0)
        assertEquals("Once", onceAlarm.getScheduleSummary())

        val everydayAlarm = AlarmEntity(hour = 8, minute = 30, name = "Test", personName = "User", repeatDays = 127)
        assertEquals("Every day", everydayAlarm.getScheduleSummary())

        val weekdaysAlarm = AlarmEntity(hour = 8, minute = 30, name = "Test", personName = "User", repeatDays = 31)
        assertEquals("Weekdays (Mon-Fri)", weekdaysAlarm.getScheduleSummary())

        val weekendsAlarm = AlarmEntity(hour = 8, minute = 30, name = "Test", personName = "User", repeatDays = 96)
        assertEquals("Weekends (Sat-Sun)", weekendsAlarm.getScheduleSummary())
    }

    @Test
    fun alarmScheduler_calculatesFutureTriggerTime() {
        val now = System.currentTimeMillis()
        val nextTrigger = AlarmScheduler.calculateNextTriggerTime(
            hour = 8,
            minute = 37,
            second = 0,
            repeatDays = 0
        )
        assertTrue(nextTrigger >= now)
    }

    @Test
    fun alarmScheduler_specificDateScheduling() {
        val specificCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 3)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 25)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val trigger = AlarmScheduler.calculateNextTriggerTime(
            hour = 14,
            minute = 25,
            second = 0,
            specificDateMillis = specificCal.timeInMillis
        )
        assertEquals(specificCal.timeInMillis, trigger)
    }
}
