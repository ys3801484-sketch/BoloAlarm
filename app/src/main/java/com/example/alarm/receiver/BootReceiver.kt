package com.example.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.scheduler.AlarmScheduler
import com.example.data.AppDatabase
import com.example.data.model.AlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received action: $action - restoring alarms...")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(context, scope)
                    val enabledAlarms = db.alarmDao().getEnabledAlarms()
                    val scheduler = AlarmScheduler(context)

                    for (alarm in enabledAlarms) {
                        scheduler.scheduleAlarm(alarm)
                    }

                    // Also restore bedtime wake-up alarm if active
                    val bedtime = db.bedtimeDao().getBedtimeSchedule()
                    if (bedtime != null && bedtime.isEnabled) {
                        val wakeAlarm = AlarmEntity(
                            id = 999999,
                            hour = bedtime.wakeHour,
                            minute = bedtime.wakeMinute,
                            name = "Wake-Up Alarm",
                            personName = bedtime.personName,
                            category = "Wake Up",
                            language = bedtime.language,
                            repeatDays = bedtime.repeatDays,
                            isEnabled = true,
                            isVoiceEnabled = bedtime.isVoiceEnabled,
                            isSoundEnabled = bedtime.isSoundEnabled,
                            isVibrationEnabled = bedtime.isVibrationEnabled
                        )
                        scheduler.scheduleAlarm(wakeAlarm)
                    }
                } catch (e: Exception) {
                    // Handled safely without crashing
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
