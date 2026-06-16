package com.halaprix.leakwatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daily battery summary aggregated from raw readings.
 * 
 * Computed once per day at midnight by DailyAggregationWorker.
 * Stores min/max/avg battery level and total drain for the day.
 */
@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey
    val date: String, // ISO 8601 date: "2026-06-16"
    val minLevel: Int, // 0-100%
    val maxLevel: Int, // 0-100%
    val avgLevel: Int, // 0-100%
    val totalDrain: Int, // 0-100% (maxLevel - minLevel, clamped)
    val readingCount: Int // number of raw readings aggregated
)
