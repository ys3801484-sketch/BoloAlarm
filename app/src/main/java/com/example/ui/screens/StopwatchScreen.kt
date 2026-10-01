package com.example.ui.screens

import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.BoloViewModel

@Composable
fun StopwatchScreen(viewModel: BoloViewModel) {
    val elapsedMillis by viewModel.stopwatchElapsedMillis.collectAsState()
    val isRunning by viewModel.isStopwatchRunning.collectAsState()
    val laps by viewModel.stopwatchLaps.collectAsState()

    val minutes = (elapsedMillis / 60000) % 60
    val seconds = (elapsedMillis / 1000) % 60
    val millisHundredths = (elapsedMillis % 1000) / 10

    val formattedMain = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    val formattedMillis = String.format(Locale.getDefault(), ".%02d", millisHundredths)

    val fastestLap = laps.minByOrNull { it.lapSplitMillis }
    val slowestLap = laps.maxByOrNull { it.lapSplitMillis }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        text = "Stopwatch",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "High precision lap timer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Digital Stopwatch Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formattedMain,
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formattedMillis,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset / Lap button
                        if (isRunning) {
                            OutlinedButton(
                                onClick = { viewModel.lapStopwatch() },
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("lap_button")
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = "Lap")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.resetStopwatch() },
                                enabled = elapsedMillis > 0,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("reset_stopwatch_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset")
                            }
                        }

                        // Start / Pause button
                        Button(
                            onClick = {
                                if (isRunning) {
                                    viewModel.pauseStopwatch()
                                } else {
                                    viewModel.startStopwatch()
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .size(76.dp)
                                .testTag("start_pause_stopwatch_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pause" else "Start",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lap List
            if (laps.isNotEmpty()) {
                Text(
                    text = "Laps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(laps) { lap ->
                        val isFastest = laps.size > 1 && lap == fastestLap
                        val isSlowest = laps.size > 1 && lap == slowestLap

                        val lapMin = (lap.lapSplitMillis / 60000) % 60
                        val lapSec = (lap.lapSplitMillis / 1000) % 60
                        val lapMs = (lap.lapSplitMillis % 1000) / 10
                        val splitFormatted = String.format(Locale.getDefault(), "%02d:%02d.%02d", lapMin, lapSec, lapMs)

                        val totMin = (lap.totalElapsedMillis / 60000) % 60
                        val totSec = (lap.totalElapsedMillis / 1000) % 60
                        val totMs = (lap.totalElapsedMillis % 1000) / 10
                        val totFormatted = String.format(Locale.getDefault(), "%02d:%02d.%02d", totMin, totSec, totMs)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isFastest -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    isSlowest -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Lap ${lap.lapNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (isFastest) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Fastest",
                                            fontSize = 11.sp,
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (isSlowest) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Slowest",
                                            fontSize = 11.sp,
                                            color = Color(0xFFDC2626),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+$splitFormatted",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isFastest) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = totFormatted,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
