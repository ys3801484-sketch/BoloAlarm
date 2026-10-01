package com.example.ui.screens

import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BedtimeScheduleEntity
import com.example.tts.PersonalizedMessageEngine
import com.example.ui.components.BoloTimePickerDialog

@Composable
fun BedtimeScreen(
    schedule: BedtimeScheduleEntity?,
    is24Hour: Boolean,
    onSaveSchedule: (BedtimeScheduleEntity) -> Unit,
    onTestBedtimeVoice: (person: String, isSleepOrWake: Boolean) -> Unit
) {
    var bedHour by remember(schedule) { mutableIntStateOf(schedule?.bedHour ?: 22) }
    var bedMinute by remember(schedule) { mutableIntStateOf(schedule?.bedMinute ?: 30) }
    var wakeHour by remember(schedule) { mutableIntStateOf(schedule?.wakeHour ?: 6) }
    var wakeMinute by remember(schedule) { mutableIntStateOf(schedule?.wakeMinute ?: 30) }
    var isEnabled by remember(schedule) { mutableStateOf(schedule?.isEnabled ?: true) }
    var isVoiceEnabled by remember(schedule) { mutableStateOf(schedule?.isVoiceEnabled ?: true) }
    var reminderBeforeMins by remember(schedule) { mutableIntStateOf(schedule?.reminderBeforeMinutes ?: 15) }
    var personName by remember(schedule) { mutableStateOf(schedule?.personName ?: "Yaseen") }
    var language by remember(schedule) { mutableStateOf(schedule?.language ?: "hi") }

    var editingBedTime by remember { mutableStateOf(false) }
    var editingWakeTime by remember { mutableStateOf(false) }

    fun formatTime(h: Int, m: Int): Pair<String, String> {
        return if (is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", h, m) to ""
        } else {
            val displayHour = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            String.format(Locale.getDefault(), "%d:%02d", displayHour, m) to if (h >= 12) "PM" else "AM"
        }
    }

    val (bedTimeStr, bedAmPm) = formatTime(bedHour, bedMinute)
    val (wakeTimeStr, wakeAmPm) = formatTime(wakeHour, wakeMinute)

    // Calculate sleep duration
    var bedMinutesTotal = bedHour * 60 + bedMinute
    var wakeMinutesTotal = wakeHour * 60 + wakeMinute
    if (wakeMinutesTotal <= bedMinutesTotal) {
        wakeMinutesTotal += 24 * 60
    }
    val diffMins = wakeMinutesTotal - bedMinutesTotal
    val sleepHours = diffMins / 60
    val sleepMinsRemaining = diffMins % 60

    if (editingBedTime) {
        BoloTimePickerDialog(
            initialHour = bedHour,
            initialMinute = bedMinute,
            is24Hour = is24Hour,
            onTimeSelected = { h, m ->
                bedHour = h
                bedMinute = m
                editingBedTime = false
                onSaveSchedule(
                    (schedule ?: BedtimeScheduleEntity()).copy(
                        bedHour = h,
                        bedMinute = m,
                        wakeHour = wakeHour,
                        wakeMinute = wakeMinute,
                        isEnabled = isEnabled,
                        isVoiceEnabled = isVoiceEnabled,
                        personName = personName,
                        language = language,
                        reminderBeforeMinutes = reminderBeforeMins
                    )
                )
            },
            onDismiss = { editingBedTime = false }
        )
    }

    if (editingWakeTime) {
        BoloTimePickerDialog(
            initialHour = wakeHour,
            initialMinute = wakeMinute,
            is24Hour = is24Hour,
            onTimeSelected = { h, m ->
                wakeHour = h
                wakeMinute = m
                editingWakeTime = false
                onSaveSchedule(
                    (schedule ?: BedtimeScheduleEntity()).copy(
                        bedHour = bedHour,
                        bedMinute = bedMinute,
                        wakeHour = h,
                        wakeMinute = m,
                        isEnabled = isEnabled,
                        isVoiceEnabled = isVoiceEnabled,
                        personName = personName,
                        language = language,
                        reminderBeforeMinutes = reminderBeforeMins
                    )
                )
            },
            onDismiss = { editingWakeTime = false }
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bedtime & Sleep",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Consistent routine with wind-down reminders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = {
                        isEnabled = it
                        onSaveSchedule(
                            (schedule ?: BedtimeScheduleEntity()).copy(
                                isEnabled = it,
                                bedHour = bedHour,
                                bedMinute = bedMinute,
                                wakeHour = wakeHour,
                                wakeMinute = wakeMinute
                            )
                        )
                    },
                    modifier = Modifier.testTag("bedtime_enabled_switch")
                )
            }

            // Sleep Duration Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NightlightRound,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Planned Sleep Time",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${sleepHours}h ${sleepMinsRemaining}m",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (sleepHours in 7..9) "Ideal recommended sleep duration" else "Adjust times for 7-9 hours of rest",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Bedtime and Wake-up Times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Bedtime Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { editingBedTime = true }
                        .testTag("bedtime_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bedtime", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = bedTimeStr,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (bedAmPm.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = bedAmPm,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tap to edit", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Wake-up Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { editingWakeTime = true }
                        .testTag("wakeup_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wake Up", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = wakeTimeStr,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (wakeAmPm.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = wakeAmPm,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tap to edit", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Wind-down Reminder
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Wind-down Reminder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Get notified prior to bedtime to disconnect and get ready for sleep.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Off", 15 to "15m before", 30 to "30m before", 45 to "45m before").forEach { (m, label) ->
                            FilterChip(
                                selected = reminderBeforeMins == m,
                                onClick = {
                                    reminderBeforeMins = m
                                    onSaveSchedule(
                                        (schedule ?: BedtimeScheduleEntity()).copy(
                                            reminderBeforeMinutes = m,
                                            bedHour = bedHour,
                                            bedMinute = bedMinute,
                                            wakeHour = wakeHour,
                                            wakeMinute = wakeMinute
                                        )
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Personalized Voice Announcements for Sleep & Wake
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Voice Announcements", fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = isVoiceEnabled,
                            onCheckedChange = {
                                isVoiceEnabled = it
                                onSaveSchedule(
                                    (schedule ?: BedtimeScheduleEntity()).copy(
                                        isVoiceEnabled = it,
                                        bedHour = bedHour,
                                        bedMinute = bedMinute,
                                        wakeHour = wakeHour,
                                        wakeMinute = wakeMinute
                                    )
                                )
                            }
                        )
                    }

                    // Test Voice Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestBedtimeVoice(personName, true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Bedtime", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onTestBedtimeVoice(personName, false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Wake-up", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}
