package com.halaprix.leakwatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Battery reading from the watch.
 * Mirrors the watch-side BatteryRecord model.
 */
@Entity(tableName = "battery_readings")
data class BatteryReading(
    /** Unix timestamp in ms (primary key) */
    @PrimaryKey
    val ts: Long,
    
    /** Battery state of charge, 0-100 */
    val level: Int,
    
    /** 0 = unknown, 1 = AC, 2 = USB, 3 = wireless */
    val pluggedType: Int,
    
    /** 0 = unknown, 1 = charging, 2 = not charging, 3 = full */
    val chargingStatus: Int,
    
    /** Voltage in microvolts */
    val voltage: Long,
    
    /** Temperature in 0.1°C */
    val temperature: Int,
    
    /** True if battery is present */
    val isPresent: Boolean,
    
    /** Timestamp when this reading was received on the phone (ms) */
    val receivedAt: Long = System.currentTimeMillis()
)
