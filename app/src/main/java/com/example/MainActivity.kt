package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AlarmEntity
import com.example.notification.NotificationHelper
import com.example.ui.navigation.BoloNavTab
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.BedtimeScreen
import com.example.ui.screens.CreateEditAlarmScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StopwatchScreen
import com.example.ui.screens.TimersScreen
import com.example.ui.screens.WorldClockScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BoloViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels for alarms, timers, bedtime
        NotificationHelper.createNotificationChannels(this)

        setContent {
            val boloViewModel: BoloViewModel = viewModel()
            val preferences by boloViewModel.preferences.collectAsState()

            val isDarkTheme = when (preferences.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            // Notification permission launcher for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* isGranted */ }

                LaunchedEffect(Unit) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(850)
                showSplash = false
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                    },
                    label = "splash_transition"
                ) { isSplash ->
                    if (isSplash) {
                        BoloSplashScreen()
                    } else {
                        BoloMainApp(viewModel = boloViewModel)
                    }
                }
            }
        }
    }
}

/**
 * Professional BoloAlarm Splash Screen supporting light and dark mode.
 */
@Composable
fun BoloSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_boloalarm_logo),
                contentDescription = "BoloAlarm Logo",
                modifier = Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(26.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BoloMainApp(viewModel: BoloViewModel) {
    var selectedTab by remember { mutableStateOf(BoloNavTab.ALARMS) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }
    var alarmToEdit by remember { mutableStateOf<AlarmEntity?>(null) }

    val alarms by viewModel.allAlarms.collectAsState()
    val worldCities by viewModel.worldClockCities.collectAsState()
    val bedtimeSchedule by viewModel.bedtimeSchedule.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val availableVoices by viewModel.ttsManager.availableVoices.collectAsState()

    val canExactAlarm = viewModel.alarmScheduler.canScheduleExactAlarms()

    when (currentScreen) {
        is AppScreen.CreateEditAlarm -> {
            CreateEditAlarmScreen(
                initialAlarm = alarmToEdit,
                is24Hour = preferences.use24HourFormat,
                defaultPersonName = preferences.defaultPersonName,
                availableVoices = availableVoices,
                onSaveAlarm = { savedAlarm ->
                    viewModel.saveAlarm(savedAlarm)
                    currentScreen = AppScreen.MainTabs
                },
                onTestVoice = { person, cat, custom, lang, speed, pitch, voice ->
                    viewModel.testAlarmVoice(person, cat, custom, lang, speed, pitch, voice)
                },
                onQuickTestAlarm = { alarm ->
                    viewModel.quickTestAlarmRinging(alarm)
                },
                onBack = {
                    currentScreen = AppScreen.MainTabs
                }
            )
        }
        is AppScreen.Settings -> {
            SettingsScreen(
                preferences = preferences,
                onUpdatePreferences = { update ->
                    viewModel.updatePreferences(update)
                },
                onTestVoice = { person, lang, speed ->
                    viewModel.testAlarmVoice(
                        personName = person,
                        category = "Breakfast",
                        customMessage = "",
                        language = lang,
                        speechSpeed = speed
                    )
                },
                onTestVibration = {
                    viewModel.testVibration()
                },
                onBack = {
                    currentScreen = AppScreen.MainTabs
                }
            )
        }
        is AppScreen.MainTabs -> {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        BoloNavTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(tab.icon, contentDescription = tab.title)
                                },
                                label = {
                                    Text(tab.title, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (selectedTab) {
                        BoloNavTab.ALARMS -> {
                            AlarmsScreen(
                                alarms = alarms,
                                is24Hour = preferences.use24HourFormat,
                                canScheduleExactAlarms = canExactAlarm,
                                onAddAlarmClick = {
                                    alarmToEdit = null
                                    currentScreen = AppScreen.CreateEditAlarm
                                },
                                onEditAlarmClick = { alarm ->
                                    alarmToEdit = alarm
                                    currentScreen = AppScreen.CreateEditAlarm
                                },
                                onToggleAlarm = { alarm ->
                                    viewModel.toggleAlarm(alarm)
                                },
                                onDeleteAlarm = { alarm ->
                                    viewModel.deleteAlarm(alarm)
                                },
                                onDuplicateAlarm = { alarm ->
                                    viewModel.duplicateAlarm(alarm)
                                },
                                onQuickTestAlarm = { alarm ->
                                    viewModel.quickTestAlarmRinging(alarm)
                                },
                                onSettingsClick = {
                                    currentScreen = AppScreen.Settings
                                }
                            )
                        }
                        BoloNavTab.WORLD_CLOCK -> {
                            WorldClockScreen(
                                cities = worldCities,
                                is24Hour = preferences.use24HourFormat,
                                onAddCity = { name, country, tz ->
                                    viewModel.addCity(name, country, tz)
                                },
                                onDeleteCity = { city ->
                                    viewModel.deleteCity(city)
                                }
                            )
                        }
                        BoloNavTab.TIMERS -> {
                            TimersScreen()
                        }
                        BoloNavTab.STOPWATCH -> {
                            StopwatchScreen(viewModel = viewModel)
                        }
                        BoloNavTab.BEDTIME -> {
                            BedtimeScreen(
                                schedule = bedtimeSchedule,
                                is24Hour = preferences.use24HourFormat,
                                onSaveSchedule = { schedule ->
                                    viewModel.saveBedtimeSchedule(schedule)
                                },
                                onTestBedtimeVoice = { person, isSleep ->
                                    viewModel.testAlarmVoice(
                                        personName = person,
                                        category = if (isSleep) "Sleep" else "Wake Up",
                                        customMessage = "",
                                        language = bedtimeSchedule?.language ?: "hi",
                                        speechSpeed = 1.0f
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class AppScreen {
    data object MainTabs : AppScreen()
    data object CreateEditAlarm : AppScreen()
    data object Settings : AppScreen()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
