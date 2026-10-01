package com.example.ui.screens

import java.util.Locale

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onUpdatePreferences: ((AppPreferences) -> AppPreferences) -> Unit,
    onTestVoice: (person: String, lang: String, speed: Float) -> Unit,
    onTestVibration: () -> Unit,
    onBack: () -> Unit
) {
    var defaultName by remember(preferences) { mutableStateOf(preferences.defaultPersonName) }
    var speed by remember(preferences) { mutableFloatStateOf(preferences.defaultSpeechSpeed) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // 1. Time Format
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("24-Hour Time Format", fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (preferences.use24HourFormat) "e.g. 13:00" else "e.g. 1:00 PM",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = preferences.use24HourFormat,
                            onCheckedChange = { checked ->
                                onUpdatePreferences { it.copy(use24HourFormat = checked) }
                            },
                            modifier = Modifier.testTag("24hr_switch")
                        )
                    }
                }
            }

            // 2. App Theme
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brightness4, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Appearance Theme", fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "System Default", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = {
                                    onUpdatePreferences { it.copy(themeMode = mode) }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // 3. Default Person & Voice Settings
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Personal Voice Defaults", fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = defaultName,
                        onValueChange = {
                            defaultName = it
                            onUpdatePreferences { p -> p.copy(defaultPersonName = it) }
                        },
                        label = { Text("Default Person Name") },
                        placeholder = { Text("e.g. Yaseen") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_default_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Default Language
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Default Voice Language", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("hi" to "Hindi", "en" to "English", "default" to "System").forEach { (code, label) ->
                                FilterChip(
                                    selected = preferences.defaultLanguage == code,
                                    onClick = {
                                        onUpdatePreferences { it.copy(defaultLanguage = code) }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    // Speech Speed
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speech Speed", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1fx", speed)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = speed,
                            onValueChange = {
                                speed = it
                                onUpdatePreferences { p -> p.copy(defaultSpeechSpeed = it) }
                            },
                            valueRange = 0.6f..1.5f,
                            steps = 9
                        )
                    }

                    // Test Voice Button
                    OutlinedButton(
                        onClick = {
                            onTestVoice(defaultName, preferences.defaultLanguage, speed)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Voice with Current Settings")
                    }
                }
            }

            // 4. Default Snooze Duration
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
                    Text("Default Snooze Duration", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20, 30).forEach { mins ->
                            FilterChip(
                                selected = preferences.defaultSnoozeMinutes == mins,
                                onClick = {
                                    onUpdatePreferences { it.copy(defaultSnoozeMinutes = mins) }
                                },
                                label = { Text("${mins} min") }
                            )
                        }
                    }
                }
            }

            // 5. Vibration Test
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Alarm Vibration", fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = preferences.globalVibrationEnabled,
                            onCheckedChange = { checked ->
                                onUpdatePreferences { it.copy(globalVibrationEnabled = checked) }
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = onTestVibration,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Vibration Pattern")
                    }
                }
            }

            // 6. Privacy & Offline Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Privacy Shield",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privacy-by-Design & On-Device",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "All alarms, personal names, reminders, and voice synthesis happen 100% locally on your device. Zero cloud tracking.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
