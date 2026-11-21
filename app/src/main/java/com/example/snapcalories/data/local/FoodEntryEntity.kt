package com.example.snapcalories.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One ROW of the `food_entries` table, as stored in SQLite. Each property = one column.
 *
 * Not the same as domain `FoodEntry`: this is the storage shape (SQLite only knows numbers/text, so
 * `createdAt` is epoch millis, not `Instant`). The repository converts Entity ↔ domain, so screens never
 * see this class and a DB change never ripples into the UI.
 *
 * `indices = [Index("epochDay")]` = a lookup index (like a book's index) → "all foods of day X" is fast.
 */
@Entity(tableName = "food_entries", indices = [Index("epochDay")])
data class FoodEntryEntity(
    // autoGenerate + default 0 → when inserting, pass id = 0 and Room picks the next free id.
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodName: String,
    val calories: Int,
    val createdAt: Long, // epoch millis — drives the 30-day cleanup
    val epochDay: Long,  // local calendar day — drives "today" and per-day grouping
)
