package com.uvrp.itsmantenimientoapp

import android.net.Uri

/**
 * Normaliza el texto leído de un QR de celda/ubicación para cruzarlo con [codigo_unico_global]
 * en la tabla local (sincronizada con el servidor al iniciar sesión).
 *
 * Soporta: código plano, o URL cuyo último segmento de ruta o parámetro `codigo` / `celda` / `u` sea el código.
 */
object InventarioUbicacionQr {

    fun normalizarPayload(raw: String): String {
        var s = raw.trim().lines().firstOrNull()?.trim().orEmpty()
        if (s.isEmpty()) return ""

        if (s.startsWith("http", ignoreCase = true)) {
            try {
                val uri = Uri.parse(s)
                listOf("codigo", "celda", "u", "id", "ubicacion", "q").forEach { key ->
                    uri.getQueryParameter(key)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
                }
                val segments = uri.pathSegments
                for (i in segments.size - 1 downTo 0) {
                    val seg = segments[i].trim()
                    if (seg.isEmpty()) continue
                    val lower = seg.lowercase()
                    if (lower in listOf("ubicacion", "celda", "inv", "api", "inventario", "sync", "v1")) continue
                    if (seg.length <= 128) return seg
                }
            } catch (_: Exception) { }
        }

        return s
    }
}
