package com.example.ui.components

import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoloTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dial / Stepper, 1: Direct Type

    // Stepper state
    var selectedHour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    // Manual type state
    val initDisplayHour = if (is24Hour) {
        String.format(Locale.getDefault(), "%02d", initialHour)
    } else {
        val h = when {
            initialHour == 0 -> 12
            initialHour > 12 -> initialHour - 12
            else -> initialHour
        }
        String.format(Locale.getDefault(), "%02d", h)
    }
    var manualHourText by remember { mutableStateOf(initDisplayHour) }
    var manualMinuteText by remember { mutableStateOf(String.format(Locale.getDefault(), "%02d", initialMinute)) }
    var manualIsPm by remember { mutableStateOf(initialHour >= 12) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    val displayHour = if (is24Hour) {
        selectedHour
    } else {
        when {
            selectedHour == 0 -> 12
            selectedHour > 12 -> selectedHour - 12
            else -> selectedHour
        }
    }
    val isPm = selectedHour >= 12

    fun validateAndSubmit() {
        if (selectedTab == 0) {
            onTimeSelected(selectedHour, selectedMinute)
        } else {
            // Validate typed input
            val h = manualHourText.toIntOrNull()
            val m = manualMinuteText.toIntOrNull()

            if (h == null || m == null) {
                errorMessage = "Please enter valid numeric values."
                return
            }

            if (is24Hour) {
                if (h !in 0..23) {
                    errorMessage = "Hour must be between 00 and 23."
                    return
                }
            } else {
                if (h !in 1..12) {
                    errorMessage = "Hour must be between 01 and 12."
                    return
                }
            }

            if (m !in 0..59) {
                errorMessage = "Minute must be between 00 and 59."
                return
            }

            val finalHour = if (is24Hour) {
                h
            } else {
                if (manualIsPm) {
                    if (h == 12) 12 else h + 12
                } else {
                    if (h == 12) 0 else h
                }
            }

            onTimeSelected(finalHour, m)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Alarm Time",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Toggle: Stepper Picker vs Direct Type
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .padding(bottom = 16.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            errorMessage = null
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Picker", fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            errorMessage = null
                            manualHourText = if (is24Hour) {
                                String.format(Locale.getDefault(), "%02d", selectedHour)
                            } else {
                                val h = when {
                                    selectedHour == 0 -> 12
                                    selectedHour > 12 -> selectedHour - 12
                                    else -> selectedHour
                                }
                                String.format(Locale.getDefault(), "%02d", h)
                            }
                            manualMinuteText = String.format(Locale.getDefault(), "%02d", selectedMinute)
                            manualIsPm = selectedHour >= 12
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Type (Manual)", fontSize = 13.sp)
                            }
                        }
                    )
                }

                if (selectedTab == 0) {
                    // Stepper / Wheel View
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Hour selector
                        NumberWheelPicker(
                            value = displayHour,
                            range = if (is24Hour) 0..23 else 1..12,
                            formatTwoDigits = true,
                            onValueChange = { newDisplayHour ->
                                selectedHour = if (is24Hour) {
                                    newDisplayHour
                                } else {
                                    if (isPm) {
                                        if (newDisplayHour == 12) 12 else newDisplayHour + 12
                                    } else {
                                        if (newDisplayHour == 12) 0 else newDisplayHour
                                    }
                                }
                            },
                            tag = "hour_picker"
                        )

                        Text(
                            text = ":",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Minute selector (all 00..59)
                        NumberWheelPicker(
                            value = selectedMinute,
                            range = 0..59,
                            formatTwoDigits = true,
                            onValueChange = { selectedMinute = it },
                            tag = "minute_picker"
                        )

                        if (!is24Hour) {
                            Spacer(modifier = Modifier.width(14.dp))
                            // AM/PM Selector
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AmPmButton(
                                    label = "AM",
                                    isSelected = !isPm,
                                    onClick = {
                                        if (isPm) {
                                            selectedHour = (selectedHour - 12).coerceAtLeast(0)
                                        }
                                    }
                                )
                                AmPmButton(
                                    label = "PM",
                                    isSelected = isPm,
                                    onClick = {
                                        if (!isPm) {
                                            selectedHour = (selectedHour + 12).coerceAtMost(23)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Direct Manual Numeric Entry View
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (is24Hour) "Enter 24-Hour Time (00:00 - 23:59)" else "Enter 12-Hour Time (01:00 - 12:59)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Hour input
                            OutlinedTextField(
                                value = manualHourText,
                                onValueChange = {
                                    if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                                        manualHourText = it
                                        errorMessage = null
                                        if (it.length == 2) {
                                            focusManager.moveFocus(FocusDirection.Next)
                                        }
                                    }
                                },
                                label = { Text("HH") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                ),
                                modifier = Modifier
                                    .width(76.dp)
                                    .testTag("manual_hour_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Text(
                                text = ":",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            // Minute input
                            OutlinedTextField(
                                value = manualMinuteText,
                                onValueChange = {
                                    if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                                        manualMinuteText = it
                                        errorMessage = null
                                    }
                                },
                                label = { Text("MM") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                modifier = Modifier
                                    .width(76.dp)
                                    .testTag("manual_minute_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )

                            if (!is24Hour) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AmPmButton(
                                        label = "AM",
                                        isSelected = !manualIsPm,
                                        onClick = { manualIsPm = false }
                                    )
                                    AmPmButton(
                                        label = "PM",
                                        isSelected = manualIsPm,
                                        onClick = { manualIsPm = true }
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { validateAndSubmit() },
                modifier = Modifier.testTag("time_picker_confirm")
            ) {
                Text("Set Time", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("time_picker_cancel")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NumberWheelPicker(
    value: Int,
    range: IntRange,
    formatTwoDigits: Boolean,
    onValueChange: (Int) -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(tag)
    ) {
        IconButton(
            onClick = {
                val next = if (value + 1 > range.last) range.first else value + 1
                onValueChange(next)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increment")
        }

        Box(
            modifier = Modifier
                .size(width = 68.dp, height = 64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (formatTwoDigits) String.format(Locale.getDefault(), "%02d", value) else value.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        IconButton(
            onClick = {
                val prev = if (value - 1 < range.first) range.last else value - 1
                onValueChange(prev)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrement")
        }
    }
}

@Composable
private fun AmPmButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .testTag("ampm_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
