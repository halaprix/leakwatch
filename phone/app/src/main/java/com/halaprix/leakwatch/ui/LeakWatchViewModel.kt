package com.halaprix.leakwatch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halaprix.leakwatch.data.AppDatabase
import com.halaprix.leakwatch.data.BatteryReading
import com.halaprix.leakwatch.data.DailySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for LeakWatch main UI.
 * 
 * Provides:
 * - Latest battery reading (live)
 * - Daily summaries (history)
 * - Mock data insertion (for testing)
 */
class LeakWatchViewModel(application: Application) : AndroidViewModel(application) {
    
    private val db = AppDatabase.getDatabase(application)
    private val readingDao = db.batteryReadingDao()
    private val summaryDao = db.dailySummaryDao()
    
    /**
     * Latest battery reading (live updates via Flow).
     */
    val latestReading: Flow<BatteryReading?> = readingDao.getLatestReading()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    /**
     * All daily summaries (history tab).
     */
    val dailySummaries: Flow<List<DailySummary>> = summaryDao.getAllSummaries()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    /**
     * Total reading count.
     */
    val readingCount: Flow<Int> = readingDao.getReadingCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    /**
     * Average drain rate in %/hour, computed from daily summaries.
     * Assumes 24h per day. Returns null if no data.
     */
    fun calculateAvgDrainRate(summaries: List<DailySummary>): Float? {
        if (summaries.isEmpty()) return null
        val totalDrain = summaries.sumOf { it.totalDrain }
        val totalDays = summaries.size
        return totalDrain.toFloat() / (totalDays * 24)
    }
    
    /**
     * Best day (lowest drain).
     */
    fun bestDay(summaries: List<DailySummary>): DailySummary? {
        return summaries.minByOrNull { it.totalDrain }
    }
    
    /**
     * Worst day (highest drain).
     */
    fun worstDay(summaries: List<DailySummary>): DailySummary? {
        return summaries.maxByOrNull { it.totalDrain }
    }
    
    /**
     * Weekly analytics: last 7 days average drain.
     */
    fun weeklyAvgDrain(summaries: List<DailySummary>): Float? {
        val last7 = summaries.takeLast(7)
        if (last7.isEmpty()) return null
        return last7.map { it.totalDrain }.average().toFloat()
    }
    
    /**
     * Monthly analytics: last 30 days average drain.
     */
    fun monthlyAvgDrain(summaries: List<DailySummary>): Float? {
        val last30 = summaries.takeLast(30)
        if (last30.isEmpty()) return null
        return last30.map { it.totalDrain }.average().toFloat()
    }
    
    /**
     * Trend: compare last 7 days vs previous 7 days.
     * Returns: "improving", "worsening", "stable", or null if insufficient data.
     */
    fun drainTrend(summaries: List<DailySummary>): String? {
        if (summaries.size < 14) return null
        val recent7 = summaries.takeLast(7).map { it.totalDrain }.average()
        val previous7 = summaries.dropLast(7).takeLast(7).map { it.totalDrain }.average()
        val diff = recent7 - previous7
        return when {
            diff < -2 -> "improving"
            diff > 2 -> "worsening"
            else -> "stable"
        }
    }
    
    /**
     * Insert a batch of mock readings for testing.
     * Simulates 24 hours of data with realistic battery drain.
     */
    fun insertMockBatch(count: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val intervalMs = 120_000L // 120 seconds
            val startLevel = 85
            val drainPerReading = 0.05f // ~0.05% per 2 minutes = ~0.6%/hour
            
            val mockReadings = (0 until count).map { i ->
                BatteryReading(
                    ts = now - (count - i) * intervalMs,
                    level = (startLevel - i * drainPerReading).toInt().coerceIn(0, 100),
                    voltage = 4200 - (i * 2), // mV, decreasing
                    temperature = 25 + (i % 5), // °C, slight variation
                    status = 3 // Discharging
                )
            }
            
            readingDao.insertAll(mockReadings)
        }
    }
    
    /**
     * Clear all data (for testing).
     */
    fun clearAllData() {
        viewModelScope.launch {
            readingDao.deleteAll()
            summaryDao.deleteAll()
        }
    }
}
