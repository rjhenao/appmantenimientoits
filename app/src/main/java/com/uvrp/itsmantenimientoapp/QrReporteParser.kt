package com.uvrp.itsmantenimientoapp

import android.net.Uri

object QrReporteParser {

    private val pathRegex = Regex("""/r/(\d+)/([a-fA-F0-9]{64})""")

    /**
     * @return id locación y token hex64, o null si no es un enlace de reporte.
     */
    fun parse(raw: String): Pair<Int, String>? {
        val s = raw.trim().trimEnd('/')
        if (!s.startsWith("http://", ignoreCase = true) && !s.startsWith("https://", ignoreCase = true)) {
            return null
        }
        val path = Uri.parse(s).path?.trimEnd('/') ?: return null
        val m = pathRegex.find(path) ?: return null
        return m.groupValues[1].toInt() to m.groupValues[2]
    }
}
