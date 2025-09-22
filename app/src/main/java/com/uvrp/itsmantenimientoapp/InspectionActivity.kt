package com.uvrp.itsmantenimientoapp

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.adapters.InspectionAdapter
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import com.uvrp.itsmantenimientoapp.models.Activity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InspectionActivity : AppCompatActivity() {

    private lateinit var rvActivities: RecyclerView
    private lateinit var btnFinishInspection: Button
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: InspectionAdapter
    private var activityList = listOf<Activity>()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inspection)

        dbHelper = DatabaseHelper(this)

        // --- Inicialización de Vistas ---
        rvActivities = findViewById(R.id.rvActivities)
        btnFinishInspection = findViewById(R.id.btnFinishInspection)


        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // Llama al helper para configurar el menú
        HeaderHelper.setupHeader(this, drawerLayout, navView)
        // Puedes establecer un título para el toolbar si lo deseas
        supportActionBar?.title = "Inspección Diaria"



        // --- Configuración del RecyclerView ---
        setupRecyclerView()

        // --- Lógica del Botón ---
        btnFinishInspection.setOnClickListener {
            // Aquí iría el ID del usuario que ha iniciado sesión. Usamos '1' como ejemplo.
            val prefs = getSharedPreferences("Sesion" , MODE_PRIVATE)
            val currentUserId = prefs.getInt("idUser" , -1)
            saveInspection(currentUserId, activityList)
        }
    }

    private fun setupRecyclerView() {
        // Carga las actividades desde la base de datos.
        activityList = dbHelper.getAllActivities()

        if (activityList.isEmpty()) {
            Toast.makeText(this, "No hay actividades en el catálogo. Por favor, añádelas primero.", Toast.LENGTH_LONG).show()
        }

        // Crea el adaptador y lo asigna al RecyclerView.
        adapter = InspectionAdapter(activityList)
        rvActivities.adapter = adapter
        rvActivities.layoutManager = LinearLayoutManager(this)
    }

    private fun saveInspection(userId: Int, activities: List<Activity>) {
        val inspectionId = dbHelper.addInspection(userId)


        if (inspectionId == -1L) {
            Toast.makeText(this, "Error al crear la inspección", Toast.LENGTH_SHORT).show()
            return
        }

        // Guarda el estado de cada actividad en la tabla de relación.
        dbHelper.addInspectionActivities(inspectionId, activities)

        Toast.makeText(this, "Inspección guardada con éxito", Toast.LENGTH_LONG).show()
        // Cierra la actividad después de guardar.
        finish()
    }
}
