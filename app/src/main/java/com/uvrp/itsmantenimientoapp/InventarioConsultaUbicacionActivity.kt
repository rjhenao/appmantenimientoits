package com.uvrp.itsmantenimientoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import java.util.Locale

class InventarioConsultaUbicacionActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private var idUbicacionSel: Int? = null
    private var textoUbicacionSeleccionado: String? = null
    private lateinit var adapterProductos: ProductosUbicacionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventario_consulta_ubicacion)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawerLayout, navView)
        supportActionBar?.title = getString(R.string.inv_consulta_ubicacion_toolbar)
        RetrofitClient.init(applicationContext)
        db = DatabaseHelper(this)

        val actUbi = findViewById<MaterialAutoCompleteTextView>(R.id.actBuscarUbicacion)
        val tvU = findViewById<TextView>(R.id.tvUbicacionSeleccionada)
        val btnConsultar = findViewById<MaterialButton>(R.id.btnConsultar)
        val btnScanUbi = findViewById<MaterialButton>(R.id.btnEscanearQrUbicacion)
        val cardVerUbicacion = findViewById<MaterialCardView>(R.id.cardVerUbicacion)
        val cardResumen = findViewById<MaterialCardView>(R.id.cardResumen)
        val tvUbicacionDetalle = findViewById<TextView>(R.id.tvUbicacionDetalle)
        val tvResumen = findViewById<TextView>(R.id.tvResumen)
        val tvListaTitulo = findViewById<TextView>(R.id.tvListaProductosTitulo)
        val rvProductos = findViewById<RecyclerView>(R.id.rvProductosUbicacion)
        val tvSinProductos = findViewById<TextView>(R.id.tvSinProductos)

        actUbi.threshold = 2
        val adapterUbi = ArrayAdapter<DatabaseHelper.InvUbicacionRow>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        actUbi.setAdapter(adapterUbi)
        var ubiRows: List<DatabaseHelper.InvUbicacionRow> = emptyList()

        adapterProductos = ProductosUbicacionAdapter()
        rvProductos.layoutManager = FullyExpandedLinearLayoutManager(this)
        rvProductos.setHasFixedSize(false)
        rvProductos.isNestedScrollingEnabled = false
        rvProductos.adapter = adapterProductos

        fun limpiarResultados() {
            cardVerUbicacion.visibility = View.GONE
            cardResumen.visibility = View.GONE
            tvListaTitulo.visibility = View.GONE
            rvProductos.visibility = View.GONE
            tvSinProductos.visibility = View.GONE
            adapterProductos.submit(emptyList())
        }

        fun refrescarUbicacionesDesdeTexto(q: String) {
            ubiRows = if (q.length >= 2) db.buscarUbicacionesInventarioLocal(q) else emptyList()
            adapterUbi.clear()
            adapterUbi.addAll(ubiRows)
            adapterUbi.notifyDataSetChanged()
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
                textoUbicacionSeleccionado = null
                idUbicacionSel = null
                tvU.text = getString(R.string.inv_consulta_ubicacion_sin_seleccion)
                limpiarResultados()
                refrescarUbicacionesDesdeTexto(q)
            }
        }

        fun aplicarUbicacion(row: DatabaseHelper.InvUbicacionRow) {
            idUbicacionSel = row.id
            textoUbicacionSeleccionado = row.label
            tvU.text = getString(R.string.inv_consulta_ubicacion_seleccionada, row.label)
            actUbi.setTextWithoutWatcher(row.label, ubiWatcher)
            limpiarResultados()
        }

        fun aplicarUbicacionDesdeQr(contents: String) {
            val row = db.resolverUbicacionPorCodigoQr(contents)
                ?: run {
                    Toast.makeText(this, getString(R.string.inv_qr_celda_no_encontrada), Toast.LENGTH_LONG).show()
                    return
                }
            aplicarUbicacion(row)
        }

        fun mostrarResultados(
            detalle: String,
            resumen: String,
            productos: List<DatabaseHelper.InvUbicacionProductoRow>
        ) {
            cardVerUbicacion.visibility = View.VISIBLE
            tvUbicacionDetalle.text = detalle
            cardResumen.visibility = View.VISIBLE
            tvResumen.text = resumen
            adapterProductos.submit(productos)
            if (productos.isEmpty()) {
                tvListaTitulo.visibility = View.GONE
                rvProductos.visibility = View.GONE
                tvSinProductos.visibility = View.VISIBLE
            } else {
                tvListaTitulo.visibility = View.VISIBLE
                rvProductos.visibility = View.VISIBLE
                tvSinProductos.visibility = View.GONE
            }
            rvProductos.post {
                rvProductos.requestLayout()
                findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollConsultaUbicacion)?.requestLayout()
            }
        }

        fun construirResumenDesdeFilas(productos: List<DatabaseHelper.InvUbicacionProductoRow>): String {
            val total = productos.size
            val consumibles = productos.count { it.tipo.equals("consumible", ignoreCase = true) }
            val prestamos = productos.count { it.tipo.equals("prestamo", ignoreCase = true) }
            return getString(
                R.string.inv_consulta_ubicacion_resumen_linea,
                total,
                consumibles,
                prestamos
            )
        }

        fun mapearDesdeApi(
            body: ApiService.UbicacionExistenciasResponse,
            ubicacionId: Int
        ): List<DatabaseHelper.InvUbicacionProductoRow> {
            return body.data.orEmpty().map { item ->
                db.upsertExistenciaEnCache(item.invProductoId, ubicacionId, item.cantidad)
                DatabaseHelper.InvUbicacionProductoRow(
                    invProductoId = item.invProductoId,
                    codigoEtiqueta = item.codigoEtiqueta,
                    nombre = item.nombre,
                    tipo = item.tipo,
                    unidadCodigo = item.unidadCodigo,
                    cantidad = item.cantidad
                )
            }
        }

        fun detalleDesdeApi(ubicacion: ApiService.UbicacionExistenciasUbicacionDto): String {
            val lineas = mutableListOf<String>()
            lineas.add(getString(R.string.inv_consulta_ubicacion_codigo, ubicacion.codigoUnicoGlobal))
            ubicacion.bodegaNombre?.takeIf { it.isNotBlank() }?.let {
                lineas.add(getString(R.string.inv_consulta_ubicacion_bodega, it))
            }
            ubicacion.estanteCodigo?.takeIf { it.isNotBlank() }?.let {
                lineas.add(getString(R.string.inv_consulta_ubicacion_estante, it))
            }
            val fila = ubicacion.fila
            val columna = ubicacion.columna
            if (fila != null && columna != null) {
                lineas.add(getString(R.string.inv_consulta_ubicacion_fila_col, fila, columna))
            }
            return lineas.joinToString("\n")
        }

        fun detalleDesdeLocal(row: DatabaseHelper.InvUbicacionRow): String {
            return getString(R.string.inv_consulta_ubicacion_codigo, row.label)
        }

        fun resumenDesdeApi(resumen: ApiService.UbicacionExistenciasResumenDto?): String {
            val total = resumen?.productosDistintos ?: 0
            val consumibles = resumen?.porTipo?.get("consumible") ?: 0
            val prestamos = resumen?.porTipo?.get("prestamo") ?: 0
            return getString(R.string.inv_consulta_ubicacion_resumen_linea, total, consumibles, prestamos)
        }

        fun consultarExistencias() {
            val ubicacionId = idUbicacionSel
            if (ubicacionId == null) {
                Toast.makeText(this, getString(R.string.inv_consulta_ubicacion_falta_celda), Toast.LENGTH_SHORT).show()
                return
            }
            btnConsultar.isEnabled = false
            Thread {
                var desdeRed = false
                var productos = emptyList<DatabaseHelper.InvUbicacionProductoRow>()
                var detalle = ""
                var resumen = ""
                try {
                    val resp = RetrofitClient.instance.inventarioUbicacionExistencias(ubicacionId).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        if (body != null) {
                            desdeRed = true
                            productos = mapearDesdeApi(body, ubicacionId)
                            detalle = body.ubicacion?.let { detalleDesdeApi(it) }
                                ?: detalleDesdeLocal(DatabaseHelper.InvUbicacionRow(ubicacionId, textoUbicacionSeleccionado.orEmpty()))
                            resumen = body.resumen?.let { resumenDesdeApi(it) }
                                ?: construirResumenDesdeFilas(productos)
                        }
                    }
                } catch (_: Exception) {
                }

                if (!desdeRed) {
                    productos = db.listarExistenciasUbicacionLocal(ubicacionId)
                    val label = textoUbicacionSeleccionado.orEmpty()
                    detalle = detalleDesdeLocal(DatabaseHelper.InvUbicacionRow(ubicacionId, label))
                    resumen = construirResumenDesdeFilas(productos)
                }

                val tieneSnapshot = getSharedPreferences("Sesion", MODE_PRIVATE)
                    .getBoolean("inv_existencias_sync_ok", false)

                runOnUiThread {
                    btnConsultar.isEnabled = true
                    mostrarResultados(detalle, resumen, productos)
                    if (!desdeRed && productos.isEmpty() && !tieneSnapshot) {
                        Toast.makeText(
                            this,
                            getString(R.string.inv_consulta_ubicacion_sin_datos_offline),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.start()
        }

        val scanUbicacionLauncher = registerForActivityResult(ScanContract()) { result ->
            val c = result.contents?.trim().orEmpty()
            if (c.isEmpty()) return@registerForActivityResult
            aplicarUbicacionDesdeQr(c)
            if (idUbicacionSel != null) {
                consultarExistencias()
            }
        }

        val requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                abrirEscaneoQrCelda(scanUbicacionLauncher)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.inv_consulta_ubicacion_permiso_camara),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        fun solicitarEscaneoQrCelda() {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> abrirEscaneoQrCelda(scanUbicacionLauncher)
                else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnScanUbi.setOnClickListener { solicitarEscaneoQrCelda() }

        actUbi.addTextChangedListener(ubiWatcher)
        actUbi.setOnItemClickListener { parent, _, position, _ ->
            val row = parent.getItemAtPosition(position) as? DatabaseHelper.InvUbicacionRow
                ?: return@setOnItemClickListener
            aplicarUbicacion(row)
        }

        btnConsultar.setOnClickListener { consultarExistencias() }
    }

    /**
     * Permite que el RecyclerView muestre todos los ítems dentro del NestedScrollView (scroll de la pantalla).
     */
    private class FullyExpandedLinearLayoutManager(context: android.content.Context) :
        LinearLayoutManager(context) {

        override fun onMeasure(
            recycler: RecyclerView.Recycler,
            state: RecyclerView.State,
            widthSpec: Int,
            heightSpec: Int
        ) {
            super.onMeasure(
                recycler,
                state,
                widthSpec,
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
    }

    private fun abrirEscaneoQrCelda(scanUbicacionLauncher: ActivityResultLauncher<ScanOptions>) {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(getString(R.string.inv_scan_prompt_celda))
            setBeepEnabled(false)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
        }
        scanUbicacionLauncher.launch(options)
    }

    private class ProductosUbicacionAdapter :
        RecyclerView.Adapter<ProductosUbicacionAdapter.ProductoViewHolder>() {

        private val items = mutableListOf<DatabaseHelper.InvUbicacionProductoRow>()

        fun submit(nuevos: List<DatabaseHelper.InvUbicacionProductoRow>) {
            items.clear()
            items.addAll(nuevos)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_inv_ubicacion_producto, parent, false)
            return ProductoViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvNombre = itemView.findViewById<TextView>(R.id.tvProductoNombre)
            private val tvCodigo = itemView.findViewById<TextView>(R.id.tvProductoCodigo)
            private val tvTipo = itemView.findViewById<TextView>(R.id.tvProductoTipo)
            private val tvCantidad = itemView.findViewById<TextView>(R.id.tvProductoCantidad)

            fun bind(row: DatabaseHelper.InvUbicacionProductoRow) {
                val ctx = itemView.context
                tvNombre.text = row.nombre
                tvCodigo.text = row.codigoEtiqueta
                val tipoLabel = when (row.tipo.lowercase(Locale.getDefault())) {
                    "consumible" -> ctx.getString(R.string.inv_consulta_ubicacion_tipo_consumible)
                    "prestamo" -> ctx.getString(R.string.inv_consulta_ubicacion_tipo_prestamo)
                    else -> row.tipo
                }
                tvTipo.text = tipoLabel
                val unidad = row.unidadCodigo?.trim().orEmpty()
                tvCantidad.text = if (unidad.isNotEmpty()) {
                    ctx.getString(R.string.inv_consulta_ubicacion_cantidad_unidad, row.cantidad, unidad)
                } else {
                    row.cantidad
                }
            }
        }
    }
}
