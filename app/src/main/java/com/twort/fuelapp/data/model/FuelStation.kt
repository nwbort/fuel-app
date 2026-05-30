package com.twort.fuelapp.data.model

import com.google.gson.annotations.SerializedName

data class FuelStation(
    val name: String,
    val brand: String,
    val address: String,
    @SerializedName("price_cpl") val priceCpl: Double,
    @SerializedName("distance_km") val distanceKm: Double,
    @SerializedName("effective_price_cpl") val effectivePriceCpl: Double,
    val lat: Double? = null,
    val lng: Double? = null,
)

data class FuelResponse(
    val best: FuelStation,
    val alternatives: List<FuelStation>,
)

data class FuelSettings(
    val radiusKm: Int = 25,
    val fuelType: String = "U91",
    val tankLitres: Int = 50,
    val economyL100: Double = 10.0,
)

val FUEL_TYPES = listOf(
    "U91" to "Unleaded 91",
    "U95" to "Premium 95",
    "U98" to "Premium 98",
    "E10" to "E10",
    "DL" to "Diesel",
    "LPG" to "LPG",
)
