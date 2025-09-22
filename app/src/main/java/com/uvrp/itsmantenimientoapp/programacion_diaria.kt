package com.uvrp.itsmantenimientoapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class programacion_diaria : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_programacion_diaria)

        val idLocacion = intent.getStringExtra("idLocacion") ?: "-1"
        val idSistema = intent.getStringExtra("idSistema") ?: "-1"
        val idSubsistema = intent.getStringExtra("idSubsistema") ?: "-1"
        val idLocacionInt = idLocacion.toIntOrNull() ?: -1
        val idSistemaInt = idSistema.toIntOrNull() ?: -1
        val idSubsistemaInt = idSubsistema.toIntOrNull() ?: -1

        // Configurar el Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configurar el RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val dbHelper = DatabaseHelper(this)
        val items = dbHelper.getProgramaciones(idLocacionInt , idSistemaInt , idSubsistemaInt)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TableAdapter(items)
    }

    // Inflar el menú en el Toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Manejar clics en los ítems del menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                syncData()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun syncData() {
        // Lógica para sincronizar datos
        Toast.makeText(this, "Sincronizando datos...", Toast.LENGTH_SHORT).show()
        // Aquí puedes agregar la lógica para sincronizar con un servidor o base de datos remota
    }

    private fun logout() {
        // Lógica para cerrar sesión
        Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show()

        // Limpiar las SharedPreferences
        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Elimina todos los datos almacenados en las SharedPreferences
        editor.apply() // Aplica los cambios

        // Redirigir a MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // Cerrar la actividad actual
        finish()
    }
}