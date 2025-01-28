package com.example.itsmantenimiento

data class User(
    val id: Int,
    val name: String,
    val documento: Int,
    val email: String,
    val activo: Int,
    val created_at: String,
    val password: String,
    val updated_at: String
)

