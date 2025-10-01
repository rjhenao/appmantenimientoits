package com.uvrp.itsmantenimientoapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uvrp.itsmantenimientoapp.R
import com.uvrp.itsmantenimientoapp.models.ActividadMantenimiento

class ActividadesProgramadasAdapter(
    private val actividades: List<ActividadMantenimiento>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<ActividadesProgramadasAdapter.ActividadViewHolder>() {

    interface OnItemClickListener {
        fun onRegistrarClick(actividadId: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActividadViewHolder {
        val layoutRes = if (viewType == 2) {
            R.layout.item_actividad_no_programada
        } else {
            R.layout.item_actividad_programada
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return ActividadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActividadViewHolder, position: Int) {
        val actividad = actividades[position]
        holder.bind(actividad, listener)
    }

    override fun getItemCount(): Int = actividades.size

    override fun getItemViewType(position: Int): Int {
        return actividades[position].estado
    }

    class ActividadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Views comunes
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        private val tvCuadrilla: TextView = itemView.findViewById(R.id.tvCuadrilla)
        private val btnRegistrar: Button = itemView.findViewById(R.id.btnRegistrarActividad)
        
        // Views específicos para actividades programadas
        private val tvInfo: TextView? = itemView.findViewById(R.id.tvInfo)
        private val tvPrs: TextView? = itemView.findViewById(R.id.tvPrs)
        private val tvCantidad: TextView? = itemView.findViewById(R.id.tvCantidad)
        private val tvObservacion: TextView? = itemView.findViewById(R.id.tvObservacion)
        
        // Views específicos para actividades no programadas
        private val tvUbicacion: TextView? = itemView.findViewById(R.id.tvUbicacion)
        private val tvProgreso: TextView? = itemView.findViewById(R.id.tvProgreso)
        private val tvCantidadNoProg: TextView? = itemView.findViewById(R.id.tvCantidad)
        private val layoutObservacion: View? = itemView.findViewById(R.id.layoutObservacion)
        private val tvObservacionNoProg: TextView? = itemView.findViewById(R.id.tvObservacion)

        fun bind(actividad: ActividadMantenimiento, listener: OnItemClickListener) {
            tvDescripcion.text = actividad.descripcion
            tvCuadrilla.text = "Cuadrilla: ${actividad.cuadrillaNombre}"

            if (actividad.estado == 2) {
                // Actividad no programada
                tvUbicacion?.text = "UF: ${actividad.uf} | Sentido: ${actividad.sentido} | Lado: ${actividad.lado}"
                tvProgreso?.text = "PR Inicial: ${actividad.prInicial} | PR Final: ${actividad.prFinal}"
                tvCantidadNoProg?.text = "Cantidad: ${actividad.cantidad}"
                
                if (actividad.observacion.isNotEmpty()) {
                    layoutObservacion?.visibility = View.VISIBLE
                    tvObservacionNoProg?.text = actividad.observacion
                } else {
                    layoutObservacion?.visibility = View.GONE
                }
            } else {
                // Actividad programada
                tvInfo?.text = "UF: ${actividad.uf} | Sentido: ${actividad.sentido} | Lado: ${actividad.lado}"
                tvPrs?.text = "PR Inicial: ${actividad.prInicial} | PR Final: ${actividad.prFinal}"
                tvCantidad?.text = "Cantidad: ${actividad.cantidad}"
                tvObservacion?.text = "Observación: ${actividad.observacion}"
            }

            btnRegistrar.setOnClickListener {
                listener.onRegistrarClick(actividad.id)
            }
        }
    }
}