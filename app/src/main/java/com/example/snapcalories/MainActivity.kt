package com.example.snapcalories

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.snapcalories.ui.theme.SnapCaloriesTheme
import com.example.snapcalories.ui.today.TodayScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity of the app. In Compose apps the Activity is just a "host": it sets the theme and
 * shows the first screen (later: a NavHost that switches between screens). No business logic here.
 *
 * `@AndroidEntryPoint` lets Hilt inject things into this Activity and — important — into the
 * `hiltViewModel()` calls of the screens it shows. Every Activity that uses Hilt needs it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status bar / navigation bar (modern Android look). Scaffold's innerPadding
        // then tells us how much space those bars take so content isn't hidden under them.
        enableEdgeToEdge()
        // setContent = "from here on, the UI is Compose" (replaces setContentView(R.layout...)).
        setContent {
            // Wrap everything in the theme so all Material components get our colors/typography.
            SnapCaloriesTheme {
                // The start screen. It has its own Scaffold (toolbar, FAB), so none here.
                // Task 5 replaces this with a NavHost; the lambda then becomes navController.navigate(History).
                TodayScreen(onNavigateToHistory = { /* Task 5 */ })
            }
        }
    }
}
