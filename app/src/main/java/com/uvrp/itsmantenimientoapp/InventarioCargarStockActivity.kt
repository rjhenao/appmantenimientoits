package com.uvrp.itsmantenimientoapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class InventarioCargarStockActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private var idProductoSel: Int? = null
    private var idUbicacionSel: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventario_cargar_stock)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawerLayout, navView)
        supportActionBar?.title = "Inventario — Cargar stock"
        RetrofitClient.init(applicationContext)
        db = DatabaseHelper(this)

        val (uu, pp, ub) = db.contarCatalogoInventario()
        if (pp == 0 || ub == 0) {
            Toast.makeText(
                this,
                "Catálogo de inventario vacío. Conéctese y use Sincronizar en Inicio o vuelva a iniciar sesión con red.",
                Toast.LENGTH_LONG
            ).show()
        }

        val actProd = findViewById<MaterialAutoCompleteTextView>(R.id.actBuscarProducto)
        val actUbi = findViewById<MaterialAutoCompleteTextView>(R.id.actBuscarUbicacion)
        actProd.threshold = 2
        actUbi.threshold = 2

        val tvP = findViewById<TextView>(R.id.tvProductoSeleccionado)
        val tvU = findViewById<TextView>(R.id.tvUbicacionSeleccionada)
        val etCant = findViewById<TextInputEditText>(R.id.etCantidad)
        val tilCantidad = findViewById<TextInputLayout>(R.id.tilCantidad)
        val etNota = findViewById<TextInputEditText>(R.id.etNota)
        val btn = findViewById<MaterialButton>(R.id.btnGuardar)
        val btnScanUbi = findViewById<MaterialButton>(R.id.btnEscanearQrUbicacion)

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

        fun refrescarUbicacionesDesdeTexto(q: String) {
            ubiRows = if (q.length >= 2) db.buscarUbicacionesInventarioLocal(q) else emptyList()
            adapterUbi.clear()
            ubiRows.forEach { adapterUbi.add(it) }
            adapterUbi.notifyDataSetChanged()
        }

        val prodWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                idProductoSel = null
                tvP.text = "Producto: (ninguno)"
                actualizarUnidadEnCantidad(null)
                val q = s?.toString()?.trim() ?: ""
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
                idUbicacionSel = null
                tvU.text = "Ubicación: (ninguna)"
                refrescarUbicacionesDesdeTexto(s?.toString()?.trim() ?: "")
            }
        }

        val scanUbicacionLauncher = registerForActivityResult(ScanContract()) { result ->
            val contents = result.contents ?: return@registerForActivityResult
            val row = db.resolverUbicacionPorCodigoQr(contents)
                ?: run {
                    Toast.makeText(this, getString(R.string.inv_qr_celda_no_encontrada), Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }
            idUbicacionSel = row.id
            tvU.text = "Ubicación: ${row.label}"
            ubiRows = listOf(row)
            adapterUbi.clear()
            adapterUbi.add(row)
            adapterUbi.notifyDataSetChanged()
            actUbi.setTextWithoutWatcher(row.label, ubiWatcher)
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
            val uMed = row.unidadCodigo?.trim().orEmpty()
            tvP.text = if (uMed.isNotEmpty()) {
                "Producto: ${row.codigoEtiqueta} — ${row.nombre} ($uMed)"
            } else {
                "Producto: ${row.codigoEtiqueta} — ${row.nombre}"
            }
            actProd.setTextWithoutWatcher(row.toString(), prodWatcher)
            actualizarUnidadEnCantidad(row.unidadCodigo)
        }

        actUbi.addTextChangedListener(ubiWatcher)

        actUbi.setOnItemClickListener { parent, _, position, _ ->
            val row = parent.getItemAtPosition(position) as? DatabaseHelper.InvUbicacionRow
                ?: return@setOnItemClickListener
            idUbicacionSel = row.id
            tvU.text = "Ubicación: ${row.label}"
            actUbi.setTextWithoutWatcher(row.label, ubiWatcher)
        }

        btn.setOnClickListener {
            val pid = idProductoSel
            val uid = idUbicacionSel
            val cant = etCant.text?.toString()?.trim().orEmpty()
            if (pid == null || uid == null) {
                Toast.makeText(this, "Seleccione producto y ubicación (lista o escaneo).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (cant.isEmpty()) {
                Toast.makeText(this, "Indique cantidad.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nota = etNota.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val req = ApiService.InventarioAjusteRequest(
                invProductoId = pid,
                invUbicacionId = uid,
                cantidad = cant,
                nota = nota
            )
            Thread {
                try {
                    val r = RetrofitClient.instance.inventarioAjusteEntrada(req).execute()
                    runOnUiThread {
                        if (r.isSuccessful) {
                            Toast.makeText(this, "Stock registrado en servidor.", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            db.insertarPendienteCargarStock(pid, uid, cant, nota)
                            Toast.makeText(
                                this,
                                "No se pudo enviar (${r.code()}). Quedó pendiente.",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    }
                } catch (_: Exception) {
                    db.insertarPendienteCargarStock(pid, uid, cant, nota)
                    runOnUiThread {
                        Toast.makeText(this, "Sin conexión: pendiente de sincronizar.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }.start()
        }
    }
}
