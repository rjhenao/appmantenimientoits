package com.uvrp.itsmantenimientoapp

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*

object FuncionesGenerales {

    fun sincronizarMantenimientos(context: Context, onResult: (Boolean) -> Unit) {
        val dbHelper = DatabaseHelper(context)

        val progressDialog = AlertDialog.Builder(context)
            .setView(LayoutInflater.from(context).inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()

        progressDialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            val resultado = withContext(Dispatchers.IO) {
                dbHelper.sincronizarManteninimientosTerminados() // Ahora devuelve un resultado real
            }

            progressDialog.dismiss()

            if (resultado == 1) {
                Toast.makeText(context, "Sincronización exitosa.", Toast.LENGTH_LONG).show()
                onResult(true)
            } else {
                Toast.makeText(context, "No se realizó ningún movimiento.", Toast.LENGTH_LONG).show()
                onResult(false)
            }
        }
    }

}
