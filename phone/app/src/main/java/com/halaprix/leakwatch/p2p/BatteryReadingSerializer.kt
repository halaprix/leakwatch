package com.halaprix.leakwatch.p2p

import com.halaprix.leakwatch.data.BatteryReading
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialization helper for BatteryReading P2P transfer.
 * 
 * Format: JSON array of objects, each with:
 * - ts: Long (Unix ms)
 * - level: Int (0-100)
 * - pluggedType: Int (0=unknown, 1=AC, 2=USB, 3=wireless)
 * - chargingStatus: Int (0=unknown, 1=charging, 2=not charging, 3=full)
 * - voltage: Long (microvolts)
 * - temperature: Int (0.1°C units)
 * - isPresent: Boolean
 */
object BatteryReadingSerializer {
    
    /**
     * Serialize a list of BatteryReading to JSON byte array.
     */
    fun serialize(readings: List<BatteryReading>): ByteArray {
        val jsonArray = JSONArray()
        for (reading in readings) {
            val json = JSONObject().apply {
                put("ts", reading.ts)
                put("level", reading.level)
                put("pluggedType", reading.pluggedType)
                put("chargingStatus", reading.chargingStatus)
                put("voltage", reading.voltage)
                put("temperature", reading.temperature)
                put("isPresent", reading.isPresent)
            }
            jsonArray.put(json)
        }
        return jsonArray.toString().toByteArray(Charsets.UTF_8)
    }
    
    /**
     * Deserialize JSON byte array to list of BatteryReading.
     */
    fun deserialize(data: ByteArray): List<BatteryReading> {
        val jsonString = String(data, Charsets.UTF_8)
        val jsonArray = JSONArray(jsonString)
        val readings = mutableListOf<BatteryReading>()
        
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            readings.add(
                BatteryReading(
                    ts = json.getLong("ts"),
                    level = json.getInt("level"),
                    pluggedType = json.getInt("pluggedType"),
                    chargingStatus = json.getInt("chargingStatus"),
                    voltage = json.getLong("voltage"),
                    temperature = json.getInt("temperature"),
                    isPresent = json.getBoolean("isPresent"),
                    receivedAt = System.currentTimeMillis()
                )
            )
        }
        
        return readings
    }
}
