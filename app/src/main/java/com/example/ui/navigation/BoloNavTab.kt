package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

enum class BoloNavTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    ALARMS("Alarms", Icons.Default.Alarm, "tab_alarms"),
    WORLD_CLOCK("World Clock", Icons.Default.Public, "tab_world_clock"),
    TIMERS("Timers", Icons.Default.HourglassTop, "tab_timers"),
    STOPWATCH("Stopwatch", Icons.Default.Timer, "tab_stopwatch"),
    BEDTIME("Bedtime", Icons.Default.Bedtime, "tab_bedtime")
}
