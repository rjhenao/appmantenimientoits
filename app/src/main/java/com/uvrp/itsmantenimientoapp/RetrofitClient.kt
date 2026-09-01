package com.uvrp.itsmantenimientoapp

import ApiService
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TAG = "RetrofitClient"

    @Volatile
    private var activeBaseUrl: String? = null

    @Volatile
    private var apiService: ApiService? = null

    @Volatile
    private var appContext: Context? = null

    /** URL activa (para toasts / diagnóstico). */
    fun baseUrl(): String = activeBaseUrl ?: "—"

    /**
     * ¿El backend responde? Si [url] es null, prueba la URL activa o la primera de la lista.
     */
    fun pingServer(url: String? = null): Boolean {
        val target = url?.trim().orEmpty().ifEmpty { activeBaseUrl ?: EndpointResolver.API_BASE_URLS.first() }
        return EndpointResolver.ping(target)
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Resuelve el endpoint (cache → 181 → 190 → LAN → dominio) y prepara Retrofit.
     */
    fun connectBlocking(context: Context, forceRefresh: Boolean = false): Boolean {
        init(context)
        if (!forceRefresh && activeBaseUrl != null && pingServer(activeBaseUrl)) {
            return true
        }
        val resolved = EndpointResolver.resolveApiBaseUrl(context.applicationContext, forceRefresh)
        if (resolved == null) {
            activeBaseUrl = null
            apiService = null
            Log.e(TAG, "Sin servidor alcanzable en ningún endpoint")
            return false
        }
        setActiveBaseUrl(resolved)
        return true
    }

    suspend fun connect(context: Context, forceRefresh: Boolean = false): Boolean =
        withContext(Dispatchers.IO) { connectBlocking(context, forceRefresh) }

    private fun setActiveBaseUrl(url: String) {
        if (activeBaseUrl == url && apiService != null) return
        activeBaseUrl = url
        apiService = buildRetrofit(url).create(ApiService::class.java)
        Log.i(TAG, "Retrofit activo → $url")
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
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

    val instance: ApiService
        get() {
            val ctx = appContext
            if (apiService == null && ctx != null) {
                connectBlocking(ctx)
            }
            return apiService
                ?: throw IllegalStateException("Sin conexión al servidor ITSOM. Verifique red e intente de nuevo.")
        }
}
