package com.uvrp.itsmantenimientoapp

import ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    //producción
    //private const val BASE_URL = "http://192.168.0.198:8009"
    //pruebas
    private const val BASE_URL = "http://181.225.65.82:8196"
    //private const val BASE_URL = "http://181.225.65.82:8190"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}