package com.uvrp.itsmantenimientoapp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExtrasOfflineSync {
    private const val TAG = "ExtrasOfflineSync"

    data class SyncResult(val exito: Boolean, val mensaje: String)

    suspend fun sincronizarCatalogoTurnos(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            RetrofitClient.init(context.applicationContext)
            val prefs = context.getSharedPreferences("Sesion", Context.MODE_PRIVATE)
            if (prefs.getString("api_token", null).isNullOrBlank()) {
                Log.w(TAG, "Sin token para catálogo turnos")
                return@withContext false
            }
            val resp = RetrofitClient.instance.extrasCatalogoTurnos().execute()
            if (!resp.isSuccessful) {
                Log.e(TAG, "catalogo turnos HTTP ${resp.code()}")
                return@withContext false
            }
            val data = resp.body()?.data.orEmpty()
            DatabaseHelper(context).reemplazarCatalogoExtrasTurnos(data)
            prefs.edit().putLong("extras_turnos_sync_at", System.currentTimeMillis()).apply()
            Log.i(TAG, "Turnos extras: ${data.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error catálogo turnos: ${e.message}", e)
            false
        }
    }
}
