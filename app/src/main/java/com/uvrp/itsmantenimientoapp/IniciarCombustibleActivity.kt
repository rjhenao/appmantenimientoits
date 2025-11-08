package com.uvrp.itsmantenimientoapp

import ApiService.Vehiculo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class IniciarCombustibleActivity : AppCompatActivity() {

    private var idVehiculoSeleccionado: Int? = null
    private var placaa: String? = null
    private var vehiculoSeleccionado: Vehiculo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_iniciar_combustible)

        // Configurar header y menú
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawerLayout, navView)

        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompletePlacas)
        val btnRegistrarCombustible = findViewById<Button>(R.id.btnRegistrarCombustible)
        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
        val idUsuario = sharedPreferences.getInt("idUser", -1)

        // Cargar vehículos
        lifecycleScope.launch {
            try {
                val vehiculos = RetrofitClient.instance.getVehiculos()
                val placasList = vehiculos.map { it.placa }

                val adapter = object : ArrayAdapter<String>(
                    this@IniciarCombustibleActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    placasList.toMutableList()
                ) {
                    private var fullList: List<String> = placasList

                    override fun getFilter(): Filter {
                        return object : Filter() {
                            override fun performFiltering(constraint: CharSequence?): FilterResults {
                                val filterResults = FilterResults()
                                if (!constraint.isNullOrEmpty()) {
                                    val input = constraint.toString().trim().lowercase()
                                    val suggestions = fullList.filter {
                                        it.lowercase().contains(input)
                                    }
                                    filterResults.values = suggestions
                                    filterResults.count = suggestions.size
                                } else {
                                    filterResults.values = fullList
                                    filterResults.count = fullList.size
                                }
                                return filterResults
                            }

                            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                                clear()
                                if (results?.values != null) {
                                    @Suppress("UNCHECKED_CAST")
                                    addAll(results.values as List<String>)
                                }
                                notifyDataSetChanged()
                            }

                            override fun convertResultToString(resultValue: Any?): CharSequence {
                                return resultValue?.toString() ?: ""
                            }
                        }
                    }
                }

                autoComplete.setAdapter(adapter)
                autoComplete.threshold = 1

                autoComplete.setOnItemClickListener { parent, _, position, _ ->
                    val placaSeleccionada = parent.getItemAtPosition(position) as String
                    vehiculoSeleccionado = vehiculos.firstOrNull { it.placa == placaSeleccionada }
                    placaa = vehiculoSeleccionado?.placa
                    idVehiculoSeleccionado = vehiculoSeleccionado?.id

                    Log.d("AutoComplete", "Placa seleccionada: $placaa, ID: $idVehiculoSeleccionado")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@IniciarCombustibleActivity, "Error al cargar vehículos", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón Registrar Combustible
        btnRegistrarCombustible.setOnClickListener {
            if (vehiculoSeleccionado != null) {
                val idUsuario = sharedPreferences.getInt("idUser", -1)
                if (idUsuario == -1) {
                    Toast.makeText(this, "No se encontró el ID de usuario", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Validar offline: Verificar que existe un preoperacional iniciado (Estado = 2) del usuario
                val prefs = getSharedPreferences("PreoperacionalesIniciados", MODE_PRIVATE)
                val idPreoperacional = prefs.getInt("idPreoperacional_$idUsuario", -1)
                val idVehiculoGuardado = prefs.getInt("idVehiculo_$idUsuario", -1)

                if (idPreoperacional == -1 || idVehiculoGuardado != idVehiculoSeleccionado) {
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ Preoperacional Requerido")
                        .setMessage("Para registrar combustible, primero debe tener un preoperacional iniciado (Estado = 2) para este vehículo.\n\nEl preoperacional debe estar con el chequeo de actividades completo. Por favor, complete el chequeo de actividades del preoperacional antes de registrar combustible.")
                        .setPositiveButton("Entendido", null)
                        .show()
                    return@setOnClickListener
                }

                // Si existe preoperacional iniciado, continuar a RegistrarCombustibleActivity
                val placaGuardada = prefs.getString("placa_$idUsuario", null)
                val intent = Intent(this, RegistrarCombustibleActivity::class.java)
                intent.putExtra("idPreoperacional", idPreoperacional)
                intent.putExtra("idVehiculo", idVehiculoSeleccionado!!)
                intent.putExtra("idUsuario", idUsuario)
                intent.putExtra("placa", placaa ?: placaGuardada ?: "")
                startActivity(intent)

            } else {
                Toast.makeText(this, "Seleccione un vehículo válido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (HeaderHelper.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

