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
    private const val FALLBACK_BASE_URL = "http://181.225.65.82:8196/"

    @Volatile
    private var activeBaseUrl: String? = null

    @Volatile
    private var apiService: ApiService? = null

    @Volatile
    private var appContext: Context? = null

    /** true si el último connect/ping resolvió un servidor alcanzable. */
    @Volatile
    var lastConnectOk: Boolean = false
        private set

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
     * Resuelve el endpoint: última URL de producción que funcionó, luego 181, 190, LAN y dominio.
     */
    fun connectBlocking(context: Context, forceRefresh: Boolean = false): Boolean {
        init(context)
        if (!forceRefresh && activeBaseUrl != null && pingServer(activeBaseUrl)) {
            lastConnectOk = true
            if (apiService == null) {
                setActiveBaseUrl(activeBaseUrl!!)
            }
            return true
        }
        val resolved = EndpointResolver.resolveApiBaseUrl(context.applicationContext, forceRefresh)
        if (resolved == null) {
            lastConnectOk = false
            Log.e(TAG, "Sin servidor alcanzable en ningún endpoint")
            // No dejar apiService en null: evita IllegalStateException en producción.
            ensureClientReady(FALLBACK_BASE_URL)
            return false
        }
        lastConnectOk = true
        setActiveBaseUrl(resolved)
        return true
    }

    suspend fun connect(context: Context, forceRefresh: Boolean = false): Boolean =
        withContext(Dispatchers.IO) { connectBlocking(context, forceRefresh) }

    /** ApiService o null si aún no hay Context de aplicación. */
    fun apiOrNull(): ApiService? {
        val ctx = appContext
        if (apiService == null && ctx != null) {
            connectBlocking(ctx)
        } else if (apiService == null) {
            ensureClientReady(FALLBACK_BASE_URL)
        }
        return apiService
    }

    private fun setActiveBaseUrl(url: String) {
        if (activeBaseUrl == url && apiService != null) return
        activeBaseUrl = url
        apiService = buildRetrofit(url).create(ApiService::class.java)
        Log.i(TAG, "Retrofit activo → $url")
    }

    private fun ensureClientReady(preferredUrl: String) {
        val url = activeBaseUrl?.takeIf { it.isNotBlank() } ?: preferredUrl
        if (apiService == null || activeBaseUrl != url) {
            setActiveBaseUrl(url)
            Log.w(TAG, "Cliente API en modo degradado → $url (sin ping OK)")
        }
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

    /**
     * Nunca lanza: si no hay red, usa fallback para que los callers fallen por IO/HTTP controlado
     * en lugar de tumbar la app con IllegalStateException (Play Console).
     */
    val instance: ApiService
        get() {
            return apiOrNull()
                ?: run {
                    ensureClientReady(FALLBACK_BASE_URL)
                    apiService!!
                }
        }
}
