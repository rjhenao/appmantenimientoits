package com.uvrp.itsmantenimientoapp

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FotoAdapter(
    private val fotos: MutableList<File>,
    private val onEliminarClick: (File) -> Unit
) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFoto: ImageView = itemView.findViewById(R.id.imgFotoMasiva)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminarFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_foto_masiva, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val foto = fotos[position]
        
        try {
            // Cargar imagen
            val bitmap = BitmapFactory.decodeFile(foto.absolutePath)
            if (bitmap != null) {
                holder.imgFoto.setImageBitmap(bitmap)
            } else {
                holder.imgFoto.setImageResource(android.R.drawable.ic_menu_camera)
            }
            
            // Configurar botón eliminar
            holder.btnEliminar.setOnClickListener {
                onEliminarClick(foto)
            }
            
        } catch (e: Exception) {
            Log.e("FotoAdapter", "Error al cargar foto: ${e.message}", e)
            holder.imgFoto.setImageResource(android.R.drawable.ic_menu_camera)
        }
    }

    override fun getItemCount(): Int = fotos.size

    // Método para eliminar foto de la lista
    fun eliminarFoto(file: File) {
        fotos.remove(file)
        notifyDataSetChanged()
    }
}