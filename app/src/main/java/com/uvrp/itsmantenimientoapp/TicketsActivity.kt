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
    private lateinit var btnSincronizar: MaterialButton
    private lateinit var cardNotifications: MaterialCardView
    private lateinit var tvNotificationTitle: TextView
    private lateinit var tvNotificationDescription: TextView
    private lateinit var chipTicketCount: Chip

    private lateinit var dbHelper: DatabaseHelper
    private var ticketsList: List<Ticket> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tickets)

        // Inicializar componentes
        initializeViews()
        setupHeader()
        setupRecyclerView()
        // setupSincronizarButton() // Comentado - sincronización manejada desde otro módulo
        
        // Cargar tickets iniciales
        cargarTickets()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        rvTickets = findViewById(R.id.rvTickets)
        btnSincronizar = findViewById(R.id.btnSincronizarTickets)
        cardNotifications = findViewById(R.id.cardNotifications)
        tvNotificationTitle = findViewById(R.id.tvNotificationTitle)
        tvNotificationDescription = findViewById(R.id.tvNotificationDescription)
        chipTicketCount = findViewById(R.id.chipTicketCount)

        dbHelper = DatabaseHelper(this)
    }

    private fun setupHeader() {
        HeaderHelper.setupHeader(this, drawerLayout, navView)
    }

    private fun setupRecyclerView() {
        rvTickets.layoutManager = LinearLayoutManager(this)
    }

    // Función comentada - sincronización manejada desde otro módulo
    /*
    private fun setupSincronizarButton() {
        btnSincronizar.setOnClickListener {
            btnSincronizar.text = "Sincronizando..."
            btnSincronizar.isEnabled = false

            CoroutineScope(Dispatchers.Main).launch {
                val exito = withContext(Dispatchers.IO) {
                    try {
                        val api = RetrofitClient.instance
                        val response = api.getTickets().execute()
                        
                        if (response.isSuccessful) {
                            val ticketResponse = response.body()
                            if (ticketResponse != null && ticketResponse.success) {
                                dbHelper.insertarOActualizarTickets(ticketResponse.data)
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        Log.e("TicketsActivity", "Error al sincronizar tickets", e)
                        false
                    }
                }

                btnSincronizar.text = "Sincronizar Ahora"
                btnSincronizar.isEnabled = true

                if (exito) {
                    cargarTickets()
                    Toast.makeText(this@TicketsActivity, "Tickets sincronizados exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@TicketsActivity, "Error al sincronizar tickets", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    */

    private fun cargarTickets() {
        // Obtener tickets con estado 'abierto' y 'en_progreso'
        ticketsList = dbHelper.obtenerTicketsPorEstado(listOf("abierto", "en_progreso"))
        
        if (ticketsList.isEmpty()) {
            // Mostrar estado sin tickets
            cardNotifications.visibility = View.VISIBLE
            chipTicketCount.visibility = View.GONE
            btnSincronizar.visibility = View.GONE // Ocultar botón de sincronizar
            rvTickets.visibility = View.GONE

            tvNotificationTitle.text = "¡No hay tickets pendientes! ✨"
            tvNotificationDescription.text = "No se encontraron tickets abiertos o en progreso."
        } else {
            // Mostrar tickets
            cardNotifications.visibility = View.VISIBLE
            chipTicketCount.visibility = View.VISIBLE
            btnSincronizar.visibility = View.GONE // Ocultar botón de sincronizar
            rvTickets.visibility = View.VISIBLE

            chipTicketCount.text = ticketsList.size.toString()
            tvNotificationTitle.text = "Tickets Pendientes"
            tvNotificationDescription.text = "Se encontraron ${ticketsList.size} tickets que requieren atención."

            // Configurar RecyclerView
            rvTickets.adapter = TicketsAdapter(ticketsList) { ticket ->
                // Acción al hacer clic en "Solucionar"
                mostrarDialogoSolucionar(ticket)
            }
        }
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

    // Adapter para la lista de tickets
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
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ticket, parent, false)
            return TicketViewHolder(view)
        }

        override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
            val ticket = tickets[position]
            
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

        override fun getItemCount(): Int = tickets.size
    }
}

