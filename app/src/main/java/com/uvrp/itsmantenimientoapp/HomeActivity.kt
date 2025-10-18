package com.uvrp.itsmantenimientoapp

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper

class HomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    // Nuevas referencias para el diseño corporativo
    private lateinit var cardNotifications: MaterialCardView
    private lateinit var tvNotificationTitle: TextView
    private lateinit var tvNotificationDescription: TextView
    private lateinit var chipPendingCount: Chip
    private lateinit var rvPendientes: RecyclerView
    private lateinit var btnSincronizar: MaterialButton
    private lateinit var tvSubtitle: TextView

    private lateinit var dbHelper: DatabaseHelper // La hacemos variable de la clase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // ==== INICIALIZAR COMPONENTES Y HELPERS ====
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        tvSubtitle = findViewById(R.id.home_subtitle)
        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
        val nombreUsuario = sharedPreferences.getString("nombre", "Usuario")

        tvSubtitle.text = "Bienvenido/a, $nombreUsuario"

        // Nuevas referencias del diseño corporativo
        cardNotifications = findViewById(R.id.cardNotifications)
        tvNotificationTitle = findViewById(R.id.tvNotificationTitle)
        tvNotificationDescription = findViewById(R.id.tvNotificationDescription)
        chipPendingCount = findViewById(R.id.chipPendingCount)
        rvPendientes = findViewById(R.id.rvPendientes)
        btnSincronizar = findViewById(R.id.btnSincronizar)

        dbHelper = DatabaseHelper(this)

        // Configurar toolbar + menú hamburguesa + NavigationView
        HeaderHelper.setupHeader(this, drawerLayout, navView)

        // ==== BOTÓN SINCRONIZAR (se configura solo una vez) ====
        btnSincronizar.setOnClickListener {
            // Mostrar estado de carga
            btnSincronizar.text = "Sincronizando..."
            btnSincronizar.isEnabled = false

            FuncionesGenerales.sincronizarTodosMantenimientos(this) { exito ->
                // Restaurar botón
                btnSincronizar.text = "Sincronizar Ahora"
                btnSincronizar.isEnabled = true

                if (exito) {
                    // Al terminar, recargamos la lista
                    cargarYMostrarPendientes()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ESTA ES LA LÍNEA CLAVE:
        // Cada vez que la pantalla vuelve a ser visible, recargamos los datos.
        Log.d("HomeActivity", "onResume: Recargando la lista de pendientes.")
        cargarYMostrarPendientes()
    }

    /**
     * Función única que se encarga de obtener los datos de la BD según el rol del usuario
     * y actualizar la interfaz de usuario con el nuevo diseño corporativo.
     */
    private fun cargarYMostrarPendientes() {
        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
        val idRol = sharedPreferences.getInt("idRol", -1)

        val pendientesCorrectivos: List<String>
        val pendientesPreventivos: List<String>

        // 1. Decidimos qué datos cargar según el rol (sin cambios aquí)
        when (idRol) {
            1, 2 -> {
                pendientesCorrectivos = dbHelper.getMantenimientosPendientes()
                pendientesPreventivos = dbHelper.getMantenimientosPendientesActividad()
            }
            5, 6, 7 -> {
                android.util.Log.d("HomeActivity", "🔍 Rol $idRol: Obteniendo pendientes...")
                pendientesCorrectivos = dbHelper.getMantenimientosPendientes()
                android.util.Log.d("HomeActivity", "📋 Correctivos: ${pendientesCorrectivos.size}")
                pendientesPreventivos = dbHelper.getMantenimientosPendientesBicatacoras()
                android.util.Log.d("HomeActivity", "📋 Preventivos (bitácoras): ${pendientesPreventivos.size}")
            }
            else -> {
                // Para roles no reconocidos, mostramos la tarjeta de "Sin Novedades"
                cardNotifications.visibility = View.VISIBLE
                chipPendingCount.visibility = View.GONE // Ocultamos el contador
                btnSincronizar.visibility = View.GONE   // y el botón de sincronizar
                rvPendientes.visibility = View.GONE     // y la lista.

                tvNotificationTitle.text = "Sin Novedades"
                tvNotificationDescription.text = "Está al día, no se registran actividades ni sincronizaciones pendientes."
                return
            }
        }

        val todosLosPendientes = pendientesCorrectivos + pendientesPreventivos

        // 2. Lógica común para actualizar la UI (aquí están los cambios)
        if (todosLosPendientes.isEmpty()) {
            // ---- CAMBIO 1: Mostrar tarjeta de "Sin Novedades" ----
            // En lugar de ocultar todo, mostramos un estado informativo.
            cardNotifications.visibility = View.VISIBLE
            chipPendingCount.visibility = View.GONE
            btnSincronizar.visibility = View.GONE
            rvPendientes.visibility = View.GONE

            tvNotificationTitle.text = "¡Todo al día! ✨"
            tvNotificationDescription.text = "No se encontraron mantenimientos pendientes por sincronizar."

        } else {
            // Hay pendientes, mostrar la tarjeta con toda la información
            cardNotifications.visibility = View.VISIBLE
            chipPendingCount.visibility = View.VISIBLE
            btnSincronizar.visibility = View.VISIBLE
            rvPendientes.visibility = View.VISIBLE

            // Actualizar el contador en el chip
            chipPendingCount.text = todosLosPendientes.size.toString()

            // ---- CAMBIO 2: Textos dinámicos según el rol ----
            val esRolProgramado = idRol in 5..7
            val textoCorrectivo = if (esRolProgramado) "No Programados" else "Correctivos"
            val textoPreventivo = if (esRolProgramado) "Programados" else "Preventivos"

            // Actualizar el título
            val tituloMantenimientos = when {
                pendientesCorrectivos.isNotEmpty() && pendientesPreventivos.isNotEmpty() ->
                    "Mantenimientos Pendientes"
                pendientesCorrectivos.isNotEmpty() ->
                    "Mantenimientos $textoCorrectivo Pendientes"
                pendientesPreventivos.isNotEmpty() ->
                    "Mantenimientos $textoPreventivo Pendientes"
                else -> "Mantenimientos Pendientes"
            }
            tvNotificationTitle.text = tituloMantenimientos

            // Crear descripción detallada con los nuevos textos
            val descripcion = buildString {
                append("Se han encontrado ")
                append(todosLosPendientes.size)
                append(" mantenimiento")
                if (todosLosPendientes.size > 1) append("s")
                append(" pendientes por sincronizar:\n")

                if (pendientesCorrectivos.isNotEmpty()) {
                    append("• $textoCorrectivo: ${pendientesCorrectivos.size}\n")
                }
                if (pendientesPreventivos.isNotEmpty()) {
                    append("• $textoPreventivo: ${pendientesPreventivos.size}")
                }
            }
            tvNotificationDescription.text = descripcion.trim()

            // Configurar el RecyclerView (sin cambios)
            rvPendientes.layoutManager = LinearLayoutManager(this)
            rvPendientes.adapter = PendientesAdapter(todosLosPendientes)
        }
    }

    // ==== ADAPTER MEJORADO PARA EL NUEVO DISEÑO ====
    class PendientesAdapter(private val pendientes: List<String>) :
        RecyclerView.Adapter<PendientesAdapter.PendienteViewHolder>() {

        inner class PendienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTag: TextView = itemView.findViewById(R.id.tvTag)
            val tvTipo: TextView = itemView.findViewById(R.id.tvTipo)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendienteViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pendiente_simple, parent, false)
            return PendienteViewHolder(view)
        }

        override fun onBindViewHolder(holder: PendienteViewHolder, position: Int) {
            val tag = pendientes[position]
            holder.tvTag.text = tag
            holder.tvTipo.text = "Pendiente de sincronización"
        }

        override fun getItemCount(): Int = pendientes.size
    }

    // ==== MANEJO DEL BOTÓN HAMBURGUESA (Sin cambios) ====
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (HeaderHelper.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }
}