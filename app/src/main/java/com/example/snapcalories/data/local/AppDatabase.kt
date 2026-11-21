package com.example.snapcalories.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The database itself: lists its tables (entities) and hands out its DAOs.
 * `abstract` → Room generates the real class. Created once for the whole app (Hilt @Singleton in
 * DatabaseModule, Session 4b) — opening a database is expensive.
 *
 * - `version = 1`: schema version. Change a table later → bump it and write a Migration.
 * - `exportSchema = false`: don't write schema JSON files (useful for big apps with migrations; overkill here).
 */
@Database(entities = [FoodEntryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodEntryDao(): FoodEntryDao
}
