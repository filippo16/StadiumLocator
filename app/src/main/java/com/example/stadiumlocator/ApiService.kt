// ApiService.kt
package com.example.stadiumlocator


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("/")
    suspend fun getStadiums(): Response<List<Stadium>>

    @POST("/")
    suspend fun addStadium(@Body stadium: Stadium): Response<Void>

    @DELETE("/?nome=")
    suspend fun deleteStadium(@Query("nome") nome: String): Response<Void>
}
