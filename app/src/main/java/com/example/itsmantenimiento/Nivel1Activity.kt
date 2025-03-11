package com.example.itsmantenimiento

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Nivel1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nivel1)

        // Configurar el Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar1)
        setSupportActionBar(toolbar)

        // Configurar el RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView1)
        val dbHelper = DatabaseHelper(this)
        val items = dbHelper.getNivel1()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TableAdapterLv1(items)
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

        FuncionesGenerales.sincronizarMantenimientos(this) { exito ->
            if (exito) {
                Log.d("SYNC", "La sincronización se completó correctamente.")
            } else {
                Log.d("SYNC", "Hubo un error en la sincronización.")
            }
        }

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