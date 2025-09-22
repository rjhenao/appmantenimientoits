package com.uvrp.itsmantenimientoapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uvrp.itsmantenimientoapp.R // Asegúrate de importar tu R
import com.uvrp.itsmantenimientoapp.models.Usuario

class UsuarioSeleccionAdapter(
    private var userList: List<Usuario>,
    private val selectedIds: MutableSet<Int>
) : RecyclerView.Adapter<UsuarioSeleccionAdapter.UserViewHolder>() {

    // CAMBIO 1: El ViewHolder ahora recibe una 'View' normal
    // y busca cada componente con findViewById.
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.textview_nombre_usuario)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_usuario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // CAMBIO 2: Se "infla" el layout de la forma tradicional,
        // lo que nos da un objeto 'View'.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario_seleccion, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        // CAMBIO 3: Accedemos a los componentes directamente desde el 'holder',
        // en lugar de usar 'holder.binding'.
        holder.userName.text = user.nombre

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedIds.contains(user.id)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedIds.add(user.id)
            } else {
                selectedIds.remove(user.id)
            }
        }
    }

    override fun getItemCount(): Int = userList.size

    fun updateList(newList: List<Usuario>) {
        userList = newList
        notifyDataSetChanged()
    }
}