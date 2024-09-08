package com.example.stadiumlocator

data class Stadium(
    val nome: String,
    val latitudine: Double,
    val longitudine: Double,
    var distance: Double = 0.0 // Distance inizializzata a 0.0
)
