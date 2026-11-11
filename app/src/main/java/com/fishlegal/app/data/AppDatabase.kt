package com.fishlegal.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Species::class, RegulationZone::class, RegulationRule::class, CatchLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun speciesDao(): SpeciesDao
    abstract fun regulationDao(): RegulationDao
    abstract fun catchLogDao(): CatchLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fishlegal.db"
                ).build().also { INSTANCE = it }
            }
    }
}
