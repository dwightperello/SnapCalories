package com.example.snapcalories.ui.today

import com.example.snapcalories.domain.model.FoodEntry
import com.example.snapcalories.ui.base.OrbitViewModel
import com.example.snapcalories.ui.today.TodayContract.Effect
import com.example.snapcalories.ui.today.TodayContract.Event
import com.example.snapcalories.ui.today.TodayContract.State
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject

/**
 * Brain of the Today screen. Receives Events, updates State, posts Effects. No UI code here
 * (no Compose, no Context, no navigation) → it can be unit-tested on the plain JVM.
 *
 * - `@HiltViewModel`: Hilt may create this ViewModel → the screen gets it with `hiltViewModel()`.
 * - `@Inject constructor()`: "Hilt, build me with this constructor." Empty for now; in Task 4 the
 *   use cases go in here as parameters and Hilt passes them in automatically.
 * - `OrbitViewModel<State, Event, Effect>(State())`: fill the base class's 3 slots with Today's
 *   Contract types, and start from `State()` (empty list, 0 kcal, loading).
 */
@HiltViewModel
class TodayViewModel @Inject constructor() : OrbitViewModel<State, Event, Effect>(State()) {

    // Start-up work: runs once when the screen first subscribes (in tests: runOnCreate()).
    // Hard-coded for now; Task 4 replaces this with entries read from the Room database.
    override fun onCreate() {
        val entries = listOf(
            FoodEntry(id = 1, foodName = "Chicken adobo", calories = 450, createdAt = Instant.EPOCH, epochDay = 0),
            FoodEntry(id = 2, foodName = "Rice", calories = 200, createdAt = Instant.EPOCH, epochDay = 0),
            FoodEntry(id = 3, foodName = "Banana", calories = 105, createdAt = Instant.EPOCH, epochDay = 0),
        )
        setState {
            copy(
                entries = entries,
                totalCalories = entries.sumOf { it.calories },
                isLoading = false,
            )
        }
    }

    // One branch per Event. `Event` is sealed, so the compiler forces every case to be handled.
    override fun onEvent(event: Event) {
        when (event) {
            // Camera comes in Task 6. `Unit` = "do nothing, on purpose".
            Event.CameraClicked -> Unit
            // Don't navigate here — tell the Screen to do it, once.
            Event.HistoryClicked -> postEffect(Effect.NavigateToHistory)
            // `is` because DeleteEntry is a class carrying data (the id); after `is`, Kotlin
            // "smart casts" `event` so `event.id` is available. The objects above match by equality.
            is Event.DeleteEntry -> setState {
                val remaining = entries.filter { it.id != event.id }
                // Recalculate the total from the list (the source of truth) instead of subtracting,
                // so it can never drift out of sync.
                copy(entries = remaining, totalCalories = remaining.sumOf { it.calories })
            }
        }
    }
}
