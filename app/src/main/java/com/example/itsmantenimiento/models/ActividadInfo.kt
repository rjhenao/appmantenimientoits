package com.uvrp.itsmantenimientoapp.models

/**
 * Contiene la información detallada de una actividad de bitácora.
 */
data class ActividadInfo(
    val id: Int,
    val descripcion: String,
    val tipoUnidad: String,
    val idCuadrilla: Int
)