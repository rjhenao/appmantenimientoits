package com.uvrp.itsmantenimientoapp

    data class ProgramarMantenimiento (
        val id: Int,
        val id_equipo : Int,
        val id_periodicidad : Int,
        val fecha_programado : String,
        val fecha_reprogramado : String,
        val fecha_realizado : String,
        val created_at : String,
        val updated_at : String
        )