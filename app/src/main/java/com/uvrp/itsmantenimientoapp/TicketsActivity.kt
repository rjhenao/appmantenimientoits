package com.uvrp.itsmantenimientoapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import com.uvrp.itsmantenimientoapp.models.Ticket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TicketsActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var rvTickets: RecyclerView

    private lateinit var dbHelper: DatabaseHelper
    private var ticketsList: List<Ticket> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tickets)

        // Inicializar componentes
        initializeViews()
        setupHeader()
        setupRecyclerView()
        
        // Cargar tickets iniciales
        cargarTickets()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        rvTickets = findViewById(R.id.rvTickets)
        
        // Los elementos de notificaciones ahora están en el adapter, no en el layout principal
        // btnSincronizar = findViewById(R.id.btnSincronizarTickets) // Ya no existe en el layout principal
        // cardNotifications = findViewById(R.id.cardNotifications) // Ya no existe en el layout principal
        // tvNotificationTitle = findViewById(R.id.tvNotificationTitle) // Ya no existe en el layout principal
        // tvNotificationDescription = findViewById(R.id.tvNotificationDescription) // Ya no existe en el layout principal
        // chipTicketCount = findViewById(R.id.chipTicketCount) // Ya no existe en el layout principal

        dbHelper = DatabaseHelper(this)
    }

    private fun setupHeader() {
        HeaderHelper.setupHeader(this, drawerLayout, navView)
    }

    private fun setupRecyclerView() {
        rvTickets.layoutManager = LinearLayoutManager(this)
    }


    private fun cargarTickets() {
        // Debug: verificar qué tickets están en la BD
        dbHelper.debugTicketsEnBD()
        
        // Obtener tickets con estado 'abierto' y 'en_progreso'
        ticketsList = dbHelper.obtenerTicketsPorEstado(listOf("abierto", "en_progreso"))
        
        Log.d("TicketsActivity", "Tickets cargados desde BD: ${ticketsList.size}")
        
        // Configurar RecyclerView con adapter que incluye header
        val adapter = TicketsWithHeaderAdapter(ticketsList) { ticket ->
            // Acción al hacer clic en "Solucionar"
            mostrarDialogoSolucionar(ticket)
        }
        
        rvTickets.adapter = adapter
        Log.d("TicketsActivity", "Adapter configurado. Item count: ${adapter.itemCount}")
    }

    private fun mostrarDialogoSolucionar(ticket: Ticket) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Solucionar Ticket")
        builder.setMessage("¿Desea crear un mantenimiento correctivo para el ticket '${ticket.ticketNumber}'?")
        
        builder.setPositiveButton("Sí, Crear Mantenimiento") { _, _ ->
            // Abrir MantenimientoCorrectivoActivity con los datos del ticket
            val intent = Intent(this, MantenimientoCorrectivoActivity::class.java)
            
            // Pasar datos del ticket
            intent.putExtra("ticketId", ticket.id.toString())
            intent.putExtra("ticketNumber", ticket.ticketNumber)
            intent.putExtra("ticketTitle", ticket.title)
            intent.putExtra("ticketDescription", ticket.description)
            
            // Pasar datos del equipo si están disponibles
            intent.putExtra("tagId", ticket.equipment?.id?.toString() ?: "")
            intent.putExtra("locacionDescripcion", ticket.location.name)
            intent.putExtra("sistemaDescripcion", ticket.system?.name ?: "--")
            intent.putExtra("subsistemaDescripcion", ticket.subsystem?.name ?: "--")
            intent.putExtra("tipoEquipoDescripcion", ticket.equipment?.type ?: "--")
            intent.putExtra("tagEquipoDescripcion", ticket.equipment?.tag ?: "--")
            
            startActivity(intent)
        }
        
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    override fun onResume() {
        super.onResume()
        cargarTickets()
    }

    // Adapter que incluye header de notificaciones
    class TicketsWithHeaderAdapter(
        private val tickets: List<Ticket>,
        private val onSolucionarClick: (Ticket) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val TYPE_HEADER = 0
            private const val TYPE_TICKET = 1
        }

        inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNotificationTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
            val tvNotificationDescription: TextView = itemView.findViewById(R.id.tvNotificationDescription)
            val chipTicketCount: Chip = itemView.findViewById(R.id.chipTicketCount)
            val btnSincronizar: MaterialButton = itemView.findViewById(R.id.btnSincronizarTickets)
        }

        inner class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTicketNumber: TextView = itemView.findViewById(R.id.tvTicketNumber)
            val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
            val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
            val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
            val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
            val tvEquipment: TextView = itemView.findViewById(R.id.tvEquipment)
            val tvReportedBy: TextView = itemView.findViewById(R.id.tvReportedBy)
            val tvReportedAt: TextView = itemView.findViewById(R.id.tvReportedAt)
            val btnSolucionar: MaterialButton = itemView.findViewById(R.id.btnSolucionar)
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) TYPE_HEADER else TYPE_TICKET
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_HEADER -> {
                    Log.d("TicketsWithHeaderAdapter", "onCreateViewHolder HEADER")
                    val view = android.view.LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_notification_header, parent, false)
                    HeaderViewHolder(view)
                }
                TYPE_TICKET -> {
                    Log.d("TicketsWithHeaderAdapter", "onCreateViewHolder TICKET")
                    val view = android.view.LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_ticket, parent, false)
                    TicketViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderViewHolder -> {
                    Log.d("TicketsWithHeaderAdapter", "Binding HEADER")
                    holder.tvNotificationTitle.text = "Tickets Pendientes"
                    holder.tvNotificationDescription.text = "Se encontraron ${tickets.size} tickets que requieren atención."
                    holder.chipTicketCount.text = tickets.size.toString()
                    holder.btnSincronizar.visibility = View.GONE // Ocultar el botón de sincronizar
                    
                    // Comentado: ya no se necesita el listener del botón
                    // holder.btnSincronizar.setOnClickListener {
                    //     Log.d("TicketsWithHeaderAdapter", "Botón sincronizar presionado")
                    // }
                }
                is TicketViewHolder -> {
                    val ticketIndex = position - 1 // Restar 1 porque el header está en posición 0
                    val ticket = tickets[ticketIndex]
                    
                    Log.d("TicketsWithHeaderAdapter", "Binding ticket: ${ticket.ticketNumber}")
                    
                    holder.tvTicketNumber.text = ticket.ticketNumber
                    holder.tvTitle.text = ticket.title
                    holder.tvDescription.text = ticket.description
                    holder.tvStatus.text = ticket.statusText
                    holder.tvPriority.text = ticket.priorityText
                    holder.tvLocation.text = ticket.location.name
                    holder.tvEquipment.text = ticket.equipment?.let { "${it.tag} - ${it.type}" } ?: "N/A"
                    holder.tvReportedBy.text = "Reportado por: ${ticket.reportedBy.name}"
                    holder.tvReportedAt.text = ticket.reportedAtFormatted

                    // Configurar color del estado
                    when (ticket.status) {
                        "abierto" -> holder.tvStatus.setTextColor(android.graphics.Color.RED)
                        "en_progreso" -> holder.tvStatus.setTextColor(android.graphics.Color.BLUE)
                        else -> holder.tvStatus.setTextColor(android.graphics.Color.GRAY)
                    }

                    // Configurar color de prioridad
                    when (ticket.priority) {
                        "alta" -> holder.tvPriority.setTextColor(android.graphics.Color.RED)
                        "media" -> holder.tvPriority.setTextColor(android.graphics.Color.parseColor("#FFA500"))
                        "baja" -> holder.tvPriority.setTextColor(android.graphics.Color.GREEN)
                        else -> holder.tvPriority.setTextColor(android.graphics.Color.GRAY)
                    }

                    holder.btnSolucionar.setOnClickListener {
                        onSolucionarClick(ticket)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            val count = tickets.size + 1 // +1 para el header
            Log.d("TicketsWithHeaderAdapter", "getItemCount: $count (${tickets.size} tickets + 1 header)")
            return count
        }
    }

    // Adapter original para tickets (mantenido por compatibilidad)
    class TicketsAdapter(
        private val tickets: List<Ticket>,
        private val onSolucionarClick: (Ticket) -> Unit
    ) : RecyclerView.Adapter<TicketsAdapter.TicketViewHolder>() {

        inner class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTicketNumber: TextView = itemView.findViewById(R.id.tvTicketNumber)
            val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
            val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
            val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
            val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
            val tvEquipment: TextView = itemView.findViewById(R.id.tvEquipment)
            val tvReportedBy: TextView = itemView.findViewById(R.id.tvReportedBy)
            val tvReportedAt: TextView = itemView.findViewById(R.id.tvReportedAt)
            val btnSolucionar: MaterialButton = itemView.findViewById(R.id.btnSolucionar)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TicketViewHolder {
            Log.d("TicketsAdapter", "onCreateViewHolder llamado")
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ticket, parent, false)
            return TicketViewHolder(view)
        }

        override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
            Log.d("TicketsAdapter", "onBindViewHolder llamado para posición $position")
            val ticket = tickets[position]
            
            Log.d("TicketsAdapter", "Binding ticket: ${ticket.ticketNumber} - ${ticket.title}")
            
            holder.tvTicketNumber.text = ticket.ticketNumber
            holder.tvTitle.text = ticket.title
            holder.tvDescription.text = ticket.description
            holder.tvStatus.text = ticket.statusText
            holder.tvPriority.text = ticket.priorityText
            holder.tvLocation.text = ticket.location.name
            holder.tvEquipment.text = ticket.equipment?.let { "${it.tag} - ${it.type}" } ?: "N/A"
            holder.tvReportedBy.text = "Reportado por: ${ticket.reportedBy.name}"
            holder.tvReportedAt.text = ticket.reportedAtFormatted

            // Configurar color del estado
            when (ticket.status) {
                "abierto" -> holder.tvStatus.setTextColor(android.graphics.Color.RED)
                "en_progreso" -> holder.tvStatus.setTextColor(android.graphics.Color.BLUE)
                else -> holder.tvStatus.setTextColor(android.graphics.Color.GRAY)
            }

            // Configurar color de prioridad
            when (ticket.priority) {
                "alta" -> holder.tvPriority.setTextColor(android.graphics.Color.RED)
                "media" -> holder.tvPriority.setTextColor(android.graphics.Color.parseColor("#FFA500"))
                "baja" -> holder.tvPriority.setTextColor(android.graphics.Color.GREEN)
                else -> holder.tvPriority.setTextColor(android.graphics.Color.GRAY)
            }

            holder.btnSolucionar.setOnClickListener {
                onSolucionarClick(ticket)
            }
            
            Log.d("TicketsAdapter", "Ticket ${ticket.ticketNumber} bindeado exitosamente")
        }

        override fun getItemCount(): Int {
            Log.d("TicketsAdapter", "getItemCount llamado: ${tickets.size}")
            return tickets.size
        }
    }
}

