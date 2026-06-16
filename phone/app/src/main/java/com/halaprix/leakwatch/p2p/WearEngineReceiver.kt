package com.halaprix.leakwatch.p2p

import android.content.Context
import android.util.Log
import com.halaprix.leakwatch.data.AppDatabase
import com.halaprix.leakwatch.data.BatteryReading
import com.huawei.hms.support.api.wearable.Wearable
import com.huawei.hms.support.api.wearable.data.DataEvent
import com.huawei.hms.support.api.wearable.data.DataMapItem
import com.huawei.hms.support.api.wearable.data.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Real HMS Wear Engine P2P receiver.
 * 
 * Listens for battery data from the watch via Wear Engine DataClient.
 * The watch sends data as a DataMap with key "battery_readings" containing
 * a JSON byte array of BatteryReading objects.
 * 
 * Data flow:
 * 1. Watch sends battery readings via WearEngineSender (ArkTS)
 * 2. Phone receives DataEvent in WearEngineReceiverService
 * 3. Service deserializes JSON → List<BatteryReading>
 * 4. Inserts into Room database
 * 
 * Battery budget: listener-driven, no polling. Only wakes on data arrival.
 */
class WearEngineReceiver(private val context: Context) {
    private val TAG = "WearEngineReceiver"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.batteryReadingDao()
    
    companion object {
        const val KEY_BATTERY_READINGS = "battery_readings"
        const val PATH_BATTERY_DATA = "/leakwatch/battery"
    }
    
    /**
     * Insert a batch of readings (called from WearEngineReceiverService).
     */
    suspend fun insertReadings(readings: List<BatteryReading>) {
        if (readings.isEmpty()) {
            Log.w(TAG, "Empty readings list, skipping insert")
            return
        }
        
        dao.insertAll(readings)
        Log.i(TAG, "Inserted ${readings.size} readings from watch")
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

/**
 * WearableListenerService that receives data from the watch.
 * 
 * This service is declared in AndroidManifest.xml and automatically invoked
 * by HMS Wear Engine when data arrives from the paired watch.
 * 
 * Data format: DataMap with key "battery_readings" containing JSON byte array.
 */
class WearEngineReceiverService : WearableListenerService() {
    private val TAG = "WearEngineReceiverService"
    private lateinit var receiver: WearEngineReceiver
    
    override fun onCreate() {
        super.onCreate()
        receiver = WearEngineReceiver(applicationContext)
        Log.i(TAG, "WearEngineReceiverService created")
    }
    
    override fun onDataChanged(event: DataEvent) {
        Log.d(TAG, "onDataChanged: ${event.type}, path=${event.dataItem.uri.path}")
        
        when (event.type) {
            DataEvent.TYPE_CHANGED -> {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val dataMap = dataMapItem.dataMap
                
                if (dataMap.containsKey(WearEngineReceiver.KEY_BATTERY_READINGS)) {
                    val rawData = dataMap.getByteArray(WearEngineReceiver.KEY_BATTERY_READINGS)
                    
                    if (rawData != null) {
                        try {
                            val readings = BatteryReadingSerializer.deserialize(rawData)
                            
                            // Insert into database
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                receiver.insertReadings(readings)
                            }
                            
                            Log.i(TAG, "Received ${readings.size} readings from watch")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to deserialize battery readings", e)
                        }
                    } else {
                        Log.w(TAG, "battery_readings key present but value is null")
                    }
                } else {
                    Log.w(TAG, "DataMap missing battery_readings key")
                }
            }
            DataEvent.TYPE_DELETED -> {
                Log.d(TAG, "Data deleted (ignored)")
            }
        }
    }
}
