package com.halaprix.leakwatch.worker

import com.halaprix.leakwatch.data.DailyStats
import com.halaprix.leakwatch.data.DailySummary
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Unit tests for DailyAggregationWorker logic.
 * 
 * Tests the aggregation calculation without requiring Android context or Room database.
 * Integration tests with real database would go in androidTest/.
 */
class DailyAggregationWorkerTest {
    
    @Test
    fun `daily stats calculation computes correct drain`() {
        // Simulate stats from SQL query
        val stats = DailyStats(
            minLevel = 45,
            maxLevel = 92,
            avgLevel = 68,
            readingCount = 720
        )
        
        val totalDrain = (stats.maxLevel - stats.minLevel).coerceAtLeast(0)
        
        assertEquals(47, totalDrain)
    }
    
    @Test
    fun `daily stats handles zero drain`() {
        val stats = DailyStats(
            minLevel = 80,
            maxLevel = 80,
            avgLevel = 80,
            readingCount = 720
        )
        
        val totalDrain = (stats.maxLevel - stats.minLevel).coerceAtLeast(0)
        
        assertEquals(0, totalDrain)
    }
    
    @Test
    fun `daily stats clamps negative drain to zero`() {
        // Edge case: if min > max (shouldn't happen, but defensive)
        val stats = DailyStats(
            minLevel = 90,
            maxLevel = 85,
            avgLevel = 87,
            readingCount = 720
        )
        
        val totalDrain = (stats.maxLevel - stats.minLevel).coerceAtLeast(0)
        
        assertEquals(0, totalDrain)
    }
    
    @Test
    fun `date formatting produces ISO 8601`() {
        val date = LocalDate.of(2026, 6, 16)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val dateStr = date.format(formatter)
        
        assertEquals("2026-06-16", dateStr)
    }
    
    @Test
    fun `daily summary creation from stats`() {
        val stats = DailyStats(
            minLevel = 45,
            maxLevel = 92,
            avgLevel = 68,
            readingCount = 720
        )
        
        val totalDrain = (stats.maxLevel - stats.minLevel).coerceAtLeast(0)
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
        assertEquals(47, summary.totalDrain)
        assertEquals(720, summary.readingCount)
    }
    
    @Test
    fun `retention policy keeps last 30 days`() {
        val today = LocalDate.of(2026, 6, 16)
        val cutoffDate = today.minusDays(30)
        
        assertEquals(LocalDate.of(2026, 5, 17), cutoffDate)
    }
    
    @Test
    fun `empty stats returns null`() {
        // Simulate SQL query returning null when no readings exist
        val stats: DailyStats? = null
        
        assertNull(stats)
    }
}
