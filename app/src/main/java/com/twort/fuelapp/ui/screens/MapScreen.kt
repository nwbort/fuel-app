package com.twort.fuelapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.twort.fuelapp.data.model.FuelStation
import com.twort.fuelapp.viewmodel.FuelViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: FuelViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedStation by remember { mutableStateOf<FuelStation?>(null) }

    val cameraPositionState = rememberCameraPositionState()
    var centered by remember { mutableStateOf(false) }

    // Center the camera once, as soon as we have somewhere to point it.
    LaunchedEffect(uiState.userLocation, uiState.stations) {
        if (centered) return@LaunchedEffect
        val target = uiState.userLocation
            ?.let { LatLng(it.latitude, it.longitude) }
            ?: uiState.stations.firstOrNull()
                ?.takeIf { it.lat != null && it.lng != null }
                ?.let { LatLng(it.lat!!, it.lng!!) }
        if (target != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(target, 12f)
            centered = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Map", style = MaterialTheme.typography.titleLarge)
                        if (uiState.stations.isNotEmpty()) {
                            Text(
                                text = "${uiState.settings.fuelType} · ${uiState.stations.size} stations",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // userLocation is only non-null once the permission has been granted,
                // so it doubles as a safe guard for enabling the my-location layer.
                properties = MapProperties(isMyLocationEnabled = uiState.userLocation != null),
                onMapClick = { selectedStation = null },
            ) {
                uiState.stations.forEachIndexed { index, station ->
                    val lat = station.lat
                    val lng = station.lng
                    if (lat != null && lng != null) {
                        Marker(
                            state = rememberMarkerState(
                                key = station.name + station.address,
                                position = LatLng(lat, lng),
                            ),
                            title = "${station.priceCpl.roundToInt()}¢/L · " +
                                station.brand.ifBlank { station.name },
                            snippet = station.address,
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (index == 0) BitmapDescriptorFactory.HUE_GREEN
                                else BitmapDescriptorFactory.HUE_ORANGE,
                            ),
                            onClick = {
                                selectedStation = station
                                true
                            },
                        )
                    }
                }
            }

            selectedStation?.let { station ->
                SelectedStationCard(
                    station = station,
                    onNavigate = { navigateTo(context, station) },
                    onDismiss = { selectedStation = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SelectedStationCard(
    station: FuelStation,
    onNavigate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${station.priceCpl.roundToInt()}¢/L",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${"%.1f".format(station.distanceKm)} km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss")
                }
            }
            Text(
                text = station.brand.ifBlank { station.name },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = station.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Effective: ${"%.1f".format(station.effectivePriceCpl)}¢/L incl. drive cost",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onNavigate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Navigate")
            }
        }
    }
}
