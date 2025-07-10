package com.uvrp.itsmantenimientoapp

import ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    //producción
    //private const val BASE_URL = "http://10.208.5.53"
    //pruebas
    private const val BASE_URL = "http://181.225.65.82:8196"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}