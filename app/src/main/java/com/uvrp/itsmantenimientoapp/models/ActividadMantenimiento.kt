package com.uvrp.itsmantenimientoapp.models

data class ActividadMantenimiento(
    val id: Int,
    val descripcion: String,
    val cuadrillaNombre: String,
    val uf: String,
    val sentido: String,
    val lado: String,
    val prInicial: String,
    val prFinal: String,
    val cantidad: String,
    val tipoUnidad: String,
    val observacion: String,
    val estado: Int // 1 = Programada, 2 = No Programada
)