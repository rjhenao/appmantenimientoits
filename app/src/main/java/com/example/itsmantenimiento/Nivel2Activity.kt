package com.example.itsmantenimiento

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Nivel2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nivel2)

        val idLocacion = intent.getStringExtra("idLocacion") ?: "-1"
        val idLocacionInt = idLocacion.toIntOrNull() ?: -1


        val toolbar: Toolbar = findViewById(R.id.toolbar2)
            setSupportActionBar(toolbar)


            // Configurar el RecyclerView
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerView2)
            val dbHelper = DatabaseHelper(this)
            val items = dbHelper.getNivel2(idLocacionInt)

            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = TableAdapterLv2(items)



    }
}