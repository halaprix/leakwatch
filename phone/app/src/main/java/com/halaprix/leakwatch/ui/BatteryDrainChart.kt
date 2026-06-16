package com.halaprix.leakwatch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.halaprix.leakwatch.data.DailySummary
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer

/**
 * Battery drain chart showing daily drain over time.
 * Uses Vico charts library for visualization.
 */
@Composable
fun BatteryDrainChart(summaries: List<DailySummary>) {
    if (summaries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "No data yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    val modelProducer = remember { CartesianChartModelProducer() }
    
    // Prepare data: x = day index, y = drain %
    val drainData = summaries.map { it.totalDrain.toFloat() }
    
    LaunchedEffect(summaries) {
        modelProducer.runTransaction {
            lineSeries { series(drainData) }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Daily Battery Drain (%)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        CartesianChartHost(
            chart = rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    listOf(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(
                                LineCartesianLayer.LineFill.Area.fill(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ),
                            thickness = 2.dp
                        )
                    )
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Legend / stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Days: ${summaries.size}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Avg drain: ${summaries.map { it.totalDrain }.average().toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
