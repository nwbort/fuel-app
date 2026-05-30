package com.twort.fuelapp.data.api

import com.twort.fuelapp.data.model.FuelResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface FuelApiService {
    @GET("cheapest")
    suspend fun getCheapest(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int = 25,
        @Query("fueltype") fuelType: String = "U91",
        @Query("tank") tank: Int = 50,
        @Query("economy") economy: Double = 10.0,
    ): FuelResponse

    companion object {
        private const val BASE_URL = "https://fuel-api.nicholas-twort.workers.dev/"

        fun create(): FuelApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FuelApiService::class.java)
        }
    }
}
