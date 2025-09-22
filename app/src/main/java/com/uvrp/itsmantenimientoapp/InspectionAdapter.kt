package com.uvrp.itsmantenimientoapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.uvrp.itsmantenimientoapp.R
import com.uvrp.itsmantenimientoapp.models.Activity

class InspectionAdapter(val activities: List<Activity>) :
    RecyclerView.Adapter<InspectionAdapter.ViewHolder>() {

    // El ViewHolder contiene las vistas para cada item de la lista.
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.cbActivity)
    }

    // Crea una nueva vista (invocado por el layout manager).
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        // Infla el layout personalizado para el item.
        val activityView = inflater.inflate(R.layout.item_activity, parent, false)
        return ViewHolder(activityView)
    }

    // Reemplaza el contenido de una vista (invocado por el layout manager).
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Obtiene el objeto de datos para esta posición.
        val activity: Activity = activities[position]

        // Asigna los datos a las vistas.
        val checkBox = holder.checkBox
        checkBox.text = activity.description
        checkBox.isChecked = activity.isChecked

        // Actualiza el estado en nuestro modelo de datos cuando el checkbox cambia.
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            activity.isChecked = isChecked
        }
    }

    // Devuelve el tamaño total de la lista de datos.
    override fun getItemCount(): Int {
        return activities.size
    }
}
