package com.halaprix.leakwatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for daily battery summaries.
 */
@Dao
interface DailySummaryDao {
    
    /**
     * Insert or replace a daily summary.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: DailySummary)
    
    /**
     * Get all daily summaries ordered by date ascending.
     */
    @Query("SELECT * FROM daily_summaries ORDER BY date ASC")
    fun getAllSummaries(): Flow<List<DailySummary>>
    
    /**
     * Get summaries from the last N days.
     * @param startDate ISO 8601 date string (e.g., "2026-06-01")
     */
    @Query("SELECT * FROM daily_summaries WHERE date >= :startDate ORDER BY date ASC")
    fun getSummariesSince(startDate: String): Flow<List<DailySummary>>
    
    /**
     * Get a specific day's summary.
     * @param date ISO 8601 date string (e.g., "2026-06-16")
     */
    @Query("SELECT * FROM daily_summaries WHERE date = :date LIMIT 1")
    suspend fun getSummaryForDate(date: String): DailySummary?
    
    /**
     * Delete summaries older than the given date.
     * Used for rolling window cleanup.
     * @param cutoffDate ISO 8601 date string (exclusive)
     */
    @Query("DELETE FROM daily_summaries WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)
    
    /**
     * Get the count of summaries.
     */
    @Query("SELECT COUNT(*) FROM daily_summaries")
    fun getSummaryCount(): Flow<Int>
}
