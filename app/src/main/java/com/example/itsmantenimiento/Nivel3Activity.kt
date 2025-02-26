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

class Nivel3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
Log.d("dandiuhq89d9" , "d89jdsu89hdusadyua")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nivel3)

        val idLocacion = intent.getStringExtra("idLocacion") ?: "-1"
        val idSistema = intent.getStringExtra("idSistema") ?: "-1"
        val idLocacionInt = idLocacion.toIntOrNull() ?: -1
        val idSistemaInt = idSistema.toIntOrNull() ?: -1


        val toolbar: Toolbar = findViewById(R.id.toolbar3)
            setSupportActionBar(toolbar)

        Log.d("hdhdndheyu" , "jkjkjkj");

            // Configurar el RecyclerView
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerView3)
            val dbHelper = DatabaseHelper(this)
            val items = dbHelper.getNivel3(idLocacionInt , idSistemaInt)

            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = TableAdapterLv3(items)



    }
}