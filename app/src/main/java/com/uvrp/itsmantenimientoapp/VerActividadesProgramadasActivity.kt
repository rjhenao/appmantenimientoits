package com.uvrp.itsmantenimientoapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.adapters.ActividadesProgramadasAdapter
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper

class VerActividadesProgramadasActivity : AppCompatActivity(), ActividadesProgramadasAdapter.OnItemClickListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var fabAgregarActividad: FloatingActionButton
    private var bitacoraId: Int = -1
    private var esAdminBitacoras: Boolean = false
    private var puedeCrearNoProgramada: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_actividades_programadas)

        // Inicializar vistas
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        recyclerView = findViewById(R.id.rvActividadesProgramadas)
        fabAgregarActividad = findViewById(R.id.fabAgregarActividad)
        dbHelper = DatabaseHelper(this)

        // INSERTA ESTAS LÍNEAS PARA EL MENÚ
        HeaderHelper.setupHeader(this, drawerLayout, navView)
        supportActionBar?.title = "Actividades Programadas"

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Obtener el ID de la bitácora desde el Intent
        bitacoraId = intent.getIntExtra("NUMERO_BITACORA", -1)
        val prefs = getSharedPreferences("Sesion" , Context.MODE_PRIVATE)
        val idUser = prefs.getInt("idUser" , -1)
        val idRol = prefs.getInt("idRol" , -1)
        esAdminBitacoras = idRol == 1 || idRol == 5
        puedeCrearNoProgramada = idRol == 1 || idRol == 5 || idRol == 6
        fabAgregarActividad.isEnabled = puedeCrearNoProgramada
        
        // Configurar FAB
        setupFabListener()

        if (bitacoraId != -1) {
            val actividades = dbHelper.getActividadesPorBitacora(bitacoraId , idUser, esAdminBitacoras)
            val adapter = ActividadesProgramadasAdapter(actividades, this)
            recyclerView.adapter = adapter
        } else {
            Toast.makeText(this, "ID de Bitácora no encontrado.", Toast.LENGTH_SHORT).show()
            // Puedes manejar el error o cerrar la actividad
            finish()
        }
    }

    override fun onRegistrarClick(actividadId: Int) {
        // Lógica para el botón "Registrar Actividad"
        Toast.makeText(this, "Se presionó el botón para la actividad con ID: $actividadId", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, RegistrarMantenimientoBitacora::class.java)
        intent.putExtra("numero_actividad", actividadId)
        startActivity(intent)
    }

    // INSERTA ESTOS MÉTODOS PARA EL MENÚ
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (HeaderHelper.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupFabListener() {
        fabAgregarActividad.setOnClickListener {
            abrirCrearActividad()
        }
    }

    private fun abrirCrearActividad() {
        if (bitacoraId == -1) {
            Toast.makeText(this, "Error: No se pudo obtener el ID de la bitácora", Toast.LENGTH_SHORT).show()
            return
        }

        if (!puedeCrearNoProgramada) {
            Toast.makeText(this, "No tienes permisos para crear actividades no programadas.", Toast.LENGTH_LONG).show()
            return
        }
        
        val intent = Intent(this, CrearActividadNoProgramadaActivity::class.java)
        intent.putExtra("id_bitacora", bitacoraId)
        startActivityForResult(intent, REQUEST_CODE_CREAR_ACTIVIDAD)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_CREAR_ACTIVIDAD && resultCode == RESULT_OK) {
            // Recargar la lista de actividades
            val prefs = getSharedPreferences("Sesion", Context.MODE_PRIVATE)
            val idUser = prefs.getInt("idUser", -1)
            val actividades = dbHelper.getActividadesPorBitacora(bitacoraId, idUser, esAdminBitacoras)
            val adapter = ActividadesProgramadasAdapter(actividades, this)
            recyclerView.adapter = adapter
            
            Toast.makeText(this, "Actividad no programada creada exitosamente", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_CODE_CREAR_ACTIVIDAD = 1001
    }
}