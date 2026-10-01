package com.example.ui.screens

import java.util.Locale

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timer.TimerService

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimersScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val timerState by TimerService.timerState.collectAsState()
    val remainingSeconds by TimerService.remainingSeconds.collectAsState()
    val totalSeconds by TimerService.totalDurationSeconds.collectAsState()
    val runningTitle by TimerService.timerTitle.collectAsState()

    var timerLabel by remember { mutableStateOf("") }
    var hoursInput by remember { mutableStateOf("00") }
    var minutesInput by remember { mutableStateOf("05") }
    var secondsInput by remember { mutableStateOf("00") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val presets = listOf(
        "1 sec" to 1L,
        "7 sec" to 7L,
        "30 sec" to 30L,
        "1m 15s" to 75L,
        "5m 37s" to 337L,
        "10 min" to 600L,
        "17m 42s" to 1062L,
        "25m Pomodoro" to 1500L,
        "1h 30m 25s" to 5425L
    )

    fun startTimerWithSeconds(secs: Long, customLabel: String) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START_TIMER
            putExtra(TimerService.EXTRA_DURATION_SECONDS, secs)
            putExtra(TimerService.EXTRA_TIMER_TITLE, customLabel.ifBlank { "Timer" })
        }
        context.startService(intent)
    }

    fun pauseTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE_TIMER
        }
        context.startService(intent)
    }

    fun resumeTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME_TIMER
        }
        context.startService(intent)
    }

    fun resetTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESET_TIMER
        }
        context.startService(intent)
    }

    fun stopTimerAlert() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_TIMER_ALERT
        }
        context.startService(intent)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Column {
                    Text(
                        text = "Timers",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Second-level custom countdown with voice alerts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (timerState == TimerService.TimerState.IDLE) {
                // Setup / Input Mode
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Custom Timer Duration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Timer Label Input
                        OutlinedTextField(
                            value = timerLabel,
                            onValueChange = { timerLabel = it },
                            label = { Text("Timer Label (Optional)") },
                            placeholder = { Text("e.g. Tea Timer, Study Session, Cooking") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("timer_label_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Direct Numeric Typing for Hours, Minutes, Seconds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Hours
                            NumericUnitField(
                                label = "Hours",
                                value = hoursInput,
                                onValueChange = {
                                    if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                                        hoursInput = it
                                        validationError = null
                                        if (it.length == 2) focusManager.moveFocus(FocusDirection.Next)
                                    }
                                },
                                onIncrement = {
                                    val current = hoursInput.toIntOrNull() ?: 0
                                    hoursInput = String.format(Locale.getDefault(), "%02d", (current + 1).coerceAtMost(99))
                                },
                                onDecrement = {
                                    val current = hoursInput.toIntOrNull() ?: 0
                                    hoursInput = String.format(Locale.getDefault(), "%02d", (current - 1).coerceAtLeast(0))
                                },
                                tag = "timer_hours_input"
                            )

                            Text(
                                text = ":",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 24.dp)
                            )

                            // Minutes
                            NumericUnitField(
                                label = "Minutes",
                                value = minutesInput,
                                onValueChange = {
                                    if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                                        minutesInput = it
                                        validationError = null
                                        if (it.length == 2) focusManager.moveFocus(FocusDirection.Next)
                                    }
                                },
                                onIncrement = {
                                    val current = minutesInput.toIntOrNull() ?: 0
                                    minutesInput = String.format(Locale.getDefault(), "%02d", if (current >= 59) 0 else current + 1)
                                },
                                onDecrement = {
                                    val current = minutesInput.toIntOrNull() ?: 0
                                    minutesInput = String.format(Locale.getDefault(), "%02d", if (current <= 0) 59 else current - 1)
                                },
                                tag = "timer_minutes_input"
                            )

                            Text(
                                text = ":",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 24.dp)
                            )

                            // Seconds
                            NumericUnitField(
                                label = "Seconds",
                                value = secondsInput,
                                onValueChange = {
                                    if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                                        secondsInput = it
                                        validationError = null
                                    }
                                },
                                onIncrement = {
                                    val current = secondsInput.toIntOrNull() ?: 0
                                    secondsInput = String.format(Locale.getDefault(), "%02d", if (current >= 59) 0 else current + 1)
                                },
                                onDecrement = {
                                    val current = secondsInput.toIntOrNull() ?: 0
                                    secondsInput = String.format(Locale.getDefault(), "%02d", if (current <= 0) 59 else current - 1)
                                },
                                tag = "timer_seconds_input"
                            )
                        }

                        if (validationError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick Presets
                        Text(
                            text = "Quick Presets",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { (name, secs) ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val h = secs / 3600
                                        val m = (secs % 3600) / 60
                                        val s = secs % 60
                                        hoursInput = String.format(Locale.getDefault(), "%02d", h)
                                        minutesInput = String.format(Locale.getDefault(), "%02d", m)
                                        secondsInput = String.format(Locale.getDefault(), "%02d", s)
                                        timerLabel = name
                                    },
                                    label = { Text(name) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Start Button
                        Button(
                            onClick = {
                                val h = hoursInput.toLongOrNull() ?: 0L
                                val m = minutesInput.toLongOrNull() ?: 0L
                                val s = secondsInput.toLongOrNull() ?: 0L

                                if (m > 59 || s > 59) {
                                    validationError = "Minutes and seconds must be between 00 and 59."
                                    return@Button
                                }

                                val totalSecs = h * 3600L + m * 60L + s
                                if (totalSecs <= 0L) {
                                    validationError = "Duration must be at least 1 second."
                                    return@Button
                                }

                                validationError = null
                                startTimerWithSeconds(totalSecs, timerLabel)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_timer_button"),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("START", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Active Countdown Mode (RUNNING / PAUSED / FINISHED)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (timerState == TimerService.TimerState.FINISHED)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Display Custom Timer Title
                        Text(
                            text = runningTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val progress = if (totalSeconds > 0) {
                            (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        val h = remainingSeconds / 3600
                        val m = (remainingSeconds % 3600) / 60
                        val s = remainingSeconds % 60
                        val formatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)

                        Box(
                            modifier = Modifier.size(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 10.dp,
                                color = if (timerState == TimerService.TimerState.FINISHED)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatted,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (timerState) {
                                        TimerService.TimerState.RUNNING -> "RUNNING"
                                        TimerService.TimerState.PAUSED -> "PAUSED"
                                        TimerService.TimerState.FINISHED -> "TIME'S UP!"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Controls: START / PAUSE / RESUME / RESET
                        if (timerState == TimerService.TimerState.FINISHED) {
                            Button(
                                onClick = { stopTimerAlert() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("stop_timer_alert_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("STOP ALERT", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // RESET Button
                                OutlinedButton(
                                    onClick = { resetTimer() },
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag("reset_timer_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "RESET")
                                }

                                // PAUSE / RESUME Button
                                Button(
                                    onClick = {
                                        if (timerState == TimerService.TimerState.RUNNING) {
                                            pauseTimer()
                                        } else {
                                            resumeTimer()
                                        }
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .testTag("play_pause_timer_button")
                                ) {
                                    Icon(
                                        imageVector = if (timerState == TimerService.TimerState.RUNNING)
                                            Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (timerState == TimerService.TimerState.RUNNING)
                                            "PAUSE" else "RESUME",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
private fun NumericUnitField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    tag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.HourglassTop, contentDescription = "Increment", modifier = Modifier.size(18.dp))
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .width(72.dp)
                .testTag(tag),
            shape = RoundedCornerShape(14.dp)
        )

        IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.HourglassBottom, contentDescription = "Decrement", modifier = Modifier.size(18.dp))
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
