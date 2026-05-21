package com.uvrp.itsmantenimientoapp

import ApiService
import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * Base URL del backend (debe terminar en /).
     * Descomente UNA línea según entorno — igual que antes; todo el proyecto sigue usando [instance].
     */
    // producción
    //private const val BASE_URL = "http://192.168.0.188:8003/"
    // pruebas
    private const val BASE_URL = "http://181.225.65.82:8196/"
    //private const val BASE_URL = "http://10.202.8.24:8000/"   
    

    @Volatile
    private var appContext: Context? = null

    /**
     * Opcional: guarda el contexto para enviar el token Sanctum (inventario) si existe en Sesión.
     * Si no se llama, el resto de módulos siguen igual que siempre (sin cabecera Bearer).
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val okHttp: OkHttpClient by lazy {
        val b = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val log = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            b.addInterceptor(log)
        }

        b.addInterceptor { chain ->
            val ctx = appContext
            val reqBuilder = chain.request().newBuilder()
            reqBuilder.header("Accept", "application/json")
            if (ctx != null) {
                val token = ctx.getSharedPreferences("Sesion", Context.MODE_PRIVATE)
                    .getString("api_token", null)?.trim().orEmpty()
                if (token.isNotEmpty()) {
                    reqBuilder.header("Authorization", "Bearer $token")
                }
            }
            chain.proceed(reqBuilder.build())
        }
        b.build()
    }

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}
