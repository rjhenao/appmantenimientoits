package com.example.itsmantenimiento

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class programacion_diaria : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_programacion_diaria)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // Datos de ejemplo
        val items = listOf(
            TableItem("Peaje Pamplonita", "Impresora"),
            TableItem("Peaje Acacios", "PC VIA"),
            TableItem("Tunel Pamplona", "CO2")
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TableAdapter(items)
    }
    }
