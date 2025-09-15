package com.uvrp.itsmantenimientoapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object FuncionesGenerales {

    fun sincronizarTodosMantenimientos(context: Context, onResult: (Boolean) -> Unit) {
        val dbHelper = DatabaseHelper(context)

        val progressDialog = AlertDialog.Builder(context)
            .setView(LayoutInflater.from(context).inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()

        progressDialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            val resultado = withContext(Dispatchers.IO) {

                // Ejecutamos cada proceso de sincronización por separado
                val exitoTerminados = dbHelper.sincronizarManteninimientosTerminados() == 1
                val exitoCorrectivos = sincronizarPendientesCorrectivos(context, dbHelper)
                val exitoBitacoras = sincronizarPendientesBitacora(context, dbHelper)

                // El resultado total es exitoso si CUALQUIERA de los procesos tuvo éxito
                exitoTerminados || exitoCorrectivos || exitoBitacoras
            }

            progressDialog.dismiss()

            if (resultado) {
                Toast.makeText(context, "Sincronización completada.", Toast.LENGTH_LONG).show()
                onResult(true)
            } else {
                Toast.makeText(context, "No había elementos nuevos por sincronizar.", Toast.LENGTH_LONG).show()
                onResult(false)
            }
        }
    }

    // ===================================================================
// FUNCIÓN AUXILIAR PARA SINCRONIZAR MANTENIMIENTOS CORRECTIVOS
// ===================================================================
    private suspend fun sincronizarPendientesCorrectivos(context: Context, dbHelper: DatabaseHelper): Boolean {
        var huboExito = false
        val pendientes = dbHelper.obtenerMantenimientosPendientes()

        for (mantenimiento in pendientes) {
            try {
                val jsonString = JSONObject().apply {
                    put("descripcion_falla", mantenimiento.descripcionFalla)
                    put("diagnostico", mantenimiento.diagnostico)
                    put("acciones", mantenimiento.acciones)
                    put("repuestos", mantenimiento.repuestos)
                    put("estado_final", mantenimiento.estadoFinal)
                    put("causa_raiz", mantenimiento.causaRaiz)
                    put("observaciones", mantenimiento.observaciones)
                    put("usuarios_checkeados", JSONArray(mantenimiento.usuarios))
                    put("id_tag_equipo", mantenimiento.idEquipo)
                }.toString()

                val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

                val imagenesParts = mantenimiento.fotos.mapNotNull { file ->
                    if (file.exists()) {
                        val compressedFile = comprimirYRedimensionarImagen(context, file)
                        MultipartBody.Part.createFormData(
                            "imagenes[]",
                            compressedFile.name,
                            compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        )
                    } else null
                }

                // Asumo que tienes una función para los correctivos en tu API
                val response = RetrofitClient.instance
                    .finalizarMantenimiento(requestBody, imagenesParts)
                    .execute()

                if (response.isSuccessful) {
                    dbHelper.marcarMantenimientoSincronizado(mantenimiento.id)
                    huboExito = true
                } else {
                    Log.e("SyncCorrectivo", "Error sincronizando mantenimiento id=${mantenimiento.id}, code=${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SyncCorrectivo", "Excepción sincronizando mantenimiento id=${mantenimiento.id}: ${e.message}", e)
            }
        }
        return huboExito
    }


    private suspend fun sincronizarPendientesBitacora(context: Context, dbHelper: DatabaseHelper): Boolean {
        var huboExito = false
        val bitacorasPendientes = dbHelper.obtenerBitacorasPendientes()

        for (bitacora in bitacorasPendientes) {
            try {
                val jsonString = JSONObject().apply {
                    put("id_actividad_programada", bitacora.idRelProgramarActividadesBitacora)
                    put("pr_inicial", bitacora.prInicial)
                    put("pr_final", bitacora.prFinal)
                    put("cantidad", bitacora.cantidad)
                    put("observaciones", bitacora.observacion)
                    put("usuarios_checkeados", JSONArray(bitacora.usuarios))
                }.toString()

                val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

                val imagenesParts = bitacora.fotos.mapNotNull { file ->
                    if (file.exists()) {
                        val compressedFile = comprimirYRedimensionarImagen(context, file)
                        MultipartBody.Part.createFormData(
                            "imagenes[]",
                            compressedFile.name,
                            compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        )
                    } else null
                }

                // Llamamos a la función específica para bitácoras en tu API
                val response = RetrofitClient.instance
                    .finalizarMantenimientoBitacora(requestBody, imagenesParts)
                    .execute()

                if (response.isSuccessful) {
                    dbHelper.marcarBitacoraSincronizada(bitacora.id)
                    huboExito = true
                } else {
                    Log.e("SyncBitacora", "Error sincronizando bitácora id=${bitacora.id}, code=${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SyncBitacora", "Excepción sincronizando bitácora id=${bitacora.id}: ${e.message}", e)
            }
        }
        return huboExito
    }

    fun sincronizarTodosMantenimientos2(context: Context, onResult: (Boolean) -> Unit) {
        val dbHelper = DatabaseHelper(context)

        val progressDialog = AlertDialog.Builder(context)
            .setView(LayoutInflater.from(context).inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()

        progressDialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            val resultado = withContext(Dispatchers.IO) {
                var exitoTotal = false

                // 🔹 1️⃣ Sincronizar mantenimientos terminados locales
                val resultadoTerminados = dbHelper.sincronizarManteninimientosTerminados()
                if (resultadoTerminados == 1) exitoTotal = true

                // 🔹 2️⃣ Sincronizar mantenimientos pendientes con API
                val pendientes = dbHelper.obtenerMantenimientosPendientes()
                for (mantenimiento in pendientes) {
                    try {
                        val jsonString = JSONObject().apply {
                            put("descripcion_falla", mantenimiento.descripcionFalla)
                            put("diagnostico", mantenimiento.diagnostico)
                            put("acciones", mantenimiento.acciones)
                            put("repuestos", mantenimiento.repuestos)
                            put("estado_final", mantenimiento.estadoFinal)
                            put("causa_raiz", mantenimiento.causaRaiz)
                            put("observaciones", mantenimiento.observaciones)
                            put("usuarios_checkeados", JSONArray(mantenimiento.usuarios))
                            put("id_tag_equipo", mantenimiento.idEquipo)
                        }.toString()

                        val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

                        val imagenesParts = mantenimiento.fotos.map { file ->
                            val compressedFile = comprimirYRedimensionarImagen(context, file)
                            MultipartBody.Part.createFormData(
                                "imagenes[]",
                                compressedFile.name,
                                compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            )
                        }

                        val response = RetrofitClient.instance
                            .finalizarMantenimiento(requestBody, imagenesParts)
                            .execute()

                        if (response.isSuccessful) {
                            dbHelper.marcarMantenimientoSincronizado(mantenimiento.id)
                            exitoTotal = true
                        } else {
                            Log.e("Sync", "Error sincronizando mantenimiento id=${mantenimiento.id}, code=${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("Sync", "Excepción sincronizando mantenimiento id=${mantenimiento.id}: ${e.message}", e)
                    }
                }

                exitoTotal
            }

            progressDialog.dismiss()

            if (resultado) {
                Toast.makeText(context, "Todos los mantenimientos sincronizados.", Toast.LENGTH_LONG).show()
                onResult(true)
            } else {
                Toast.makeText(context, "No había mantenimientos pendientes por sincronizar.", Toast.LENGTH_LONG).show()
                onResult(false)
            }
        }
    }


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

    fun sincronizarMantenimientosPendientes(context: Context, onResult: (Boolean) -> Unit) {
        val dbHelper = DatabaseHelper(context)

        val progressDialog = AlertDialog.Builder(context)
            .setView(LayoutInflater.from(context).inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()

        progressDialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            val resultado = withContext(Dispatchers.IO) {
                val pendientes = dbHelper.obtenerMantenimientosPendientes()
                var exito = false

                for (mantenimiento in pendientes) {
                    try {
                        // 🔹 Crear JSON con los datos
                        val jsonString = JSONObject().apply {
                            put("descripcion_falla", mantenimiento.descripcionFalla)
                            put("diagnostico", mantenimiento.diagnostico)
                            put("acciones", mantenimiento.acciones)
                            put("repuestos", mantenimiento.repuestos)
                            put("estado_final", mantenimiento.estadoFinal)
                            put("causa_raiz", mantenimiento.causaRaiz)
                            put("observaciones", mantenimiento.observaciones)
                            put("usuarios_checkeados", JSONArray(mantenimiento.usuarios))
                            put("id_tag_equipo", mantenimiento.idEquipo)
                        }.toString()

                        val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

                        // 🔹 Adjuntar imágenes (redimensionadas y comprimidas)
                        val imagenesParts = mantenimiento.fotos.map { file ->
                            val compressedFile = comprimirYRedimensionarImagen(context, file)
                            MultipartBody.Part.createFormData(
                                "imagenes[]",
                                compressedFile.name,
                                compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            )
                        }

                        val response = RetrofitClient.instance
                            .finalizarMantenimiento(requestBody, imagenesParts)
                            .execute()

                        if (response.isSuccessful) {
                            dbHelper.marcarMantenimientoSincronizado(mantenimiento.id)
                            exito = true
                        } else {
                            Log.e("Sync445667", "Error al sincronizar mantenimiento id=${mantenimiento.id}, code=${response.code()}")
                        }



                    } catch (e: Exception) {
                        Log.e("Sync", "Excepción sincronizando mantenimiento ${mantenimiento.id}: ${e.message}", e)
                    }
                }

                return@withContext if (exito) 1 else 0
            }

            progressDialog.dismiss()

            if (resultado == 1) {
                Toast.makeText(context, "Mantenimientos Correctivos sincronizados.", Toast.LENGTH_LONG).show()
                onResult(true)
            } else {
                Toast.makeText(context, "No había mantenimientos pendientes por sincronizar.", Toast.LENGTH_LONG).show()
                onResult(false)
            }
        }
    }


    /**
     * 🔹 Redimensiona la imagen para que no supere 1080x1920 y además la comprime al 75% de calidad.
     */
    fun comprimirYRedimensionarImagen(context: Context, file: File, maxAncho: Int = 1080, maxAlto: Int = 1920, quality: Int = 75): File {
        // Obtener dimensiones sin cargar la imagen completa
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opciones)

        var escala = 1
        while (opciones.outWidth / escala > maxAncho || opciones.outHeight / escala > maxAlto) {
            escala *= 2
        }

        // Decodificar con escala calculada
        val opcionesEscala = BitmapFactory.Options().apply { inSampleSize = escala }
        val bitmapReducido = BitmapFactory.decodeFile(file.absolutePath, opcionesEscala)

        // Crear archivo temporal comprimido
        val compressedFile = File(context.cacheDir, "compressed_${file.name}")
        val outputStream = FileOutputStream(compressedFile)
        bitmapReducido.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.flush()
        outputStream.close()

        return compressedFile
    }






}
