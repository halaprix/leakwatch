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
            readingDao.deleteOlderThan(System.currentTimeMillis())
            summaryDao.deleteOlderThan("1970-01-01")
        }
    }
}
