package com.example.snapcalories.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.snapcalories.ui.theme.SnapCaloriesTheme

@Composable
fun CalorieTotalCard(
    totalCalories: Int,
    modifier: Modifier = Modifier,
    label: String = "Total today",
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = "$totalCalories kcal",
                style = MaterialTheme.typography.displaySmall,
            )

        }
    }
}

@Preview
@Composable
private fun CalorieTotalCardPreview() {
    SnapCaloriesTheme {
        CalorieTotalCard(totalCalories = 450)
    }
}
