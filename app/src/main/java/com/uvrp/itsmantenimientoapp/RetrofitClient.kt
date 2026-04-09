package com.uvrp.itsmantenimientoapp

import ApiService
import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * Base URL del backend (debe terminar en /).
     * Descomente UNA línea según entorno — igual que antes; todo el proyecto sigue usando [instance].
     */
    // producción
    // private const val BASE_URL = "http://192.168.1.15:8009/"
    // pruebas
    private const val BASE_URL = "http://181.225.65.82:8196/"
    //private const val BASE_URL = "http://192.168.1.139:8000/"   
    

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
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val ctx = appContext
                val reqBuilder = chain.request().newBuilder()
                if (ctx != null) {
                    val token = ctx.getSharedPreferences("Sesion", Context.MODE_PRIVATE)
                        .getString("api_token", null)?.trim().orEmpty()
                    if (token.isNotEmpty()) {
                        reqBuilder.header("Authorization", "Bearer $token")
                        reqBuilder.header("Accept", "application/json")
                    }
                }
                chain.proceed(reqBuilder.build())
            }
            .build()
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
