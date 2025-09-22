package com.uvrp.itsmantenimientoapp

import Actividad
import ActividadEstado
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Queue
import java.util.TimeZone

// Clase de datos para las filas de la tabla
data class TableItem(val column1f1: String, val column2f1: String , val column3f1: String ,
                     val column1f2: String, val column2f2: String , val column3f2: String ,
                     val column1f3: String, val column2f3: String , val idmantenimiento: Int)



// Adaptador para el RecyclerView
class TableAdapter(private val items: List<TableItem>) : RecyclerView.Adapter<TableAdapter.TableViewHolder>() {

    // ViewHolder para enlazar los elementos del layout de la fila
    class TableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val column1f1TextView: TextView = itemView.findViewById(R.id.column1_row1)
        val column2f1TextView: TextView = itemView.findViewById(R.id.column2_row1)
        val column3f1TextView: TextView = itemView.findViewById(R.id.column3_row1)
        val column1f2TextView: TextView = itemView.findViewById(R.id.column1_row2)
        val column2f2TextView: TextView = itemView.findViewById(R.id.column2_row2)
        val column3f2TextView: TextView = itemView.findViewById(R.id.column3_row2)
        val column1f3TextView: TextView = itemView.findViewById(R.id.column1_row3)
        val column2f3TextView: TextView = itemView.findViewById(R.id.column2_row3)
        val buttonColumn3f3: View = itemView.findViewById(R.id.column3_row3)

    }

    // Infla el diseño de cada fila
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.table_row, parent, false)
        return TableViewHolder(view)
    }

    // Asocia los datos con las vistas de cada fila
    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        val item = items[position]
        holder.column1f1TextView.text = item.column1f1
        holder.column2f1TextView.text = item.column2f1
        holder.column3f1TextView.text = item.column3f1
        holder.column1f2TextView.text = item.column1f2
        holder.column2f2TextView.text = item.column2f2
        holder.column3f2TextView.text = item.column3f2
        holder.column1f3TextView.text = item.column1f3
        holder.column2f3TextView.text = item.column2f3

        holder.buttonColumn3f3.setOnClickListener {
            Log.d("TableAdapter1", "🟢 Botón presionado para ID: ${items[position].idmantenimiento}")

            val context = holder.itemView.context
            val dbHelper = DatabaseHelper(context)

            // Obtener el idUser desde SharedPreferences

            val sharedPreferences = holder.itemView.context.getSharedPreferences("Sesion", android.content.Context.MODE_PRIVATE)
            val idUser = sharedPreferences.getInt("idUser", -1) // -1 si no está logeado

            if (idUser == -1) {
                Log.e("TableAdapter", "⚠️ No se pudo obtener el ID del usuario logeado")
                return@setOnClickListener
            }

            val idMantenimiento = items[position].idmantenimiento
            val fechaInicial = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("America/Bogota")
            }.format(Date())

            val fechaFinal = "" // Vacío
            val estado = 0
            val sincronizado = 0

            //0 no se encuentra mantenimiento iniciando por el usuario ... 1 si encuentra mantenimiento iniciado por el usuario
            val valManteniniento = dbHelper.validarManteninientoActividad(idUser, idMantenimiento)

            if (valManteniniento) {
                Log.d("DebugITS", "✅ Se encontró un mantenimiento iniciado")
            } else {
                // Insertar en la BD
                val resultado = dbHelper.insertarRelTecnicoMantenimiento(idUser, idMantenimiento, fechaInicial, fechaFinal, estado, sincronizado)
                Log.d("DebugITS3", "$resultado")

                if (resultado > 0) {
                    Log.d("TableAdapter", "✅ Registro insertado en rel_tecnico_mantenimiento: ID=$resultado")
                } else {
                    Log.e("TableAdapter", "❌ Error al insertar en rel_tecnico_mantenimiento")
                }
            }

            // Continuar con la lógica de abrir la nueva actividad
            //val resultadoActividades = dbHelper.getActividadesMantenimiento(idMantenimiento)
            //val actividades = resultadoActividades.actividades

            val resultadoActividades: Pair<List<Actividad>, List<ActividadEstado>> = dbHelper.getActividadesMantenimiento(idMantenimiento)

            val (actividades, estados) = resultadoActividades



            if (actividades.isNotEmpty()) {
                val intent = Intent(context, MantenimientoActivity::class.java)
                intent.putExtra("idmantenimiento", idMantenimiento)
                intent.putParcelableArrayListExtra("actividades", ArrayList(actividades))
                intent.putParcelableArrayListExtra("estadosmantenimientos", ArrayList(estados))
                intent.putExtra("column1f1", item.column1f1)
                intent.putExtra("column2f1", item.column2f1)
                intent.putExtra("column3f1", item.column3f1)
                intent.putExtra("column1f2", item.column1f2)
                intent.putExtra("column2f2", item.column2f2)
                intent.putExtra("column3f2", item.column3f2)
                intent.putExtra("column1f3", item.column1f3)
                intent.putExtra("column2f3", item.column2f3)

                context.startActivity(intent)
            } else {
                Log.d("TableAdapter", "⚠️ No se encontraron actividades para ID: $idMantenimiento")
            }
        }




    }




    fun buscarPorID(id: Int) {
        Log.d("TableAdapter", "Botón presionado para ID: $id")
        // Aquí puedes hacer una búsqueda en la base de datos o realizar la acción deseada.
    }



    // Devuelve el número de elementos en la lista
    override fun getItemCount(): Int = items.size
}