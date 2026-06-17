package com.halaprix.leakwatch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halaprix.leakwatch.data.BatteryReading
import com.halaprix.leakwatch.data.DailySummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Main LeakWatch screen with tabs: Today, History, Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeakWatchScreen(viewModel: LeakWatchViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Today", "History", "Settings")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LeakWatch") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            when (selectedTab) {
                0 -> TodayTab(viewModel)
                1 -> HistoryTab(viewModel)
                2 -> SettingsTab(viewModel)
            }
        }
    }
}

/**
 * Today tab: shows latest reading + quick stats.
 */
@Composable
fun TodayTab(viewModel: LeakWatchViewModel) {
    val latestReading by viewModel.latestReading.collectAsState(initial = null)
    val readingCount by viewModel.readingCount.collectAsState(initial = 0)
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Battery",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = latestReading?.let { "${it.level}%" } ?: "--",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    latestReading?.let { reading ->
                        Text(
                            text = "${reading.voltage}mV • ${reading.temperature}°C",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow("Total readings", readingCount.toString())
                    StatRow("Last update", latestReading?.let { formatTimestamp(it.ts) } ?: "Never")
                    
                    // Drain rate from summaries
                    val summaries by viewModel.dailySummaries.collectAsState(initial = emptyList())
                    val avgDrainRate = viewModel.calculateAvgDrainRate(summaries)
                    if (avgDrainRate != null) {
                        StatRow("Avg drain rate", "%.2f%%/hour".format(avgDrainRate))
                        
                        val best = viewModel.bestDay(summaries)
                        val worst = viewModel.worstDay(summaries)
                        if (best != null) {
                            StatRow("Best day", "${best.date} (${best.totalDrain}%)")
                        }
                        if (worst != null) {
                            StatRow("Worst day", "${worst.date} (${worst.totalDrain}%)")
                        }
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = { viewModel.insertMockBatch(50) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Insert 50 Mock Readings")
            }
        }
    }
}

/**
 * History tab: shows daily summaries + chart.
 */
@Composable
fun HistoryTab(viewModel: LeakWatchViewModel) {
    val summaries by viewModel.dailySummaries.collectAsState(initial = emptyList())
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Battery drain chart
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                BatteryDrainChart(summaries)
            }
        }
        
        if (summaries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No daily summaries yet.\nSummaries are generated at midnight.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(summaries.reversed()) { summary ->
                DailySummaryCard(summary)
            }
        }
    }
}

/**
 * Daily summary card.
 */
@Composable
fun DailySummaryCard(summary: DailySummary) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = summary.date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatRow("Min", "${summary.minLevel}%")
            StatRow("Max", "${summary.maxLevel}%")
            StatRow("Avg", "${summary.avgLevel}%")
            StatRow("Drain", "${summary.totalDrain}%")
            StatRow("Readings", summary.readingCount.toString())
        }
    }
}

/**
 * Settings tab: app info + analytics + actions.
 */
@Composable
fun SettingsTab(viewModel: LeakWatchViewModel) {
    val summaries by viewModel.dailySummaries.collectAsState(initial = emptyList())
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow("Version", BuildConfig.VERSION_NAME)
                    StatRow("Build", "analytics")
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val weeklyAvg = viewModel.weeklyAvgDrain(summaries)
                    val monthlyAvg = viewModel.monthlyAvgDrain(summaries)
                    val trend = viewModel.drainTrend(summaries)
                    
                    if (weeklyAvg != null) {
                        StatRow("Last 7 days avg", "%.1f%%/day".format(weeklyAvg))
                    }
                    if (monthlyAvg != null) {
                        StatRow("Last 30 days avg", "%.1f%%/day".format(monthlyAvg))
                    }
                    if (trend != null) {
                        val trendEmoji = when (trend) {
                            "improving" -> "📉"
                            "worsening" -> "📈"
                            else -> "➡️"
                        }
                        StatRow("Trend", "$trendEmoji $trend")
                    }
                    
                    if (summaries.isEmpty()) {
                        Text(
                            text = "No analytics yet. Data will appear after midnight aggregation runs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Raw readings are kept for 30 days. Daily summaries are computed at midnight and also kept for 30 days.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        item {
            OutlinedButton(
                onClick = { viewModel.clearAllData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All Data")
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatTimestamp(ts: Long): String {
    return Instant.ofEpochMilli(ts)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}
