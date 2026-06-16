package com.halaprix.leakwatch

import android.app.Application
import android.util.Log
import androidx.work.*
import com.halaprix.leakwatch.worker.DailyAggregationWorker
import java.util.concurrent.TimeUnit

/**
 * LeakWatch Application class.
 * 
 * Initializes WorkManager for daily aggregation of battery readings.
 * The worker runs once per day at midnight (or as close as WorkManager allows).
 * 
 * Battery budget: WorkManager is system-managed and batches work efficiently.
 * The daily aggregation query is O(n) with n bounded by ~720 readings/day.
 * Total energy cost: negligible (<0.01%/day).
 */
class LeakWatchApplication : Application() {
    
    companion object {
        const val TAG = "LeakWatchApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "LeakWatch Application starting")
        scheduleDailyAggregation()
    }
    
    /**
     * Schedule daily aggregation worker to run at midnight.
     * 
     * Uses PeriodicWorkRequest with 24h interval.
     * WorkManager will batch this with other system work for efficiency.
     * The worker is idempotent — running it multiple times per day is safe.
     */
    private fun scheduleDailyAggregation() {
        val workManager = WorkManager.getInstance(this)
        
        // Check if work is already enqueued
        val workInfo = workManager.getWorkInfosForUniqueWork(DailyAggregationWorker.WORK_NAME)
        
        // Use enqueueUniquePeriodicWork to avoid duplicate scheduling
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false) // Run even if battery is low (we're a battery monitor)
            .setRequiresCharging(false) // Don't require charging
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No network needed
            .build()
        
        val aggregationRequest = PeriodicWorkRequestBuilder<DailyAggregationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .addTag(DailyAggregationWorker.TAG)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            DailyAggregationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            aggregationRequest
        )
        
        Log.i(TAG, "Daily aggregation worker scheduled")
    }
    
    /**
     * Calculate delay until next midnight (UTC).
     * This ensures the worker runs at the start of each day.
     */
    private fun calculateInitialDelay(): Long {
        val now = System.currentTimeMillis()
        val tomorrow = java.time.LocalDate.now()
            .plusDays(1)
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        return tomorrow - now
    }
}
