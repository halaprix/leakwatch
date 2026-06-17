package com.halaprix.leakwatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for battery readings.
 */
@Dao
interface BatteryReadingDao {
    
    /**
     * Insert a single battery reading.
     * If a reading with the same timestamp exists, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: BatteryReading)
    
    /**
     * Insert multiple battery readings.
     * If readings with the same timestamps exist, replace them.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<BatteryReading>)
    
    /**
     * Get all readings ordered by timestamp ascending.
     */
    @Query("SELECT * FROM battery_readings ORDER BY ts ASC")
    fun getAllReadings(): Flow<List<BatteryReading>>
    
    /**
     * Get readings from the last N hours.
     */
    @Query("SELECT * FROM battery_readings WHERE ts >= :sinceTs ORDER BY ts ASC")
    fun getReadingsSince(sinceTs: Long): Flow<List<BatteryReading>>
    
    /**
     * Get the most recent reading.
     */
    @Query("SELECT * FROM battery_readings ORDER BY ts DESC LIMIT 1")
    fun getLatestReading(): Flow<BatteryReading?>
    
    /**
     * Get readings for a specific day (UTC).
     * @param dayStartTs Start of day in ms (UTC)
     * @param dayEndTs End of day in ms (UTC)
     */
    @Query("SELECT * FROM battery_readings WHERE ts >= :dayStartTs AND ts < :dayEndTs ORDER BY ts ASC")
    fun getReadingsForDay(dayStartTs: Long, dayEndTs: Long): Flow<List<BatteryReading>>
    
    /**
     * Delete readings older than the given timestamp.
     * Used for rolling window cleanup (e.g., keep only last 30 days).
     */
    @Query("DELETE FROM battery_readings WHERE ts < :cutoffTs")
    suspend fun deleteOlderThan(cutoffTs: Long)

    /**
     * Delete all readings.
     * Used by the UI's "Clear all data" action.
     */
    @Query("DELETE FROM battery_readings")
    suspend fun deleteAll()
    
    /**
     * Get the count of readings.
     */
    @Query("SELECT COUNT(*) FROM battery_readings")
    fun getReadingCount(): Flow<Int>
    
    /**
     * Get readings for a specific day range (suspend, not Flow).
     * Used by DailyAggregationWorker for synchronous access.
     */
    @Query("SELECT * FROM battery_readings WHERE ts >= :dayStartTs AND ts < :dayEndTs ORDER BY ts ASC")
    suspend fun getReadingsForDaySync(dayStartTs: Long, dayEndTs: Long): List<BatteryReading>
    
    /**
     * Get daily stats: min level, max level, avg level, count.
     * Returns null if no readings for the day.
     */
    @Query("""
        SELECT 
            MIN(level) as minLevel,
            MAX(level) as maxLevel,
            CAST(ROUND(AVG(level)) AS INTEGER) as avgLevel,
            COUNT(*) as readingCount
        FROM battery_readings 
        WHERE ts >= :dayStartTs AND ts < :dayEndTs
    """)
    suspend fun getDailyStats(dayStartTs: Long, dayEndTs: Long): DailyStats?
}

/**
 * Raw stats result from SQL aggregation query.
 */
data class DailyStats(
    val minLevel: Int,
    val maxLevel: Int,
    val avgLevel: Int,
    val readingCount: Int
)
