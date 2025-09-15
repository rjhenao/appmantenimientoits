package com.uvrp.itsmantenimientoapp.adapters

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PendientesAdapter(private val pendientes: List<String>) :
    RecyclerView.Adapter<PendientesAdapter.PendienteViewHolder>() {

    // 1️⃣ ViewHolder: Define cómo se verá cada item
    inner class PendienteViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    // 2️⃣ Crear cada ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendienteViewHolder {
        val textView = TextView(parent.context).apply {
            setPadding(16, 16, 16, 16)
            textSize = 14f
        }
        return PendienteViewHolder(textView)
    }

    // 3️⃣ Llenar los datos en cada ViewHolder
    override fun onBindViewHolder(holder: PendienteViewHolder, position: Int) {
        holder.textView.text = "Tag: ${pendientes[position]}"
    }

    // 4️⃣ Número de items
    override fun getItemCount(): Int = pendientes.size
}
