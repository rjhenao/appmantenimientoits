package com.uvrp.itsmantenimientoapp

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MantenimientoFlotaActivity : AppCompatActivity() {

    private var carga = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mantenimiento_flota)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val nav = findViewById<NavigationView>(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawer, nav)
        supportActionBar?.title = "Mantenimiento de flota"
        findViewById<Button>(R.id.btnCorrectivoLibre).setOnClickListener {
            startActivity(Intent(this, CorrectivoLibreFlotaActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (HeaderHelper.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 91 && resultCode == RESULT_OK) {
            val id = data?.getIntExtra("id_ciclo", -1) ?: -1
            quitar(id)
            cargar()
        }
    }

    private fun quitar(id: Int) {
        val lista = findViewById<LinearLayout>(R.id.listaMantenimientos)
        for (i in lista.childCount - 1 downTo 0) {
            if (lista.getChildAt(i).tag == id) {
                lista.removeViewAt(i)
            }
        }
        if (lista.childCount == 0) {
            findViewById<TextView>(R.id.textoVacio).visibility = android.view.View.VISIBLE
        }
    }

    private fun cargar() {
        val token = ++carga
        val idUsuario = getSharedPreferences("Sesion", MODE_PRIVATE).getInt("idUser", -1)
        val lista = findViewById<LinearLayout>(R.id.listaMantenimientos)
        val vacio = findViewById<TextView>(R.id.textoVacio)
        RetrofitClient.instance.mantenimientoFlotaPendientes(idUsuario)
            .enqueue(object : Callback<ApiService.FlotaPendientesResponse> {
                override fun onResponse(call: Call<ApiService.FlotaPendientesResponse>, response: Response<ApiService.FlotaPendientesResponse>) {
                    if (token != carga) return
                    if (!response.isSuccessful) {
                        Toast.makeText(this@MantenimientoFlotaActivity, "Sin permiso para este módulo", Toast.LENGTH_LONG).show()
                        return
                    }
                    val items = response.body()?.items.orEmpty()
                    lista.removeAllViews()
                    vacio.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                    items.forEach { item ->
                        val caja = LinearLayout(this@MantenimientoFlotaActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(24, 24, 24, 24)
                            setBackgroundColor(0xFFFFFFFF.toInt())
                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.bottomMargin = 16
                            layoutParams = params
                            tag = item.id
                        }
                        caja.addView(TextView(this@MantenimientoFlotaActivity).apply {
                            text = "${item.placa ?: "—"} · ${item.nombre ?: "Mantenimiento"}"
                            setTypeface(typeface, Typeface.BOLD)
                        })
                        val situacion = when (item.situacion) {
                            "vencido" -> "Vencido"
                            "proximo" -> "Próximo"
                            else -> "Programado"
                        }
                        val clase = if (item.clase == "correctivo") "Correctivo" else "Preventivo"
                        val cuando = item.fechaProgramada?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                        caja.addView(TextView(this@MantenimientoFlotaActivity).apply { text = "$clase · $situacion$cuando" })
                        caja.addView(Button(this@MantenimientoFlotaActivity).apply {
                            text = "Realizar"
                            setOnClickListener {
                                startActivityForResult(Intent(this@MantenimientoFlotaActivity, EjecutarMantenimientoFlotaActivity::class.java).apply {
                                    putExtra("item", Gson().toJson(item))
                                }, 91)
                            }
                        })
                        lista.addView(caja)
                    }
                }

                override fun onFailure(call: Call<ApiService.FlotaPendientesResponse>, t: Throwable) {
                    if (token != carga) return
                    Toast.makeText(this@MantenimientoFlotaActivity, "No se pudo cargar: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}
