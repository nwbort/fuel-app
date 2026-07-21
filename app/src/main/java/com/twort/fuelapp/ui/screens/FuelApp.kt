package com.twort.fuelapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.twort.fuelapp.viewmodel.FuelViewModel

private enum class FuelTab(val label: String, val icon: ImageVector) {
    List("List", Icons.Rounded.FormatListBulleted),
    Map("Map", Icons.Rounded.Map),
}

@Composable
fun FuelApp(viewModel: FuelViewModel) {
    var selectedTab by remember { mutableStateOf(FuelTab.List) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                FuelTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        // Only consume the bottom inset here; each screen keeps its own top bar.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
        ) {
            when (selectedTab) {
                FuelTab.List -> MainScreen(viewModel = viewModel)
                FuelTab.Map -> MapScreen(viewModel = viewModel)
            }
        }
    }
}
