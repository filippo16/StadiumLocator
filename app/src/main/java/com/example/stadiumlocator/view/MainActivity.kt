package com.example.stadiumlocator.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.stadiumlocator.R
import com.example.stadiumlocator.model.Stadium
import com.example.stadiumlocator.viewmodel.StadiumViewModel
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private val viewModel: StadiumViewModel by viewModels()
    private lateinit var stadiumContainer: LinearLayout
    private lateinit var optionsButton: Button
    private lateinit var updateButton: Button
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var isDeleteGetStadiumSuccess: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
        setupPermissionLauncher()
        checkAndRequestPermissions()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getStadiums()
    }

    private fun initView() {
        stadiumContainer = findViewById(R.id.stadiumContainer)
        optionsButton = findViewById(R.id.optionsButton)
        updateButton = findViewById(R.id.updateButton)

        optionsButton.setOnClickListener {
            startActivity(Intent(this, OptionsActivity::class.java))
        }

        updateButton.setOnClickListener {
            viewModel.getStadiums()
            if(isDeleteGetStadiumSuccess) {
                Toast.makeText(this@MainActivity, "Lista aggiornata!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.getStadiums()
            } else {
                showSnackbar("Permission denied!")
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.getStadiums()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun observeViewModel() {
        viewModel.stadiums.observe(this) { stadiumList ->
            updateUI(stadiumList)
            isDeleteGetStadiumSuccess = true // qui va bene perché sarà sempre true
        }

        viewModel.feedback.observe(this) { feedback ->
            val message = feedback.first
            val isSuccess = feedback.second

            if (message.isNotEmpty()) {
                showSnackbar(message)
            }

            isDeleteGetStadiumSuccess = isSuccess
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(stadiums: List<Stadium>?) {
        stadiumContainer.removeAllViews()

        stadiums?.forEach { stadium ->
            val stadiumView = layoutInflater.inflate(R.layout.stadium_item, stadiumContainer, false)
            val stadiumName = stadiumView.findViewById<TextView>(R.id.stadiumName)
            val deleteButton = stadiumView.findViewById<ImageButton>(R.id.deleteButton)

            stadiumName.text = "${stadium.nome} - ${"%.2f".format(stadium.distance)} km"
            deleteButton.setOnClickListener {
                viewModel.deleteStadium(stadium)
                if(isDeleteGetStadiumSuccess) {
                    Toast.makeText(this@MainActivity, "${stadium.nome} rimosso!", Toast.LENGTH_SHORT).show()
                }
            }

            stadiumContainer.addView(stadiumView)
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
