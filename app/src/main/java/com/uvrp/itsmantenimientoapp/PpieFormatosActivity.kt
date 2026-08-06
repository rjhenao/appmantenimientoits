package com.uvrp.itsmantenimientoapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper

class PpieFormatosActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(applicationContext)
        setContentView(R.layout.activity_ppie_formatos)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val nav = findViewById<NavigationView>(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawer, nav)

        db = DatabaseHelper(this)
        val rv = findViewById<RecyclerView>(R.id.rvPpieFormatos)
        val empty = findViewById<TextView>(R.id.tvPpieEmpty)
        val formatos = db.obtenerPpieFormatos()
        if (formatos.isEmpty()) {
            empty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            empty.visibility = View.GONE
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = object : RecyclerView.Adapter<FmtVH>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FmtVH {
                    val v = LayoutInflater.from(parent.context)
                        .inflate(android.R.layout.simple_list_item_2, parent, false)
                    return FmtVH(v)
                }

                override fun getItemCount() = formatos.size

                override fun onBindViewHolder(holder: FmtVH, position: Int) {
                    val f = formatos[position]
                    holder.t1.text = f.code
                    holder.t2.text = f.title
                    holder.itemView.setOnClickListener {
                        startActivity(
                            Intent(this@PpieFormatosActivity, PpieDiligenciarActivity::class.java)
                                .putExtra("format_id", f.id)
                                .putExtra("format_code", f.code)
                                .putExtra("format_title", f.title)
                        )
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (HeaderHelper.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    private class FmtVH(v: View) : RecyclerView.ViewHolder(v) {
        val t1: TextView = v.findViewById(android.R.id.text1)
        val t2: TextView = v.findViewById(android.R.id.text2)
    }
}
