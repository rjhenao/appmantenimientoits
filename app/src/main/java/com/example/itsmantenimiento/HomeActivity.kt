package com.example.itsmantenimiento

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val toolbar: Toolbar = findViewById(R.id.toolbar_home)
        setSupportActionBar(toolbar)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_its -> {

                    val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
                    val idRol = sharedPreferences.getInt("idRol", -1) // -1 es el valor por defecto si no se encuentra

                    if (idRol == 1 || idRol ==2 ) {
                        val intent = Intent(this, Nivel1Activity::class.java)
                        startActivity(intent)
                        //finish()
                    } else {
                        Toast.makeText(this, "No tiene permisos para acceder como ITS, comuníquese con un administrador.", Toast.LENGTH_LONG).show()
                    }

                    true
                }
                R.id.nav_preoperacional -> {
                    val intent = Intent(this, iniciarPreoperacional::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_cerrarsesion -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        // Mostrar texto en el contenido central
        val contentText = findViewById<TextView>(R.id.home_content)
        contentText.text = "Estás en la pantalla Home"
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
