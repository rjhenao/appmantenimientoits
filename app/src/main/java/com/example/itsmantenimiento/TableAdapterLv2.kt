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
data class TableItemLv2(val column1f1: String , val idLocacion: String , val sistema: String , val idSistema : String)

// Adaptador para el RecyclerView
class TableAdapterLv2(private val items: List<TableItemLv2>) : RecyclerView.Adapter<TableAdapterLv2.TableViewHolderLv2>() {

    // ViewHolder para enlazar los elementos del layout de la fila
    class TableViewHolderLv2(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val column1f1TextView: TextView = itemView.findViewById(R.id.column1_row1)
        val column1f2TextView: TextView = itemView.findViewById(R.id.column1_row2)
        val buttonColumn3f3: View = itemView.findViewById(R.id.column3_row3)

    }

    // Infla el diseño de cada fila
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolderLv2 {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.table_row_lv2, parent, false)
        return TableViewHolderLv2(view)
    }

    // Asocia los datos con las vistas de cada fila
    override fun onBindViewHolder(holder: TableViewHolderLv2, position: Int) {
        val item = items[position]
        holder.column1f1TextView.text = item.column1f1
        holder.column1f2TextView.text = item.sistema


        holder.buttonColumn3f3.setOnClickListener {

            val context = holder.itemView.context
            val dbHelper = DatabaseHelper(context)
            val idLocacion = items[position].idLocacion
            val idSistema = items[position].idSistema

            // Obtener el idUser desde SharedPreferences


            val sharedPreferences = holder.itemView.context.getSharedPreferences("Sesion", android.content.Context.MODE_PRIVATE)
            val idUser = sharedPreferences.getInt("idUser", -1) // -1 si no está logeado

            if (idUser == -1) {
                Log.e("TableAdapter", "⚠️ No se pudo obtener el ID del usuario logeado")
                return@setOnClickListener
            }

            val intent = Intent(context, Nivel3Activity::class.java)
            intent.putExtra("idLocacion" , idLocacion)
            intent.putExtra("idSistema" , idSistema)
            Log.d("hdhdndheyuqqq" , "$idLocacion , $idSistema");

            context.startActivity(intent)
        }
    }




    fun buscarPorID(id: Int) {
        Log.d("TableAdapter", "Botón presionado para ID: $id")
        // Aquí puedes hacer una búsqueda en la base de datos o realizar la acción deseada.
    }



    // Devuelve el número de elementos en la lista
    override fun getItemCount(): Int = items.size
}