package com.uvrp.itsmantenimientoapp

import ApiService

object CompraUiHelper {

    fun formatSegundos(segundos: Int?): String {
        val s = (segundos ?: 0).coerceAtLeast(0)
        val dias = s / 86400
        val horas = (s % 86400) / 3600
        val mins = (s % 3600) / 60
        return when {
            dias > 0 -> "${dias}d ${horas}h"
            horas > 0 -> "${horas}h ${mins}m"
            mins > 0 -> "${mins}m"
            else -> "<1m"
        }
    }

    fun resumenDuraciones(items: List<ApiService.CompraDuracionEstadoDto>?): String {
        if (items.isNullOrEmpty()) return ""
        return items.joinToString("  ·  ") { d ->
            val nombre = d.estado ?: "—"
            val t = formatSegundos(d.segundos)
            if (d.actual == true) "$nombre: $t (actual)" else "$nombre: $t"
        }
    }
}
