package com.example.snapcalories.domain.model

import java.time.Instant // NOT kotlin.time.Instant — our Clock, Room and LocalDate all use java.time.

/**
 * One food you ate — the core thing the app tracks.
 *
 * Lives in `domain/` = "what the app is about". Plain Kotlin only: no Android, no Room, no Compose
 * imports. The database (data layer) and the screens (ui layer) both convert to/from this class,
 * so it never changes just because storage or UI changes.
 */
data class FoodEntry(
    val id: Long,           // Unique id; Room auto-generates it as a Long. Used to delete one entry.
    val foodName: String,   // e.g. "Chicken adobo"
    val calories: Int,      // kcal
    val createdAt: Instant, // Exact moment it was saved (a point on the timeline, no timezone).
    val epochDay: Long,     // The local calendar day as a number (days since 1970-01-01).
                            // Makes "entries for today" a simple number comparison.
)
