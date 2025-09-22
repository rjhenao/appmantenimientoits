package com.uvrp.itsmantenimientoapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
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