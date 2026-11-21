package com.example.snapcalories.ui.today

import com.example.snapcalories.domain.model.FoodEntry
import com.example.snapcalories.ui.base.UiEvent
import com.example.snapcalories.ui.base.UiSideEffect
import com.example.snapcalories.ui.base.UiState

/**
 * The Today screen's "contract" — read this one file to know everything the screen can
 * SHOW (State), RECEIVE from the user (Event) and TRIGGER once (Effect).
 *
 * `object` = just a namespace, so names read as `TodayContract.State` and don't clash with
 * `HistoryContract.State` etc. Pattern to copy for every new screen: XxxContract { State, Event, Effect }.
 */
object TodayContract {

    /**
     * Everything on screen right now. Immutable `data class` → change it with `copy(...)`.
     * All fields have defaults so `State()` = the starting screen (passed as the ViewModel's initialState).
     */
    data class State(
        val entries: List<FoodEntry> = emptyList(), // Today's foods, shown in the list.
        val totalCalories: Int = 0,                 // Sum shown in the total card.
        val isLoading: Boolean = true,              // true at start: data not loaded yet → show a spinner.
    ) : UiState

    /**
     * What the user can do on this screen. `sealed` = a closed list → in the ViewModel's
     * `when (event)` the compiler forces you to handle every case (add one, forget it → build error).
     *   - `data object` = action with no extra info.
     *   - `data class`  = action that carries info (which entry to delete).
     */
    sealed interface Event : UiEvent {
        data object CameraClicked : Event
        data object HistoryClicked : Event
        data class DeleteEntry(val id: Long) : Event
    }

    /**
     * One-time instructions for the UI. The ViewModel never navigates itself — it posts an Effect,
     * and the Screen composable handles it (e.g. calls onNavigateToHistory()).
     */
    sealed interface Effect : UiSideEffect {
        data object NavigateToHistory : Effect
    }
}
