package com.example.snapcalories.ui.today

import com.example.snapcalories.domain.model.FoodEntry
import com.example.snapcalories.ui.today.TodayContract.Effect
import com.example.snapcalories.ui.today.TodayContract.Event
import com.example.snapcalories.ui.today.TodayContract.State
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import java.time.Instant

/**
 * Unit tests for TodayViewModel — plain JVM (app/src/test), no phone or emulator needed.
 * Pattern: runTest { viewModel.testWithInternalState(this) { ACT → ASSERT } }
 *
 * orbit-test records every State/Effect the ViewModel emits; each expect…/await… call consumes the NEXT one,
 * in order. Anything left unconsumed at the end fails the test (catches unexpected extra emissions).
 */
class TodayViewModelTest {

    // Backticks let a test name be a readable sentence — it shows up like that in the test report.
    @Test
    fun `init loads hardcoded entries and total`() = runTest {
        TodayViewModel().testWithInternalState(this) {
            // ACT: run the ViewModel's start-up work (OrbitViewModel.onCreate). Not automatic in tests.
            runOnCreate()

            // We don't retype the 3 hard-coded entries here; we check the RULES instead.
            // awaitInternalState() = "give me the next state" so we can assert on it ourselves.
            val loaded = awaitInternalState()
            assertEquals(3, loaded.entries.size)
            assertEquals(loaded.entries.sumOf { it.calories }, loaded.totalCalories)
            assertFalse(loaded.isLoading)
        }
    }

    @Test
    fun `HistoryClicked posts NavigateToHistory`() = runTest {
        TodayViewModel().testWithInternalState(this) {
            // ACT: do what the UI does when the user taps History.
            containerHost.onEvent(Event.HistoryClicked)
            // ASSERT: the ViewModel doesn't navigate itself — it posts a one-time Effect for the Screen.
            expectSideEffect(Effect.NavigateToHistory)
        }
    }

    @Test
    fun `DeleteEntry removes entry and recalculates total`() = runTest {
        // ARRANGE: start from a known, already-loaded state (skips start-up work entirely).
        val loaded = State(
            entries = listOf(
                FoodEntry(id = 1, foodName = "Rice", calories = 100, createdAt = Instant.EPOCH, epochDay = 0),
                FoodEntry(id = 2, foodName = "Adobo", calories = 200, createdAt = Instant.EPOCH, epochDay = 0),
            ),
            totalCalories = 300,
            isLoading = false,
        )

        TodayViewModel().testWithInternalState(this, initialState = loaded) {
            // ACT: user deletes entry 1.
            containerHost.onEvent(Event.DeleteEntry(id = 1))
            // ASSERT: next state = previous state with only these fields changed.
            // Inside copy(...), `entries` is still the OLD list → filter out id 1. 300 - 100 = 200.
            expectInternalState {
                copy(entries = entries.filter { it.id != 1L }, totalCalories = 200)
            }
        }
    }
}
