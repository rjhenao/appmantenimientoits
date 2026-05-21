package com.uvrp.itsmantenimientoapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.button.MaterialButton
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import java.math.BigDecimal
import java.util.Locale

class InventarioSalidaActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private var idProductoSel: Int? = null
    private var tipoProductoInv: String = ""
    private var idUbicacionSel: Int? = null
    private var existenciasOnline: List<ApiService.ExistenciaItemDto> = emptyList()
    private var idUsuarioResponsableSel: Int? = null
    private lateinit var usuariosLista: List<DatabaseHelper.UsuarioInventarioSalidaRow>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventario_salida)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawerLayout, navView)
        supportActionBar?.title = "Inventario — Salida"
        RetrofitClient.init(applicationContext)
        db = DatabaseHelper(this)
        usuariosLista = db.listarUsuariosActivosParaInventarioSalida()

        val actProd = findViewById<MaterialAutoCompleteTextView>(R.id.actBuscarProducto)
        val actUbi = findViewById<MaterialAutoCompleteTextView>(R.id.actBuscarUbicacion)
        actProd.threshold = 2
        actUbi.threshold = 2

        val tvP = findViewById<TextView>(R.id.tvProductoSeleccionado)
        val tvU = findViewById<TextView>(R.id.tvUbicacionSeleccionada)
        val tvTipoSalida = findViewById<TextView>(R.id.tvTipoSalidaSegunProducto)
        val etCant = findViewById<TextInputEditText>(R.id.etCantidad)
        val tilCantidad = findViewById<TextInputLayout>(R.id.tilCantidad)
        val tilUsuarioResponsable = findViewById<TextInputLayout>(R.id.tilUsuarioResponsable)
        val actUsuario = findViewById<MaterialAutoCompleteTextView>(R.id.actUsuarioResponsable)
        val tilDestinoUso = findViewById<TextInputLayout>(R.id.tilDestinoUso)
        val etDestinoUso = findViewById<TextInputEditText>(R.id.etDestinoUso)
        val tilObsSalida = findViewById<TextInputLayout>(R.id.tilObsSalida)
        val etObsSalida = findViewById<TextInputEditText>(R.id.etObsSalida)
        val btn = findViewById<MaterialButton>(R.id.btnRegistrar)
        val btnScanUbi = findViewById<MaterialButton>(R.id.btnEscanearQrUbicacion)

        val userAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            usuariosLista
        )
        actUsuario.setAdapter(userAdapter)
        actUsuario.threshold = 1

        val adapterProd = ArrayAdapter<DatabaseHelper.InvProductoRow>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        val adapterUbi = ArrayAdapter<DatabaseHelper.InvUbicacionRow>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        actProd.setAdapter(adapterProd)
        actUbi.setAdapter(adapterUbi)

        var prodRows: List<DatabaseHelper.InvProductoRow> = emptyList()
        var ubiRows: List<DatabaseHelper.InvUbicacionRow> = emptyList()
        /** Evita que la carga tardía de existencias borre una celda ya fijada por QR. */
        var ubicacionFijadaPorQr = false
        /** Texto exacto del campo cuando el usuario eligió de la lista o QR (evita deselección fantasma). */
        var textoProductoSeleccionado: String? = null
        var textoUbicacionSeleccionado: String? = null
        /** Texto del responsable al elegirlo en la lista (evita que el watcher borre el id al reenganchar el listener). */
        var textoUsuarioSeleccionado: String? = null

        val usuarioWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim() ?: ""
                if (idUsuarioResponsableSel != null && q == textoUsuarioSeleccionado) {
                    return
                }
                idUsuarioResponsableSel = null
            }
        }
        actUsuario.setOnItemClickListener { parent, _, position, _ ->
            val row = parent.getItemAtPosition(position) as? DatabaseHelper.UsuarioInventarioSalidaRow
                ?: return@setOnItemClickListener
            idUsuarioResponsableSel = row.id
            val tUsu = row.toString()
            textoUsuarioSeleccionado = tUsu
            actUsuario.setTextWithoutWatcher(tUsu, usuarioWatcher)
        }
        actUsuario.addTextChangedListener(usuarioWatcher)

        fun esProductoConsumible(): Boolean =
            tipoProductoInv.equals("consumible", ignoreCase = true)

        fun tipoMovimientoApi(): String = when (tipoProductoInv.lowercase(Locale.getDefault())) {
            "consumible" -> "consumo"
            "prestamo" -> "prestamo"
            else -> "consumo"
        }

        fun actualizarPanelPersonaYDestino() {
            val tieneProducto = idProductoSel != null && tipoProductoInv.isNotBlank()
            if (!tieneProducto) {
                tilUsuarioResponsable.visibility = View.GONE
                tilDestinoUso.visibility = View.GONE
                tilObsSalida.visibility = View.GONE
                return
            }
            tilUsuarioResponsable.visibility = View.VISIBLE
            tilUsuarioResponsable.hint = if (esProductoConsumible()) {
                getString(R.string.inv_salida_hint_usuario_consumo)
            } else {
                getString(R.string.inv_salida_hint_usuario_prestamo)
            }
            tilDestinoUso.visibility = if (esProductoConsumible()) View.VISIBLE else View.GONE
            tilObsSalida.visibility = if (esProductoConsumible()) View.GONE else View.VISIBLE
        }

        fun actualizarTipoSalidaUi() {
            when {
                idProductoSel == null || tipoProductoInv.isBlank() -> {
                    tvTipoSalida.text = getString(R.string.inv_salida_tipo_sin_producto)
                }
                esProductoConsumible() -> {
                    tvTipoSalida.text = getString(R.string.inv_salida_tipo_consumo)
                }
                tipoProductoInv.equals("prestamo", ignoreCase = true) -> {
                    tvTipoSalida.text = getString(R.string.inv_salida_tipo_prestamo)
                }
                else -> {
                    tvTipoSalida.text = getString(R.string.inv_salida_tipo_sin_producto)
                }
            }
            actualizarPanelPersonaYDestino()
        }

        fun parseCantidadUsuario(s: String): BigDecimal? {
            val t = s.trim().replace(',', '.').replace(" ", "")
            if (t.isEmpty()) return null
            return try {
                BigDecimal(t)
            } catch (_: Exception) {
                null
            }
        }

        fun actualizarUnidadEnCantidad(unidad: String?) {
            val u = unidad?.trim().orEmpty()
            if (u.isEmpty()) {
                tilCantidad.suffixText = null
                tilCantidad.helperText = getString(R.string.inv_cantidad_seleccione_producto)
            } else {
                tilCantidad.suffixText = u
                tilCantidad.helperText = getString(R.string.inv_cantidad_unidad_suffix, u)
            }
        }
        actualizarUnidadEnCantidad(null)
        actualizarTipoSalidaUi()

        fun refrescarUbicacionesDesdeTexto(q: String) {
            val uidSel = idUbicacionSel
            val filaAntes = uidSel?.let { id -> ubiRows.find { it.id == id } }

            var nuevas = when {
                q.length < 2 -> emptyList()
                existenciasOnline.isNotEmpty() -> {
                    val qq = q.lowercase()
                    existenciasOnline.filter {
                        it.label.lowercase().contains(qq) || it.invUbicacionId.toString().contains(q)
                    }.map {
                        DatabaseHelper.InvUbicacionRow(it.invUbicacionId, "${it.label} — disp. ${it.cantidad}")
                    }
                }
                else -> db.buscarUbicacionesInventarioLocal(q)
            }

            // Tras cargar existencias en red, el filtro puede dejar fuera la celda del QR y el adapter vacío borra el campo.
            if (uidSel != null && nuevas.none { it.id == uidSel }) {
                val restaurar = filaAntes
                    ?: existenciasOnline.find { it.invUbicacionId == uidSel }?.let { ex ->
                        DatabaseHelper.InvUbicacionRow(ex.invUbicacionId, "${ex.label} — disp. ${ex.cantidad}")
                    }
                    ?: textoUbicacionSeleccionado?.let { lab -> DatabaseHelper.InvUbicacionRow(uidSel, lab) }
                if (restaurar != null) {
                    nuevas = listOf(restaurar) + nuevas.filter { it.id != uidSel }
                }
            }

            ubiRows = nuevas
            adapterUbi.clear()
            ubiRows.forEach { adapterUbi.add(it) }
            adapterUbi.notifyDataSetChanged()
        }

        val prodWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim() ?: ""
                if (idProductoSel != null && q == textoProductoSeleccionado) {
                    prodRows = if (q.length >= 2) db.buscarProductosInventarioLocal(q) else emptyList()
                    adapterProd.clear()
                    prodRows.forEach { adapterProd.add(it) }
                    adapterProd.notifyDataSetChanged()
                    return
                }
                textoProductoSeleccionado = null
                idProductoSel = null
                tipoProductoInv = ""
                existenciasOnline = emptyList()
                tvP.text = "Producto: (ninguno)"
                actualizarUnidadEnCantidad(null)
                actualizarTipoSalidaUi()
                textoUsuarioSeleccionado = null
                actUsuario.setTextWithoutWatcher("", usuarioWatcher)
                idUsuarioResponsableSel = null
                prodRows = if (q.length >= 2) db.buscarProductosInventarioLocal(q) else emptyList()
                adapterProd.clear()
                prodRows.forEach { adapterProd.add(it) }
                adapterProd.notifyDataSetChanged()
            }
        }

        val ubiWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim() ?: ""
                if (idUbicacionSel != null && q == textoUbicacionSeleccionado) {
                    refrescarUbicacionesDesdeTexto(q)
                    return
                }
                // Celda fijada por QR: el Autocomplete puede retocar el texto o disparar eventos tardíos; no borrar salvo vaciar.
                if (ubicacionFijadaPorQr && idUbicacionSel != null) {
                    if (q.isEmpty()) {
                        textoUbicacionSeleccionado = null
                        ubicacionFijadaPorQr = false
                        idUbicacionSel = null
                        tvU.text = "Celda: (ninguna)"
                        refrescarUbicacionesDesdeTexto(q)
                        return
                    }
                    val t = textoUbicacionSeleccionado
                    val sigueSiendoLaMisma = t != null && (
                        t == q || t.contains(q) || q.contains(t) ||
                            ubiRows.any { it.id == idUbicacionSel && (it.label == q || it.label.contains(q) || q.contains(it.label)) }
                        )
                    if (sigueSiendoLaMisma) {
                        if (q != t) textoUbicacionSeleccionado = q
                        refrescarUbicacionesDesdeTexto(q)
                        return
                    }
                }
                textoUbicacionSeleccionado = null
                ubicacionFijadaPorQr = false
                idUbicacionSel = null
                tvU.text = "Celda: (ninguna)"
                refrescarUbicacionesDesdeTexto(q)
            }
        }

        fun aplicarUbicacionDesdeQr(contents: String) {
            val row = db.resolverUbicacionPorCodigoQr(contents)
                ?: run {
                    Toast.makeText(this, getString(R.string.inv_qr_celda_no_encontrada), Toast.LENGTH_LONG).show()
                    return
                }
            ubicacionFijadaPorQr = true
            if (existenciasOnline.isNotEmpty()) {
                val ex = existenciasOnline.find { it.invUbicacionId == row.id }
                if (ex != null) {
                    idUbicacionSel = ex.invUbicacionId
                    val line = "${ex.label} — disp. ${ex.cantidad}"
                    textoUbicacionSeleccionado = line
                    tvU.text = "Celda: $line"
                    ubiRows = listOf(DatabaseHelper.InvUbicacionRow(ex.invUbicacionId, line))
                    adapterUbi.clear()
                    adapterUbi.add(ubiRows[0])
                    adapterUbi.notifyDataSetChanged()
                    actUbi.setTextWithoutWatcher(line, ubiWatcher)
                    return
                }
                Toast.makeText(
                    this,
                    "${getString(R.string.inv_qr_celda_sin_stock_producto)} Se usará catálogo local.",
                    Toast.LENGTH_LONG
                ).show()
            }
            idUbicacionSel = row.id
            textoUbicacionSeleccionado = row.label
            tvU.text = "Celda: ${row.label}"
            ubiRows = listOf(row)
            adapterUbi.clear()
            adapterUbi.add(row)
            adapterUbi.notifyDataSetChanged()
            actUbi.setTextWithoutWatcher(row.label, ubiWatcher)
        }

        val scanUbicacionLauncher = registerForActivityResult(ScanContract()) { result ->
            val c = result.contents ?: return@registerForActivityResult
            aplicarUbicacionDesdeQr(c)
        }

        btnScanUbi.setOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt(getString(R.string.inv_scan_prompt_celda))
            options.setBeepEnabled(false)
            options.setBarcodeImageEnabled(false)
            options.setOrientationLocked(true)
            scanUbicacionLauncher.launch(options)
        }

        actProd.addTextChangedListener(prodWatcher)

        actProd.setOnItemClickListener { parent, _, position, _ ->
            val row = parent.getItemAtPosition(position) as? DatabaseHelper.InvProductoRow
                ?: return@setOnItemClickListener
            idProductoSel = row.id
            tipoProductoInv = row.tipo.trim()
            textoProductoSeleccionado = row.toString()
            val uMed = row.unidadCodigo?.trim().orEmpty()
            tvP.text = if (uMed.isNotEmpty()) {
                "Producto: ${row.codigoEtiqueta} — ${row.nombre} ($uMed)"
            } else {
                "Producto: ${row.codigoEtiqueta} — ${row.nombre}"
            }
            actProd.setTextWithoutWatcher(row.toString(), prodWatcher)
            actualizarUnidadEnCantidad(row.unidadCodigo)
            actualizarTipoSalidaUi()
            textoUsuarioSeleccionado = null
            actUsuario.setTextWithoutWatcher("", usuarioWatcher)
            idUsuarioResponsableSel = null
            existenciasOnline = emptyList()
            Thread {
                try {
                    val r = RetrofitClient.instance.inventarioExistencias(row.id).execute()
                    if (r.isSuccessful) {
                        existenciasOnline = r.body()?.data ?: emptyList()
                    }
                } catch (_: Exception) { }
                runOnUiThread {
                    val q = actUbi.text?.toString()?.trim() ?: ""
                    db.reemplazarExistenciasProductoEnCache(row.id, existenciasOnline)
                    if (!ubicacionFijadaPorQr && existenciasOnline.isNotEmpty() && idUbicacionSel != null) {
                        val ok = existenciasOnline.any { it.invUbicacionId == idUbicacionSel }
                        if (!ok) {
                            idUbicacionSel = null
                            textoUbicacionSeleccionado = null
                            tvU.text = "Celda: (ninguna)"
                            actUbi.setTextWithoutWatcher("", ubiWatcher)
                            adapterUbi.clear()
                            ubiRows = emptyList()
                            Toast.makeText(this, getString(R.string.inv_celda_no_aplica_producto), Toast.LENGTH_LONG).show()
                        }
                    }
                    refrescarUbicacionesDesdeTexto(q)
                    if (existenciasOnline.isEmpty()) {
                        Toast.makeText(
                            this,
                            "Sin stock en línea o sin red: puede buscar celda en catálogo local.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.start()
        }

        actUbi.addTextChangedListener(ubiWatcher)

        actUbi.setOnItemClickListener { parent, _, position, _ ->
            val row = parent.getItemAtPosition(position) as? DatabaseHelper.InvUbicacionRow
                ?: return@setOnItemClickListener
            ubicacionFijadaPorQr = false
            idUbicacionSel = row.id
            textoUbicacionSeleccionado = row.label
            tvU.text = "Celda: ${row.label}"
            actUbi.setTextWithoutWatcher(row.label, ubiWatcher)
        }

        btn.setOnClickListener {
            val pid = idProductoSel
            val uid = idUbicacionSel
            val cant = etCant.text?.toString()?.trim().orEmpty()
            if (pid == null || uid == null) {
                Toast.makeText(this, "Seleccione producto y celda.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (cant.isEmpty()) {
                Toast.makeText(this, "Indique cantidad.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val qUsuario = actUsuario.text?.toString()?.trim().orEmpty()
            val usuarioSel = idUsuarioResponsableSel?.let { id -> usuariosLista.find { it.id == id } }
                ?: usuariosLista.find { it.toString() == qUsuario }
            if (usuarioSel == null) {
                Toast.makeText(this, "Seleccione un usuario de la lista (no solo escriba el nombre).", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            idUsuarioResponsableSel = usuarioSel.id
            val tipo = tipoMovimientoApi()
            val reqConsumo = tipo == "consumo"
            val uMed = prodRows.find { it.id == pid }?.unidadCodigo?.trim().orEmpty()
            val req = if (reqConsumo) {
                ApiService.InventarioSalidaRequest(
                    invProductoId = pid,
                    invUbicacionId = uid,
                    cantidad = cant,
                    tipoMovimiento = tipo,
                    responsableRecibeNombre = usuarioSel.nombre,
                    responsableRecibeDocumento = usuarioSel.documento,
                    destinoUso = etDestinoUso.text?.toString()?.trim()
                )
            } else {
                ApiService.InventarioSalidaRequest(
                    invProductoId = pid,
                    invUbicacionId = uid,
                    cantidad = cant,
                    tipoMovimiento = tipo,
                    prestadoANombre = usuarioSel.nombre,
                    prestadoADocumento = usuarioSel.documento,
                    observacionSalida = etObsSalida.text?.toString()?.trim()
                )
            }

            val json = Gson().toJson(req)
            Thread {
                var disponibleBd: BigDecimal? = null
                try {
                    val rEx = RetrofitClient.instance.inventarioExistencias(pid).execute()
                    if (rEx.isSuccessful) {
                        val list = rEx.body()?.data ?: emptyList()
                        runOnUiThread { existenciasOnline = list }
                        db.reemplazarExistenciasProductoEnCache(pid, list)
                        val ex = list.find { it.invUbicacionId == uid }
                        disponibleBd = if (ex != null) {
                            parseCantidadUsuario(ex.cantidad)
                        } else {
                            BigDecimal.ZERO
                        }
                    }
                } catch (_: Exception) {
                }
                if (disponibleBd == null) {
                    val cached = db.obtenerCantidadCacheExistencia(pid, uid)
                    disponibleBd = if (cached != null) parseCantidadUsuario(cached) else null
                }
                if (disponibleBd == null) {
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.inv_salida_stock_sin_datos), Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }
                val solicitado = parseCantidadUsuario(cant)
                if (solicitado == null || solicitado <= BigDecimal.ZERO) {
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.inv_salida_cantidad_invalida), Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }
                val pendLocal = db.sumaPendienteSalidaMismaCelda(pid, uid)
                val saldo = disponibleBd.subtract(pendLocal)
                if (solicitado > saldo) {
                    val maxDetalle = buildString {
                        append(saldo.stripTrailingZeros().toPlainString())
                        if (uMed.isNotEmpty()) append(' ').append(uMed)
                    }
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.inv_salida_stock_excede, maxDetalle),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@Thread
                }
                try {
                    val r = RetrofitClient.instance.inventarioSalida(req).execute()
                    runOnUiThread {
                        if (r.isSuccessful) {
                            Toast.makeText(this, "Salida registrada.", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            db.insertarPendienteSalida(json)
                            Toast.makeText(this, "No se pudo enviar. Quedó pendiente.", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                } catch (_: Exception) {
                    db.insertarPendienteSalida(json)
                    runOnUiThread {
                        Toast.makeText(this, "Sin conexión: pendiente de sincronizar.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }.start()
        }
    }
}
