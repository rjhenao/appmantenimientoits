package com.uvrp.itsmantenimientoapp

import ApiService.SincronizacionInspeccion
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
import okhttp3.RequestBody
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
                val exitoInspecciones = sincronizarInspeccionesCompletas(dbHelper)
                val exitoFotosMasivas = sincronizarFotosMasivas(context, dbHelper)
                val exitoCombustible = sincronizarCombustiblesPendientes(context, dbHelper).exito

                // El resultado total es exitoso si CUALQUIERA de los procesos tuvo éxito
                exitoTerminados || exitoCorrectivos || exitoBitacoras || exitoInspecciones || exitoFotosMasivas || exitoCombustible
            }

            progressDialog.dismiss()

            if (resultado) {
                // Si la sincronización fue exitosa, sincronizar también los tickets
                withContext(Dispatchers.IO) {
                    try {
                        val api = RetrofitClient.instance
                        val response = api.getTickets().execute()
                        
                        if (response.isSuccessful) {
                            val ticketResponse = response.body()
                            if (ticketResponse != null && ticketResponse.success) {
                                dbHelper.insertarOActualizarTickets(ticketResponse.data)
                                Log.i("SyncTickets", "Tickets sincronizados exitosamente después de mantenimientos")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SyncTickets", "Error sincronizando tickets después de mantenimientos: ${e.message}", e)
                    }
                    Unit // Retorno explícito para evitar que el if sea interpretado como expresión
                }
                
                Toast.makeText(context, "Sincronización completada.", Toast.LENGTH_LONG).show()
                onResult(true)
            } else {
                Toast.makeText(context, "No había elementos nuevos por sincronizar.", Toast.LENGTH_LONG).show()
                onResult(false)
            }
        }
    }

    /**
     * Sincroniza solo los combustibles pendientes (usado por el botón "Sincronizar Combustibles" en Home).
     */
    fun sincronizarSoloCombustibles(context: Context, onResult: (Boolean) -> Unit) {
        val dbHelper = DatabaseHelper(context)
        val progressDialog = AlertDialog.Builder(context)
            .setView(LayoutInflater.from(context).inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()
        progressDialog.show()
        CoroutineScope(Dispatchers.Main).launch {
            val hadPendientes = withContext(Dispatchers.IO) {
                dbHelper.obtenerCombustiblesPendientes().isNotEmpty()
            }
            val resultado = withContext(Dispatchers.IO) {
                sincronizarCombustiblesPendientes(context, dbHelper)
            }
            progressDialog.dismiss()
            val mensaje = when {
                resultado.exito -> "Combustibles sincronizados correctamente."
                hadPendientes -> "No se pudo sincronizar. Revisa tu conexión e intenta de nuevo."
                else -> "No había combustibles pendientes."
            }
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            onResult(resultado.exito)
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
                // Obtener relaciones con tickets para este mantenimiento
                val relacionesTickets = dbHelper.obtenerRelacionesNoSincronizadas()
                    .filter { it.second == mantenimiento.id }
                    .map { mapOf("idTicket" to it.first) }

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
                    
                    // Agregar relaciones con tickets si existen
                    if (relacionesTickets.isNotEmpty()) {
                        put("relaciones_tickets", JSONArray(relacionesTickets))
                    }
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
                    
                    // Marcar relaciones como sincronizadas
                    relacionesTickets.forEach { relacion ->
                        val idTicket = relacion["idTicket"] as Int
                        dbHelper.marcarRelacionComoSincronizada(idTicket, mantenimiento.id)
                    }
                    
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

    private suspend fun sincronizarInspeccionesCompletas(dbHelper: DatabaseHelper): Boolean {
        // 1. Obtenemos ambos listados de pendientes
        val usuariosPendientes = dbHelper.obtenerInspeccionUsuariosPendientes()
        val actividadesPendientes = dbHelper.obtenerRelActividadesPendientes()
        Log.e("adasd3dfe3df", "dddd32d11111d: $usuariosPendientes ")
        Log.e("dd32d32d32", "d32d3 $actividadesPendientes")

        // 2. Si ambos están vacíos, no hay nada que hacer
        if (usuariosPendientes.isEmpty() && actividadesPendientes.isEmpty()) {
            return false
        }

        // 3. Creamos el paquete de datos para enviar a la API
        val paqueteDeSincronizacion = SincronizacionInspeccion(
            usuarios = usuariosPendientes,
            actividades = actividadesPendientes
        )

        // 4. Realizamos la llamada a la API
        return try {
            val response = RetrofitClient.instance
                .sincronizarInspeccionCompleta(paqueteDeSincronizacion)
                .execute()

            if (response.isSuccessful) {
                // 5. ¡CORREGIDO! Llamamos a la nueva función unificada
                dbHelper.marcarInspeccionesComoSincronizadas(usuariosPendientes, actividadesPendientes)
                true // La sincronización tuvo éxito
            } else {
                Log.e("SyncCompleto", "Error al sincronizar inspección. Código: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("SyncCompleto", "Excepción al sincronizar inspección: ${e.message}", e)
            false
        }
    }

    private suspend fun sincronizarPendientesBitacora(context: Context, dbHelper: DatabaseHelper): Boolean {
        var huboExito = false
        val bitacorasPendientes = dbHelper.obtenerBitacorasPendientes()

        if (bitacorasPendientes.isEmpty()) {
            return true
        }

        Log.i("SyncBitacora", "Iniciando sincronización de ${bitacorasPendientes.size} bitácoras...")

        for (bitacora in bitacorasPendientes) {
            try {
                // ... (Creación del jsonObject)
                val jsonObject = JSONObject().apply {
                    put("id_actividad_programada", bitacora.idRelProgramarActividadesBitacora)
                    put("pr_inicial", bitacora.prInicial)
                    put("pr_final", bitacora.prFinal)
                    put("cantidad", bitacora.cantidad)
                    put("observaciones", bitacora.observacion)
                    put("usuarios_checkeados", JSONArray(bitacora.usuarios))
                    put("estado", bitacora.estado)
                    
                    // Campos adicionales para actividades no programadas
                    if (bitacora.estado == 2) {
                        put("id_bitacora", bitacora.idBitacora)
                        put("id_actividad", bitacora.idActividad)
                        put("id_cuadrilla", bitacora.idCuadrilla)
                        put("uf", bitacora.uf)
                        put("sentido", bitacora.sentido)
                        put("lado", bitacora.lado)
                        put("supervisor_responsable", bitacora.supervisorResponsable)
                    }
                }
                val jsonRequestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

                // >>>>> LOG DE CONTROL 1: VERIFICAR RUTAS DE FOTOS DESDE LA BD <<<<<
                Log.d("SyncDebug", "Bitácora ID ${bitacora.id}: Fotos recuperadas de la BD: ${bitacora.fotos.map { it.path }}")


                val imagenesParts = bitacora.fotos.mapNotNull { file ->
                    // >>>>> LOG DE CONTROL 2: PROCESANDO CADA ARCHIVO <<<<<
                    Log.d("SyncDebug", "Procesando archivo: ${file.path}")

                    if (file.exists() && file.canRead()) {
                        // >>>>> LOG DE CONTROL 3: CONFIRMAR QUE EL ARCHIVO EXISTE <<<<<
                        Log.d("SyncDebug", "--> Archivo EXISTE y se puede leer. Creando MultipartBody.Part.")
                        val compressedFile = comprimirYRedimensionarImagen(context, file)
                        MultipartBody.Part.createFormData("imagenes[]", compressedFile.name, compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                    } else {
                        // >>>>> LOG DE CONTROL 3.1: ADVERTIR SI NO EXISTE O NO SE PUEDE LEER <<<<<
                        Log.w("SyncDebug", "--> ¡ATENCIÓN! El archivo NO EXISTE o no se puede leer en esa ruta.")
                        null
                    }
                }

                // >>>>> LOG DE CONTROL 4: VERIFICAR CUÁNTAS IMÁGENES SE VAN A ENVIAR <<<<<
                Log.d("SyncDebug", "Total de partes de imagen CREADAS para enviar: ${imagenesParts.size}")

                // Llama a la API con el JSON y las imágenes
                val response = RetrofitClient.instance
                    .finalizarMantenimientoBitacora(jsonRequestBody, imagenesParts)
                    .execute()

                // ... (resto del código para procesar la respuesta)
                if (response.isSuccessful) {
                    Log.i("SyncBitacora", "✅ Bitácora ID=${bitacora.id} sincronizada.")
                    dbHelper.marcarBitacoraSincronizada(bitacora.id, bitacora.estado == 2)
                    huboExito = true
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("SyncBitacora", "❌ Error del servidor al sincronizar bitácora ID=${bitacora.id}. Código: ${response.code()}. Body: $errorBody")
                }

            } catch (e: Exception) {
                Log.e("SyncBitacora", "🚨 Excepción al procesar bitácora ID=${bitacora.id}: ${e.message}", e)
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





    /**
     * Sincronizar fotos masivas de mantenimiento preventivo
     */
    private suspend fun sincronizarFotosMasivas(context: Context, dbHelper: DatabaseHelper): Boolean {
        return try {
            Log.d("FUNCIONES_GENERALES", "🔄 Iniciando sincronización de fotos masivas...")
            
            val resultado = dbHelper.sincronizarFotosMasivas()
            val exito = resultado == 1
            
            if (exito) {
                Log.d("FUNCIONES_GENERALES", "✅ Sincronización de fotos masivas exitosa")
            } else {
                Log.w("FUNCIONES_GENERALES", "⚠️ Sincronización de fotos masivas falló o no había datos")
            }
            
            exito
            
        } catch (e: Exception) {
            Log.e("FUNCIONES_GENERALES", "❌ Error en sincronización de fotos masivas: ${e.message}")
            false
        }
    }

    /**
     * Resultado de la sincronización de combustibles: éxito y mensaje de error si falló.
     */
    private data class ResultadoSyncCombustible(val exito: Boolean, val errorServidor: String?)

    /**
     * Sincronizar combustibles pendientes
     */
    private suspend fun sincronizarCombustiblesPendientes(context: Context, dbHelper: DatabaseHelper): ResultadoSyncCombustible {
        val combustiblesPendientes = dbHelper.obtenerCombustiblesPendientes()

        if (combustiblesPendientes.isEmpty()) {
            return ResultadoSyncCombustible(exito = false, errorServidor = null)
        }

        var huboExito = false
        var ultimoError: String? = null

        Log.i("SyncCombustible", "🛢️ Iniciando sincronización de ${combustiblesPendientes.size} combustible(s)...")

        for (combustible in combustiblesPendientes) {
            try {
                val jsonObject = org.json.JSONObject().apply {
                    put("id_preoperacional", combustible.idPreoperacional)
                    put("id_vehiculo", combustible.idVehiculo)
                    put("id_usuario", combustible.idUsuario)
                    put("kilometraje_inicial", combustible.kilometrajeInicial)
                    put("cantidad_galones", combustible.cantidadGalones)
                    put("valor_galon", combustible.valorGalon)
                    put("valor_total", combustible.valorTotal)
                    put("observacion", combustible.observacion ?: "")
                    put("fecha_tanqueo", combustible.fechaTanqueo)
                }

                val jsonRequestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val fotoPart: okhttp3.MultipartBody.Part? = combustible.rutaFotoTicket?.let { ruta ->
                    val file = File(ruta)
                    if (file.exists() && file.canRead()) {
                        val compressedFile = comprimirYRedimensionarImagen(context, file)
                        MultipartBody.Part.createFormData(
                            "foto_ticket",
                            compressedFile.name,
                            compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        )
                    } else {
                        Log.w("SyncCombustible", "⚠️ Foto del ticket no existe o no se puede leer: $ruta")
                        null
                    }
                }

                val response = RetrofitClient.instance
                    .sincronizarCombustible(jsonRequestBody, fotoPart)
                    .execute()

                Log.d("SyncCombustible", "📡 Respuesta recibida para combustible ID=${combustible.id}, code=${response.code()}, isSuccessful=${response.isSuccessful}")

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("SyncCombustible", "📦 ResponseBody: $responseBody")

                    if (responseBody?.success == true) {
                        dbHelper.marcarCombustibleSincronizado(combustible.id)
                        huboExito = true
                        Log.i("SyncCombustible", "✅ Combustible ID ${combustible.id} sincronizado exitosamente")
                    } else {
                        val errorMessage = responseBody?.message ?: "Sin mensaje de error"
                        ultimoError = "Servidor: $errorMessage"
                        Log.e("SyncCombustible", "❌ Error en respuesta del servidor para combustible ID ${combustible.id}: $errorMessage")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    ultimoError = "HTTP ${response.code()}: ${errorBody.take(80)}"
                    Log.e("SyncCombustible", "❌ Error sincronizando combustible ID=${combustible.id}, code=${response.code()}, body=$errorBody")
                }
            } catch (e: Exception) {
                ultimoError = "Error: ${e.message ?: "Sin conexión"}"
                Log.e("SyncCombustible", "❌ Excepción sincronizando combustible ID=${combustible.id}: ${e.message}", e)
            }
        }

        return ResultadoSyncCombustible(exito = huboExito, errorServidor = ultimoError)
    }

}
