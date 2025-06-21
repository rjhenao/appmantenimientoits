package com.example.itsmantenimiento

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class FotoAdapter(
    private val fotos: MutableList<File>,
    private val onEliminarClick: (File) -> Unit
) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foto_miniatura, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        holder.bind(fotos[position])
    }

    override fun getItemCount(): Int = fotos.size

    fun eliminarFoto(file: File) {
        val position = fotos.indexOf(file)
        if (position != -1) {
            fotos.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageFoto: ImageView = itemView.findViewById(R.id.imageFoto)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)

        fun bind(file: File) {
            Glide.with(itemView.context)
                .load(Uri.fromFile(file))
                .centerCrop()
                .into(imageFoto)

            btnEliminar.setOnClickListener {
                onEliminarClick(file)
            }
        }
    }
}
