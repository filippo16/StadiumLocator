package com.example.stadiumlocator.repository

import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.stadiumlocator.model.Stadium
import com.example.stadiumlocator.network.ApiService
import com.example.stadiumlocator.network.RetrofitClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StadiumRepository(application: Application) {

    sealed class Result {
        data class Success(val stadiums: List<Stadium>) : Result()
        data class Error(val message: String) : Result()
    }

    sealed class StadiumResult {
        data class Add(val stadium: Stadium) : StadiumResult()
        data class Error(val message: String) : StadiumResult()
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val applicationContext = application.applicationContext

    suspend fun getNearbyStadiums(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val locationResult = getLastKnownLocation()
                if (locationResult != null) {
                    val apiService = RetrofitClient.instance.create(ApiService::class.java)
                    val response = apiService.getStadiums()

                    if (response.isSuccessful) {
                        val stadiums = response.body()?.toMutableList()?.apply {
                            forEach { stadium ->
                                val stadiumLocation = Location("").apply {
                                    latitude = stadium.latitudine
                                    longitude = stadium.longitudine
                                }
                                val currentLocation = Location("").apply {
                                    latitude = locationResult.latitude
                                    longitude = locationResult.longitude
                                }
                                stadium.distance = currentLocation.distanceTo(stadiumLocation).toDouble() / 1000 // in chilometri
                            }
                            sortBy { it.distance }
                        } ?: emptyList()
                        Result.Success(stadiums)
                    } else {
                        Result.Error("Errore nel prendere gli stadi: ${response.message()}")
                    }
                } else {
                    Result.Error("Impossibile prendere la posizione")
                }
            } catch (e: Exception) {
                // Gestisci eccezioni generali, come timeout o errori di rete
                Result.Error("Errore durante il recupero degli stadi: ${e.message}")
            }
        }
    }

    private suspend fun checkLocationPermission(): Boolean {
        return withContext(Dispatchers.Main) {ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED}
    }

    private suspend fun getLastKnownLocation(): Location? {
        return withContext(Dispatchers.IO) {
            if(!checkLocationPermission()) {
                return@withContext null
            }
            try {
                val locationTask = fusedLocationClient.lastLocation
                val location = locationTask.await()
                location
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun deleteStadium(stadium: Stadium): Result {
        return withContext(Dispatchers.IO) {
            try {
                val apiService = RetrofitClient.instance.create(ApiService::class.java)
                val response = apiService.deleteStadium(stadium.nome)

                if (response.isSuccessful) {
                    Result.Success(emptyList())
                } else {
                    Result.Error("Errore durante l'eliminazione dello stadio: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Errore sconosciuto: ${e.message}")
            }
        }
    }

    suspend fun addStadium(stadiumName: String): StadiumResult {
        return withContext(Dispatchers.IO) {
            try {
                val stadium: Stadium
                val currentLocation = getLastKnownLocation()
                if (currentLocation != null) {
                    stadium = Stadium(stadiumName, currentLocation.latitude, currentLocation.longitude, 0.0) // Possiamo ipotizzare che la distanza sia 0.0
                }
                else {
                    return@withContext  StadiumResult.Error("Impossibile prendere la posizione")
                }
                val apiService = RetrofitClient.instance.create(ApiService::class.java)
                // val stadiumToSend = Stadium(stadium.nome, stadium.latitudine, stadium.longitudine)
                val response = apiService.addStadium(stadium)

                if (response.isSuccessful) {
                    StadiumResult.Add(stadium)
                } else {
                    StadiumResult.Error("Failed to add stadium: ${response.message()}")
                }

            } catch (e: Exception) {
                StadiumResult.Error("Network request failed: ${e.message}")
            }
        }
    }

}
