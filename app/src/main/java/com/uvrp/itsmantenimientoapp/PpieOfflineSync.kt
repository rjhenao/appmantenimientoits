package com.uvrp.itsmantenimientoapp

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PpieOfflineSync {
    private const val TAG = "PpieOfflineSync"

    data class SyncResult(val exito: Boolean, val mensaje: String, val enviadas: Int = 0, val fallidas: Int = 0)

    suspend fun sincronizarCatalogo(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            RetrofitClient.init(context.applicationContext)
            val prefs = context.getSharedPreferences("Sesion", Context.MODE_PRIVATE)
            val token = prefs.getString("api_token", null)?.trim().orEmpty()
            if (token.isEmpty()) {
                Log.w(TAG, "Sin api_token para catálogo PPIE")
                return@withContext false
            }
            val resp = RetrofitClient.instance.ppieCatalogo().execute()
            if (!resp.isSuccessful) {
                Log.e(TAG, "catalogo HTTP ${resp.code()}")
                return@withContext false
            }
            val body = resp.body() ?: return@withContext false
            val puede = body.puedePpie == true
            prefs.edit().putBoolean("puede_ppie", puede).apply()
            val db = DatabaseHelper(context)
            db.reemplazarCatalogoPpie(body.formatos.orEmpty())
            prefs.edit().putLong("ppie_catalogo_sync_at", System.currentTimeMillis()).apply()
            Log.i(TAG, "PPIE catálogo: puede=$puede formatos=${body.formatos?.size ?: 0}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sync catálogo PPIE: ${e.message}", e)
            false
        }
    }

    suspend fun sincronizarPendientes(context: Context): SyncResult = withContext(Dispatchers.IO) {
        RetrofitClient.init(context.applicationContext)
        val prefs = context.getSharedPreferences("Sesion", Context.MODE_PRIVATE)
        if (prefs.getString("api_token", null).isNullOrBlank()) {
            return@withContext SyncResult(
                false,
                "Sin sesión API (token). Cierre sesión e inicie de nuevo con internet."
            )
        }
        val db = DatabaseHelper(context)
        val pendientes = db.obtenerPpiePendientes()
        if (pendientes.isEmpty()) {
            return@withContext SyncResult(false, "No hay PPIE pendientes", 0, 0)
        }

        // Refrescar catálogo antes de reenviar (alineación de actividades)
        sincronizarCatalogo(context)

        val api = RetrofitClient.instance
        val gson = Gson()
        var enviadas = 0
        var fallidas = 0
        val errores = mutableListOf<String>()

        for (row in pendientes) {
            try {
                val req = gson.fromJson(row.jsonPayload, ApiService.PpieSubmitRequest::class.java)
                if (req == null) {
                    fallidas++
                    errores.add("#${row.id}: JSON local inválido")
                    continue
                }
                val r = api.ppieSubmit(req).execute()
                val body = r.body()
                if (r.isSuccessful && body?.ok == true) {
                    db.marcarPpiePendienteSincronizado(row.id.toLong())
                    enviadas++
                } else {
                    fallidas++
                    val rawErr = try { r.errorBody()?.string() } catch (_: Exception) { null }
                    val msg = body?.message
                        ?: MobileAuthHelper.extractMessage(rawErr)
                        ?: when (r.code()) {
                            401 -> "Sesión API vencida (401). Cierre sesión e inicie de nuevo con internet."
                            403 -> "Sin permiso PPIE o formato no autorizado."
                            422 -> "Datos rechazados por el servidor (422)."
                            else -> "HTTP ${r.code()}"
                        }
                    errores.add("${row.formatCode ?: "PPIE"}: $msg")
                    Log.e(TAG, "Pendiente PPIE id=${row.id} code=${r.code()} msg=$msg raw=${rawErr?.take(300)}")
                }
            } catch (e: Exception) {
                fallidas++
                errores.add("#${row.id}: ${e.message ?: "error de red"}")
                Log.e(TAG, "Excepción PPIE local id=${row.id}: ${e.message}")
            }
        }

        val ok = fallidas == 0 && enviadas > 0
        val mensaje = when {
            ok -> "Se enviaron $enviadas ficha(s) PPIE"
            enviadas > 0 -> "Enviadas $enviadas, fallaron $fallidas. ${errores.firstOrNull().orEmpty()}"
            else -> errores.firstOrNull() ?: "No se pudo enviar ninguna PPIE"
        }
        SyncResult(exito = ok, mensaje = mensaje, enviadas = enviadas, fallidas = fallidas)
    }
}
