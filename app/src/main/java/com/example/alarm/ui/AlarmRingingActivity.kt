package com.example.alarm.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.receiver.AlarmReceiver
import com.example.alarm.service.AlarmRingingService
import com.example.ui.components.SwipeToStopSlider
import com.example.ui.theme.MyApplicationTheme

/**
 * Compact lock-screen alarm interaction UI.
 *
 * Appears as a compact floating horizontal control over the lock screen using Android's
 * officially supported alarm/full-screen notification mechanisms (setShowWhenLocked / setTurnScreenOn).
 *
 * Strictly avoids full-screen takeovers, large clocks, large titles, or giant buttons,
 * focusing purely on the minimal horizontal swipe-to-stop control and a compact snooze pill.
 */
class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on and show over lock screen via official Android APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val alarmName = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_NAME) ?: "Alarm"
        val snoozeMinutes = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 10)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                CompactAlarmLockScreenUI(
                    alarmName = alarmName,
                    snoozeMinutes = snoozeMinutes,
                    onStopAlarm = {
                        stopAlarm()
                    },
                    onSnoozeAlarm = {
                        snoozeAlarm()
                    }
                )
            }
        }
    }

    private fun stopAlarm() {
        val stopIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_STOP_RINGING
        }
        startService(stopIntent)
        finishAndRemoveTask()
    }

    private fun snoozeAlarm() {
        val snoozeIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_SNOOZE_RINGING
        }
        startService(snoozeIntent)
        finishAndRemoveTask()
    }
}

@Composable
fun CompactAlarmLockScreenUI(
    alarmName: String,
    snoozeMinutes: Int,
    onStopAlarm: () -> Unit,
    onSnoozeAlarm: () -> Unit
) {
    // Semi-transparent scrim to keep lock-screen wallpaper visible while guaranteeing high legibility
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Subtle alarm indicator chip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alarmName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Separate compact Snooze action pill (Requirement 9)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.90f),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .testTag("snooze_button")
                    .clickable(onClick = onSnoozeAlarm)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = "Snooze",
                        tint = Color(0xFFFCD34D),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Snooze ($snoozeMinutes min)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Compact horizontal swipe-to-stop control matching visual reference:
            // ┌─────────────────────────────────────────────┐
            // │  [  >>  ]                  Stop Ringing      │
            // └─────────────────────────────────────────────┘
            SwipeToStopSlider(
                onSwipeComplete = onStopAlarm,
                label = "Stop Ringing",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
