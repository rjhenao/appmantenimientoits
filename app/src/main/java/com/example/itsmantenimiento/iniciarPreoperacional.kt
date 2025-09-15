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
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper // Asegúrate de importar tu helper
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class iniciarPreoperacional : AppCompatActivity() {

    private var idVehiculoSeleccionado: Int? = null
    private var placaa: String? = null
    private var vehiculoSeleccionado: Vehiculo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_iniciar_preoperacional)

        // 1. Llama al HeaderHelper para configurar el menú y la barra de herramientas
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawerLayout, navView)

        // El resto de la lógica de tu actividad permanece igual
        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompletePlacas)
        val btnIniciar = findViewById<Button>(R.id.btnIniciarPreoperacional)
        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
        val idUsuario = sharedPreferences.getInt("idUser", -1)


        // ... (todo tu código de Retrofit, listeners del botón, etc., se queda como está)

        if (idUsuario != -1) {
            RetrofitClient.instance.validarUsuario(idUsuario)
                .enqueue(object : Callback<UsuarioValidadoResponse> {
                    override fun onResponse(call: Call<UsuarioValidadoResponse>, response: Response<UsuarioValidadoResponse>) {
                        if (response.isSuccessful) {
                            val mensaje = response.body()?.success ?: "Desconocido"
                            findViewById<TextView>(R.id.textEstadoSesion).text = "Estado de sesión: $mensaje"
                        } else {
                            Log.e("ValidarUsuario", "❌ Error: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<UsuarioValidadoResponse>, t: Throwable) {
                        Log.e("ValidarUsuario", "❌ Fallo de conexión: ${t.message}")
                    }
                })
        }

        lifecycleScope.launch {
            try {
                val vehiculos = RetrofitClient.instance.getVehiculos()
                val placasList = vehiculos.map { it.placa }

                val adapter = object : ArrayAdapter<String>(
                    this@iniciarPreoperacional,
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
                Toast.makeText(this@iniciarPreoperacional, "Error al cargar vehículos", Toast.LENGTH_SHORT).show()
            }
        }

        btnIniciar.setOnClickListener {
            if (vehiculoSeleccionado != null) {
                val idUsuario = sharedPreferences.getInt("idUser", -1)
                if (idUsuario == -1) {
                    Toast.makeText(this, "No se encontró el ID de usuario", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnIniciar.isEnabled = false
                val progressDialog = AlertDialog.Builder(this)
                    .setView(R.layout.dialog_loading)
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                RetrofitClient.instance.validarVehiculoLicencia(idUsuario, vehiculoSeleccionado!!.id)
                    .enqueue(object : Callback<ApiService.ValidarVehiculoResponse> {
                        override fun onResponse(call: Call<ApiService.ValidarVehiculoResponse>, response: Response<ApiService.ValidarVehiculoResponse>) {
                            progressDialog.dismiss()
                            btnIniciar.isEnabled = true

                            if (response.isSuccessful) {
                                val data = response.body()
                                val mensajes = mutableListOf<String>()
                                var estadoEncontrado: Int? = null
                                var esUsuarioActual = false

                                data?.let {
                                    if (it.vehiculo_con_preoperacional_abierto == true) {
                                        it.aVehiculo?.firstOrNull()?.let { v ->
                                            if (v.idUsuario != idUsuario) {
                                                mensajes.add("El vehículo: ${v.placa} tiene preoperacional abierto por: ${v.nombre}")
                                            } else {
                                                estadoEncontrado = v.estado
                                                esUsuarioActual = true
                                            }
                                        }
                                    }

                                    if (it.vehiculo_usuario_con_preoperacional_abierto == true) {
                                        it.aUsuario?.firstOrNull()?.let { u ->
                                            if (u.idVehiculo != vehiculoSeleccionado!!.id) {
                                                mensajes.add("El usuario: ${u.nombre} tiene un preoperacional abierto con: ${u.placa}")
                                            } else {
                                                estadoEncontrado = u.estado
                                                esUsuarioActual = true
                                            }
                                        }
                                    }

                                    if (it.licencia_vencida_estado == true) mensajes.add("¡La licencia está vencida!")
                                    if (it.licencia_bloqueadas == true) mensajes.add("La licencia está bloqueada.")
                                    if (it.licencia_vencida_fecha == true) mensajes.add("La licencia está vencida por fecha.")
                                    if (it.v_full_amparo == true) mensajes.add("El full amparo está vencido.")
                                    if (it.v_impuesto == true) mensajes.add("El impuesto está vencido.")
                                    if (it.v_soat == true) mensajes.add("El SOAT está vencido.")
                                    if (it.v_tecnomecanica == true) mensajes.add("La tecnomecánica está vencida.")
                                    if (it.v_estado == true) mensajes.add("Hay un estado pendiente de revisión.")
                                }

                                if (mensajes.isNotEmpty()) {
                                    AlertDialog.Builder(this@iniciarPreoperacional)
                                        .setTitle("Advertencias")
                                        .setMessage(mensajes.joinToString("\n"))
                                        .setPositiveButton("Cerrar", null)
                                        .show()
                                    return
                                }

                                if (esUsuarioActual) {
                                    when (estadoEncontrado) {
                                        1 -> startFormulario()
                                        2 -> startFinalizar()
                                        else -> abrirPreoperacional(idUsuario)
                                    }
                                } else {
                                    abrirPreoperacional(idUsuario)
                                }

                            } else {
                                Toast.makeText(this@iniciarPreoperacional, "Error de validación", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ApiService.ValidarVehiculoResponse>, t: Throwable) {
                            progressDialog.dismiss()
                            btnIniciar.isEnabled = true
                            Toast.makeText(this@iniciarPreoperacional, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                            FirebaseCrashlytics.getInstance().apply {
                                log("📡 Error en validarVehiculoLicencia")
                                setCustomKey("UsuarioID", idUsuario)
                                setCustomKey("VehiculoID", vehiculoSeleccionado?.id ?: -1)
                                recordException(t)
                            }
                        }
                    })
            } else {
                Toast.makeText(this, "Seleccione un vehículo válido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 2. Agrega esta función para que el icono de hamburguesa funcione
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (HeaderHelper.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startFormulario() {
        val intent = Intent(this, FormularioActividadActivity::class.java)
        intent.putExtra("idVehiculo", idVehiculoSeleccionado!!)
        intent.putExtra("placaa", placaa!!)
        startActivity(intent)
    }

    private fun startFinalizar() {
        val intent = Intent(this, FinalizarPreoperacionalActivity::class.java)
        intent.putExtra("idVehiculo", idVehiculoSeleccionado!!)
        intent.putExtra("placaa", placaa!!)
        startActivity(intent)
    }

    private fun abrirPreoperacional(idUsuario: Int) {
        val request = ApiService.PreoperacionalRequest(idVehiculoSeleccionado!!, idUsuario)
        RetrofitClient.instance.abrirPreoperacional(request)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        startFormulario()
                    } else {
                        AlertDialog.Builder(this@iniciarPreoperacional)
                            .setTitle("Error")
                            .setMessage("No se pudo registrar el preoperacional. Código: ${response.code()}")
                            .setPositiveButton("Cerrar", null)
                            .show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    AlertDialog.Builder(this@iniciarPreoperacional)
                        .setTitle("Error de red")
                        .setMessage("No se pudo conectar con el servidor.\n${t.localizedMessage}")
                        .setPositiveButton("Cerrar", null)
                        .show()
                }
            })
    }
}

