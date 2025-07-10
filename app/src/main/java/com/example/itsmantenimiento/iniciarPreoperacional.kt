package com.uvrp.itsmantenimientoapp

import ApiService.Vehiculo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class iniciarPreoperacional : AppCompatActivity() {

    private var idVehiculoSeleccionado: Int? = null
    private var placaa: String? = null
    private var vehiculoSeleccionado: Vehiculo? = null
    private var valUsu: Int = 0
    private var valEstadoUsu: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_iniciar_preoperacional)

        val toolbar: Toolbar = findViewById(R.id.toolbar_home)
        setSupportActionBar(toolbar)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val spinner: Spinner = findViewById(R.id.spinnerOpciones)
        val btnIniciar = findViewById<Button>(R.id.btnIniciarPreoperacional)

        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
        val idUsuario = sharedPreferences.getInt("idUser", -1)

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_its -> {
                    val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
                    val idRol = sharedPreferences.getInt("idRol", -1)
                    if (idRol == 1 || idRol == 2) {
                        val intent = Intent(this, Nivel1Activity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No tiene permisos para acceder como ITS.", Toast.LENGTH_LONG).show()
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


        if (idUsuario != -1) {
            RetrofitClient.instance.validarUsuario(idUsuario)
                .enqueue(object : Callback<UsuarioValidadoResponse> {
                    override fun onResponse(
                        call: Call<UsuarioValidadoResponse>,
                        response: Response<UsuarioValidadoResponse>
                    ) {
                        if (response.isSuccessful) {
                            val mensaje = response.body()?.success ?: "Desconocido"
                            val estadoView = findViewById<TextView>(R.id.textEstadoSesion)
                            estadoView.text = "Estado de sesión: $mensaje"

                        } else {
                            Log.e("ValidarUsuario", "❌ Error: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<UsuarioValidadoResponse>, t: Throwable) {
                        Log.e("ValidarUsuario", "❌ Fallo de conexión: ${t.message}")
                    }
                })
        } else {
            Log.e("ValidarUsuario", "No se encontró idUser en SharedPreferences")
        }


        btnIniciar.setOnClickListener {
            if (vehiculoSeleccionado != null) {
                val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
                val idUsuario = sharedPreferences.getInt("idUser", -1)

                if (idUsuario != -1) {
                    val call = RetrofitClient.instance.validarVehiculoLicencia(
                        idUsuario,
                        vehiculoSeleccionado!!.id
                    )

                    call.enqueue(object : Callback<ApiService.ValidarVehiculoResponse> {
                        override fun onResponse(
                            call: Call<ApiService.ValidarVehiculoResponse>,
                            response: Response<ApiService.ValidarVehiculoResponse>
                        ) {
                            if (response.isSuccessful) {
                                val data = response.body()
                                val mensajes = mutableListOf<String>()


                                data?.let {
                                    if (it.vehiculo_con_preoperacional_abierto == true) {
                                        it.aVehiculo?.firstOrNull()?.let { v ->
                                            if (v.idUsuario != idUsuario) {
                                                mensajes.add("El vehículo: ${v.placa} cuenta con preoperacional abierto por: ${v.nombre}")
                                            } else {
                                                valEstadoUsu = v.estado
                                                valUsu = 1
                                            }
                                        }
                                    }
                                    if (it.vehiculo_usuario_con_preoperacional_abierto == true) {
                                        it.aUsuario?.firstOrNull()?.let { u ->
                                            if (u.idVehiculo != vehiculoSeleccionado!!.id) {
                                                mensajes.add("El usuario: ${u.nombre} tiene un vehículo con preoperacional abierto: ${u.placa}.")
                                            } else {
                                                valEstadoUsu = u.estado
                                                valUsu = 1
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

                                // ✅ Mostrar diálogo si hay advertencias
                                if (mensajes.isNotEmpty()) {
                                    AlertDialog.Builder(this@iniciarPreoperacional)
                                        .setTitle("Advertencias")
                                        .setMessage(mensajes.joinToString("\n"))
                                        .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
                                        .show()
                                    return // Detiene la ejecución aquí
                                }

                                if (valUsu == 1) {
                                    when (valEstadoUsu) {
                                        1 -> {
                                            val intent = Intent(this@iniciarPreoperacional, FormularioActividadActivity::class.java)
                                            intent.putExtra("idVehiculo", idVehiculoSeleccionado!!)
                                            intent.putExtra("placaa", placaa!!)
                                            startActivity(intent)
                                        }
                                        2 -> {
                                            val intent = Intent(this@iniciarPreoperacional,
                                                FinalizarPreoperacionalActivity::class.java)
                                            intent.putExtra("idVehiculo", idVehiculoSeleccionado!!)
                                            intent.putExtra("placaa", placaa!!)
                                            startActivity(intent)
                                            //Toast.makeText(this@iniciarPreoperacional, "⚠️ Usuario inhabilitado temporalmente.", Toast.LENGTH_SHORT).show()
                                        }
                                        else -> {

                                        }
                                    }

                                } else {
                                    // Si no hay advertencias, continuar con la apertura del preoperacional
                                    val request = ApiService.PreoperacionalRequest(
                                        idVehiculo = idVehiculoSeleccionado!!,
                                        idUsuario = idUsuario
                                    )

                                    RetrofitClient.instance.abrirPreoperacional(request).enqueue(object : Callback<Void> {
                                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                            if (response.isSuccessful) {
                                                val intent = Intent(this@iniciarPreoperacional, FormularioActividadActivity::class.java)
                                                intent.putExtra("idVehiculo", idVehiculoSeleccionado!!)
                                                intent.putExtra("placaa", placaa!!)
                                                startActivity(intent)
                                            } else {
                                                AlertDialog.Builder(this@iniciarPreoperacional)
                                                    .setTitle("Error")
                                                    .setMessage("No se pudo registrar el preoperacional. Código: ${response.code()}")
                                                    .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
                                                    .show()
                                            }
                                        }

                                        override fun onFailure(call: Call<Void>, t: Throwable) {
                                            AlertDialog.Builder(this@iniciarPreoperacional)
                                                .setTitle("Error de red")
                                                .setMessage("No se pudo conectar con el servidor. Verifica tu conexión.\n${t.localizedMessage}")
                                                .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
                                                .show()
                                        }
                                    })
                                }


                            } else {
                                Log.e("API", "Error en la respuesta: ${response.errorBody()?.string()}")
                                Toast.makeText(
                                    this@iniciarPreoperacional,
                                    "Error en la respuesta del servidor",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<ApiService.ValidarVehiculoResponse>,
                            t: Throwable
                        ) {
                            Log.e("API", "Error en la llamada: ${t.message}")
                            Toast.makeText(
                                this@iniciarPreoperacional,
                                "Error al conectar con el servidor: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(this, "No se encontró el ID de usuario", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Seleccione un vehículo válido", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            try {
                val vehiculos = RetrofitClient.instance.getVehiculos()
                val placasList = mutableListOf("Seleccionar vehículo").apply {
                    addAll(vehiculos.map { it.placa })
                }

                val adapter = ArrayAdapter(
                    this@iniciarPreoperacional,
                    android.R.layout.simple_spinner_item,
                    placasList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>, view: View?, position: Int, id: Long
                    ) {
                        if (position == 0) {
                            vehiculoSeleccionado = null
                            return
                        }
                        vehiculoSeleccionado = vehiculos[position - 1]
                        idVehiculoSeleccionado = vehiculoSeleccionado?.id
                        placaa = vehiculoSeleccionado?.placa
                        Log.d("Spinner", "ID: ${vehiculoSeleccionado?.id}, Placa: ${placaa}")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@iniciarPreoperacional,
                    "Error al cargar vehículos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val toggle = androidx.appcompat.app.ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun logout() {
        Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show()
        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}
