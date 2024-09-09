package com.example.stadiumlocator.view

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.stadiumlocator.R
import com.example.stadiumlocator.viewmodel.StadiumViewModel

class OptionsActivity : AppCompatActivity() {

    private lateinit var stadiumNameEditText: EditText
    private lateinit var addStadiumButton: Button
    private val viewModel: StadiumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        stadiumNameEditText = findViewById(R.id.stadiumNameEditText)
        addStadiumButton = findViewById(R.id.addStadiumButton)
        addStadiumButton.isEnabled = true

        observeViewModel()

        addStadiumButton.setOnClickListener {
            val stadiumName = stadiumNameEditText.text.toString()
            if (stadiumName.isNotEmpty()) {
                viewModel.addStadium(stadiumName)
            } else {
                Toast.makeText(this, "Metti un nome per lo stadio!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {

        viewModel.feedback.observe(this) { feedback ->
            val message = feedback.first

            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

}
