package com.example.itsmantenimiento

data class RelTecnicoMantenimiento(
    val id : Int ,
    val idUser : Int ,
    val idMantenimiento : Int ,
    val horaInicial : String ,
    val horaFinal : String ,
    val estado : Int ,
    val sincronizado : Int
)
