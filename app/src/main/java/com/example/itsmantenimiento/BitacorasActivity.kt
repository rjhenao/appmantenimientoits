package com.uvrp.itsmantenimientoapp

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
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper

// Imports añadidos para resolver el error
import com.uvrp.itsmantenimientoapp.Bitacora
import com.uvrp.itsmantenimientoapp.BitacorasAdapter
import com.uvrp.itsmantenimientoapp.DatabaseHelper


class BitacorasActivity : AppCompatActivity(), BitacorasAdapter.OnItemClickListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitacoras)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerBitacoras)

        HeaderHelper.setupHeader(this, drawerLayout, navView)
        supportActionBar?.title = "Bitácoras"

        recyclerView.layoutManager = LinearLayoutManager(this)

        dbHelper = DatabaseHelper(this)

        val listaDeBitacoras = dbHelper.getTodasLasBitacoras()
        val adapter = BitacorasAdapter(listaDeBitacoras, this)
        recyclerView.adapter = adapter
    }

    override fun onRegistrarClick(bitacora: Bitacora) {
        val existeInspeccion = dbHelper.existeInspeccionHoy(this)
        val intent: Intent

        if (existeInspeccion) {
            Toast.makeText(this, "Ya existe una inspección registrada el día de hoy.", Toast.LENGTH_LONG).show()
            intent = Intent(this, VerActividadesProgramadasActivity::class.java).apply {
                val hjj = bitacora.numero.toInt()

                putExtra("NUMERO_BITACORA", hjj)
            }
        } else {
            Toast.makeText(this, "Aún no ha registrado la inspección de hoy.", Toast.LENGTH_LONG).show()
            intent = Intent(this, InspectionActivity::class.java)
            // No se agrega putExtra aquí porque InspectionActivity no lo necesita.
            // Si InspectionActivity lo necesitara, lo añadirías aquí.
        }



        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (HeaderHelper.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}