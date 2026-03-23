package com.spotik.app.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterRequest(
    val name: String,
    val age: Int,
    val city: String,
    val avatarUrl: String?,
)

data class RegisterResponse(
    val success: Boolean,
    val userId: String?,
    val message: String?,
)

interface SpotikApi {
    @POST("/api/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse
}

object RetrofitClient {
    private const val BASE_URL = "http://212.193.26.216:8080/"

    val api: SpotikApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotikApi::class.java)
    }
}

