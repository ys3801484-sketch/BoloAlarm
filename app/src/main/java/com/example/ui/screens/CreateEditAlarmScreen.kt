package com.example.ui.screens

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmEntity
import com.example.tts.BoloTTSManager
import com.example.tts.PersonalizedMessageEngine
import com.example.ui.components.BoloDatePickerDialog
import com.example.ui.components.BoloTimePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditAlarmScreen(
    initialAlarm: AlarmEntity?,
    is24Hour: Boolean,
    defaultPersonName: String,
    availableVoices: List<BoloTTSManager.VoiceInfo>,
    onSaveAlarm: (AlarmEntity) -> Unit,
    onTestVoice: (person: String, category: String, customMessage: String, lang: String, speed: Float, pitch: Float, voice: String) -> Unit,
    onQuickTestAlarm: (AlarmEntity) -> Unit,
    onBack: () -> Unit
) {
    val isEditing = initialAlarm != null
    val context = LocalContext.current

    // Time State
    var selectedHour by remember { mutableIntStateOf(initialAlarm?.hour ?: 8) }
    var selectedMinute by remember { mutableIntStateOf(initialAlarm?.minute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Schedule Type: "RECURRING", "SPECIFIC_DATE", "ONCE"
    var scheduleType by remember {
        mutableStateOf(
            when {
                initialAlarm?.specificDateMillis != null -> "SPECIFIC_DATE"
                initialAlarm != null && initialAlarm.repeatDays > 0 -> "RECURRING"
                else -> "ONCE"
            }
        )
    }
    var specificDateMillis by remember { mutableStateOf(initialAlarm?.specificDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Title & Category
    var alarmTitle by remember { mutableStateOf(initialAlarm?.name ?: "Wake Up") }
    var selectedCategory by remember { mutableStateOf(initialAlarm?.category ?: "Wake Up") }

    // Person & Message
    var personName by remember { mutableStateOf(initialAlarm?.personName ?: defaultPersonName) }
    var customMessage by remember { mutableStateOf(initialAlarm?.customMessage ?: "") }

    // Voice & TTS Settings
    var isVoiceEnabled by remember { mutableStateOf(initialAlarm?.isVoiceEnabled ?: true) }
    var selectedLanguage by remember { mutableStateOf(initialAlarm?.language ?: "hi") }
    var selectedVoiceName by remember { mutableStateOf(initialAlarm?.voiceName ?: "") }
    var speechSpeed by remember { mutableFloatStateOf(initialAlarm?.speechSpeed ?: 1.0f) }
    var speechPitch by remember { mutableFloatStateOf(initialAlarm?.speechPitch ?: 1.0f) }
    var voiceDropdownExpanded by remember { mutableStateOf(false) }

    // Sound Settings
    var isSoundEnabled by remember { mutableStateOf(initialAlarm?.isSoundEnabled ?: true) }
    var soundUri by remember { mutableStateOf(initialAlarm?.soundUri ?: "default") }
    var volume by remember { mutableIntStateOf(initialAlarm?.volume ?: 90) }
    var isPreviewingSound by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Vibration Settings
    var isVibrationEnabled by remember { mutableStateOf(initialAlarm?.isVibrationEnabled ?: true) }
    var vibrationPatternType by remember { mutableStateOf(initialAlarm?.vibrationPatternType ?: "normal") }
    var customVibrationText by remember { mutableStateOf(initialAlarm?.customVibrationPattern ?: "0,800,400,800") }

    // Snooze & Ring Duration Settings
    var snoozeMinutesText by remember { mutableStateOf((initialAlarm?.snoozeDurationMinutes ?: 10).toString()) }
    var ringDurationSeconds by remember { mutableIntStateOf(initialAlarm?.ringDurationSeconds ?: 300) }

    // Days Repeat Bitmask
    var repeatDays by remember { mutableIntStateOf(initialAlarm?.repeatDays ?: 0) }

    var validationError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.stop()
            previewPlayer?.release()
        }
    }

    fun formatDisplayTime(h: Int, m: Int): Pair<String, String> {
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

    val (timeText, amPmText) = formatDisplayTime(selectedHour, selectedMinute)

    val liveSpeechPreview = PersonalizedMessageEngine.generateMessage(
        personName = personName,
        category = selectedCategory,
        customMessage = customMessage,
        language = selectedLanguage
    )

    // Time Picker Dialog
    if (showTimePicker) {
        BoloTimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = is24Hour,
            onTimeSelected = { h, m ->
                selectedHour = h
                selectedMinute = m
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        BoloDatePickerDialog(
            initialDateMillis = specificDateMillis,
            onDateSelected = { _, _, _, timeMillis ->
                specificDateMillis = timeMillis
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Alarm" else "New Alarm",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("alarm_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val snoozeVal = snoozeMinutesText.toIntOrNull() ?: 10
                            if (snoozeVal !in 1..120) {
                                validationError = "Snooze duration must be between 1 and 120 minutes."
                                return@TextButton
                            }
                            if (scheduleType == "SPECIFIC_DATE" && specificDateMillis == null) {
                                validationError = "Please select a specific date for this alarm."
                                return@TextButton
                            }
                            if (vibrationPatternType == "custom") {
                                val patternClean = customVibrationText.trim()
                                val isValid = patternClean.split(",").all { it.trim().toLongOrNull() != null && it.trim().toLong() >= 0 }
                                if (!isValid || patternClean.isEmpty()) {
                                    validationError = "Invalid vibration pattern. Use comma-separated positive milliseconds (e.g. 0,800,400,800)."
                                    return@TextButton
                                }
                            }

                            val alarmToSave = (initialAlarm ?: AlarmEntity(
                                hour = selectedHour,
                                minute = selectedMinute,
                                name = alarmTitle,
                                personName = personName
                            )).copy(
                                hour = selectedHour,
                                minute = selectedMinute,
                                name = alarmTitle.ifBlank { "Alarm" },
                                personName = personName.ifBlank { defaultPersonName },
                                category = selectedCategory,
                                customMessage = customMessage,
                                language = selectedLanguage,
                                voiceName = selectedVoiceName,
                                speechSpeed = speechSpeed,
                                speechPitch = speechPitch,
                                repeatDays = if (scheduleType == "RECURRING") repeatDays else 0,
                                specificDateMillis = if (scheduleType == "SPECIFIC_DATE") specificDateMillis else null,
                                isVoiceEnabled = isVoiceEnabled,
                                isSoundEnabled = isSoundEnabled,
                                isVibrationEnabled = isVibrationEnabled,
                                vibrationPatternType = vibrationPatternType,
                                customVibrationPattern = customVibrationText,
                                soundUri = soundUri,
                                volume = volume,
                                snoozeDurationMinutes = snoozeVal,
                                ringDurationSeconds = ringDurationSeconds,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSaveAlarm(alarmToSave)
                        },
                        modifier = Modifier.testTag("save_alarm_button")
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Validation Error Display
            if (validationError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. Time Display Hero Card (Interactive)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
                    .testTag("alarm_time_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeText,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (amPmText.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = amPmText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tap to change time (Picker or Manual Type)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 2. Schedule Type & Repeat Configuration
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
                    Text(
                        text = "Schedule Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = scheduleType == "ONCE",
                            onClick = { scheduleType = "ONCE"; repeatDays = 0 },
                            label = { Text("One-Time") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = scheduleType == "RECURRING",
                            onClick = {
                                scheduleType = "RECURRING"
                                if (repeatDays == 0) repeatDays = 127
                            },
                            label = { Text("Weekly Repeat") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = scheduleType == "SPECIFIC_DATE",
                            onClick = {
                                scheduleType = "SPECIFIC_DATE"
                                if (specificDateMillis == null) specificDateMillis = System.currentTimeMillis()
                            },
                            label = { Text("Specific Date") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // If Specific Date:
                    if (scheduleType == "SPECIFIC_DATE") {
                        val dateFormatted = if (specificDateMillis != null) {
                            SimpleDateFormat("EEE, d MMMM yyyy", Locale.getDefault()).format(Date(specificDateMillis!!))
                        } else "Select Date"

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Date: $dateFormatted", fontWeight = FontWeight.Bold)
                        }
                    }

                    // If Recurring:
                    if (scheduleType == "RECURRING") {
                        // Preset Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = repeatDays == 127,
                                onClick = { repeatDays = 127 },
                                label = { Text("Every day") }
                            )
                            FilterChip(
                                selected = repeatDays == 31,
                                onClick = { repeatDays = 31 },
                                label = { Text("Weekdays") }
                            )
                            FilterChip(
                                selected = repeatDays == 96,
                                onClick = { repeatDays = 96 },
                                label = { Text("Weekends") }
                            )
                        }

                        // Day Bubbles (Mon - Sun)
                        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (i in 0..6) {
                                val isSelected = (repeatDays and (1 shl i)) != 0
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable {
                                            repeatDays = repeatDays xor (1 shl i)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLabels[i],
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Custom Title & Category
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
                    Text(
                        text = "Alarm Title & Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = alarmTitle,
                        onValueChange = { alarmTitle = it },
                        label = { Text("Custom Title / Label") },
                        placeholder = { Text("e.g. Morning Study Session, Breakfast Reminder") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alarm_title_input")
                    )

                    Text(
                        text = "Quick Categories",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (category in PersonalizedMessageEngine.CATEGORIES) {
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = category
                                    if (alarmTitle.isBlank() || PersonalizedMessageEngine.CATEGORIES.contains(alarmTitle)) {
                                        alarmTitle = category
                                    }
                                },
                                label = { Text(category) }
                            )
                        }
                    }
                }
            }

            // 4. Personalized Voice & Reminder Speech
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
                            Text(
                                text = "Speak Personalized Reminder",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Switch(
                            checked = isVoiceEnabled,
                            onCheckedChange = { isVoiceEnabled = it },
                            modifier = Modifier.testTag("voice_enabled_switch")
                        )
                    }

                    if (isVoiceEnabled) {
                        // Person Name (Independent per alarm)
                        OutlinedTextField(
                            value = personName,
                            onValueChange = { personName = it },
                            label = { Text("Person's Name") },
                            placeholder = { Text("e.g. Yaseen, Rahul, Aman") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("person_name_input")
                        )

                        // Custom Message Text Box
                        OutlinedTextField(
                            value = customMessage,
                            onValueChange = { customMessage = it },
                            label = { Text("Custom Message (Optional)") },
                            placeholder = { Text("e.g. Yaseen, 7:43 baj gaye hain. Breakfast karne ka time ho gaya hai.") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_message_input"),
                            minLines = 2,
                            maxLines = 4
                        )

                        // Language Selection
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("hi" to "Hindi", "en" to "English", "default" to "System").forEach { (code, label) ->
                                FilterChip(
                                    selected = selectedLanguage == code,
                                    onClick = { selectedLanguage = code },
                                    label = { Text(label) }
                                )
                            }
                        }

                        // Speech Speed & Pitch Sliders
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Speech Speed: ${String.format(Locale.getDefault(), "%.1fx", speechSpeed)}", style = MaterialTheme.typography.labelMedium)
                            }
                            Slider(
                                value = speechSpeed,
                                onValueChange = { speechSpeed = it },
                                valueRange = 0.5f..2.0f,
                                steps = 14
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Speech Pitch: ${String.format(Locale.getDefault(), "%.1fx", speechPitch)}", style = MaterialTheme.typography.labelMedium)
                            }
                            Slider(
                                value = speechPitch,
                                onValueChange = { speechPitch = it },
                                valueRange = 0.5f..2.0f,
                                steps = 14
                            )
                        }

                        // Live Speech Preview Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Spoken Reminder Preview:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"$liveSpeechPreview\"",
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // TEST VOICE Button
                        OutlinedButton(
                            onClick = {
                                onTestVoice(personName, selectedCategory, customMessage, selectedLanguage, speechSpeed, speechPitch, selectedVoiceName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_voice_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TEST VOICE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. Sound & Volume Controls
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
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Alarm Sound", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = isSoundEnabled,
                            onCheckedChange = { isSoundEnabled = it }
                        )
                    }

                    if (isSoundEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("default" to "Standard Alarm", "ringtone" to "Ringtone", "gentle" to "Gentle").forEach { (type, label) ->
                                FilterChip(
                                    selected = soundUri == type,
                                    onClick = { soundUri = type },
                                    label = { Text(label) }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (isPreviewingSound) {
                                    previewPlayer?.stop()
                                    previewPlayer?.release()
                                    previewPlayer = null
                                    isPreviewingSound = false
                                } else {
                                    try {
                                        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                                        previewPlayer = MediaPlayer.create(context, uri).apply {
                                            start()
                                            setOnCompletionListener {
                                                isPreviewingSound = false
                                            }
                                        }
                                        isPreviewingSound = true
                                    } catch (e: Exception) {
                                        isPreviewingSound = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(if (isPreviewingSound) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPreviewingSound) "Stop Sound Preview" else "Preview Alarm Sound")
                        }
                    }
                }
            }

            // 6. Custom Vibration Pattern
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
                            Icon(Icons.Default.Vibration, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vibration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = isVibrationEnabled,
                            onCheckedChange = { isVibrationEnabled = it }
                        )
                    }

                    if (isVibrationEnabled) {
                        Text("Vibration Pattern", style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("short" to "Short", "normal" to "Normal", "long" to "Long", "heartbeat" to "Heartbeat", "custom" to "Custom").forEach { (pattern, label) ->
                                FilterChip(
                                    selected = vibrationPatternType == pattern,
                                    onClick = { vibrationPatternType = pattern },
                                    label = { Text(label) }
                                )
                            }
                        }

                        if (vibrationPatternType == "custom") {
                            OutlinedTextField(
                                value = customVibrationText,
                                onValueChange = { customVibrationText = it },
                                label = { Text("Custom Pattern (wait, vibrate, wait, vibrate in ms)") },
                                placeholder = { Text("0,800,400,800") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 7. Custom Snooze & Ring Duration
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
                    Text("Custom Snooze Duration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = snoozeMinutesText,
                            onValueChange = {
                                if (it.all { ch -> ch.isDigit() } && it.length <= 3) {
                                    snoozeMinutesText = it
                                }
                            },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(110.dp)
                                .testTag("custom_snooze_input")
                        )

                        Text("min (1 - 120 mins allowed)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Quick Chips for Snooze
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 5, 7, 10, 15, 20, 30).forEach { mins ->
                            FilterChip(
                                selected = snoozeMinutesText == mins.toString(),
                                onClick = { snoozeMinutesText = mins.toString() },
                                label = { Text("${mins}m") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Alarm Ring Duration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            30 to "30 sec",
                            60 to "1 min",
                            120 to "2 min",
                            300 to "5 min",
                            600 to "10 min",
                            900 to "15 min",
                            -1 to "Until Stopped"
                        ).forEach { (secs, label) ->
                            FilterChip(
                                selected = ringDurationSeconds == secs,
                                onClick = { ringDurationSeconds = secs },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // 8. Full Preview Card & Immediate TEST ALARM Button
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Alarm Summary Preview",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "• Time: $timeText $amPmText (${if (scheduleType == "RECURRING") "Weekly" else if (scheduleType == "SPECIFIC_DATE") "Specific Date" else "One-Time"})",
                        fontSize = 13.sp
                    )
                    Text(text = "• Title: ${alarmTitle.ifBlank { "Alarm" }} | Person: ${personName.ifBlank { "User" }}", fontSize = 13.sp)
                    Text(text = "• Voice: ${if (isVoiceEnabled) "ON (${selectedLanguage.uppercase()})" else "OFF"} | Sound: ${if (isSoundEnabled) "ON" else "OFF"} | Vibration: ${if (isVibrationEnabled) "ON ($vibrationPatternType)" else "OFF"}", fontSize = 13.sp)
                    Text(text = "• Snooze: $snoozeMinutesText mins | Ring Duration: ${if (ringDurationSeconds == -1) "Until stopped" else "${ringDurationSeconds}s"}", fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(4.dp))

                    // TEST ALARM Button
                    Button(
                        onClick = {
                            val tempAlarm = AlarmEntity(
                                id = 99999,
                                hour = selectedHour,
                                minute = selectedMinute,
                                name = alarmTitle.ifBlank { "Test Alarm" },
                                personName = personName.ifBlank { defaultPersonName },
                                category = selectedCategory,
                                customMessage = customMessage,
                                language = selectedLanguage,
                                voiceName = selectedVoiceName,
                                speechSpeed = speechSpeed,
                                speechPitch = speechPitch,
                                isVoiceEnabled = isVoiceEnabled,
                                isSoundEnabled = isSoundEnabled,
                                isVibrationEnabled = isVibrationEnabled,
                                vibrationPatternType = vibrationPatternType,
                                customVibrationPattern = customVibrationText,
                                soundUri = soundUri,
                                snoozeDurationMinutes = snoozeMinutesText.toIntOrNull() ?: 10,
                                ringDurationSeconds = ringDurationSeconds
                            )
                            onQuickTestAlarm(tempAlarm)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("test_full_alarm_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TEST FULL ALARM (Sound + Voice + Vibration)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}
