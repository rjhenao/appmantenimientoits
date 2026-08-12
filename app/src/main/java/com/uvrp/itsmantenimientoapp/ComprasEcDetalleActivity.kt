package com.uvrp.itsmantenimientoapp

import ApiService
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ComprasEcDetalleActivity : AppCompatActivity() {

    private var ecId: Int = -1
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var tvError: TextView
    private lateinit var tvRef: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvSituacion: TextView
    private lateinit var rowPipeline: LinearLayout
    private lateinit var containerDuraciones: LinearLayout
    private lateinit var containerItems: LinearLayout
    private lateinit var containerHistorial: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(applicationContext)

        val prefs = getSharedPreferences("Sesion", MODE_PRIVATE)
        if (!prefs.getBoolean("puede_compras_seguimiento", prefs.getInt("idRol", -1) == 1)) {
            Toast.makeText(this, "No tiene permiso para ver Compras", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        ecId = intent.getIntExtra(ComprasSeguimientoActivity.EXTRA_EC_ID, -1)
        if (ecId <= 0) {
            Toast.makeText(this, "EC no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_compras_ec_detalle)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val nav = findViewById<NavigationView>(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawer, nav)

        swipe = findViewById(R.id.swipeDetalle)
        tvError = findViewById(R.id.tvDetError)
        tvRef = findViewById(R.id.tvDetRef)
        tvEstado = findViewById(R.id.tvDetEstado)
        tvSituacion = findViewById(R.id.tvDetSituacion)
        rowPipeline = findViewById(R.id.rowPipeline)
        containerDuraciones = findViewById(R.id.containerDuraciones)
        containerItems = findViewById(R.id.containerItems)
        containerHistorial = findViewById(R.id.containerHistorial)

        swipe.setOnRefreshListener { cargar() }
        cargar()
    }

    private fun cargar() {
        swipe.isRefreshing = true
        tvError.visibility = View.GONE
        RetrofitClient.instance.comprasDetalle(ecId).enqueue(object : Callback<ApiService.CompraDetalleResponse> {
            override fun onResponse(
                call: Call<ApiService.CompraDetalleResponse>,
                response: Response<ApiService.CompraDetalleResponse>
            ) {
                swipe.isRefreshing = false
                if (!response.isSuccessful) {
                    tvError.text = "Error al cargar detalle (${response.code()})"
                    tvError.visibility = View.VISIBLE
                    return
                }
                val ec = response.body()?.ec
                if (ec == null) {
                    tvError.text = "Sin datos de la EC"
                    tvError.visibility = View.VISIBLE
                    return
                }
                pintar(ec)
            }

            override fun onFailure(call: Call<ApiService.CompraDetalleResponse>, t: Throwable) {
                swipe.isRefreshing = false
                tvError.text = "Sin conexión: ${t.message ?: ""}"
                tvError.visibility = View.VISIBLE
            }
        })
    }

    private fun pintar(ec: ApiService.CompraEcDetalleDto) {
        tvRef.text = ec.referenciaEc ?: "EC #${ec.id}"
        tvEstado.text = ec.estadoEtiqueta ?: ec.estadoNombre ?: "—"

        val sb = StringBuilder()
        ec.elaborador?.let { sb.append("Elaborador: $it\n") }
        ec.tipoPedido?.let { sb.append("Tipo: $it\n") }
        sb.append("Días en estado: ${ec.diasEnEstado ?: "—"} · Proceso: ${ec.diasTotalesProceso ?: "—"}d\n")
        if (!ec.fechaRequeridaEntrega.isNullOrBlank()) {
            sb.append("Entrega requerida: ${ec.fechaRequeridaEntrega}")
            ec.diasParaEntrega?.let { sb.append(" ($it d)") }
            ec.porcentajePlazo?.let { sb.append(" · Plazo ${it.toInt()}%") }
            sb.append("\n")
        }
        val tot = ec.cotizacionTotal ?: 0
        if (tot > 0) {
            sb.append("Cotizaciones: ${ec.cotizacionRespondidas ?: 0}/$tot")
            sb.append(" (pend. ${ec.cotizacionPendientes ?: 0})\n")
        }
        sb.append("Siguiente: ${ec.siguientePaso ?: "—"}\n")
        sb.append("Urgencia: ${etiquetaUrgencia(ec.alertaGlobal, ec.porcentajePlazo)}")
        tvSituacion.text = sb.toString().trim()

        containerDuraciones.removeAllViews()
        val durs = ec.duracionesPorEstado.orEmpty()
        if (durs.isEmpty()) {
            containerDuraciones.addView(textoSecundario("Sin tiempos registrados"))
        } else {
            durs.forEach { d ->
                val nombre = d.estado ?: "—"
                val t = CompraUiHelper.formatSegundos(d.segundos)
                val label = if (d.actual == true) "$nombre · $t (actual)" else "$nombre · $t"
                val tv = bloqueTexto(label)
                if (d.actual == true) {
                    tv.setTypeface(null, Typeface.BOLD)
                    tv.setTextColor(ContextCompat.getColor(this, R.color.azul_corporativo))
                }
                containerDuraciones.addView(tv)
            }
        }

        rowPipeline.removeAllViews()
        ec.pipeline.orEmpty().forEach { step ->
            val tv = TextView(this).apply {
                text = step.label ?: step.key ?: "—"
                textSize = 11f
                setPadding(18, 12, 18, 12)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = 8
                layoutParams = lp
                when {
                    step.current == true -> {
                        setBackgroundColor(ContextCompat.getColor(context, R.color.azul_corporativo))
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                    }
                    step.done == true -> {
                        setBackgroundColor(Color.parseColor("#C8E6C9"))
                        setTextColor(Color.parseColor("#1B5E20"))
                    }
                    else -> {
                        setBackgroundColor(Color.parseColor("#ECEFF1"))
                        setTextColor(Color.parseColor("#607D8B"))
                    }
                }
            }
            rowPipeline.addView(tv)
        }

        containerItems.removeAllViews()
        val items = ec.items.orEmpty()
        if (items.isEmpty()) {
            containerItems.addView(textoSecundario("Sin ítems"))
        } else {
            items.forEach { it ->
                val line = buildString {
                    append(it.idDetalle ?: "#${it.id}")
                    append(" · ")
                    append(it.descripcion ?: "—")
                    if (it.cantidad != null) {
                        append(" · ")
                        append(it.cantidad)
                        it.unidad?.let { u -> append(" $u") }
                    }
                }
                containerItems.addView(bloqueTexto(line))
            }
        }

        containerHistorial.removeAllViews()
        val hist = ec.historial.orEmpty()
        if (hist.isEmpty()) {
            containerHistorial.addView(textoSecundario("Sin historial"))
        } else {
            hist.forEach { h ->
                val desde = h.desde ?: "—"
                val hacia = h.hacia ?: "—"
                val fecha = h.fecha?.replace('T', ' ')?.take(19) ?: ""
                val line = "$fecha\n$desde → $hacia\n${h.usuario ?: "Sistema"}"
                val obs = h.observacion?.takeIf { it.isNotBlank() }
                containerHistorial.addView(bloqueTexto(if (obs != null) "$line\n$obs" else line))
            }
        }
    }

    private fun etiquetaUrgencia(alerta: String?, pct: Double?): String {
        val p = pct?.let { "${it.toInt()}% del plazo" }
        return when (alerta) {
            "rojo" -> "Rojo (≥90%${p?.let { " · $it" } ?: ""})"
            "ambar" -> "Amarillo (70–90%${p?.let { " · $it" } ?: ""})"
            else -> "Verde (<70%${p?.let { " · $it" } ?: ""})"
        }
    }

    private fun textoSecundario(t: String): TextView = TextView(this).apply {
        text = t
        textSize = 13f
        setTextColor(Color.parseColor("#78909C"))
        setPadding(4, 8, 4, 8)
    }

    private fun bloqueTexto(t: String): TextView = TextView(this).apply {
        text = t
        textSize = 13f
        setTextColor(Color.parseColor("#263238"))
        setBackgroundResource(R.drawable.bg_info_field)
        setPadding(24, 20, 24, 20)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = 8
        layoutParams = lp
    }
}
