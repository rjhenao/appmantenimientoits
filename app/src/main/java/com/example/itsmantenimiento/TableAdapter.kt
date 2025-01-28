package com.example.itsmantenimiento

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Clase de datos para las filas de la tabla
data class TableItem(val column1: String, val column2: String)

// Adaptador para el RecyclerView
class TableAdapter(private val items: List<TableItem>) : RecyclerView.Adapter<TableAdapter.TableViewHolder>() {

    // ViewHolder para enlazar los elementos del layout de la fila
    class TableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val column1TextView: TextView = itemView.findViewById(R.id.column1)
        val column2TextView: TextView = itemView.findViewById(R.id.column2)
    }

    // Infla el diseño de cada fila
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.table_row, parent, false)
        return TableViewHolder(view)
    }

    // Asocia los datos con las vistas de cada fila
    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        val item = items[position]
        holder.column1TextView.text = item.column1
        holder.column2TextView.text = item.column2
    }

    // Devuelve el número de elementos en la lista
    override fun getItemCount(): Int = items.size
}