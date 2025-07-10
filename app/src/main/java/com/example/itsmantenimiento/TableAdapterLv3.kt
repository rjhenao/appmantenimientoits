package com.uvrp.itsmantenimientoapp

import Actividad
import ActividadEstado
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Queue
import java.util.TimeZone

// Clase de datos para las filas de la tabla
data class TableItemLv3(val column1f1: String , val idLocacion: String , val sistema: String , val idSistema : String , val subsistema: String , val idSubsistema : String)

// Adaptador para el RecyclerView
class TableAdapterLv3(private val items: List<TableItemLv3>) : RecyclerView.Adapter<TableAdapterLv3.TableViewHolderLv3>() {

    // ViewHolder para enlazar los elementos del layout de la fila
    class TableViewHolderLv3(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val column1f1TextView: TextView = itemView.findViewById(R.id.column1_row1)
        val column1f2TextView: TextView = itemView.findViewById(R.id.column1_row2)
        val column1f3TextView: TextView = itemView.findViewById(R.id.column1_row3)
        val buttonColumn3f3: View = itemView.findViewById(R.id.column3_row3)

    }

    // Infla el diseño de cada fila
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolderLv3 {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.table_row_lv3, parent, false)
        return TableViewHolderLv3(view)
    }

    // Asocia los datos con las vistas de cada fila
    override fun onBindViewHolder(holder: TableViewHolderLv3, position: Int) {
        val item = items[position]
        holder.column1f1TextView.text = item.column1f1
        holder.column1f2TextView.text = item.sistema
        holder.column1f3TextView.text = item.subsistema


        holder.buttonColumn3f3.setOnClickListener {

            val context = holder.itemView.context
            val dbHelper = DatabaseHelper(context)
            val idLocacion = items[position].idLocacion
            val idSistema = items[position].idSistema
            val idSubsistema = items[position].idSubsistema

            // Obtener el idUser desde SharedPreferences

            val sharedPreferences = holder.itemView.context.getSharedPreferences("Sesion", android.content.Context.MODE_PRIVATE)
            val idUser = sharedPreferences.getInt("idUser", -1) // -1 si no está logeado

            if (idUser == -1) {
                Log.e("TableAdapter", "⚠️ No se pudo obtener el ID del usuario logeado")
                return@setOnClickListener
            }

            val intent = Intent(context, programacion_diaria::class.java)
            intent.putExtra("idLocacion" , idLocacion)
            intent.putExtra("idSistema" , idSistema)
            intent.putExtra("idSubsistema" , idSubsistema)
            context.startActivity(intent)
        }
    }


    // Devuelve el número de elementos en la lista
    override fun getItemCount(): Int = items.size
}