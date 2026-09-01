package com.uvrp.itsmantenimientoapp



import ApiService

import android.graphics.Bitmap

import android.graphics.Color

import android.graphics.Typeface

import android.graphics.pdf.PdfRenderer

import android.os.Bundle

import android.os.ParcelFileDescriptor

import android.view.View

import android.widget.CheckBox

import android.widget.EditText

import android.widget.ImageView

import android.widget.LinearLayout

import android.widget.ProgressBar

import android.widget.ScrollView

import android.widget.TextView

import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.ContextCompat

import androidx.drawerlayout.widget.DrawerLayout

import androidx.lifecycle.lifecycleScope

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.google.android.material.button.MaterialButton

import com.google.android.material.navigation.NavigationView

import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.Job

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import org.json.JSONObject

import retrofit2.Call

import retrofit2.Callback

import retrofit2.Response

import java.io.File

import java.text.NumberFormat

import java.util.Locale



class ComprasEcDetalleActivity : AppCompatActivity() {



    private var ecId: Int = -1

    private var enviando = false

    private var modoAdjudicacion = false

    private var cargandoPdf = false

    private var pdfJob: Job? = null

    private val pdfBitmaps = mutableListOf<Bitmap>()



    private lateinit var swipe: SwipeRefreshLayout

    private lateinit var scrollDetalle: ScrollView

    private lateinit var tvError: TextView

    private lateinit var tvRef: TextView

    private lateinit var tvEstado: TextView

    private lateinit var tvSituacion: TextView

    private lateinit var rowPipeline: LinearLayout

    private lateinit var containerDuraciones: LinearLayout

    private lateinit var containerItems: LinearLayout

    private lateinit var containerHistorial: LinearLayout

    private lateinit var blockAdjudicacion: LinearLayout

    private lateinit var containerAdjudicacion: LinearLayout

    private lateinit var blockAcciones: LinearLayout

    private lateinit var tvAccionesTitulo: TextView

    private lateinit var etObservacion: EditText

    private lateinit var cbAceptarFirma: CheckBox

    private lateinit var btnAprobar: MaterialButton

    private lateinit var btnRechazar: MaterialButton

    private lateinit var btnPdfEc: MaterialButton

    private lateinit var btnPdfCdp: MaterialButton

    private lateinit var blockPdfViewer: LinearLayout

    private lateinit var tvPdfTitulo: TextView

    private lateinit var tvPdfEstado: TextView

    private lateinit var progressPdf: ProgressBar

    private lateinit var pdfZoomContainer: PinchZoomLayout

    private lateinit var containerPdfPages: LinearLayout

    private lateinit var btnCerrarPdf: MaterialButton



    private val money = NumberFormat.getCurrencyInstance(Locale("es", "CO"))



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

        scrollDetalle = findViewById(R.id.scrollDetalle)

        tvError = findViewById(R.id.tvDetError)

        tvRef = findViewById(R.id.tvDetRef)

        tvEstado = findViewById(R.id.tvDetEstado)

        tvSituacion = findViewById(R.id.tvDetSituacion)

        rowPipeline = findViewById(R.id.rowPipeline)

        containerDuraciones = findViewById(R.id.containerDuraciones)

        containerItems = findViewById(R.id.containerItems)

        containerHistorial = findViewById(R.id.containerHistorial)

        blockAdjudicacion = findViewById(R.id.blockAdjudicacion)

        containerAdjudicacion = findViewById(R.id.containerAdjudicacion)

        blockAcciones = findViewById(R.id.blockAcciones)

        tvAccionesTitulo = findViewById(R.id.tvAccionesTitulo)

        etObservacion = findViewById(R.id.etObservacionGerencia)

        cbAceptarFirma = findViewById(R.id.cbAceptarFirma)

        btnAprobar = findViewById(R.id.btnAprobar)

        btnRechazar = findViewById(R.id.btnRechazar)

        btnPdfEc = findViewById(R.id.btnPdfEc)

        btnPdfCdp = findViewById(R.id.btnPdfCdp)

        blockPdfViewer = findViewById(R.id.blockPdfViewer)

        tvPdfTitulo = findViewById(R.id.tvPdfTitulo)

