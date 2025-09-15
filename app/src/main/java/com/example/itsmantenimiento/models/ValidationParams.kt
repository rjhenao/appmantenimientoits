package com.uvrp.itsmantenimientoapp.models

data class ValidationParams(
    val prInicialMin: Double,
    val prInicialMax: Double,
    val prFinalMin: Double,
    val prFinalMax: Double,
    val cantidadMin: Double,
    val cantidadMax: Double
)