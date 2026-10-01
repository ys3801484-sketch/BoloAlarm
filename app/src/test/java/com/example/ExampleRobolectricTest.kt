package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.receiver.AlarmReceiver
import com.example.alarm.ui.AlarmRingingActivity
import com.example.tts.PersonalizedMessageEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string resources from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val tagline = context.getString(R.string.app_tagline)
        assertEquals("BoloAlarm", appName)
        assertEquals("Alarm jo sirf bajta nahi, bolkar yaad dilata hai.", tagline)
    }

    @Test
    fun `personalized message engine generates hindi message`() {
        val message = PersonalizedMessageEngine.generateMessage(
            personName = "Yaseen",
            category = "Breakfast",
            customMessage = "",
            language = "hi"
        )
        assertTrue(message.contains("Yaseen"))
        assertTrue(message.contains("breakfast"))
    }

    @Test
    fun `personalized message engine generates english message`() {
        val message = PersonalizedMessageEngine.generateMessage(
            personName = "Yaseen",
            category = "Breakfast",
            customMessage = "",
            language = "en"
        )
        assertTrue(message.contains("Yaseen"))
        assertTrue(message.contains("breakfast"))
    }

    @Test
    fun `alarm ringing activity launches with intent extras`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_NAME, "Breakfast")
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 10)
        }
        val scenario = ActivityScenario.launch<AlarmRingingActivity>(intent)
        assertNotNull(scenario)
        scenario.close()
    }

    @Test
    fun `main activity launches successfully`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MainActivity::class.java)
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        assertNotNull(scenario)
        scenario.close()
    }

    @Test
    fun `alarm scheduler calculates future trigger time correctly`() {
        val now = System.currentTimeMillis()
        val nextTrigger = com.example.alarm.scheduler.AlarmScheduler.calculateNextTriggerTime(
            hour = 8,
            minute = 30,
            repeatDays = 0,
            specificDateMillis = null
        )
        assertTrue(nextTrigger >= now)
    }

    @Test
    fun `alarm entity correctly evaluates repeating days`() {
        val alarm = com.example.data.model.AlarmEntity(
            hour = 8,
            minute = 0,
            name = "Test",
            personName = "Test",
            repeatDays = 127 // Every day
        )
        assertTrue(alarm.isRepeating())
        assertEquals("Every day", alarm.getScheduleSummary())
        assertTrue(alarm.isDaySelected(0)) // Mon
        assertTrue(alarm.isDaySelected(6)) // Sun
    }

    @Test
    fun `alarm receiver safely ignores untrusted intent actions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = AlarmReceiver()
        val maliciousIntent = Intent("untrusted.action.ATTACK")
        // Should not throw or perform any action
        receiver.onReceive(context, maliciousIntent)
    }

    @Test
    fun `alarm receiver ignores invalid alarm ids`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = AlarmReceiver()
        val intentWithInvalidId = Intent(AlarmReceiver.ACTION_TRIGGER_ALARM).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, -5L)
        }
        // Should safely return without scheduling any service
        receiver.onReceive(context, intentWithInvalidId)
    }

    @Test
    fun `alarm entity and dao manage alarm time labels and days of week`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
        val dao = db.alarmDao()

        val testAlarm = com.example.data.model.AlarmEntity(
            hour = 6,
            minute = 45,
            name = "Morning Yoga",
            personName = "Yaseen",
            category = "Fitness",
            repeatDays = 31, // Weekdays
            isEnabled = true
        )

        val insertedId = dao.insertAlarm(testAlarm)
        assertTrue(insertedId > 0)

        val fetched = dao.getAlarmById(insertedId)
        assertNotNull(fetched)
        assertEquals(6, fetched?.hour)
        assertEquals(45, fetched?.minute)
        assertEquals("Morning Yoga", fetched?.name)
        assertEquals(31, fetched?.repeatDays)
        assertTrue(fetched?.isDaySelected(0) == true) // Monday
        assertTrue(fetched?.isDaySelected(4) == true) // Friday
        assertTrue(fetched?.isDaySelected(5) == false) // Saturday

        // Clean up
        dao.deleteAlarmById(insertedId)
    }
}
