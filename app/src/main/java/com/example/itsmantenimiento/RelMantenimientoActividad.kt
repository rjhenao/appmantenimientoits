package com.example.itsmantenimiento

data class RelMantenimientoActividad(
    val id : Int ,
    val id_manteniniento : Int ,
    val id_actividad : Int ,
    val id_manteniniento_usuario : Int ,
    val path : String,
    val estado : Int ,
    val sincronizado : Int
)
