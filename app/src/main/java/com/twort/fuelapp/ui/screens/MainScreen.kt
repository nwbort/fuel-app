package com.twort.fuelapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twort.fuelapp.data.model.FUEL_TYPES
import com.twort.fuelapp.data.model.FuelSettings
import com.twort.fuelapp.data.model.FuelStation
import com.twort.fuelapp.ui.components.StationCard
import com.twort.fuelapp.viewmodel.FuelViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FuelViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    // The list is ordered by price, so the physically closest station is a separate pick.
    val nearest = remember(uiState.stations) {
        uiState.stations.minByOrNull { it.distanceKm }
    }

    val scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            nearest?.let { station ->
                ExtendedFloatingActionButton(
                    onClick = { navigateTo(context, station) },
                    icon = { Icon(Icons.Rounded.NearMe, contentDescription = null) },
                    text = { Text("Nearest · ${"%.1f".format(station.distanceKm)} km") },
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cheapest Fuel",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (uiState.stations.isNotEmpty()) {
                            Text(
                                text = "${uiState.settings.fuelType} · ${uiState.settings.radiusKm} km radius",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "content",
            ) { state ->
                when {
                    state.error != null && state.stations.isEmpty() -> ErrorState(
                        message = state.error,
                        onRetry = { viewModel.refresh() },
                    )
                    state.stations.isEmpty() && !state.isLoading -> EmptyState()
                    else -> StationList(
                        stations = state.stations,
                        onNavigate = { navigateTo(context, it) },
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = uiState.settings,
            onDismiss = { showSettings = false },
            onApply = { viewModel.updateSettings(it) },
        )
    }
}

@Composable
private fun StationList(
    stations: List<FuelStation>,
    onNavigate: (FuelStation) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        stations.firstOrNull()?.let { best ->
            item(key = "best") {
                BestStationCard(station = best, onNavigate = { onNavigate(best) })
                Spacer(Modifier.height(4.dp))
            }
        }

        if (stations.size > 1) {
            item(key = "alt-header") {
                Text(
                    text = "Alternatives",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            itemsIndexed(
                stations.drop(1),
                key = { _, s -> s.name + s.address },
            ) { index, station ->
                StationCard(
                    station = station,
                    rank = index + 1,
                    onNavigate = { onNavigate(station) },
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun BestStationCard(station: FuelStation, onNavigate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "★ Best",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${"%.1f".format(station.distanceKm)} km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${station.priceCpl.roundToInt()}¢/L",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                lineHeight = 52.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = station.brand.ifBlank { station.name },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Text(
                text = station.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Effective: ${"%.1f".format(station.effectivePriceCpl)}¢/L incl. drive cost",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNavigate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(Icons.Rounded.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Navigate")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⛽", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Pull down to find fuel nearby",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onRetry) {
                Text("Try again")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    settings: FuelSettings,
    onDismiss: () -> Unit,
    onApply: (FuelSettings) -> Unit,
) {
    var radius by remember { mutableStateOf(settings.radiusKm.toFloat()) }
    var fuelType by remember { mutableStateOf(settings.fuelType) }
    var tank by remember { mutableStateOf(settings.tankLitres.toFloat()) }
    var economy by remember { mutableStateOf(settings.economyL100.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Search Settings", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Close")
                }
            }

            SliderSetting(
                label = "Radius: ${radius.roundToInt()} km",
                value = radius,
                onValueChange = { radius = it },
                range = 5f..100f,
                steps = 18,
            )
            SliderSetting(
                label = "Tank size: ${tank.roundToInt()} L",
                value = tank,
                onValueChange = { tank = it },
                range = 20f..120f,
                steps = 19,
            )
            SliderSetting(
                label = "Economy: ${"%.1f".format(economy)} L/100km",
                value = economy,
                onValueChange = { economy = it },
                range = 4f..20f,
                steps = 31,
            )

            Text("Fuel Type", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FUEL_TYPES.forEach { (code, _) ->
                    val isSelected = fuelType == code
                    Surface(
                        onClick = { fuelType = code },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onApply(
                        FuelSettings(
                            radiusKm = radius.roundToInt(),
                            fuelType = fuelType,
                            tankLitres = tank.roundToInt(),
                            economyL100 = economy.toDouble(),
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Apply & Search")
            }
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
    }
}

fun navigateTo(context: Context, station: FuelStation) {
    val uri = if (station.lat != null && station.lng != null) {
        Uri.parse("google.navigation:q=${station.lat},${station.lng}")
    } else {
        Uri.parse("geo:0,0?q=${Uri.encode(station.address)}")
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?daddr=${Uri.encode(station.address)}"))
        )
    }
}
