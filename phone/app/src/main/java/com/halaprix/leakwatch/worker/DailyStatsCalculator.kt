package com.halaprix.leakwatch.worker

import com.halaprix.leakwatch.data.BatteryReading

/**
 * Pure-Kotlin helpers for the daily aggregation worker.
 *
 * Kept Android-free so they can be unit-tested under JVM JUnit without
 * an instrumentation runner.
 */
object DailyStatsCalculator {

    /**
     * Compute the day's real discharge from a chronologically ordered
     * sequence of battery readings.
     *
     * Total drain = sum of every downward step between consecutive
     * readings, where a "downward step" is a strictly negative
     * `level` delta (i.e. battery went down). Charging segments
     * (level increased) are ignored; equal levels contribute zero.
     *
     * Why this and not `maxLevel - minLevel`:
     *   A day that charged 20 -> 100 reports "80% drain" under
     *   max-min, which is nonsense. A day that drained 80 -> 20 then
     *   was charged 20 -> 60 reports "40% drain" under max-min
     *   (60 - 20) when the real discharge was 60%. The only metric
     *   that matches what a battery monitor is supposed to surface is
     *   the sum of negative deltas between consecutive samples.
     *
     * The readings are assumed sorted by `ts` ascending. If the input
     * is empty or has a single element, drain is 0.
     */
    fun computeTotalDrain(readings: List<BatteryReading>): Int {
        if (readings.size < 2) return 0
        var drain = 0
        for (i in 1 until readings.size) {
            val delta = readings[i].level - readings[i - 1].level
            if (delta < 0) drain += -delta
        }
        return drain
    }
}
