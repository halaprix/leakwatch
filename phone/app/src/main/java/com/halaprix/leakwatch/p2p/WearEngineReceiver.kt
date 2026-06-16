package com.halaprix.leakwatch.p2p

import android.content.Context
import android.util.Log
import com.halaprix.leakwatch.data.AppDatabase
import com.halaprix.leakwatch.data.BatteryReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Stub P2P receiver that simulates receiving battery data from the watch.
 * Real HMS Wear Engine P2P integration lands in v0.4.0-alpha.1.
 */
class WearEngineReceiver(private val context: Context) {
    private val TAG = "WearEngineReceiver"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.batteryReadingDao()
    
    @Volatile
    private var isReceiving = false
    
    /**
     * Start receiving mock battery data.
     * Simulates the watch sending battery readings every 120 seconds.
     */
    fun startReceiving() {
        if (isReceiving) {
            Log.w(TAG, "Already receiving")
            return
        }
        
        isReceiving = true
        Log.i(TAG, "Starting mock P2P receiver (real P2P in v0.4.0-alpha.1)")
        
        scope.launch {
            while (isReceiving) {
                try {
                    val mockReading = generateMockReading()
                    dao.insert(mockReading)
                    Log.d(TAG, "Inserted mock reading: level=${mockReading.level}%, temp=${mockReading.temperature/10.0}°C")
                    delay(120_000) // 120 seconds, matching watch polling interval
                } catch (e: Exception) {
                    Log.e(TAG, "Error in mock receiver loop", e)
                    delay(5_000) // Retry after 5s on error
                }
            }
        }
    }
    
    /**
     * Stop receiving mock data.
     */
    fun stopReceiving() {
        isReceiving = false
        Log.i(TAG, "Stopped mock P2P receiver")
    }
    
    /**
     * Generate a mock battery reading.
     * Simulates realistic battery drain over time.
     */
    private fun generateMockReading(): BatteryReading {
        val now = System.currentTimeMillis()
        
        // Simulate battery level between 20-100%
        val level = (20..100).random()
        
        // Simulate plugged type (mostly unplugged)
        val pluggedType = if ((0..10).random() < 2) 2 else 0 // 2=USB, 0=unplugged
        
        // Charging status based on plugged type
        val chargingStatus = when (pluggedType) {
            2 -> 1 // Charging
            else -> 2 // Not charging
        }
        
        // Voltage: 3500-4200 mV (3.5V - 4.2V typical Li-ion)
        val voltage = (3500..4200).random().toLong() * 1000 // Convert to microvolts
        
        // Temperature: 25-35°C (stored as 0.1°C units)
        val temperature = (250..350).random()
        
        return BatteryReading(
            ts = now,
            level = level,
            pluggedType = pluggedType,
            chargingStatus = chargingStatus,
            voltage = voltage,
            temperature = temperature,
            isPresent = true,
            receivedAt = now
        )
    }
    
    /**
     * Insert a batch of mock readings for testing.
     * Useful for populating the database with historical data.
     */
    suspend fun insertMockBatch(count: Int = 50) {
        val readings = mutableListOf<BatteryReading>()
        val now = System.currentTimeMillis()
        
        for (i in 0 until count) {
            val ts = now - (count - i) * 120_000L // Spread over time
            readings.add(
                BatteryReading(
                    ts = ts,
                    level = (20..100).random(),
                    pluggedType = if ((0..10).random() < 2) 2 else 0,
                    chargingStatus = if ((0..10).random() < 2) 1 else 2,
                    voltage = (3500..4200).random().toLong() * 1000,
                    temperature = (250..350).random(),
                    isPresent = true,
                    receivedAt = now
                )
            )
        }
        
        dao.insertAll(readings)
        Log.i(TAG, "Inserted $count mock readings")
    }
}
