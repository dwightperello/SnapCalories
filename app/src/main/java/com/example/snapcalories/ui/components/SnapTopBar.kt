package com.example.snapcalories.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.snapcalories.R
import com.example.snapcalories.ui.theme.SnapCaloriesTheme


/**
 * App-wide toolbar. Shared by Today, History, Day detail.
 * - onBack = null → no back arrow (Today is the start screen)
 * - actions = icons on the right (Today puts the History button here)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
           if(onBack != null){
               IconButton(onClick = onBack) {
                   Icon(
                       painter = painterResource(R.drawable.ic_arrow_back),
                       contentDescription = "Back"
                   )
               }
           }
        },
        actions = actions,
    )
}


@Preview(name = "Today (no back)")
@Composable
private fun SnapTopBarPreview() {
    SnapCaloriesTheme { SnapTopBar(title = "Today") }
}

@Preview(name = "History (with back)")
@Composable
private fun SnapTopBarWithBackPreview() {
    SnapCaloriesTheme { SnapTopBar(title = "History", onBack = {}) }
}