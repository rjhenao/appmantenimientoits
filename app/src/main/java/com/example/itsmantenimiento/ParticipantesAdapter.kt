package com.uvrp.itsmantenimientoapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uvrp.itsmantenimientoapp.R
import com.uvrp.itsmantenimientoapp.models.Usuario

class ParticipantesAdapter(
    private val participantes: List<Usuario>,
    savedSelection: Set<String>,
    private val onSelectionChanged: () -> Unit // Callback para guardar datos
) : RecyclerView.Adapter<ParticipantesAdapter.ParticipanteViewHolder>() {

    // Este Set guardará los IDs de los usuarios seleccionados
    private val selectedIds = mutableSetOf<Int>()

    init {
        // Al iniciar, marcamos los checkboxes que ya estaban seleccionados
        participantes.forEach {
            if (savedSelection.contains(it.id.toString())) {
                selectedIds.add(it.id)
            }
        }
    }

    inner class ParticipanteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.textview_nombre_usuario)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_usuario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipanteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario_seleccion, parent, false)
        return ParticipanteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipanteViewHolder, position: Int) {
        val participante = participantes[position]
        holder.userName.text = participante.nombre

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedIds.contains(participante.id)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedIds.add(participante.id)
            } else {
                selectedIds.remove(participante.id)
            }
            // Llama al callback para que la Activity guarde los datos
            onSelectionChanged()
        }
    }

    override fun getItemCount(): Int = participantes.size

    fun getSelectedUserIds(): Set<Int> {
        return selectedIds
    }
}