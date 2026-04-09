package com.uvrp.itsmantenimientoapp

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.uvrp.itsmantenimientoapp.DatabaseHelper.ActividadBitacora
import java.util.Locale

/**
 * Lista desplegable con búsqueda por texto y filas multilínea.
 * El ancho del popup a veces coincide con el campo (estrecho); [aplicarAnchoTextoCompleto] fuerza
 * [TextView.maxWidth] al ancho de pantalla para que el texto haga salto de línea y no corte con "...".
 */
class ActividadBitacoraDropdownAdapter(
    private val context: Context,
    private val todas: List<ActividadBitacora>
) : BaseAdapter(), Filterable {

    private val inflater = LayoutInflater.from(context)
    private var mostrar: ArrayList<ActividadBitacora> = ArrayList(todas)

    /** Ancho máximo para el texto (píxeles), p.ej. ancho pantalla menos márgenes. */
    private var anchoTextoPx: Int = calcularAnchoTextoPorDefecto()

    private fun calcularAnchoTextoPorDefecto(): Int {
        val dm = context.resources.displayMetrics
        val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, dm).toInt()
        return (dm.widthPixels - margin).coerceAtLeast(200)
    }

    /** Llamar desde la Activity tras conocer el ancho real del popup (opcional). */
    fun setAnchoDisponibleParaTexto(px: Int) {
        if (px > 0) {
            anchoTextoPx = px
            notifyDataSetChanged()
        }
    }

    private val filtro = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val res = FilterResults()
            val q = constraint?.toString()?.trim()?.lowercase(Locale.getDefault()).orEmpty()
            val list = if (q.length < 3) {
                todas
            } else {
                todas.filter {
                    it.descripcion.lowercase(Locale.getDefault()).contains(q)
                }
            }
            res.values = list
            res.count = list.size
            return res
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            mostrar.clear()
            if (results != null && results.count > 0) {
                mostrar.addAll(results.values as List<ActividadBitacora>)
            }
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter = filtro

    override fun getCount(): Int = mostrar.size

    override fun getItem(position: Int): ActividadBitacora = mostrar[position]

    override fun getItemId(position: Int): Long = position.toLong()

    private fun aplicarAnchoTextoCompleto(tv: TextView) {
        tv.apply {
            setSingleLine(false)
            maxLines = 50
            ellipsize = null
            setHorizontallyScrolling(false)
            // Clave: sin un ancho máximo acotado, en listas el texto a veces se dibuja en una línea y se corta.
            maxWidth = anchoTextoPx
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                breakStrategy = android.text.Layout.BREAK_STRATEGY_BALANCED
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hyphenationFrequency = android.text.Layout.HYPHENATION_FREQUENCY_NONE
            }
        }
    }

    private fun enlazarFila(convertView: View?, parent: ViewGroup?, actividad: ActividadBitacora): View {
        val v = convertView ?: inflater.inflate(R.layout.item_autocomplete_actividad_dropdown, parent, false)
        v.layoutParams = AbsListView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val tv = v.findViewById<TextView>(R.id.text_actividad_item)
        aplicarAnchoTextoCompleto(tv)
        tv.text = actividad.descripcion
        return v
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return enlazarFila(convertView, parent, getItem(position))
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Algunas versiones usan esta variante para el popup; debe aplicar la misma lógica.
        return enlazarFila(convertView, parent, getItem(position))
    }
}
