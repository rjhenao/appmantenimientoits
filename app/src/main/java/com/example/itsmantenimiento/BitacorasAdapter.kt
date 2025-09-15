package com.uvrp.itsmantenimientoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip

data class Bitacora(
    val numero: String,
    val estado: String,
    val supervisor: String,
    val periodo: String
)

class BitacorasAdapter(
    private val bitacoras: List<Bitacora>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<BitacorasAdapter.BitacoraViewHolder>() {

    interface OnItemClickListener {
        fun onRegistrarClick(bitacora: Bitacora)
    }

    inner class BitacoraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numeroBitacora: TextView = itemView.findViewById(R.id.tvNumeroBitacora)
        val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        val supervisor: TextView = itemView.findViewById(R.id.tvSupervisorValue)
        val periodo: TextView = itemView.findViewById(R.id.tvPeriodoValue)
        val btnRegistrar: Button = itemView.findViewById(R.id.btnRegistrar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BitacoraViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bitacora, parent, false)
        return BitacoraViewHolder(view)
    }

    override fun getItemCount(): Int = bitacoras.size

    override fun onBindViewHolder(holder: BitacoraViewHolder, position: Int) {
        val bitacora = bitacoras[position]
        val context = holder.itemView.context

        holder.numeroBitacora.text = "Bitácora #${bitacora.numero}"
        holder.chipStatus.text = bitacora.estado
        holder.supervisor.text = bitacora.supervisor
        holder.periodo.text = bitacora.periodo

        if (bitacora.estado.equals("Activa", ignoreCase = true)) {
            holder.chipStatus.setChipBackgroundColorResource(R.color.green_success_light)
            holder.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.green_success_dark))
        } else {
            holder.chipStatus.setChipBackgroundColorResource(R.color.divider_color)
            holder.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        holder.btnRegistrar.setOnClickListener {
            listener.onRegistrarClick(bitacora)
        }
    }
}