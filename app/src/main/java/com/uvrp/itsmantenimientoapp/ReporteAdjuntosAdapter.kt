package com.uvrp.itsmantenimientoapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ReporteAdjuntosAdapter(
    private val onRemove: (Int) -> Unit,
) : RecyclerView.Adapter<ReporteAdjuntosAdapter.VH>() {

    var uris: List<Uri> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.imgPreview)
        val remove: ImageButton = itemView.findViewById(R.id.btnRemoveAdjunto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte_adjunto_preview, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val uri = uris[position]
        Glide.with(holder.img).load(uri).centerCrop().into(holder.img)
        holder.remove.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onRemove(pos)
        }
    }

    override fun getItemCount(): Int = uris.size
}
