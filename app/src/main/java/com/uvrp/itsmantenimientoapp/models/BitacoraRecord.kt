package com.uvrp.itsmantenimientoapp.models

import java.io.File

data class BitacoraRecord(
    val id: Int, // ID de la tabla rel_bitacora_actividades
    val idRelProgramarActividadesBitacora: Int,
    val prInicial: String,
    val prFinal: String,
    val cantidad: Double,
    val observacion: String,
    val usuarios: List<Int>,
    val fotos: List<File>
)