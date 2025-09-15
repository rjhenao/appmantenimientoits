package com.uvrp.itsmantenimientoapp

import java.io.File

data class MantenimientoCorrectivo(
    val id: Int,
    val idEquipo: Int,
    val descripcionFalla: String,
    val diagnostico: String,
    val acciones: String,
    val repuestos: String,
    val estadoFinal: String,
    val causaRaiz: String,         // 👈 antes era "causa"
    val observaciones: String,
    val usuarios: List<Int>,       // 👈 antes era "usuariosIds"
    val fotos: List<File>          // 👈 en BD guardas paths, aquí manejas objetos File
)


