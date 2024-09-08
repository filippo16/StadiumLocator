package com.example.stadiumlocator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var stadiumContainer: LinearLayout
    private lateinit var optionsButton: Button
    private lateinit var updateButton: Button
    private var stadiums: List<Stadium>? = null

    private var latG: Double? = null
    private var lonG: Double? = null
    private var isGetStadiumSuccess: Boolean = false
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
        setupPermission()
        updateStadiumList(1)
    }

    override fun onResume() {
        super.onResume()
        updateStadiumList(1)
    }

    private fun initView() {
        stadiumContainer = findViewById(R.id.stadiumContainer)
        optionsButton = findViewById(R.id.optionsButton)
        updateButton = findViewById(R.id.updateButton)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        optionsButton.setOnClickListener {
            startActivity(Intent(this, OptionsActivity::class.java))
        }

        updateButton.setOnClickListener {
            updateStadiumList()
        }
    }

    private fun setupPermission() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // I permessi sono stati concessi, avvia il recupero della posizione e degli stadi
                fetchLocationAndStadiums()
            } else {
                showSnackbar("Permission denied!")
            }
        }
    }

    private suspend fun checkLocationPermission(): Boolean {
        return withContext(Dispatchers.Main) {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                false
            }
        }
    }

    private suspend fun getLastKnownLocation(): Location? {
        return withContext(Dispatchers.IO) {
            val permissionGranted = checkLocationPermission()
            if(!permissionGranted) {
                return@withContext null
            }
            try {
                val locationTask = fusedLocationClient.lastLocation
                val location = locationTask.await()

                if (location != null) {
                    location
                } else {
                    withContext(Dispatchers.Main) {
                        showSnackbar("Impossibile prendere la posizione")
                    }
                    null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar("Fallito il processo di localizzazione: ${e.message}")
                }
                null
            }
        }
    }

    private fun fetchLocationAndStadiums() {
        CoroutineScope(Dispatchers.Main).launch {
            val location = getLastKnownLocation()
            if (location != null) {
                latG = location.latitude
                lonG = location.longitude
                fetchNearbyStadiums()  // Chiama fetchNearbyStadiums solo se la posizione è ottenuta
            } else {
                showSnackbar("Impossibile prendere la posizione")
            }
        }
    }


    private suspend fun fetchNearbyStadiums() {
        try {
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            val response = apiService.getStadiums() // Chiama la funzione nel service api
            isGetStadiumSuccess = false

            if (response.isSuccessful) {
                val stadiums = response.body()?.toMutableList()?.apply {
                    forEach { stadium ->
                        val stadiumLocation = Location("").apply {
                            latitude = stadium.latitudine
                            longitude = stadium.longitudine
                        }
                        val currentLocation = Location("").apply {
                            latitude = latG ?: 0.0
                            longitude = lonG ?: 0.0
                        }
                        stadium.distance = currentLocation.distanceTo(stadiumLocation).toDouble() / 1000 // in chilometri
                    }
                    sortBy { it.distance }
                }
                this.stadiums = stadiums
                isGetStadiumSuccess = true
                updateUI()
            } else {
                withContext(Dispatchers.Main) {
                    showSnackbar("Fallito il processo di ricezione stadi: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showSnackbar("Richiesta network fallita: ${e.message}")
            }
        }
    }


    private fun updateStadiumList(type: Int = 0) {
        CoroutineScope(Dispatchers.Main).launch {
            fetchLocationAndStadiums()
        }

        if(isGetStadiumSuccess && type == 0)  {
            Toast.makeText(this@MainActivity, "Lista aggiornata!", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        stadiumContainer.removeAllViews() // Rimuove tutte le viste precedenti

        stadiums?.forEach { stadium ->
            val stadiumView = layoutInflater.inflate(R.layout.stadium_item, stadiumContainer, false)
            val stadiumName = stadiumView.findViewById<TextView>(R.id.stadiumName)
            val deleteButton = stadiumView.findViewById<ImageButton>(R.id.deleteButton)

            stadiumName.text = "${stadium.nome} - ${"%.2f".format(stadium.distance)} km"
            deleteButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    deleteStadiumToServer(stadium)
                }
            }

            stadiumContainer.addView(stadiumView)
        }
    }

    private fun removeStadium(stadium: Stadium) {
        stadiums = stadiums?.filter { it != stadium }
        updateUI()
    }

    private suspend fun deleteStadiumToServer(stadium: Stadium) {
        try {
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            val response = apiService.deleteStadium(stadium.nome)

            if (response.isSuccessful) {
                // Rimuovi lo stadio dalla lista e aggiorna l'interfaccia utente
                removeStadium(stadium)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "${stadium.nome} rimosso!", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Gestisci errore nella risposta
                withContext(Dispatchers.Main) {
                    showSnackbar("Fallimento nell'eliminazione: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            // Gestisci eventuali errori durante la chiamata di rete
            withContext(Dispatchers.Main) {
                showSnackbar("Fallimento della connessione: ${e.message}")
            }
        }
    }


    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
