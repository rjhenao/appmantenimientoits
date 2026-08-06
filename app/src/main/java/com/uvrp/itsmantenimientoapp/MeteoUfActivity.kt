package com.uvrp.itsmantenimientoapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MeteoUfActivity : AppCompatActivity() {

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var container: LinearLayout
    private lateinit var tvMeta: TextView
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(applicationContext)
        setContentView(R.layout.activity_meteo_uf)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val nav = findViewById<NavigationView>(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawer, nav)

        swipe = findViewById(R.id.swipeMeteo)
        container = findViewById(R.id.containerMeteoUfs)
        tvMeta = findViewById(R.id.tvMeteoMeta)
        tvError = findViewById(R.id.tvMeteoError)

        swipe.setOnRefreshListener { cargar(true) }
        cargar(false)
    }

    private fun cargar(force: Boolean) {
        swipe.isRefreshing = true
        tvError.visibility = View.GONE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val r = RetrofitClient.instance.meteoUf(if (force) 1 else null).execute()
                    if (r.isSuccessful) Pair(r.body(), null as String?)
                    else Pair(null, r.body()?.message ?: "HTTP ${r.code()}")
                } catch (e: Exception) {
                    Pair(null, e.message ?: "Sin conexión")
                }
            }
            swipe.isRefreshing = false
            val body = result.first
            val err = result.second
            if (body == null) {
                tvError.visibility = View.VISIBLE
                tvError.text = err ?: "No se pudo consultar el clima"
                Toast.makeText(this@MeteoUfActivity, tvError.text, Toast.LENGTH_SHORT).show()
                return@launch
            }
            tvMeta.text = "Actualizado: ${formatFecha(body.generadoEn)} · ${body.zonaHoraria ?: "America/Bogota"}"
            container.removeAllViews()
            val inflater = LayoutInflater.from(this@MeteoUfActivity)
            for (uf in body.ufs.orEmpty()) {
                val row = inflater.inflate(R.layout.item_meteo_uf, container, false)
                row.findViewById<TextView>(R.id.tvMeteoUfName).text = uf.uf ?: "UF"
                row.findViewById<TextView>(R.id.tvMeteoEstacion).text = uf.estacion ?: ""

                val chipLluvia = row.findViewById<TextView>(R.id.chipLluvia)
                val chipTemp = row.findViewById<TextView>(R.id.chipTemp)
                val chipHum = row.findViewById<TextView>(R.id.chipHumedad)
                chipTemp.background = rounded(Color.parseColor("#ECEFF1"), 12f)
                chipHum.background = rounded(Color.parseColor("#E0F2F1"), 12f)

                if (uf.lloviendo == true) {
                    val sev = uf.severidad ?: "—"
                    val mm = String.format(Locale.US, "%.2f", uf.intensidadMm ?: 0.0)
                    chipLluvia.text = "Lluvia\n$mm mm · $sev"
                    chipLluvia.setTextColor(Color.parseColor("#B71C1C"))
                    chipLluvia.background = rounded(Color.parseColor("#FFCDD2"), 12f)
                } else {
                    chipLluvia.text = "Sin lluvia"
                    chipLluvia.setTextColor(Color.parseColor("#1565C0"))
                    chipLluvia.background = rounded(Color.parseColor("#E3F2FD"), 12f)
                }
                chipTemp.text = "Temp\n${fmtNum(uf.temperaturaC)} °C"
                chipHum.text = "Humedad\n${fmtNum(uf.humedad)} %"

                val tend = (uf.precipTendencia ?: "SIN_DATO").uppercase()
                val tendLabel = when (tend) {
                    "SUBE" -> "Tendencia precipitación: SUBE (aumentando)"
                    "BAJA" -> "Tendencia precipitación: BAJA (disminuyendo)"
                    "ESTABLE" -> "Tendencia precipitación: ESTABLE"
                    else -> "Tendencia precipitación: sin dato"
                }
                var tendExtra = tendLabel
                if (uf.duracionMinutos != null && uf.lloviendo == true) {
                    tendExtra += " · Episodio lluvia: ${uf.duracionMinutos} min"
                }
                row.findViewById<TextView>(R.id.tvMeteoTendencia).text = tendExtra

                val alertas = mutableListOf<String>()
                if (uf.vientoAlto == true) alertas.add("Viento alto")
                if (uf.posibleNiebla == true) alertas.add("Posible niebla")
                val tvAlertas = row.findViewById<TextView>(R.id.tvMeteoAlertas)
                if (alertas.isNotEmpty()) {
                    tvAlertas.visibility = View.VISIBLE
                    tvAlertas.text = alertas.joinToString(" · ")
                } else {
                    tvAlertas.visibility = View.GONE
                }

                val box = row.findViewById<LinearLayout>(R.id.boxCierres)
                val tvTit = row.findViewById<TextView>(R.id.tvCierresTitulo)
                val tvDet = row.findViewById<TextView>(R.id.tvCierresDetalle)
                val items = uf.cierresActivosUf?.items.orEmpty()
                val cant = uf.cierresActivosUf?.cantidad ?: items.size
                if (cant > 0) {
                    box.visibility = View.VISIBLE
                    box.background = rounded(Color.parseColor("#FFEBEE"), 12f)
                    tvTit.text = if (cant == 1) "1 cierre activo" else "$cant cierres activos"
                    tvDet.text = if (items.isEmpty()) {
                        "Detalle de PR no disponible en esta respuesta."
                    } else {
                        items.joinToString("\n") { c ->
                            val tipo = c.tipoEvento ?: "Cierre"
                            val pr = listOfNotNull(c.prInicial, c.prFinal)
                                .joinToString(" → ")
                                .ifEmpty { "PR no informado" }
                            val desde = formatFecha(c.fechaHoraInicio)
                            "• $tipo\n  PR $pr\n  Desde $desde"
                        }
                    }
                } else {
                    box.visibility = View.GONE
                }

                container.addView(row)
            }
        }
    }

    private fun rounded(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
        }

    private fun fmtNum(v: Double?): String =
        if (v == null) "—" else String.format(Locale.US, "%.1f", v)

    private fun formatFecha(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CO")).apply {
                timeZone = TimeZone.getTimeZone("America/Bogota")
            }
            val parsed = try {
                inFmt.parse(raw)
            } catch (_: Exception) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(raw)
            }
            if (parsed != null) outFmt.format(parsed) else raw
        } catch (_: Exception) {
            raw
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (HeaderHelper.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }
}
