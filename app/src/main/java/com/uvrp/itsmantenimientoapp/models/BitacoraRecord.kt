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
    val supervisorResponsable: Int? = null,
    val registroPrInicial: String? = null,
    val registroPrFinal: String? = null,
    val registroCantidad: Double? = null,
    val registroObservacion: String? = null,
    val registroSentido: String? = null,
    val registroLado: String? = null,
    val idRegistroNpLocal: Int? = null,
    /** UUID inmutable de la cabecera NP (idempotencia en servidor). */
    val clientUuid: String? = null
)