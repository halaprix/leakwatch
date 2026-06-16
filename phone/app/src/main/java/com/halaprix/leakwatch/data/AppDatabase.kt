package com.halaprix.leakwatch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for LeakWatch phone app.
 * Stores battery readings received from the watch.
 */
@Database(
    entities = [BatteryReading::class, DailySummary::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun batteryReadingDao(): BatteryReadingDao
    abstract fun dailySummaryDao(): DailySummaryDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "leakwatch.db"
                )
                .fallbackToDestructiveMigration() // TODO: proper migrations in v1.0.0
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
