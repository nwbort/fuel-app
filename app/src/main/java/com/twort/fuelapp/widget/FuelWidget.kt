package com.twort.fuelapp.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.material3.ColorProviders
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.twort.fuelapp.MainActivity
import com.twort.fuelapp.data.repository.FuelRepository
import kotlin.math.roundToInt

class FuelWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
        val name = prefs[stringPreferencesKey(FuelRepository.WIDGET_KEY_NAME)] ?: ""
        val brand = prefs[stringPreferencesKey(FuelRepository.WIDGET_KEY_BRAND)] ?: ""
        val address = prefs[stringPreferencesKey(FuelRepository.WIDGET_KEY_ADDRESS)] ?: ""
        val price = prefs[stringPreferencesKey(FuelRepository.WIDGET_KEY_PRICE)]?.toDoubleOrNull()
        val distance = prefs[stringPreferencesKey(FuelRepository.WIDGET_KEY_DISTANCE)]?.toDoubleOrNull()

        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .padding(16.dp)
                    .clickable(actionRunCallback<OpenAppCallback>()),
                contentAlignment = Alignment.TopStart,
            ) {
                if (name.isEmpty()) {
                    EmptyState()
                } else {
                    StationContent(
                        name = name,
                        brand = brand,
                        address = address,
                        priceCpl = price,
                        distanceKm = distance,
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyState() {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "⛽",
                style = TextStyle(fontSize = 32.sp),
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = "Tap to find fuel",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                ),
            )
        }
    }

    @Composable
    private fun StationContent(
        name: String,
        brand: String,
        address: String,
        priceCpl: Double?,
        distanceKm: Double?,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
        ) {
            Text(
                text = "⛽ Cheapest Nearby",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = if (priceCpl != null) "${priceCpl.roundToInt()}¢/L" else "--",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = brand.ifBlank { name },
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Text(
                text = address,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
            if (distanceKm != null) {
                Text(
                    text = "${"%.1f".format(distanceKm)} km away",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primary)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable(actionRunCallback<NavigateCallback>()),
                ) {
                    Text(
                        text = "Navigate →",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

class NavigateCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val prefs = androidx.glance.appwidget.state.getAppWidgetState(
            context,
            PreferencesGlanceStateDefinition,
            glanceId,
        )
        val address = prefs[stringPreferencesKey(FuelRepository.WIDGET_KEY_ADDRESS)] ?: return
        val lat = prefs[stringPreferencesKey(FuelRepository.WIDGET_KEY_LAT)]?.toDoubleOrNull()
        val lng = prefs[stringPreferencesKey(FuelRepository.WIDGET_KEY_LNG)]?.toDoubleOrNull()

        val uri = if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            Uri.parse("google.navigation:q=$lat,$lng")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val fallback = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://maps.google.com/?daddr=${Uri.encode(address)}")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(fallback)
        }
    }
}

class OpenAppCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }
}

class FuelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = FuelWidget()
}
