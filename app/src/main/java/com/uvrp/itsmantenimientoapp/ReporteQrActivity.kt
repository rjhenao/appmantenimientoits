package com.uvrp.itsmantenimientoapp

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.uvrp.itsmantenimientoapp.models.PublicReporteLocacionItem
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

/**
 * Formulario público alineado con solicitud soporte ITS (Microsoft Forms).
 * No requiere login. La locación se toma del QR verificado en servidor.
 */
class ReporteQrActivity : AppCompatActivity() {

    private data class EnvioOutcome(
        val exito: Boolean,
        val ticketNumber: String? = null,
        val detalleUsuario: String? = null,
    )

    private lateinit var dbHelper: DatabaseHelper
    private var locacionId: Int = -1
    private var qrToken: String = ""
    /** Si es true, el envío usa [sin_escaneo_qr] en API (sin token del QR). */
    private var sinEscaneoQr: Boolean = false
    private var maxAttachments: Int = 4

    private val selectedUris = mutableListOf<Uri>()
    private lateinit var adjuntosAdapter: ReporteAdjuntosAdapter
    private var pendingCameraUri: Uri? = null

    private val api get() = RetrofitClient.instance

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (ok == true && uri != null) {
            addUrisFromPicker(listOf(uri))
        }
    }

    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris -> addUrisFromPicker(uris) }

    private val pickFilesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris -> addUrisFromPicker(uris) }

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val raw = result?.contents?.trim().orEmpty()
        if (raw.isEmpty()) return@registerForActivityResult
        val p = QrReporteParser.parse(raw)
        if (p == null) {
            Toast.makeText(this, getString(R.string.reporte_qr_invalido), Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }
        applyQrData(p.first, p.second)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_qr)
        RetrofitClient.init(applicationContext)
        dbHelper = DatabaseHelper(this)

        adjuntosAdapter = ReporteAdjuntosAdapter { pos ->
            if (pos in selectedUris.indices) {
                selectedUris.removeAt(pos)
                refreshAdjuntosUi()
            }
        }
        findViewById<RecyclerView>(R.id.rvAdjuntosPreview).apply {
            layoutManager = LinearLayoutManager(this@ReporteQrActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = adjuntosAdapter
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarReporteQr)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        toolbar.title = getString(R.string.reporte_public_title)

        findViewById<MaterialButton>(R.id.btnReporteQrEscanearLocacion).setOnClickListener {
            mostrarOpcionesLocacionDialog()
        }

        findViewById<MaterialButton>(R.id.btnAdjuntarArchivos).setOnClickListener { mostrarMenuAdjuntos() }

        findViewById<MaterialButton>(R.id.btnEnviarReporteQr).setOnClickListener { enviar() }

        CoroutineScope(Dispatchers.Main).launch { cargarCatalogosSpinners() }

        var applied = false
        val exLoc = intent.getIntExtra(EXTRA_LOCACION_ID, -1)
        val exTok = intent.getStringExtra(EXTRA_QR_TOKEN)?.trim().orEmpty()
        if (exLoc > 0 && exTok.length == 64) {
            applyQrData(exLoc, exTok)
            applied = true
        }
        if (!applied) {
            intent?.dataString?.let { deep ->
                QrReporteParser.parse(deep)?.let { applyQrData(it.first, it.second) }
            }
        }

        if (intent.getBooleanExtra(EXTRA_OPEN_LOCACION_PICKER, false)) {
            window.decorView.post { mostrarSelectorLocacion() }
        }
    }

    private fun mostrarOpcionesLocacionDialog() {
        val opts = arrayOf(
            getString(R.string.reporte_scan_opcion_escanear),
            getString(R.string.reporte_scan_opcion_sin_escanear),
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.reporte_scan_opcion_titulo)
            .setItems(opts) { _, which ->
                when (which) {
                    0 -> lanzarEscaneoQr()
                    1 -> mostrarSelectorLocacion()
                }
            }
            .show()
    }

    private fun lanzarEscaneoQr() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt(getString(R.string.reporte_qr_scan_prompt))
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(false)
        options.setOrientationLocked(true)
        scanLauncher.launch(options)
    }

    private fun mostrarSelectorLocacion() {
        CoroutineScope(Dispatchers.Main).launch {
            val lista: List<PublicReporteLocacionItem>? = withContext(Dispatchers.IO) {
                try {
                    val r = api.publicReporteLocaciones().execute()
                    val b = r.body()
                    if (r.isSuccessful && b?.success == true && !b.data.isNullOrEmpty()) {
                        b.data
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }

            val opciones: List<PublicReporteLocacionItem> = lista?.takeIf { it.isNotEmpty() }
                ?: dbHelper.getLocaciones().map { PublicReporteLocacionItem(it.first, it.second) }

            if (opciones.isEmpty()) {
                Toast.makeText(
                    this@ReporteQrActivity,
                    getString(R.string.reporte_loc_lista_vacia),
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }

            val nombres = opciones.map { it.nombre?.trim().orEmpty().ifBlank { "—" } }.toTypedArray()
            AlertDialog.Builder(this@ReporteQrActivity)
                .setTitle(R.string.reporte_elige_locacion_titulo)
                .setItems(nombres) { _, which ->
                    val item = opciones[which]
                    aplicarLocacionManual(item.id, item.nombre?.trim().orEmpty())
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun aplicarLocacionManual(id: Int, nombre: String) {
        sinEscaneoQr = true
        locacionId = id
        qrToken = ""
        findViewById<View>(R.id.cardFormReporteQr).visibility = View.VISIBLE
        val tvLoc = findViewById<TextView>(R.id.tvLocacionNombre)
        tvLoc.text = nombre.ifBlank { getString(R.string.reporte_loc_id_fallback, id) }
        refreshAdjuntosUi()
    }

    private fun mostrarMenuAdjuntos() {
        if (selectedUris.size >= maxAttachments) {
            Toast.makeText(this, getString(R.string.reporte_adjuntos_max, maxAttachments), Toast.LENGTH_SHORT).show()
            return
        }
        val opts = arrayOf(
            getString(R.string.reporte_adj_opcion_camara),
            getString(R.string.reporte_adj_opcion_galeria),
            getString(R.string.reporte_adj_opcion_archivo),
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.reporte_adj_menu_titulo)
            .setItems(opts) { _, which ->
                when (which) {
                    0 -> lanzarCamara()
                    1 -> pickImagesLauncher.launch("image/*")
                    2 -> pickFilesLauncher.launch("*/*")
                }
            }
            .show()
    }

    private fun lanzarCamara() {
        if (selectedUris.size >= maxAttachments) {
            Toast.makeText(this, getString(R.string.reporte_adjuntos_max, maxAttachments), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir
            val file = File.createTempFile("reporte_qr_${System.currentTimeMillis()}_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (_: Exception) {
            pendingCameraUri = null
            Toast.makeText(this, "No se pudo abrir la cámara.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addUrisFromPicker(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val cap = maxAttachments - selectedUris.size
        if (cap <= 0) {
            Toast.makeText(this, getString(R.string.reporte_adjuntos_max, maxAttachments), Toast.LENGTH_SHORT).show()
            return
        }
        selectedUris.addAll(uris.take(cap))
        refreshAdjuntosUi()
    }

    private suspend fun cargarCatalogosSpinners() {
        val triple = withContext(Dispatchers.IO) {
            try {
                val r = api.publicReporteCatalogos().execute()
                val b = r.body()
                if (b?.success == true && !b.areas.isNullOrEmpty() && !b.tiposSolicitud.isNullOrEmpty()) {
                    Triple(b.areas!!, b.tiposSolicitud!!, b.maxAttachments ?: 4)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        } ?: Triple(
            resources.getStringArray(R.array.public_report_areas_fallback).toList(),
            resources.getStringArray(R.array.public_report_tipos_fallback).toList(),
            4,
        )
        maxAttachments = triple.third.coerceIn(1, 8)
        findViewById<Spinner>(R.id.spinnerAreaSolicitante).adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, triple.first)
        findViewById<Spinner>(R.id.spinnerTipoSolicitud).adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, triple.second)
    }

    private fun applyQrData(locId: Int, token: String) {
        sinEscaneoQr = false
        locacionId = locId
        qrToken = token
        findViewById<View>(R.id.cardFormReporteQr).visibility = View.VISIBLE
        val tvLoc = findViewById<TextView>(R.id.tvLocacionNombre)
        val nombreLocal = dbHelper.getLocaciones().find { it.first == locId }?.second
        tvLoc.text = nombreLocal ?: getString(R.string.reporte_loc_cargando)
        refreshAdjuntosUi()

        CoroutineScope(Dispatchers.Main).launch {
            val nombreApi = withContext(Dispatchers.IO) {
                try {
                    val r = api.publicReporteLocacion(locId, token).execute()
                    val b = r.body()
                    if (r.isSuccessful && b?.success == true) {
                        b.data?.nombre?.trim().orEmpty().ifBlank { null }
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (isFinishing) return@launch
            tvLoc.text = nombreApi ?: nombreLocal ?: getString(R.string.reporte_loc_id_fallback, locId)
        }
    }

    private fun refreshAdjuntosUi() {
        val tv = findViewById<TextView>(R.id.tvAdjuntosResumen)
        val rv = findViewById<RecyclerView>(R.id.rvAdjuntosPreview)
        if (selectedUris.isEmpty()) {
            tv.text = getString(R.string.reporte_adjuntos_ninguno)
            rv.visibility = View.GONE
            adjuntosAdapter.uris = emptyList()
            return
        }
        tv.text = getString(R.string.reporte_adjuntos_resumen, selectedUris.size, maxAttachments)
        rv.visibility = View.VISIBLE
        adjuntosAdapter.uris = selectedUris.toList()
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0) return c.getString(i)
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun txt(s: String) = s.toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

    private fun uriToPart(uri: Uri, index: Int): MultipartBody.Part {
        val mimeOriginal = contentResolver.getType(uri) ?: "application/octet-stream"
        val displayName = queryDisplayName(uri) ?: "adjunto_$index"

        val (bytes, mimeOut, fileName) =
            if (ReporteImageOptimizer.isRasterImageMime(mimeOriginal)) {
                val opt = ReporteImageOptimizer.optimizeRasterToJpeg(contentResolver, uri, displayName)
                if (opt != null) {
                    Triple(opt.first, "image/jpeg", opt.second)
                } else {
                    val raw = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IOException("No se pudo leer: $displayName")
                    Triple(raw, mimeOriginal, displayName)
                }
            } else {
                val raw = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("No se pudo leer: $displayName")
                Triple(raw, mimeOriginal, displayName)
            }

        val body = bytes.toRequestBody(mimeOut.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("attachments[]", fileName, body)
    }

    private fun spinnerValue(sp: Spinner): String {
        val a = sp.adapter ?: return ""
        val i = sp.selectedItemPosition
        if (i < 0 || i >= a.count) return ""
        return a.getItem(i).toString()
    }

    private fun enviar() {
        if (locacionId <= 0) {
            Toast.makeText(this, getString(R.string.reporte_qr_invalido), Toast.LENGTH_LONG).show()
            return
        }
        if (!sinEscaneoQr && qrToken.length != 64) {
            Toast.makeText(this, getString(R.string.reporte_qr_invalido), Toast.LENGTH_LONG).show()
            return
        }

        val nombre = findViewById<TextInputEditText>(R.id.inputSolicitanteNombre).text?.toString()?.trim().orEmpty()
        val desc = findViewById<TextInputEditText>(R.id.inputDescripcionReporte).text?.toString()?.trim().orEmpty()
        val email = findViewById<TextInputEditText>(R.id.inputEmailReporte).text?.toString()?.trim().orEmpty()
        val area = spinnerValue(findViewById(R.id.spinnerAreaSolicitante))
        val tipo = spinnerValue(findViewById(R.id.spinnerTipoSolicitud))

        if (nombre.isEmpty() || desc.isEmpty() || email.isEmpty() || area.isEmpty() || tipo.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos obligatorios.", Toast.LENGTH_LONG).show()
            return
        }

        val btn = findViewById<MaterialButton>(R.id.btnEnviarReporteQr)
        val progress = findViewById<View>(R.id.progressEnviarReporte)
        val textoBotonOriginal = btn.text.toString()
        btn.isEnabled = false
        btn.text = getString(R.string.reporte_enviando)
        progress.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            val outcome = withContext(Dispatchers.IO) {
                try {
                    val parts = selectedUris.mapIndexed { idx, u -> uriToPart(u, idx) }
                    Log.d(TAG, "Enviando reporte QR: locacion_id=$locacionId adjuntos=${parts.size}")
                    val resp = api.publicReporteEnviar(
                        txt(locacionId.toString()),
                        txt(if (sinEscaneoQr) "" else qrToken),
                        txt(if (sinEscaneoQr) "1" else "0"),
                        txt(nombre),
                        txt(area),
                        txt(tipo),
                        txt(desc),
                        txt(email),
                        parts,
                    ).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        if (body?.success == true && !body.data?.ticketNumber.isNullOrBlank()) {
                            EnvioOutcome(true, ticketNumber = body.data?.ticketNumber)
                        } else {
                            val msg = body?.message ?: extraerMensajeServidor(resp.errorBody()?.string())
                            Log.w(TAG, "Respuesta 2xx pero success=false o sin ticket. body=$body")
                            EnvioOutcome(false, detalleUsuario = msg.ifBlank { "El servidor respondió sin número de ticket." })
                        }
                    } else {
                        val errRaw = resp.errorBody()?.string().orEmpty()
                        Log.e(TAG, "HTTP ${resp.code()} ${resp.message()} body=$errRaw")
                        val msg = extraerMensajeServidor(errRaw).ifBlank { "HTTP ${resp.code()}" }
                        EnvioOutcome(false, detalleUsuario = msg)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al enviar reporte público", e)
                    val red = when (e) {
                        is java.net.UnknownHostException -> "No hay conexión o el servidor no existe (revisar URL en la app)."
                        is java.net.SocketTimeoutException -> "Tiempo de espera agotado (red lenta o servidor no responde)."
                        is IOException -> "Error de lectura de archivos o red: ${e.message}"
                        else -> e.message ?: e.javaClass.simpleName
                    }
                    EnvioOutcome(false, detalleUsuario = red)
                }
            }
            progress.visibility = View.GONE
            btn.isEnabled = true
            btn.text = textoBotonOriginal
            if (outcome.exito && outcome.ticketNumber != null) {
                Toast.makeText(this@ReporteQrActivity, getString(R.string.reporte_enviado_ok, outcome.ticketNumber), Toast.LENGTH_LONG).show()
                finish()
            } else {
                val detalle = outcome.detalleUsuario?.take(220)?.trim().orEmpty()
                val toastText = if (detalle.isNotEmpty()) {
                    getString(R.string.reporte_enviado_error_detalle, detalle)
                } else {
                    getString(R.string.reporte_enviado_error)
                }
                Toast.makeText(this@ReporteQrActivity, toastText, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun extraerMensajeServidor(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(raw)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("\"message\"\\s*:\\s*\\[\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)?.let { return it }
        return raw.trim().take(300)
    }

    companion object {
        private const val TAG = "ReporteQr"
        const val EXTRA_LOCACION_ID = "locacion_id"
        const val EXTRA_QR_TOKEN = "qr_token"
        /** Si es true, se abre el diálogo para elegir locación sin escanear. */
        const val EXTRA_OPEN_LOCACION_PICKER = "open_locacion_picker"
    }
}
