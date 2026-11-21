package com.example.snapcalories.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.snapcalories.R // OUR app's R (our drawables) — not androidx.compose.ui.R
import com.example.snapcalories.domain.model.FoodEntry
import com.example.snapcalories.ui.theme.SnapCaloriesTheme
import java.time.Instant

/**
 * One food in a list: name, kcal, delete button. Shared by Today and Day detail.
 * Stateless: data in (`entry`), clicks out (`onDelete`) — the caller decides what "delete" means.
 */
@Composable
fun FoodEntryRow(
    entry: FoodEntry,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ListItem = Material's standard row: headline, supporting text, trailing slot on the right.
    ListItem(
        modifier = modifier,
        headlineContent = { Text(entry.foodName) },
        supportingContent = { Text("${entry.calories} kcal") },
        trailingContent = {
            IconButton(onClick = onDelete) {
                // contentDescription = what TalkBack reads aloud; say what the button does.
                Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete ${entry.foodName}")
            }
        },
    )
}

@Preview
@Composable
private fun FoodEntryRowPreview() {
    SnapCaloriesTheme {
        FoodEntryRow(
            entry = FoodEntry(id = 1, foodName = "Chicken adobo", calories = 450, createdAt = Instant.EPOCH, epochDay = 0),
            onDelete = {},
        )
    }
}
