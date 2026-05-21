package com.uvrp.itsmantenimientoapp

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InventarioOfflineSync {

    private const val TAG = "InvOfflineSync"

    suspend fun sincronizarCatalogo(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            RetrofitClient.init(context.applicationContext)
            val prefs = context.getSharedPreferences("Sesion", Context.MODE_PRIVATE)
            val idRol = prefs.getInt("idRol", -1)
            if (idRol != 1 && idRol != 2) {
                Log.d(TAG, "Rol distinto de 1/2; omitiendo catálogo inventario.")
                return@withContext false
            }
            val token = prefs.getString("api_token", null)?.trim().orEmpty()
            if (token.isEmpty()) {
                Log.w(TAG, "Sin api_token; inicie sesión con red para obtener token.")
                return@withContext false
            }
            val api = RetrofitClient.instance
            val resp = api.inventarioCatalogoOffline().execute()
            if (!resp.isSuccessful) {
                Log.e(TAG, "catalogo HTTP ${resp.code()}")
                return@withContext false
            }
            val body = resp.body() ?: return@withContext false
            val u = body.unidades ?: emptyList()
            val p = body.productos ?: emptyList()
            val ub = body.ubicaciones ?: emptyList()
            val db = DatabaseHelper(context)
            db.reemplazarCatalogoInventario(u, p, ub)
            prefs.edit().putLong("inv_catalogo_sync_at", System.currentTimeMillis()).apply()
            Log.i(TAG, "Catálogo inventario: unidades=${u.size} productos=${p.size} ubicaciones=${ub.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sync catalogo: ${e.message}", e)
            false
        }
    }

    suspend fun sincronizarPendientes(context: Context): Boolean = withContext(Dispatchers.IO) {
        RetrofitClient.init(context.applicationContext)
        val prefs = context.getSharedPreferences("Sesion", Context.MODE_PRIVATE)
        if (prefs.getString("api_token", null).isNullOrBlank()) return@withContext false
        val db = DatabaseHelper(context)
        val api = RetrofitClient.instance
        val gson = Gson()
        var ok = false

        for (row in db.obtenerPendientesCargarStock()) {
            try {
                val req = ApiService.InventarioAjusteRequest(
                    invProductoId = row.invProductoId,
                    invUbicacionId = row.invUbicacionId,
                    cantidad = row.cantidad,
                    nota = row.nota
                )
                val r = api.inventarioAjusteEntrada(req).execute()
                if (r.isSuccessful) {
                    db.marcarCargarStockSincronizado(row.id)
                    ok = true
                } else {
                    Log.e(TAG, "Pendiente cargar stock id=${row.id} code=${r.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción cargar stock local id=${row.id}: ${e.message}")
            }
        }

        for (row in db.obtenerPendientesSalida()) {
            try {
                val req = gson.fromJson(row.jsonPayload, ApiService.InventarioSalidaRequest::class.java)
                val r = api.inventarioSalida(req).execute()
                if (r.isSuccessful) {
                    db.marcarSalidaSincronizado(row.id)
                    ok = true
                } else {
                    Log.e(TAG, "Pendiente salida id=${row.id} code=${r.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción salida local id=${row.id}: ${e.message}")
            }
        }

        ok
    }
}
