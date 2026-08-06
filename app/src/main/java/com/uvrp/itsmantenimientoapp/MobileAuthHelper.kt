package com.uvrp.itsmantenimientoapp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MobileAuthHelper {
    private const val TAG = "MobileAuthHelper"

    /** Asegura Bearer Sanctum; si falta, intenta mobileLogin con documento/password de Sesión. */
    suspend fun ensureApiToken(context: Context): Boolean = withContext(Dispatchers.IO) {
        RetrofitClient.init(context.applicationContext)
        val prefs = context.getSharedPreferences("Sesion", Context.MODE_PRIVATE)
        val existing = prefs.getString("api_token", null)?.trim().orEmpty()
        if (existing.isNotEmpty()) return@withContext true

        val documento = prefs.getString("documento", null)?.trim().orEmpty()
        if (documento.isEmpty()) {
            Log.w(TAG, "Sin documento en Sesión para renovar token")
            return@withContext false
        }
        // Password no se guarda en Sesión normalmente — no podemos renovar sin re-login.
        // Intentar con lo que haya no aplica; devolver false.
        Log.w(TAG, "Sin api_token; el usuario debe iniciar sesión con red")
        false
    }

    fun extractMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val msg = Regex("\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\n", "\n")
        if (!msg.isNullOrBlank()) return msg
        // Laravel validation: "errors":{"campo":["texto"]}
        val errArr = Regex("\"errors\"\\s*:\\s*\\{\\s*\"[^\"]+\"\\s*:\\s*\\[\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\\"", "\"")
        if (!errArr.isNullOrBlank()) return errArr
        // Lista plana: "errors":["texto"]
        return Regex("\"errors\"\\s*:\\s*\\[\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\\"", "\"")
    }
}
