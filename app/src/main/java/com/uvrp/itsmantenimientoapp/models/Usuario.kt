package com.uvrp.itsmantenimientoapp.models // O el nombre del paquete que hayas creado

/**
 * Representa a un usuario o empleado que participa en un mantenimiento.
 *
 * @property id El identificador único del usuario en la base de datos.
 * @property nombre El nombre completo del usuario para mostrar en la UI.
 */
data class Usuario(
    val id: Int,
    val nombre: String
)