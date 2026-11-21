package com.example.snapcalories.ui.base

// "Marker" interfaces: empty on purpose — they're labels, not behavior.
// They let OrbitViewModel<S : UiState, E : UiEvent, F : UiSideEffect> reject random types,
// so you can't accidentally plug e.g. a String or another screen's class into the wrong slot.

/** What the screen shows right now. Implemented by each screen's `Contract.State` data class. */
interface UiState

/** Something the user did (tap, swipe...). Implemented by each screen's `Contract.Event` sealed interface. */
interface UiEvent

/** A one-time action for the UI (navigate, toast, open camera). Implemented by `Contract.Effect`. */
interface UiSideEffect
