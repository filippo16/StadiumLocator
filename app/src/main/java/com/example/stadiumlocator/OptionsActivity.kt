package com.example.stadiumlocator

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OptionsActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var stadiumNameEditText: EditText
    private lateinit var addStadiumButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        stadiumNameEditText = findViewById(R.id.stadiumNameEditText)
        addStadiumButton = findViewById(R.id.addStadiumButton)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                addStadiumButton.isEnabled = true
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            addStadiumButton.isEnabled = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        addStadiumButton.setOnClickListener {
            val stadiumName = stadiumNameEditText.text.toString()
            if (stadiumName.isNotEmpty()) {
                getLastKnownLocation(stadiumName)
            } else {
                Toast.makeText(this, "Please enter a stadium name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLastKnownLocation(stadiumName: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val stadium = Stadium(stadiumName, it.latitude, it.longitude)
                addStadiumToServer(stadium)
            } ?: run {
                Toast.makeText(this, "Unable to retrieve location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addStadiumToServer(stadium: Stadium) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val stadiumToSend = Stadium(stadium.nome, stadium.latitudine, stadium.longitudine)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = apiService.addStadium(stadiumToSend)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@OptionsActivity, "Stadium added successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@OptionsActivity, "Failed to add stadium: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OptionsActivity, "Network request failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
