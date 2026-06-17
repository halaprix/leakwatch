package com.halaprix.leakwatch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.halaprix.leakwatch.data.DailySummary

/**
 * Battery drain chart showing daily drain over time.
 *
 * Simple Compose Canvas implementation — Vico was previously used here
 * but the Vico 1.x/2.x API mismatch made the chart unbuildable. Tracked
 * in issue: "Restore Vico chart on a pinned, consistent version".
 */
@Composable
fun BatteryDrainChart(summaries: List<DailySummary>) {
    if (summaries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val drainData = summaries.map { it.totalDrain.toFloat() }
    val maxDrain = (drainData.maxOrNull() ?: 0f).coerceAtLeast(1f)

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

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val w = size.width
            val h = size.height
            val padding = 16f
            val plotW = w - padding * 2
            val plotH = h - padding * 2

            // Axes
            drawLine(
                color = axisColor,
                start = Offset(padding, padding),
                end = Offset(padding, h - padding),
                strokeWidth = 1f
            )
            drawLine(
                color = axisColor,
                start = Offset(padding, h - padding),
                end = Offset(w - padding, h - padding),
                strokeWidth = 1f
            )

            if (drainData.size < 2) return@Canvas

            val stepX = plotW / (drainData.size - 1)
            val path = Path()
            drainData.forEachIndexed { i, v ->
                val x = padding + i * stepX
                val y = h - padding - (v / maxDrain) * plotH
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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
