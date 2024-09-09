package com.example.stadiumlocator.model

data class Stadium(
    val nome: String,
    val latitudine: Double,
    val longitudine: Double,
    @Transient var distance: Double = 0.0
)
