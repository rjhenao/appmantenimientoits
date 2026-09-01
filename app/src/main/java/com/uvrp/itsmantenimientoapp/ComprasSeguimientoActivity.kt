package com.uvrp.itsmantenimientoapp

import ApiService
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ComprasSeguimientoActivity : AppCompatActivity() {

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvError: TextView
    private lateinit var tvMeta: TextView
    private lateinit var etBuscar: EditText
    private lateinit var kpiTotal: TextView
    private lateinit var kpiTramite: TextView
    private lateinit var kpiCotiz: TextView
    private lateinit var kpiAlerta: TextView
    private lateinit var kpiPendientes: TextView
    private lateinit var rowTabs: View
    private lateinit var tabPendientes: TextView
    private lateinit var tabTodas: TextView

    private var vista = "pendientes"
    private var esGerencia = false
    private var yaCargado = false

    private val adapter = EcAdapter { id ->
        startActivity(Intent(this, ComprasEcDetalleActivity::class.java).putExtra(EXTRA_EC_ID, id))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(applicationContext)

        val prefs = getSharedPreferences("Sesion", MODE_PRIVATE)
        if (!prefs.getBoolean("puede_compras_seguimiento", prefs.getInt("idRol", -1) == 1)) {
            Toast.makeText(this, "No tiene permiso para ver Compras", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_compras_seguimiento)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val nav = findViewById<NavigationView>(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawer, nav)

        swipe = findViewById(R.id.swipeCompras)
        rv = findViewById(R.id.rvCompras)
        tvEmpty = findViewById(R.id.tvComprasEmpty)
        tvError = findViewById(R.id.tvComprasError)
        tvMeta = findViewById(R.id.tvComprasMeta)
        etBuscar = findViewById(R.id.etBuscarEc)
        kpiTotal = findViewById(R.id.kpiTotal)
        kpiTramite = findViewById(R.id.kpiTramite)
        kpiCotiz = findViewById(R.id.kpiCotiz)
        kpiAlerta = findViewById(R.id.kpiAlerta)
        kpiPendientes = findViewById(R.id.kpiPendientes)
        rowTabs = findViewById(R.id.rowTabsCompras)
        tabPendientes = findViewById(R.id.tabPendientes)
        tabTodas = findViewById(R.id.tabTodas)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        rv.isNestedScrollingEnabled = false

        swipe.setOnRefreshListener { cargarLista() }
        findViewById<ImageButton>(R.id.btnBuscarEc).setOnClickListener { cargarLista() }
        etBuscar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                cargarLista()
                true
            } else false
        }
        tabPendientes.setOnClickListener {
            if (vista != "pendientes") {
                vista = "pendientes"
                pintarTabs()
                cargarLista()
            }
        }
        tabTodas.setOnClickListener {
            if (vista != "todas") {
                vista = "todas"
                pintarTabs()
                cargarLista()
            }
        }
        pintarTabs()

        cargarLista()
    }

    override fun onResume() {
        super.onResume()
        if (::swipe.isInitialized && yaCargado) {
            cargarLista()
        }
        yaCargado = true
    }

    private fun cargarLista() {
        swipe.isRefreshing = true
        tvError.visibility = View.GONE
        val q = etBuscar.text?.toString()?.trim().orEmpty().ifBlank { null }

        RetrofitClient.instance.comprasListado(
            q = q,
            estadoId = null,
            page = 1,
            vista = vista
        ).enqueue(object : Callback<ApiService.CompraListResponse> {
            override fun onResponse(
                call: Call<ApiService.CompraListResponse>,
                response: Response<ApiService.CompraListResponse>
            ) {
                swipe.isRefreshing = false
                if (response.code() == 401 || response.code() == 403) {
                    mostrarError("Sin permiso o sesión vencida (${response.code()}).")
                    return
                }
                if (!response.isSuccessful) {
                    mostrarError("Error al cargar compras (${response.code()}).")
                    return
                }
                val body = response.body()
                val items = body?.items.orEmpty()
                esGerencia = body?.esGerencia == true
                rowTabs.visibility = if (esGerencia) View.VISIBLE else View.GONE
                kpiPendientes.visibility = if (esGerencia) View.VISIBLE else View.GONE
                body?.vista?.takeIf { it.isNotBlank() }?.let { vista = it }
                pintarTabs()
                pintarKpis(body?.kpis)
                adapter.submit(items)
                val total = body?.meta?.total ?: items.size
                tvMeta.text = if (vista == "pendientes") {
                    "Pendientes de Gerencia: ${items.size} de $total"
                } else {
                    "Mostrando ${items.size} de $total EC"
                }
                tvEmpty.text = if (vista == "pendientes") {
                    "No hay EC pendientes de Gerencia."
                } else {
                    "No hay EC con los criterios actuales."
                }
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                rv.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            }

            override fun onFailure(call: Call<ApiService.CompraListResponse>, t: Throwable) {
                swipe.isRefreshing = false
                mostrarError("Sin conexión: ${t.message ?: "verifique red / servidor"}")
            }
        })
    }

    private fun pintarKpis(k: ApiService.CompraKpisDto?) {
        kpiTotal.text = "Total ${k?.total ?: 0}"
        kpiTramite.text = "Trámite ${k?.enTramite ?: 0}"
        kpiCotiz.text = "Cotización ${k?.enCotizacion ?: 0}"
        kpiAlerta.text = "Alertas ${k?.conAlerta ?: 0}"
        kpiPendientes.text = "Pendientes ${k?.pendientesGerencia ?: 0}"
    }

    private fun pintarTabs() {
        val selBg = Color.parseColor("#1B4F72")
        val unsBg = Color.parseColor("#ECEFF1")
        val selTx = Color.WHITE
        val unsTx = Color.parseColor("#607D8B")
        val pendientesSel = vista == "pendientes"
        tabPendientes.setBackgroundColor(if (pendientesSel) selBg else unsBg)
        tabPendientes.setTextColor(if (pendientesSel) selTx else unsTx)
        tabTodas.setBackgroundColor(if (!pendientesSel) selBg else unsBg)
        tabTodas.setTextColor(if (!pendientesSel) selTx else unsTx)
    }

    private fun mostrarError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
    }

    private class EcAdapter(
        private val onClick: (Int) -> Unit
    ) : RecyclerView.Adapter<EcAdapter.VH>() {

        private var data: List<ApiService.CompraEcListItemDto> = emptyList()

        fun submit(items: List<ApiService.CompraEcListItemDto>) {
            data = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_compra_ec, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(data[position], onClick)
        }

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val bar: View = itemView.findViewById(R.id.vAlertaBar)
            private val ref: TextView = itemView.findViewById(R.id.tvEcRef)
            private val estado: TextView = itemView.findViewById(R.id.tvEcEstado)
            private val meta: TextView = itemView.findViewById(R.id.tvEcMeta)
            private val paso: TextView = itemView.findViewById(R.id.tvEcPaso)
            private val tiempos: TextView = itemView.findViewById(R.id.tvEcTiempos)
            private val cotiz: TextView = itemView.findViewById(R.id.tvEcCotiz)

            fun bind(item: ApiService.CompraEcListItemDto, onClick: (Int) -> Unit) {
                val id = item.id ?: return
                ref.text = item.referenciaEc ?: "EC #$id"
                estado.text = item.estadoEtiqueta ?: item.estadoNombre ?: "—"
                val diasEstado = item.diasEnEstado?.let { "${it}d en estado" } ?: "—"
                val diasProc = item.diasTotalesProceso?.let { "${it}d proceso" } ?: ""
                val entrega = item.fechaRequeridaEntrega?.let { " · Entrega $it" } ?: ""
                val pct = item.porcentajePlazo?.let { " · Plazo ${it.toInt()}%" } ?: ""
                meta.text = listOf(diasEstado, diasProc).filter { it.isNotBlank() }.joinToString(" · ") + entrega + pct
                paso.text = item.siguientePaso ?: ""

                val resumen = CompraUiHelper.resumenDuraciones(item.duracionesPorEstado)
                if (resumen.isNotBlank()) {
                    tiempos.visibility = View.VISIBLE
                    tiempos.text = resumen
                } else {
                    tiempos.visibility = View.GONE
                }

                val tot = item.cotizacionTotal ?: 0
                if (tot > 0) {
                    cotiz.visibility = View.VISIBLE
                    cotiz.text = "Cotizaciones ${item.cotizacionRespondidas ?: 0}/$tot"
                } else {
                    cotiz.visibility = View.GONE
                }
                bar.setBackgroundColor(colorAlerta(item.alertaGlobal))
                itemView.setOnClickListener { onClick(id) }
            }

            private fun colorAlerta(a: String?): Int = when (a) {
                "rojo" -> Color.parseColor("#C62828")
                "ambar" -> Color.parseColor("#EF6C00")
                else -> Color.parseColor("#2E7D32")
            }
        }
    }

    companion object {
        const val EXTRA_EC_ID = "ec_id"
    }
}
