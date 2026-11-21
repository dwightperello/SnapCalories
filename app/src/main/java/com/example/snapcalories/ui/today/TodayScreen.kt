package com.example.snapcalories.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.snapcalories.R
import com.example.snapcalories.domain.model.FoodEntry
import com.example.snapcalories.ui.components.CalorieTotalCard
import com.example.snapcalories.ui.components.FoodEntryRow
import com.example.snapcalories.ui.components.SnapTopBar
import com.example.snapcalories.ui.theme.SnapCaloriesTheme
import com.example.snapcalories.ui.today.TodayContract.Effect
import com.example.snapcalories.ui.today.TodayContract.Event
import com.example.snapcalories.ui.today.TodayContract.State
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.time.Instant

// Pattern for EVERY screen file: XxxScreen (connected) + XxxContent (pure UI) + @Preview(s) of XxxContent.

/**
 * CONNECTED screen: gets the ViewModel (Hilt), reads State, turns Effects into navigation.
 * The only Today composable that knows the ViewModel exists. Called from MainActivity (later: the NavHost).
 *
 * Navigation comes IN as a lambda (`onNavigateToHistory`) — this screen doesn't know HOW to navigate,
 * only WHEN. The caller (NavHost in Task 5) plugs in `navController.navigate(History)`.
 */
@Composable
fun TodayScreen(
    onNavigateToHistory: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(), // Hilt creates it (and keeps it across rotation).
) {
    // State: `by` unwraps Compose's State<T> → `state` is a plain TodayContract.State that's always current.
    // Every setState in the ViewModel → this value changes → Compose redraws what depends on it.
    val state by viewModel.collectAsState()

    // Effects: run this block ONCE per effect the ViewModel posts.
    viewModel.collectSideEffect { effect ->
        when (effect) {
            Effect.NavigateToHistory -> onNavigateToHistory()
        }
    }

    // `viewModel::onEvent` = pass the function itself (same as `{ event -> viewModel.onEvent(event) }`).
    TodayContent(state = state, onEvent = viewModel::onEvent)
}

/**
 * PURE UI: draws whatever State it's given, reports clicks as Events. No ViewModel → previewable.
 */
@Composable
fun TodayContent(
    state: State,
    onEvent: (Event) -> Unit,
) {
    // Scaffold = layout slots: toolbar on top, floating button bottom-right, content in the middle.
    Scaffold(
        topBar = {
            SnapTopBar(
                title = "Today",
                // `actions` = icons on the right side of the toolbar.
                actions = {
                    IconButton(onClick = { onEvent(Event.HistoryClicked) }) {
                        Icon(painterResource(R.drawable.ic_history), contentDescription = "History")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(Event.CameraClicked) }) {
                Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = "Snap a meal")
            }
        },
    ) { innerPadding ->
        // innerPadding = space taken by the toolbar/system bars. Apply it in EVERY branch, or content
        // slides under the toolbar.
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.entries.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("No food logged yet")
                }
            }

            else -> {
                // LazyColumn = scrolling list that only builds the rows on screen (RecyclerView, no adapter).
                // contentPadding (not Modifier.padding) → the list can still scroll under the bars nicely.
                LazyColumn(contentPadding = innerPadding) {
                    // item { } = one single row (the total card at the top).
                    item {
                        CalorieTotalCard(
                            totalCalories = state.totalCalories,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    // items(list) { } = one row per entry. `key` tells Compose which row is which, so
                    // deleting one removes THAT row instead of redrawing them all.
                    items(state.entries, key = { it.id }) { entry ->
                        FoodEntryRow(
                            entry = entry,
                            onDelete = { onEvent(Event.DeleteEntry(entry.id)) },
                        )
                    }
                }
            }
        }
    }
}

// Previews: give TodayContent a fake State — no ViewModel, no Hilt needed. One preview per case.
// `showSystemUi = true` shows it inside a phone frame.

@Preview(name = "Loaded", showSystemUi = true)
@Composable
private fun TodayContentLoadedPreview() {
    val entries = listOf(
        FoodEntry(id = 1, foodName = "Chicken adobo", calories = 450, createdAt = Instant.EPOCH, epochDay = 0),
        FoodEntry(id = 2, foodName = "Rice", calories = 200, createdAt = Instant.EPOCH, epochDay = 0),
    )
    SnapCaloriesTheme {
        TodayContent(
            state = State(entries = entries, totalCalories = 650, isLoading = false),
            onEvent = {},
        )
    }
}

@Preview(name = "Empty", showSystemUi = true)
@Composable
private fun TodayContentEmptyPreview() {
    SnapCaloriesTheme {
        TodayContent(state = State(isLoading = false), onEvent = {})
    }
}
