package com.example.snapcalories.data.local

/**
 * Result of a QUERY, not a table: "total kcal for one day" (used by History).
 * Room fills any plain data class whose property names match the query's column names —
 * hence `SUM(calories) AS totalCalories` in FoodEntryDao.observeDailyTotals.
 */
data class DayTotalRow(
    val epochDay: Long,
    val totalCalories: Int,
)
