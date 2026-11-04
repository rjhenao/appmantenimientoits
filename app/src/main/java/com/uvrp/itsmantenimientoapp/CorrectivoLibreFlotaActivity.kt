package com.uvrp.itsmantenimientoapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CorrectivoLibreFlotaActivity : AppCompatActivity() {

    private var tipos = listOf<ApiService.FlotaLibreTipo>()
    private var vehiculos = listOf<ApiService.FlotaPlaca>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_correctivo_libre_flota)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_home)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Correctivo libre"
        toolbar.setNavigationOnClickListener { finish() }
        findViewById<Button>(R.id.btnContinuarCorrectivo).setOnClickListener { continuar() }
        cargar()
    }

    private fun cargar() {
        val idUsuario = getSharedPreferences("Sesion", MODE_PRIVATE).getInt("idUser", -1)
        RetrofitClient.instance.mantenimientoFlotaCorrectivoLibre(idUsuario)
            .enqueue(object : Callback<ApiService.FlotaLibreResponse> {
                override fun onResponse(call: Call<ApiService.FlotaLibreResponse>, response: Response<ApiService.FlotaLibreResponse>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@CorrectivoLibreFlotaActivity, "Sin permiso para este módulo", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }
                    tipos = response.body()?.tipos.orEmpty()
                    vehiculos = response.body()?.vehiculos.orEmpty()
                    if (tipos.isEmpty()) {
                        Toast.makeText(this@CorrectivoLibreFlotaActivity, "No hay un correctivo libre en la parametrización", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }
                    if (vehiculos.isEmpty()) {
                        Toast.makeText(this@CorrectivoLibreFlotaActivity, "No hay vehículos activos", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }
                    val placas = ArrayAdapter(this@CorrectivoLibreFlotaActivity, android.R.layout.simple_spinner_dropdown_item, vehiculos.map { it.placa })
                    val nombres = ArrayAdapter(this@CorrectivoLibreFlotaActivity, android.R.layout.simple_spinner_dropdown_item, tipos.map { it.nombre })
                    findViewById<Spinner>(R.id.spinnerPlaca).adapter = placas
                    findViewById<Spinner>(R.id.spinnerTipo).adapter = nombres
                }

                override fun onFailure(call: Call<ApiService.FlotaLibreResponse>, t: Throwable) {
                    Toast.makeText(this@CorrectivoLibreFlotaActivity, "No se pudo cargar: ${t.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            })
    }

    private fun continuar() {
        val tipo = tipos.getOrNull(findViewById<Spinner>(R.id.spinnerTipo).selectedItemPosition) ?: return
        val vehiculo = vehiculos.getOrNull(findViewById<Spinner>(R.id.spinnerPlaca).selectedItemPosition) ?: return
        val item = ApiService.FlotaPendiente(
            id = 0,
            placa = vehiculo.placa,
            nombre = tipo.nombre,
            clase = "correctivo",
            modo = "libre",
            situacion = null,
            cadaKm = null,
            proximoKm = null,
            lecturaKm = null,
            cadaHoras = null,
            proximoHorometro = null,
            lecturaHorometro = null,
            actividades = tipo.actividades,
            idTipo = tipo.id,
            idVehiculo = vehiculo.id
        )
        startActivity(Intent(this, EjecutarMantenimientoFlotaActivity::class.java).apply {
            putExtra("item", Gson().toJson(item))
        })
        finish()
    }
}
