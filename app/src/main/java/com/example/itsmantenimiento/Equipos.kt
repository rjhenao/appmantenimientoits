package com.uvrp.itsmantenimientoapp

data class Equipos(
    val id : Int ,
    val id_locacion : Int ,
    val id_sistemas : Int ,
    val id_subsistemas : Int ,
    val id_equipo : Int ,
    val id_uf : Int,
    val tag : String,
    val coordenada : String,
    val mantenimiento_programado : Int,
    val activo : Int
)
