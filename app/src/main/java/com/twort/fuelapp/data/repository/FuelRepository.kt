package com.twort.fuelapp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.twort.fuelapp.data.api.FuelApiService
import com.twort.fuelapp.data.model.FuelResponse
import com.twort.fuelapp.data.model.FuelSettings
import com.twort.fuelapp.data.model.FuelStation
import com.twort.fuelapp.widget.FuelWidget
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class FuelRepository(private val api: FuelApiService = FuelApiService.create()) {

    suspend fun fetch(
        lat: Double,
        lng: Double,
        settings: FuelSettings,
    ): Result<FuelResponse> = runCatching {
        api.getCheapest(lat, lng, settings.radiusKm, settings.fuelType, settings.tankLitres, settings.economyL100)
    }

    suspend fun loadSettings(context: Context): FuelSettings {
        val prefs = context.settingsDataStore.data.first()
        val defaults = FuelSettings()
        return FuelSettings(
            radiusKm = prefs[SETTINGS_KEY_RADIUS] ?: defaults.radiusKm,
            fuelType = prefs[SETTINGS_KEY_FUEL_TYPE] ?: defaults.fuelType,
            tankLitres = prefs[SETTINGS_KEY_TANK] ?: defaults.tankLitres,
            economyL100 = prefs[SETTINGS_KEY_ECONOMY] ?: defaults.economyL100,
        )
    }

    suspend fun saveSettings(context: Context, settings: FuelSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[SETTINGS_KEY_RADIUS] = settings.radiusKm
            prefs[SETTINGS_KEY_FUEL_TYPE] = settings.fuelType
            prefs[SETTINGS_KEY_TANK] = settings.tankLitres
            prefs[SETTINGS_KEY_ECONOMY] = settings.economyL100
        }
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
        private val SETTINGS_KEY_RADIUS = intPreferencesKey("settings_radius")
        private val SETTINGS_KEY_FUEL_TYPE = stringPreferencesKey("settings_fuel_type")
        private val SETTINGS_KEY_TANK = intPreferencesKey("settings_tank")
        private val SETTINGS_KEY_ECONOMY = doublePreferencesKey("settings_economy")

        const val WIDGET_KEY_NAME = "station_name"
        const val WIDGET_KEY_BRAND = "station_brand"
        const val WIDGET_KEY_ADDRESS = "station_address"
        const val WIDGET_KEY_PRICE = "station_price"
        const val WIDGET_KEY_DISTANCE = "station_distance"
        const val WIDGET_KEY_LAT = "station_lat"
        const val WIDGET_KEY_LNG = "station_lng"
    }
}
