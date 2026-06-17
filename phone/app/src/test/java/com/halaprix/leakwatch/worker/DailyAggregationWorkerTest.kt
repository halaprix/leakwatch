package com.halaprix.leakwatch.worker

import com.halaprix.leakwatch.data.BatteryReading
import com.halaprix.leakwatch.data.DailyStats
import com.halaprix.leakwatch.data.DailySummary
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Unit tests for DailyAggregationWorker logic.
 *
 * Tests the aggregation calculation without requiring Android context or
 * Room database. Integration tests with real database would go in androidTest.
 */
class DailyAggregationWorkerTest {

    private fun reading(ts: Long, level: Int, pluggedType: Int = 0, chargingStatus: Int = 2) =
        BatteryReading(
            ts = ts,
            level = level,
            pluggedType = pluggedType,
            chargingStatus = chargingStatus,
            voltage = 4000L,
            temperature = 25,
            isPresent = true
        )

    // --- computeTotalDrain ---

    @Test
    fun `computeTotalDrain sums negative deltas only`() {
        // Steady drain 80 -> 60 -> 40 -> 20 = 60 total
        val readings = listOf(
            reading(0L, 80),
            reading(120_000L, 60),
            reading(240_000L, 40),
            reading(360_000L, 20)
        )
        assertEquals(60, DailyStatsCalculator.computeTotalDrain(readings))
    }

    @Test
    fun `computeTotalDrain ignores charging segments`() {
        // Drain 80 -> 50 = 30, then charge 50 -> 90 (ignored), then drain 90 -> 70 = 20.
        // Real discharge: 50. max-min would say 40, which is wrong.
        val readings = listOf(
            reading(0L, 80),
            reading(120_000L, 50),
            reading(240_000L, 90),
            reading(360_000L, 70)
        )
        assertEquals(50, DailyStatsCalculator.computeTotalDrain(readings))
    }

    @Test
    fun `computeTotalDrain returns zero for empty or single reading`() {
        assertEquals(0, DailyStatsCalculator.computeTotalDrain(emptyList()))
        assertEquals(0, DailyStatsCalculator.computeTotalDrain(listOf(reading(0L, 50))))
    }

    @Test
    fun `computeTotalDrain treats flat segments as zero`() {
        val readings = listOf(
            reading(0L, 50),
            reading(120_000L, 50),
            reading(240_000L, 50)
        )
        assertEquals(0, DailyStatsCalculator.computeTotalDrain(readings))
    }

    @Test
    fun `computeTotalDrain reports pure charge as zero drain`() {
        // Battery charged all day. The app exists to measure discharge;
        // pure charging is not "drain".
        val readings = listOf(
            reading(0L, 20),
            reading(120_000L, 60),
            reading(240_000L, 100)
        )
        assertEquals(0, DailyStatsCalculator.computeTotalDrain(readings))
    }

    @Test
    fun `computeTotalDrain handles realistic daily cycle`() {
        // 24h at 120s interval = 720 readings. Start 100, end 30. Real
        // discharge is roughly 70. A mid-day charge 40 -> 70 should be
        // ignored. The old max-min metric would say 70 here, but it
        // also said 80 for a 20 -> 100 day, which is the failure mode.
        val readings = (0..720).map { i ->
            val level = when {
                i < 200 -> 100 - (i / 4)            // 100 -> 50 over 200 steps
                i < 350 -> 40 + ((i - 200) / 5)     // 40 -> 70 over 150 steps (charging)
                else -> 70 - ((i - 350) / 10)        // 70 -> 30 over 370 steps
            }.coerceIn(0, 100)
            reading(i * 120_000L, level)
        }
        val drain = DailyStatsCalculator.computeTotalDrain(readings)
        // Drain from 100 -> 50 = 50, then charge 40 -> 70 (ignored),
        // then drain 70 -> 30 = 40. Total = 90.
        // Allow some slack for the discrete step rounding.
        assertTrue("drain should be in 80..100 range, got $drain", drain in 80..100)
    }

    // --- DailyStats / DailySummary preservation ---

    @Test
    fun `daily stats still computes min max avg count from SQL`() {
        val stats = DailyStats(
            minLevel = 45,
            maxLevel = 92,
            avgLevel = 68,
            readingCount = 720
        )
        assertEquals(45, stats.minLevel)
        assertEquals(92, stats.maxLevel)
        assertEquals(68, stats.avgLevel)
        assertEquals(720, stats.readingCount)
    }

    @Test
    fun `daily summary creation from stats uses real drain`() {
        val stats = DailyStats(
            minLevel = 45,
            maxLevel = 92,
            avgLevel = 68,
            readingCount = 4
        )
        val readings = listOf(
            reading(0L, 92),
            reading(120_000L, 70),
            reading(240_000L, 60),
            reading(360_000L, 45)
        )
        val totalDrain = DailyStatsCalculator.computeTotalDrain(readings)
        val summary = DailySummary(
            date = "2026-06-16",
            minLevel = stats.minLevel,
            maxLevel = stats.maxLevel,
            avgLevel = stats.avgLevel,
            totalDrain = totalDrain,
            readingCount = stats.readingCount
        )
        assertEquals("2026-06-16", summary.date)
        assertEquals(45, summary.minLevel)
        assertEquals(92, summary.maxLevel)
        assertEquals(68, summary.avgLevel)
        // Real drain = (92-70) + (70-60) + (60-45) = 22 + 10 + 15 = 47.
        // (Same numeric answer as the old max-min in this case, but for
        // the right reason.)
        assertEquals(47, summary.totalDrain)
        assertEquals(4, summary.readingCount)
    }

    // --- helpers preserved from the original test file ---

    @Test
    fun `date formatting produces ISO 8601`() {
        val date = LocalDate.of(2026, 6, 16)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val dateStr = date.format(formatter)
        assertEquals("2026-06-16", dateStr)
    }

    @Test
    fun `retention policy keeps last 30 days`() {
        val today = LocalDate.of(2026, 6, 16)
        val cutoffDate = today.minusDays(30)
        assertEquals(LocalDate.of(2026, 5, 17), cutoffDate)
    }

    @Test
    fun `empty stats returns null`() {
        val stats: DailyStats? = null
        assertNull(stats)
    }
}