        tvPdfEstado = findViewById(R.id.tvPdfEstado)

        progressPdf = findViewById(R.id.progressPdf)

        pdfZoomContainer = findViewById(R.id.pdfZoomContainer)

        containerPdfPages = findViewById(R.id.containerPdfPages)

        btnCerrarPdf = findViewById(R.id.btnCerrarPdf)



        swipe.setOnRefreshListener { cargar() }

        btnPdfEc.setOnClickListener { cargarPdf(esCdp = false) }

        btnPdfCdp.setOnClickListener { cargarPdf(esCdp = true) }

        btnCerrarPdf.setOnClickListener { ocultarPdf() }

        btnAprobar.setOnClickListener { enviarDecision(aprobar = true) }

        btnRechazar.setOnClickListener { enviarDecision(aprobar = false) }

        cargar()

    }



    override fun onDestroy() {

        pdfJob?.cancel()

        liberarBitmapsPdf()

        super.onDestroy()

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

                    tvError.text = mensajeHttp(response)

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



        pintarAdjudicacion(ec.adjudicacion)

        pintarAcciones(ec.acciones)



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



    private fun pintarAdjudicacion(adj: ApiService.CompraAdjudicacionDto?) {

        val lineas = adj?.lineas.orEmpty()

        if (adj == null || lineas.isEmpty()) {

            blockAdjudicacion.visibility = View.GONE

            return

        }

        blockAdjudicacion.visibility = View.VISIBLE

        containerAdjudicacion.removeAllViews()

        adj.justificacion?.takeIf { it.isNotBlank() }?.let {

            containerAdjudicacion.addView(bloqueTexto("Justificación: $it"))

        }

        lineas.forEach { l ->

            val monto = l.monto?.let { money.format(it) } ?: "—"

            val opcion = l.etiquetaOpcion?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""

            containerAdjudicacion.addView(

                bloqueTexto("${l.item ?: "Ítem"}\n${l.proveedor ?: "Proveedor"}$opcion\n$monto")

            )

        }

    }



    private fun pintarAcciones(a: ApiService.CompraAccionesDto?) {

        val puedeEc = a?.puedeAprobarEc == true || a?.puedeRechazarEc == true

        val puedeAdj = a?.puedeAprobarAdjudicacion == true || a?.puedeDevolverAdjudicacion == true

        if (!puedeEc && !puedeAdj) {

            blockAcciones.visibility = View.GONE

            return

        }

        blockAcciones.visibility = View.VISIBLE

        modoAdjudicacion = puedeAdj

        if (puedeAdj) {

            tvAccionesTitulo.text = "Autorizar adjudicación"

            btnAprobar.text = "Aprobar"

            btnRechazar.text = "Devolver"

            btnAprobar.visibility = if (a?.puedeAprobarAdjudicacion == true) View.VISIBLE else View.GONE

            btnRechazar.visibility = if (a?.puedeDevolverAdjudicacion == true) View.VISIBLE else View.GONE

        } else {

            tvAccionesTitulo.text = "Autorizar especificación"

            btnAprobar.text = "Aprobar"

            btnRechazar.text = "Rechazar"

            btnAprobar.visibility = if (a?.puedeAprobarEc == true) View.VISIBLE else View.GONE

            btnRechazar.visibility = if (a?.puedeRechazarEc == true) View.VISIBLE else View.GONE

        }

    }



    private fun enviarDecision(aprobar: Boolean) {

        if (enviando) return

        val obs = etObservacion.text?.toString()?.trim().orEmpty()

        if (aprobar && !cbAceptarFirma.isChecked) {

            Toast.makeText(this, "Debe aceptar y firmar digitalmente", Toast.LENGTH_LONG).show()

            return

        }

        val minObs = if (!aprobar && modoAdjudicacion) 5 else 2

        if (obs.length < minObs && !(aprobar && modoAdjudicacion && obs.isEmpty())) {

            val msg = if (!aprobar && modoAdjudicacion) {

                "Indique el motivo (mínimo 5 caracteres)"

            } else {

                "Escriba una observación (mínimo 2 caracteres, o NA)"

            }

            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

            return

        }



        val body = ApiService.CompraGerenciaDecisionRequest(

            observacion = if (obs.isEmpty()) "NA" else obs,

            aceptarFirma = if (aprobar) 1 else null

        )

        enviando = true

        setAccionesEnabled(false)



        val call = when {

            modoAdjudicacion && aprobar -> RetrofitClient.instance.comprasAprobarAdjudicacion(ecId, body)

            modoAdjudicacion -> RetrofitClient.instance.comprasDevolverAdjudicacion(ecId, body)

            aprobar -> RetrofitClient.instance.comprasAprobarGerencia(ecId, body)

            else -> RetrofitClient.instance.comprasRechazarGerencia(ecId, body)

        }



        call.enqueue(object : Callback<ApiService.CompraAccionResponse> {

            override fun onResponse(

                call: Call<ApiService.CompraAccionResponse>,

                response: Response<ApiService.CompraAccionResponse>

            ) {

                enviando = false

                setAccionesEnabled(true)

                if (!response.isSuccessful) {

                    Toast.makeText(this@ComprasEcDetalleActivity, mensajeHttp(response), Toast.LENGTH_LONG).show()

                    return

                }

                val msg = response.body()?.message ?: "Decisión registrada"

                Toast.makeText(this@ComprasEcDetalleActivity, msg, Toast.LENGTH_LONG).show()

                etObservacion.setText("")

                cbAceptarFirma.isChecked = false

                cargar()

            }



            override fun onFailure(call: Call<ApiService.CompraAccionResponse>, t: Throwable) {

                enviando = false

                setAccionesEnabled(true)

                Toast.makeText(

                    this@ComprasEcDetalleActivity,

                    "Sin conexión: ${t.message ?: ""}",

                    Toast.LENGTH_LONG

                ).show()

            }

        })

    }



    private fun cargarPdf(esCdp: Boolean) {

        if (cargandoPdf) return

        val titulo = if (esCdp) "Certificado presupuestal (CDP)" else "Especificación de compra (EC)"

        val nombre = if (esCdp) "cdp_$ecId.pdf" else "ec_$ecId.pdf"

        val call = if (esCdp) {

            RetrofitClient.instance.comprasPdfCdp(ecId)

        } else {

            RetrofitClient.instance.comprasPdfEc(ecId)

        }



        mostrarCargandoPdf(titulo)

        cargandoPdf = true

        setPdfButtonsEnabled(false)

        pdfJob?.cancel()



        pdfJob = lifecycleScope.launch {

            try {

                val response = withContext(Dispatchers.IO) { call.execute() }

                if (!response.isSuccessful) {

                    mostrarErrorPdf(mensajeHttp(response))

                    return@launch

                }

                val bytes = withContext(Dispatchers.IO) { response.body()?.bytes() }
                if (bytes == null || bytes.isEmpty()) {
                    mostrarErrorPdf("El documento llegó vacío")
                    return@launch
                }
                val file = withContext(Dispatchers.IO) {
                    val dir = File(cacheDir, "compras_pdfs").apply { mkdirs() }
                    File(dir, nombre).apply { writeBytes(bytes) }
                }

                renderizarPdfEnPantalla(file, titulo)

            } catch (e: Exception) {

                mostrarErrorPdf("No se pudo cargar el PDF: ${e.message ?: "error desconocido"}")

            } finally {

                cargandoPdf = false

                setPdfButtonsEnabled(true)

            }

        }

    }



    private fun mostrarCargandoPdf(titulo: String) {

        blockPdfViewer.visibility = View.VISIBLE

        tvPdfTitulo.text = titulo

        tvPdfEstado.visibility = View.VISIBLE

        tvPdfEstado.text = "Cargando documento…"

        tvPdfEstado.setTextColor(Color.parseColor("#546E7A"))

        progressPdf.visibility = View.VISIBLE

        containerPdfPages.removeAllViews()

        pdfZoomContainer.resetZoom()

        liberarBitmapsPdf()

        scrollDetalle.post { scrollDetalle.smoothScrollTo(0, blockPdfViewer.top) }

    }



    private fun mostrarErrorPdf(mensaje: String) {

        blockPdfViewer.visibility = View.VISIBLE

        progressPdf.visibility = View.GONE

        containerPdfPages.removeAllViews()

        pdfZoomContainer.resetZoom()

        liberarBitmapsPdf()

        tvPdfEstado.visibility = View.VISIBLE

        tvPdfEstado.text = mensaje

        tvPdfEstado.setTextColor(Color.parseColor("#C62828"))

        scrollDetalle.post { scrollDetalle.smoothScrollTo(0, blockPdfViewer.top) }

    }



    private fun renderizarPdfEnPantalla(file: File, titulo: String) {

        blockPdfViewer.visibility = View.VISIBLE

        tvPdfTitulo.text = titulo

        progressPdf.visibility = View.GONE

        tvPdfEstado.visibility = View.GONE

        containerPdfPages.removeAllViews()

        pdfZoomContainer.resetZoom()

        liberarBitmapsPdf()



        try {

            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->

                PdfRenderer(pfd).use { renderer ->

                    if (renderer.pageCount == 0) {

                        mostrarErrorPdf("El PDF no tiene páginas")

                        return

                    }

                    val pageWidth = (resources.displayMetrics.widthPixels - dp(56)).coerceAtLeast(1)

                    for (i in 0 until renderer.pageCount) {

                        renderer.openPage(i).use { page ->

                            val ratio = pageWidth.toFloat() / page.width.coerceAtLeast(1)

                            val height = (page.height * ratio).toInt().coerceAtLeast(1)

                            val bitmap = Bitmap.createBitmap(pageWidth, height, Bitmap.Config.ARGB_8888)

                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            pdfBitmaps.add(bitmap)



                            val iv = ImageView(this).apply {

                                setImageBitmap(bitmap)

                                adjustViewBounds = true

                                scaleType = ImageView.ScaleType.FIT_CENTER

                                contentDescription = "Página ${i + 1}"

                                val lp = LinearLayout.LayoutParams(

                                    LinearLayout.LayoutParams.MATCH_PARENT,

                                    LinearLayout.LayoutParams.WRAP_CONTENT

                                )

                                lp.bottomMargin = dp(8)

                                layoutParams = lp

                            }

                            containerPdfPages.addView(iv)

                        }

                    }

                }

            }

            scrollDetalle.post { scrollDetalle.smoothScrollTo(0, blockPdfViewer.top) }

        } catch (e: Exception) {

            mostrarErrorPdf("No se pudo mostrar el PDF: ${e.message ?: "formato no válido"}")

        }

    }



    private fun ocultarPdf() {

        blockPdfViewer.visibility = View.GONE

        containerPdfPages.removeAllViews()

        pdfZoomContainer.resetZoom()

        liberarBitmapsPdf()

        tvPdfEstado.visibility = View.GONE

        progressPdf.visibility = View.GONE

    }



    private fun liberarBitmapsPdf() {

        pdfBitmaps.forEach { bmp ->

            if (!bmp.isRecycled) bmp.recycle()

        }

        pdfBitmaps.clear()

    }



    private fun setPdfButtonsEnabled(enabled: Boolean) {

        btnPdfEc.isEnabled = enabled

        btnPdfCdp.isEnabled = enabled

    }



    private fun setAccionesEnabled(enabled: Boolean) {

        btnAprobar.isEnabled = enabled

        btnRechazar.isEnabled = enabled

        etObservacion.isEnabled = enabled

        cbAceptarFirma.isEnabled = enabled

    }



    private fun dp(value: Int): Int =

        (value * resources.displayMetrics.density).toInt()



    private fun mensajeHttp(response: Response<*>): String {

        val raw = try {

            response.errorBody()?.string()

        } catch (_: Exception) {

            null

        }

        if (!raw.isNullOrBlank()) {

            try {

                val json = JSONObject(raw)

                val msg = json.optString("message").ifBlank { null }

                val errors = json.optJSONObject("errors")

                val firstError = errors?.keys()?.asSequence()?.firstOrNull()?.let { key ->

                    errors.optJSONArray(key)?.optString(0)

                }

                return firstError ?: msg ?: "Error ${response.code()}"

            } catch (_: Exception) {

                return raw.take(180)

            }

        }

        return "Error ${response.code()}"

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

