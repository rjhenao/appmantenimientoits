package com.uvrp.itsmantenimientoapp.models

// ===== DATA CLASSES PARA FOTOS MASIVAS =====

data class FotosMasivasRequest(
    val fotos: List<FotoMasivaRequest>
)

data class FotoMasivaRequest(
    val id_mantenimiento: Int,
    val ruta: String,
    val imagen_base64: String,
    val created_at: String
)

data class FotosMasivasResponse(
    val success: Boolean,
    val message: String,
    val fotos_procesadas: Int,
    val fotos_fallidas: Int,
    val errores: List<String>? = null
)
