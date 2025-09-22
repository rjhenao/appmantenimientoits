package com.uvrp.itsmantenimientoapp.models

data class Activity(
    val id: Int,
    val description: String,
    var isChecked: Boolean = false
)
