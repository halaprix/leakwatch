package com.halaprix.leakwatch.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.halaprix.leakwatch.data.AppDatabase
import com.halaprix.leakwatch.data.DailySummary
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Daily aggregation worker that computes battery stats for the previous day.
 * 
 * Runs at midnight (or as close as WorkManager allows) and:
 * 1. Queries all raw readings from yesterday
 * 2. Computes min/max/avg battery level and total drain
 * 3. Inserts a DailySummary record
 * 4. Cleans up old raw readings (keeps last 30 days)
 * 
 * Battery budget: this worker runs once per day, so its energy cost is negligible.
 * The SQL aggregation query is O(n) but n is bounded by ~720 readings/day (120s interval).
 */
class DailyAggregationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val TAG = "DailyAggregation"
        const val WORK_NAME = "daily_aggregation"
        private const val RETENTION_DAYS = 30
    }
    
    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting daily aggregation")
        
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val readingDao = db.batteryReadingDao()
            val summaryDao = db.dailySummaryDao()
            
            // Compute yesterday's stats
            val yesterday = LocalDate.now().minusDays(1)
            val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
            val dateStr = yesterday.format(dateFormatter)
            
            val dayStart = yesterday.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val dayEnd = yesterday.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            
            val stats = readingDao.getDailyStats(dayStart, dayEnd)
            
            if (stats != null && stats.readingCount > 0) {
                // Compute true discharge from the raw readings, not the
                // min/max range. See DailyStatsCalculator for the rationale.
                val readings = readingDao.getReadingsForDaySync(dayStart, dayEnd)
                val totalDrain = DailyStatsCalculator.computeTotalDrain(readings)

                val summary = DailySummary(
                    date = dateStr,
                    minLevel = stats.minLevel,
                    maxLevel = stats.maxLevel,
                    avgLevel = stats.avgLevel,
                    totalDrain = totalDrain,
                    readingCount = stats.readingCount
                )
                
                summaryDao.insert(summary)
                Log.i(
                    TAG, 
                    "Aggregated $dateStr: min=${stats.minLevel}%, max=${stats.maxLevel}%, " +
                    "avg=${stats.avgLevel}%, drain=$totalDrain%, readings=${stats.readingCount}"
                )
            } else {
                Log.w(TAG, "No readings found for $dateStr, skipping aggregation")
            }
            
            // Cleanup old raw readings (keep last RETENTION_DAYS days)
            val cutoffDate = LocalDate.now().minusDays(RETENTION_DAYS.toLong())
            val cutoffTs = cutoffDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            readingDao.deleteOlderThan(cutoffTs)
            Log.i(TAG, "Cleaned up readings older than $cutoffDate")
            
            // Cleanup old summaries (keep last RETENTION_DAYS days)
            val cutoffDateStr = cutoffDate.format(dateFormatter)
            summaryDao.deleteOlderThan(cutoffDateStr)
            Log.i(TAG, "Cleaned up summaries older than $cutoffDateStr")
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Daily aggregation failed", e)
            Result.retry()
        }
    }
}
