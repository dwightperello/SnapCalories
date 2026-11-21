package com.example.snapcalories.ui.base

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/**
 * Base class for EVERY screen's ViewModel. Write the MVI plumbing once, reuse it everywhere.
 *
 * The MVI loop:
 *   UI ──onEvent(Event)──▶ ViewModel ──State──▶ UI redraws (what's on screen)
 *                                    └─Effect─▶ UI acts once (navigate, toast...)
 *
 * Generics — each screen fills them with its own Contract types:
 *   S = State, E = Event, F = side eFfect
 *   e.g. TodayViewModel : OrbitViewModel<TodayContract.State, TodayContract.Event, TodayContract.Effect>
 *
 * `OrbitContainerHost<S, S, F>` (Orbit 12; older versions call it `ContainerHost<S, F>`):
 *   makes this class work with Orbit. The middle S = "state shown to UI" — Orbit allows it to differ
 *   from the internal state, we don't need that, so both are S.
 *
 * Usage in a child ViewModel:
 *   override fun onEvent(event: E) = when (event) {
 *       Event.Refresh -> setState { copy(isLoading = true) }
 *       Event.Back    -> postEffect(Effect.GoBack)
 *   }
 */
abstract class OrbitViewModel<S : UiState, E : UiEvent, F : UiSideEffect>(
    initialState: S, // What the screen looks like before anything happens, e.g. State() with defaults.
) : ViewModel(), OrbitContainerHost<S, S, F> {

    // Orbit's "store": holds the current State + an outbox of Effects.
    // `orbitContainer` (from orbit-viewmodel) ties it to viewModelScope → its work auto-cancels when
    // the ViewModel is destroyed. `final` = children can't replace it; every screen gets it the same way.
    // The UI reads it with `viewModel.collectAsState()` and `viewModel.collectSideEffect { }`.
    final override val container: OrbitContainer<S, S, F> = orbitContainer(initialState,onCreate = { onCreate() })

    protected open fun onCreate() {}

    // THE single entry point from the UI. Screens only ever call `viewModel.onEvent(...)`.
    // `abstract` = each screen must decide what each of its events does (usually a `when`).
    abstract fun onEvent(event: E)

    // Change the screen state. `S.() -> S` = a block that runs ON the current state and returns a new
    // one, which is why children can write `setState { copy(total = 5) }` without `state.`.
    // Orbit rules: `intent { }` starts a unit of work; `reduce { }` (only valid inside it) swaps the State.
    // `protected` = only child ViewModels may call it, never the UI.
    protected fun setState(reducer: S.() -> S) {
        intent { reduce { state.reducer() } }
    }

    // Send a one-time Effect. Why not put it in State? State is re-read on rotation; a "navigate" in State
    // would navigate again. Effects are delivered once.
    protected fun postEffect(effect: F) {
        intent { postSideEffect(effect) }
    }
}
