package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AudioSettingsEntity::class,
        AudioPresetEntity::class,
        RoutingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioDao(): AudioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bro_audio_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.audioDao())
                }
            }
        }

        suspend fun populateInitialData(dao: AudioDao) {
            // Save local default audio settings
            dao.saveSettings(AudioSettingsEntity())

            // Save default routing layout
            dao.saveRouting(RoutingEntity())

            // Initial System Presets
            // 7 bands: 60Hz, 150Hz, 400Hz, 1kHz, 2.5kHz, 6kHz, 15kHz
            // 15 bands: 25Hz, 40Hz, 63Hz, 100Hz, 160Hz, 250Hz, 400Hz, 630Hz, 1k, 1.6k, 2.5k, 4k, 6.3k, 10k, 16kHz
            // 31 bands: 20Hz, 25Hz, 31.5, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1k, 1.25k, 1.6k, 2k, 2.5k, 3.15k, 4k, 5k, 6.3k, 8k, 10k, 12.5k, 16k, 20kHz
            
            val presets = listOf(
                AudioPresetEntity(
                    name = "Flat",
                    isSystem = true,
                    gains7 = "0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                    gains15 = "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                    gains31 = List(31) { "0.0" }.joinToString(",")
                ),
                AudioPresetEntity(
                    name = "Dangdut",
                    isSystem = true,
                    gains7 = "5.5,4.0,-2.0,1.0,-1.0,4.5,6.0",
                    gains15 = "6.0,5.5,4.5,3.0,1.0,-1.5,-2.0,-1.0,0.5,1.5,2.5,4.0,5.0,5.5,6.0",
                    gains31 = "6.0,6.0,5.5,5.0,4.5,4.0,3.0,2.0,1.0,0.0,-1.0,-1.5,-2.0,-2.0,-1.5,-1.0,0.0,0.5,1.0,1.5,2.0,2.5,3.5,4.0,4.5,5.0,5.5,5.5,6.0,6.0,6.0"
                ),
                AudioPresetEntity(
                    name = "Sholawat",
                    isSystem = true,
                    gains7 = "-1.0,2.0,3.5,4.5,4.0,2.0,1.0",
                    gains15 = "-2.0,-1.0,1.0,2.5,3.5,4.0,4.5,4.5,4.0,3.5,3.0,2.5,2.0,1.5,1.0",
                    gains31 = "-2.0,-2.0,-1.5,-1.0,0.0,1.0,2.0,2.5,3.0,3.5,4.0,4.2,4.5,4.5,4.5,4.5,4.2,4.0,3.8,3.5,3.2,3.0,2.8,2.5,2.2,2.0,1.8,1.5,1.2,1.0,1.0"
                ),
                AudioPresetEntity(
                    name = "Rock",
                    isSystem = true,
                    gains7 = "4.0,2.5,-1.5,0.0,1.5,3.0,4.5",
                    gains15 = "5.0,4.5,3.5,2.0,1.0,-1.0,-1.5,-0.5,0.5,1.5,2.0,3.0,3.5,4.5,5.0",
                    gains31 = "5.0,5.0,4.5,4.0,3.5,3.0,2.0,1.5,1.0,0.0,-1.0,-1.5,-1.8,-1.5,-1.0,-0.5,0.0,0.5,1.0,1.5,1.8,2.0,2.5,3.0,3.5,3.8,4.2,4.5,4.8,5.0,5.0"
                ),
                AudioPresetEntity(
                    name = "EDM",
                    isSystem = true,
                    gains7 = "6.5,5.0,0.0,-2.0,1.5,4.0,6.0",
                    gains15 = "7.0,6.5,5.5,4.0,2.0,0.0,-1.5,-2.5,-1.5,0.5,2.0,3.5,5.0,6.0,7.0",
                    gains31 = "7.0,7.0,6.5,6.0,5.5,5.0,4.0,3.0,2.0,1.0,0.0,-1.0,-1.5,-2.0,-2.5,-2.5,-2.0,-1.0,0.0,0.5,1.0,1.8,2.5,3.2,4.0,4.8,5.5,5.8,6.2,6.8,7.0"
                ),
                AudioPresetEntity(
                    name = "Vocal",
                    isSystem = true,
                    gains7 = "-4.0,-2.0,1.5,4.0,3.5,1.5,-1.0",
                    gains15 = "-5.0,-4.0,-3.0,-1.5,0.5,1.5,2.5,3.5,4.0,3.8,3.2,2.5,1.5,-0.5,-1.5",
                    gains31 = "-5.0,-5.0,-4.5,-4.0,-3.0,-2.0,-1.0,0.0,0.5,1.0,1.8,2.5,3.0,3.5,4.0,4.0,3.9,3.8,3.5,3.0,2.8,2.5,2.0,1.8,1.2,0.5,0.0,-0.5,-1.0,-1.5,-2.0"
                ),
                AudioPresetEntity(
                    name = "Pop",
                    isSystem = true,
                    gains7 = "2.0,1.5,0.0,1.0,2.5,1.5,-1.0",
                    gains15 = "2.5,2.0,1.5,1.0,0.5,0.0,0.5,1.0,1.8,2.2,2.5,2.0,1.5,0.5,-1.0",
                    gains31 = "2.5,2.5,2.2,2.0,1.8,1.5,1.2,1.0,0.8,0.5,0.2,0.0,0.2,0.5,0.8,1.0,1.2,1.5,1.8,2.0,2.2,2.5,2.4,2.2,2.0,1.8,1.5,1.0,0.5,-0.5,-1.0"
                )
            )

            for (preset in presets) {
                dao.savePreset(preset)
            }
        }
    }
}
