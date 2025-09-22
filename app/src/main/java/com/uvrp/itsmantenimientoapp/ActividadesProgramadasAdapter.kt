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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_actividad_programada, parent, false)
        return ActividadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActividadViewHolder, position: Int) {
        val actividad = actividades[position]
        holder.bind(actividad, listener)
    }

    override fun getItemCount(): Int = actividades.size

    class ActividadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        private val tvCuadrilla: TextView = itemView.findViewById(R.id.tvCuadrilla)
        private val tvInfo: TextView = itemView.findViewById(R.id.tvInfo)
        private val tvPrs: TextView = itemView.findViewById(R.id.tvPrs)
        private val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        private val tvObservacion: TextView = itemView.findViewById(R.id.tvObservacion)
        private val btnRegistrar: Button = itemView.findViewById(R.id.btnRegistrarActividad)

        fun bind(actividad: ActividadMantenimiento, listener: OnItemClickListener) {
            tvDescripcion.text = actividad.descripcion
            tvCuadrilla.text = "Cuadrilla: ${actividad.cuadrillaNombre}"
            tvInfo.text = "UF: ${actividad.uf} | Sentido: ${actividad.sentido} | Lado: ${actividad.lado}"
            tvPrs.text = "PR Inicial: ${actividad.prInicial} | PR Final: ${actividad.prFinal}"
            tvCantidad.text = "Cantidad: ${actividad.cantidad}"
            tvObservacion.text = "Observación: ${actividad.observacion}"

            btnRegistrar.setOnClickListener {
                listener.onRegistrarClick(actividad.id)
            }
        }
    }
}