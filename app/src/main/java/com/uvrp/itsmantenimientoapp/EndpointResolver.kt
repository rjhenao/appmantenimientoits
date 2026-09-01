package com.uvrp.itsmantenimientoapp

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object EndpointResolver {

    private const val TAG = "EndpointResolver"
    private const val PREFS = "ItsomNetwork"
    private const val KEY_LAST_API = "last_api_base_url"
    private const val KEY_LAST_UPDATE = "last_update_url"

    val API_BASE_URLS: List<String> = listOf(
        "http://181.225.65.82:8196/",
        "http://190.60.36.242:8196/",
        "http://10.208.5.53/",
        "http://itsom.raulhenaor.com:8196/",
    )

    val UPDATE_JSON_URLS: List<String> = listOf(
        "http://181.225.65.82:8195/actualizaciones/version.json",
        "http://190.60.36.242:8195/actualizaciones/version.json",
        "http://10.208.5.53:8080/actualizaciones/version.json",
        "http://itsom.raulhenaor.com:8195/actualizaciones/version.json",
    )

    fun ping(url: String, timeoutSeconds: Long = 4): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .callTimeout(timeoutSeconds + 1, TimeUnit.SECONDS)
                .build()
            val req = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                resp.code in 200..599
            }
        } catch (e: Exception) {
            Log.d(TAG, "Ping falló $url: ${e.message}")
            false
        }
    }

    fun resolveApiBaseUrl(context: Context, forceRefresh: Boolean = false): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!forceRefresh) {
            val cached = prefs.getString(KEY_LAST_API, null)?.trim().orEmpty()
            if (cached.isNotEmpty() && ping(cached)) {
                Log.i(TAG, "API cache OK → $cached")
                return ensureTrailingSlash(cached)
            }
        }
        for (url in API_BASE_URLS) {
            Log.i(TAG, "Probando API → $url")
            if (ping(url)) {
                val normalized = ensureTrailingSlash(url)
                prefs.edit().putString(KEY_LAST_API, normalized).apply()
                Log.i(TAG, "API conectada → $normalized")
                return normalized
            }
        }
        return null
    }

    fun resolveUpdateJsonUrl(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_LAST_UPDATE, null)?.trim().orEmpty()
        if (cached.isNotEmpty() && ping(cached)) {
            Log.i(TAG, "Update cache OK → $cached")
            return cached
        }
        for (url in UPDATE_JSON_URLS) {
            Log.i(TAG, "Probando update → $url")
            if (ping(url)) {
                prefs.edit().putString(KEY_LAST_UPDATE, url).apply()
                Log.i(TAG, "Update URL → $url")
                return url
            }
        }
        return null
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
