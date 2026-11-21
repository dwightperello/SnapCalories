package com.example.snapcalories.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object: the list of things you can ask/tell the `food_entries` table.
 * You write only the interface + SQL; Room generates the implementation at build time (KSP)
 * and checks the SQL then → a typo is a BUILD error, not a crash on the user's phone.
 *
 * - Reads return `Flow` = a LIVE feed: Room re-emits the new list whenever the table changes,
 *   so screens update by themselves after an insert/delete.
 * - Writes are `suspend` = run in a coroutine, off the main thread (disk is slow).
 * - `:name` in the SQL = the function parameter with that name.
 */
@Dao
interface FoodEntryDao {

    // One day's foods, newest first.
    @Query("SELECT * FROM food_entries WHERE epochDay = :epochDay ORDER BY createdAt DESC")
    fun observeByDay(epochDay: Long): Flow<List<FoodEntryEntity>>

    // Total kcal per day since a moment (History). `AS totalCalories` must match DayTotalRow's property.
    // Triple-quoted string = a multi-line SQL that stays readable.
    @Query(
        """
        SELECT epochDay, SUM(calories) AS totalCalories
        FROM food_entries
        WHERE createdAt >= :sinceMillis
        GROUP BY epochDay
        ORDER BY epochDay DESC
        """
    )
    fun observeDailyTotals(sinceMillis: Long): Flow<List<DayTotalRow>>

    // Returns the new row's generated id.
    @Insert
    suspend fun insert(entity: FoodEntryEntity): Long

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    // 30-day cleanup (Task 8). Returns how many rows were deleted.
    @Query("DELETE FROM food_entries WHERE createdAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int
}
