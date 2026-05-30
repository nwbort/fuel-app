package com.twort.fuelapp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.twort.fuelapp.data.api.FuelApiService
import com.twort.fuelapp.data.model.FuelResponse
import com.twort.fuelapp.data.model.FuelSettings
import com.twort.fuelapp.data.model.FuelStation
import com.twort.fuelapp.widget.FuelWidget

class FuelRepository(private val api: FuelApiService = FuelApiService.create()) {

    suspend fun fetch(
        lat: Double,
        lng: Double,
        settings: FuelSettings,
    ): Result<FuelResponse> = runCatching {
        api.getCheapest(lat, lng, settings.radiusKm, settings.fuelType, settings.tankLitres, settings.economyL100)
    }

    suspend fun updateWidget(context: Context, best: FuelStation) {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(FuelWidget::class.java)
        ids.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[stringPreferencesKey(WIDGET_KEY_NAME)] = best.name
                    this[stringPreferencesKey(WIDGET_KEY_BRAND)] = best.brand
                    this[stringPreferencesKey(WIDGET_KEY_ADDRESS)] = best.address
                    this[stringPreferencesKey(WIDGET_KEY_PRICE)] = best.priceCpl.toString()
                    this[stringPreferencesKey(WIDGET_KEY_DISTANCE)] = best.distanceKm.toString()
                    this[stringPreferencesKey(WIDGET_KEY_LAT)] = best.lat?.toString() ?: ""
                    this[stringPreferencesKey(WIDGET_KEY_LNG)] = best.lng?.toString() ?: ""
                }
            }
            FuelWidget().update(context, id)
        }
    }

    companion object {
        const val WIDGET_KEY_NAME = "station_name"
        const val WIDGET_KEY_BRAND = "station_brand"
        const val WIDGET_KEY_ADDRESS = "station_address"
        const val WIDGET_KEY_PRICE = "station_price"
        const val WIDGET_KEY_DISTANCE = "station_distance"
        const val WIDGET_KEY_LAT = "station_lat"
        const val WIDGET_KEY_LNG = "station_lng"
    }
}
