package com.uvrp.itsmantenimientoapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper

class SeleccionEquipoActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private lateinit var spinnerLocacion: Spinner
    private lateinit var spinnerSistema: Spinner
    private lateinit var spinnerSubsistema: Spinner
    private lateinit var spinnerTipoEquipo: Spinner
    private lateinit var spinnerTagEquipo: Spinner
    private lateinit var btnAceptar: Button
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_equipo)

        // ==== INICIALIZAR HEADER GLOBAL ====
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        HeaderHelper.setupHeader(
            activity = this,
            drawerLayout = drawerLayout,
            navView = navView
        )

        // ==== INICIALIZAR COMPONENTES DE LA PANTALLA ====
        dbHelper = DatabaseHelper(this)

        spinnerLocacion = findViewById(R.id.spinnerLocacion)
        spinnerSistema = findViewById(R.id.spinnerSistema)
        spinnerSubsistema = findViewById(R.id.spinnerSubsistema)
        spinnerTipoEquipo = findViewById(R.id.spinnerTipoEquipo)
        spinnerTagEquipo = findViewById(R.id.spinnerTagEquipo)
        btnAceptar = findViewById(R.id.btnAceptar)

        cargarLocaciones()

        spinnerLocacion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val locacionId = obtenerIdDesdeSpinner(spinnerLocacion)
                cargarSistemas(locacionId)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerSistema.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sistemaId = obtenerIdDesdeSpinner(spinnerSistema)
                val locacionId = obtenerIdDesdeSpinner(spinnerLocacion)
                cargarSubsistemas(sistemaId, locacionId)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerSubsistema.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val subsistemaId = obtenerIdDesdeSpinner(spinnerSubsistema)
                cargarTiposEquipo(subsistemaId)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerTipoEquipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val locacionId = obtenerIdDesdeSpinner(spinnerLocacion)
                val sistemaId = obtenerIdDesdeSpinner(spinnerSistema)
                val subsistemaId = obtenerIdDesdeSpinner(spinnerSubsistema)
                val tipoEquipoId = obtenerIdDesdeSpinner(spinnerTipoEquipo)
                cargarTagsEquipo(locacionId, sistemaId, subsistemaId, tipoEquipoId)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnAceptar.setOnClickListener {
            val tagId = obtenerIdDesdeSpinner(spinnerTagEquipo)
            val intent = Intent(this, MantenimientoCorrectivoActivity::class.java)
            intent.putExtra("tagId", tagId.toString())
            intent.putExtra("locacionDescripcion", obtenerDescripcionDesdeSpinner(spinnerLocacion))
            intent.putExtra("sistemaDescripcion", obtenerDescripcionDesdeSpinner(spinnerSistema))
            intent.putExtra("subsistemaDescripcion", obtenerDescripcionDesdeSpinner(spinnerSubsistema))
            intent.putExtra("tipoEquipoDescripcion", obtenerDescripcionDesdeSpinner(spinnerTipoEquipo))
            intent.putExtra("tagEquipoDescripcion", obtenerDescripcionDesdeSpinner(spinnerTagEquipo))
            startActivity(intent)
        }
    }

    private fun obtenerDescripcionDesdeSpinner(spinner: Spinner): String {
        val selected = spinner.selectedItem as? Pair<Int, String>
        return selected?.second ?: ""
    }

    private fun obtenerIdDesdeSpinner(spinner: Spinner): Int {
        val selected = spinner.selectedItem as? Pair<Int, String>
        return selected?.first ?: -1
    }

    private fun cargarLocaciones() {
        val datos = dbHelper.getLocaciones()
        spinnerLocacion.adapter = crearAdapter(datos)
    }

    private fun cargarSistemas(locacionId: Int) {
        val datos = dbHelper.getSistemas(locacionId)
        spinnerSistema.adapter = crearAdapter(datos)
    }

    private fun cargarSubsistemas(sistemaId: Int, locacionId: Int) {
        val datos = dbHelper.getSubsistemas(sistemaId, locacionId)
        spinnerSubsistema.adapter = crearAdapter(datos)
    }

    private fun cargarTiposEquipo(subsistemaId: Int) {
        val datos = dbHelper.getTiposEquipo(subsistemaId)
        spinnerTipoEquipo.adapter = crearAdapter(datos)
    }

    private fun cargarTagsEquipo(locacionId: Int, sistemaId: Int, subsistemaId: Int, tipoEquipoId: Int) {
        val datos = dbHelper.getTagsEquipo(locacionId, sistemaId, subsistemaId, tipoEquipoId)
        spinnerTagEquipo.adapter = crearAdapter(datos)
    }

    private fun crearAdapter(datos: List<Pair<Int, String>>): ArrayAdapter<Pair<Int, String>> {
        return object : ArrayAdapter<Pair<Int, String>>(this, android.R.layout.simple_spinner_item, datos) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.text = getItem(position)?.second ?: ""
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.text = getItem(position)?.second ?: ""
                return view
            }
        }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }
}
