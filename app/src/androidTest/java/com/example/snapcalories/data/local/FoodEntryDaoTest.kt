package com.example.snapcalories.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test (app/src/androidTest): runs on a phone/emulator because Room needs real SQLite.
 * Each test gets a fresh in-memory database → tests are isolated and never touch real data.
 *
 * Run: green ▶ next to the class in Android Studio, or `./gradlew connectedDebugAndroidTest`.
 * Test data uses tiny made-up numbers (createdAt 100/200/300, epochDay 1/2) so results are easy to check by hand.
 */
@RunWith(AndroidJUnit4::class) // "run with the Android test runner" — required for device tests
class FoodEntryDaoTest {

    // lateinit = "assigned later" (in setUp), but never null when a test runs.
    private lateinit var db: AppDatabase
    private lateinit var dao: FoodEntryDao

    // Runs before EACH test → every test starts with an empty database.
    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), // a Context, needed by Room
            AppDatabase::class.java,
        ).build()
        dao = db.foodEntryDao()
    }

    // Runs after EACH test → free the database.
    @After
    fun tearDown() {
        db.close()
    }

    // Small helper so tests stay short: only the fields that matter, defaults for the rest.
    private fun entry(name: String, calories: Int = 100, createdAt: Long, epochDay: Long) =
        FoodEntryEntity(foodName = name, calories = calories, createdAt = createdAt, epochDay = epochDay)

    @Test
    fun observeByDay_returnsOnlyThatDay_newestFirst() = runTest {
        // ARRANGE: two foods on day 1 (older + newer), one on day 2.
        dao.insert(entry("Older", createdAt = 100, epochDay = 1))
        dao.insert(entry("Newer", createdAt = 200, epochDay = 1))
        dao.insert(entry("Other day", createdAt = 300, epochDay = 2))

        // ACT: .first() = take the Flow's current value and stop listening.
        val result = dao.observeByDay(1).first()

        // ASSERT: only day 1, newest (createdAt 200) first. Compare names → readable failure messages.
        assertEquals(listOf("Newer", "Older"), result.map { it.foodName })
    }

    @Test
    fun observeDailyTotals_sumsPerDay_andExcludesRowsBeforeSince() = runTest {
        // ARRANGE
        dao.insert(entry("Breakfast", calories = 100, createdAt = 100, epochDay = 1))
        dao.insert(entry("Lunch", calories = 200, createdAt = 150, epochDay = 1))
        dao.insert(entry("Snack", calories = 50, createdAt = 300, epochDay = 2))
        dao.insert(entry("Too old", calories = 999, createdAt = 10, epochDay = 0)) // before `since`

        // ACT: only rows with createdAt >= 100.
        val result = dao.observeDailyTotals(sinceMillis = 100).first()

        // ASSERT: one row per day, newest day first, day 0 excluded. Day 1 = 100 + 200.
        // (data classes compare by value, so we can compare whole DayTotalRow objects)
        assertEquals(
            listOf(
                DayTotalRow(epochDay = 2, totalCalories = 50),
                DayTotalRow(epochDay = 1, totalCalories = 300),
            ),
            result,
        )
    }

    @Test
    fun deleteOlderThan_removesOnlyOlderRows_andReturnsCount() = runTest {
        // ARRANGE: all on the same day so one observeByDay call sees them all.
        dao.insert(entry("A", createdAt = 100, epochDay = 1))
        dao.insert(entry("B", createdAt = 200, epochDay = 1))
        dao.insert(entry("C", createdAt = 300, epochDay = 1))

        // ACT: delete everything created before 250 → A and B.
        val deleted = dao.deleteOlderThan(cutoffMillis = 250)

        // ASSERT: the count is right AND the right row survived.
        assertEquals(2, deleted)
        assertEquals(listOf("C"), dao.observeByDay(1).first().map { it.foodName })
    }
}
