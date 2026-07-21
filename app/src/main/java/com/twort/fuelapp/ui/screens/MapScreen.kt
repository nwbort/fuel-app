package com.twort.fuelapp.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twort.fuelapp.data.model.FuelStation
import com.twort.fuelapp.viewmodel.FuelViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: FuelViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedStation by remember { mutableStateOf<FuelStation?>(null) }

    // Configure osmdroid before any MapView is created.
    remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", 0))
            userAgentValue = context.packageName
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.tertiary.toArgb()

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
        }
    }

    // Tie the MapView to the composition lifecycle.
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Center the map once, when we first have data to show.
    var centered by remember { mutableStateOf(false) }

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
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    map.overlays.clear()

                    uiState.stations.forEachIndexed { index, station ->
                        val lat = station.lat
                        val lng = station.lng
                        if (lat != null && lng != null) {
                            val marker = Marker(map).apply {
                                position = GeoPoint(lat, lng)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "${station.priceCpl.roundToInt()}¢/L · " +
                                    station.brand.ifBlank { station.name }
                                snippet = station.address
                                icon = tintedMarker(
                                    map,
                                    if (index == 0) primaryColor else secondaryColor,
                                )
                                relatedObject = station
                                setOnMarkerClickListener { m, _ ->
                                    selectedStation = m.relatedObject as? FuelStation
                                    map.controller.animateTo(m.position)
                                    true
                                }
                            }
                            map.overlays.add(marker)
                        }
                    }

                    uiState.userLocation?.let { loc ->
                        val here = GeoPoint(loc.latitude, loc.longitude)
                        if (!centered) {
                            map.controller.setCenter(here)
                            centered = true
                        }
                    }
                    if (!centered) {
                        uiState.stations.firstOrNull()
                            ?.let { s ->
                                val lat = s.lat
                                val lng = s.lng
                                if (lat != null && lng != null) {
                                    map.controller.setCenter(GeoPoint(lat, lng))
                                    centered = true
                                }
                            }
                    }

                    map.invalidate()
                },
            )

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

private fun tintedMarker(map: MapView, color: Int): Drawable {
    val base = ContextCompat.getDrawable(
        map.context,
        org.osmdroid.library.R.drawable.marker_default,
    )!!.mutate()
    val wrapped = DrawableCompat.wrap(base)
    DrawableCompat.setTint(wrapped, color)
    return wrapped
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
