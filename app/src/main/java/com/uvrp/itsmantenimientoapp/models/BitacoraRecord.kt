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
    val fotos: List<File>,
    val estado: Int = 1, // 1 = Programada, 2 = No Programada
    // Campos adicionales para actividades no programadas
    val idBitacora: Int? = null,
    val idActividad: Int? = null,
    val idCuadrilla: Int? = null,
    val uf: Int? = null,
    val sentido: String? = null,
    val lado: String? = null,
    val supervisorResponsable: Int? = null
)