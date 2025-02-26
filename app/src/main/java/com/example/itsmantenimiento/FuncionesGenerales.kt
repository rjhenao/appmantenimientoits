package com.example.itsmantenimiento

import android.content.Context
import android.widget.Toast

object  FuncionesGenerales {



    fun sincronizarMantenimientos (context: Context) {

        val dbHelper = DatabaseHelper(context)

        val sincronizarManteninimientosTerminados = dbHelper.sincronizarManteninimientosTerminados()




        Toast.makeText(context, "Sincronizando datosd2d2d2...", Toast.LENGTH_SHORT).show()
    }



}