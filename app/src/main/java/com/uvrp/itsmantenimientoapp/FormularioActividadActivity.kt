package com.uvrp.itsmantenimientoapp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.core.content.FileProvider
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FormularioActividadActivity : AppCompatActivity() {

    private lateinit var containerActividades: LinearLayout
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var placa: String
    private var idUsuario: Int? = null
    private var currentPhotoPath: String? = null
    private val REQUEST_IMAGE_CAPTURE = 1001
    private var totalActividades = 0
    private var photoFile: File? = null


    private val REQUEST_CAMERA_PERMISSION = 2001


    private var idActividadActualFoto: Int? = null
    private var botonCamaraActual: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_actividad)

        prefs = getSharedPreferences("PreoperacionalPrefs", MODE_PRIVATE)
        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
        containerActividades = findViewById(R.id.containerActividades)
        placa = intent.getStringExtra("placaa") ?: ""

        idUsuario = sharedPreferences.getInt("idUser", -1)

        val inputKmInicial = findViewById<EditText>(R.id.inputKmInicial)
        inputKmInicial.setText(obtenerKilometraje())

        inputKmInicial.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarKilometraje(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val inputObservacionInicial = findViewById<EditText>(R.id.inputObservacionInicial)
        inputObservacionInicial.setText(obtenerObservacionInicial())

        inputObservacionInicial.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarObservacionInicial(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        val btnIniciarPreoperacional = findViewById<Button>(R.id.btnIniciarPreoperacional)
        btnIniciarPreoperacional.setOnClickListener {

            //mostrarEstadosSeleccionados()
            enviarPreoperacional()
            // El botón se habilitará en onResponse o onFailure
        }

        // Botón para llenar todo con datos de prueba
        val btnLlenarPrueba = findViewById<Button>(R.id.btnLlenarPrueba)
        btnLlenarPrueba.setOnClickListener {
            llenarTodoConPrueba()
        }

        val radioPortaDocumento = findViewById<RadioGroup>(R.id.radioPortaDocumento)
        val radioPortaLicencia = findViewById<RadioGroup>(R.id.radioPortaLicencia)
        val radioSaludConductor = findViewById<RadioGroup>(R.id.radioSaludConductor)

// Listeners para guardar al cambiar
        radioPortaDocumento.setOnCheckedChangeListener { _, checkedId ->
            val valor = findViewById<RadioButton>(checkedId)?.text.toString()
            guardarSeleccionRadio("porta_documento", valor)
        }

        radioPortaLicencia.setOnCheckedChangeListener { _, checkedId ->
            val valor = findViewById<RadioButton>(checkedId)?.text.toString()
            guardarSeleccionRadio("porta_licencia", valor)
        }

        radioSaludConductor.setOnCheckedChangeListener { _, checkedId ->
            val valor = findViewById<RadioButton>(checkedId)?.text.toString()
            guardarSeleccionRadio("salud_conductor", valor)
        }

        restaurarSeleccion(radioPortaDocumento, obtenerSeleccionRadio("porta_documento"))
        restaurarSeleccion(radioPortaLicencia, obtenerSeleccionRadio("porta_licencia"))
        restaurarSeleccion(radioSaludConductor, obtenerSeleccionRadio("salud_conductor"))


        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_home)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Preoperacional - $placa"

        obtenerActividades()
    }

    private fun obtenerActividades() {
        val idVehiculo = intent.getIntExtra("idVehiculo", -1)
        val call = RetrofitClient.instance.getActividadesFormato(idVehiculo)

        call.enqueue(object : Callback<List<ActividadFormato>> {
            override fun onResponse(
                call: Call<List<ActividadFormato>>,
                response: Response<List<ActividadFormato>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { mostrarActividades(it) }
                } else {
                    FirebaseCrashlytics.getInstance().log("Error al cargar actividades: ${response.code()} - ${response.message()}")
                    mostrarError("Error al cargar actividades")
                }
            }

            override fun onFailure(call: Call<List<ActividadFormato>>, t: Throwable) {
                FirebaseCrashlytics.getInstance().recordException(t)
                mostrarError("Fallo: ${t.message}")
            }
        })
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun mostrarActividades(actividades: List<ActividadFormato>) {
        totalActividades = actividades.size
        for (actividad in actividades) {
            val itemView =
                layoutInflater.inflate(R.layout.item_actividadformato, containerActividades, false)

            val label = itemView.findViewById<TextView>(R.id.labelActividad)
            val inputObs = itemView.findViewById<EditText>(R.id.inputObservaciones)
            val btnB = itemView.findViewById<Button>(R.id.btnB)
            val btnNA = itemView.findViewById<Button>(R.id.btnNA)
            val btnNT = itemView.findViewById<Button>(R.id.btnNT)
            val btnM = itemView.findViewById<Button>(R.id.btnM)
            val btnCamara = itemView.findViewById<Button>(R.id.btnCamara)

            label.text = actividad.descripcion
            val botones = listOf(btnB, btnNA, btnNT, btnM)
            val idActividad = actividad.id

            inputObs.setText(obtenerObservacion(idActividad))
            inputObs.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    guardarObservacion(idActividad, s.toString())
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            botones.forEach { it.setBackgroundColor(ContextCompat.getColor(this, R.color.gris)) }

            when (obtenerEstadoActividad(idActividad)) {
                "B" -> btnB
                "NA" -> btnNA
                "NT" -> btnNT
                "M" -> btnM
                else -> null
            }?.setBackgroundColor(ContextCompat.getColor(this, R.color.azul))

            btnB.setOnClickListener { actualizarEstado(btnB, botones, idActividad, "B") }
            btnNA.setOnClickListener { actualizarEstado(btnNA, botones, idActividad, "NA") }
            btnNT.setOnClickListener { actualizarEstado(btnNT, botones, idActividad, "NT") }
            btnM.setOnClickListener { actualizarEstado(btnM, botones, idActividad, "M") }

            if (fotoTomadaParaActividad(idActividad)) {
                btnCamara.setBackgroundColor(ContextCompat.getColor(this, R.color.verde))
            }

            btnCamara.setOnClickListener {
                idActividadActualFoto = idActividad
                botonCamaraActual = btnCamara
                verificarPermisosCamara()
            }

            // Guardar el ID de la actividad en el tag del view para poder recuperarlo después
            itemView.tag = idActividad

            containerActividades.addView(itemView)
        }
    }

    private fun actualizarEstado(
        botonSeleccionado: Button,
        botones: List<Button>,
        idActividad: Int,
        estado: String
    ) {
        botones.forEach { it.setBackgroundColor(ContextCompat.getColor(this, R.color.gris)) }
        botonSeleccionado.setBackgroundColor(ContextCompat.getColor(this, R.color.azul))
        guardarEstadoActividad(idActividad, estado)
    }

    private fun guardarEstadoActividad(idActividad: Int, estado: String) {
        getSharedPreferences("estado_actividades", MODE_PRIVATE)
            .edit().putString(idActividad.toString(), estado).apply()
    }

    private fun obtenerEstadoActividad(idActividad: Int): String? {
        return getSharedPreferences("estado_actividades", MODE_PRIVATE)
            .getString(idActividad.toString(), null)
    }

    private fun guardarObservacion(idActividad: Int, observacion: String) {
        getSharedPreferences("observaciones_actividades", MODE_PRIVATE)
            .edit().putString(idActividad.toString(), observacion).apply()
    }

    private fun obtenerObservacion(idActividad: Int): String {
        return getSharedPreferences("observaciones_actividades", MODE_PRIVATE)
            .getString(idActividad.toString(), "") ?: ""
    }


    private fun guardarKilometraje(km: String) {
        getSharedPreferences("kilometraje_pref", MODE_PRIVATE)
            .edit().putString("km_inicial_$placa", km).apply()
    }

    private fun obtenerKilometraje(): String {
        return getSharedPreferences("kilometraje_pref", MODE_PRIVATE)
            .getString("km_inicial_$placa", "") ?: ""
    }

    private fun guardarObservacionInicial(observacionInicial: String) {
        getSharedPreferences("observacionInicial_pref", MODE_PRIVATE)
            .edit().putString("observacionInicial_$placa", observacionInicial).apply()
    }

    private fun obtenerObservacionInicial(): String {
        return getSharedPreferences("observacionInicial_pref", MODE_PRIVATE)
            .getString("observacionInicial_$placa", "") ?: ""
    }

    private fun fotoTomadaParaActividad(idActividad: Int): Boolean {
        val path = prefs.getString("foto_${idActividad}", null)
        return path?.isNotEmpty() == true && File(path).exists()
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            photoFile = try {
                crearArchivoImagen()  // ← Aquí el cambio
            } catch (ex: IOException) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                mostrarError("Error creando archivo para imagen")
                null
            }

            photoFile?.let {
                val photoURI: Uri = FileProvider.getUriForFile(this, "$packageName.provider", it)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }


    @Throws(IOException::class)
    private fun crearArchivoImagen(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "FOTO_${placa}_${idActividadActualFoto}_$timeStamp"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
            prefs.edit().putString("foto_${idActividadActualFoto}", absolutePath).apply()
        }
    }

    @Deprecated("Usa ActivityResultLauncher en versiones nuevas")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("CAMARA", "🌀 onActivityResult -> requestCode=$requestCode, resultCode=$resultCode")
        Log.d("CAMARA", "📁 photoFile es nulo? ${photoFile == null}")

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            botonCamaraActual?.setBackgroundColor(ContextCompat.getColor(this, R.color.verde))
            Toast.makeText(this, "Foto guardada correctamente.", Toast.LENGTH_SHORT).show()
            photoFile?.let {
                comprimirImagen(it)
            }
        }
    }


    private fun verificarPermisosCamara() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            abrirCamara()  // ya tiene permiso
        }
    }

    private fun comprimirImagen(file: File) {
        try {
            // Cargar el bitmap original completo

            Log.d("Compresión", "📷 Archivo original: ${file.length() / 1024} KB")

            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (originalBitmap == null) {
                mostrarError("No se pudo cargar la imagen")
                return
            }
            Log.d(
                "Compresión",
                "📏 Resolución original: ${originalBitmap.width} x ${originalBitmap.height}"
            )
            // Redimensionar manualmente a resolución más pequeña (ajusta a tu gusto)
            val targetWidth = 1024
            val aspectRatio = originalBitmap.height.toDouble() / originalBitmap.width
            val targetHeight = (targetWidth * aspectRatio).toInt()

            val resizedBitmap =
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            Log.d(
                "Compresión",
                "📏 Resolución nueva: ${resizedBitmap.width} x ${resizedBitmap.height}"
            )
            // Comprimir y sobrescribir el archivo original
            val outputStream = FileOutputStream(file)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.flush()
            outputStream.close()

            Log.d("Compresión", "✅ Imagen comprimida a ${file.length() / 1024} KB")

        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            e.printStackTrace()
            mostrarError("Error al comprimir imagen")
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara()
            } else {
                FirebaseCrashlytics.getInstance().log("Permiso de cámara denegado por el usuario.")
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarSeleccionRadio(key: String, valor: String) {
        getSharedPreferences("preoperacional_prefs", MODE_PRIVATE)
            .edit().putString("${placa}_$key", valor).apply()
    }

    private fun obtenerSeleccionRadio(key: String): String {
        return getSharedPreferences("preoperacional_prefs", MODE_PRIVATE)
            .getString("${placa}_$key", "No seleccionado") ?: "No seleccionado"
    }

    private fun restaurarSeleccion(radioGroup: RadioGroup, textoGuardado: String) {
        for (i in 0 until radioGroup.childCount) {
            val radio = radioGroup.getChildAt(i) as? RadioButton
            if (radio != null && radio.text.toString().equals(textoGuardado, ignoreCase = true)) {
                radio.isChecked = true
                break
            }
        }
    }

    /**
     * Función para llenar automáticamente todos los campos con datos de prueba
     */
    private fun llenarTodoConPrueba() {
        try {
            // 1. Llenar Kilometraje Inicial
            val inputKmInicial = findViewById<EditText>(R.id.inputKmInicial)
            inputKmInicial.setText("10000")
            guardarKilometraje("10000")

            // 2. Llenar Observación Inicial
            val inputObservacionInicial = findViewById<EditText>(R.id.inputObservacionInicial)
            inputObservacionInicial.setText("prueba")
            guardarObservacionInicial("prueba")

            // 3. Seleccionar "Sí" en los 3 RadioGroups
            val radioPortaDocumento = findViewById<RadioGroup>(R.id.radioPortaDocumento)
            val radioPortaLicencia = findViewById<RadioGroup>(R.id.radioPortaLicencia)
            val radioSaludConductor = findViewById<RadioGroup>(R.id.radioSaludConductor)

            // Seleccionar "Sí" en Porta Documento
            val rbPortaDocumentoSi = findViewById<RadioButton>(R.id.rbPortaDocumentoSi)
            rbPortaDocumentoSi?.isChecked = true
            guardarSeleccionRadio("porta_documento", "Sí")

            // Seleccionar "Sí" en Porta Licencia
            val rbPortaLicenciaSi = findViewById<RadioButton>(R.id.rbPortaLicenciaSi)
            rbPortaLicenciaSi?.isChecked = true
            guardarSeleccionRadio("porta_licencia", "Sí")

            // Seleccionar "Sí" en Salud Conductor
            val rbSaludSi = findViewById<RadioButton>(R.id.rbSaludSi)
            rbSaludSi?.isChecked = true
            guardarSeleccionRadio("salud_conductor", "Sí")

            // 4. Llenar todas las actividades dinámicas
            val actividadesCount = containerActividades.childCount
            for (i in 0 until actividadesCount) {
                val itemView = containerActividades.getChildAt(i)
                
                // Obtener el ID de la actividad desde el tag del view
                val idActividad = itemView.tag as? Int
                
                if (idActividad != null) {
                    // Buscar los elementos dentro del item
                    val btnB = itemView.findViewById<Button>(R.id.btnB)
                    val btnNA = itemView.findViewById<Button>(R.id.btnNA)
                    val btnNT = itemView.findViewById<Button>(R.id.btnNT)
                    val btnM = itemView.findViewById<Button>(R.id.btnM)
                    val inputObs = itemView.findViewById<EditText>(R.id.inputObservaciones)
                    
                    // Llenar observación con "prueba"
                    inputObs?.setText("prueba")
                    guardarObservacion(idActividad, "prueba")
                    
                    // Seleccionar estado "B" (Bueno) para esta actividad
                    val botones = listOf(btnB, btnNA, btnNT, btnM).filterNotNull()
                    if (btnB != null && botones.isNotEmpty()) {
                        actualizarEstado(btnB, botones, idActividad, "B")
                    }
                }
            }

            Toast.makeText(this, "✅ Todos los campos llenados con datos de prueba", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("LlenarPrueba", "Error al llenar campos: ${e.message}", e)
            Toast.makeText(this, "Error al llenar campos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun mostrarEstadosSeleccionados() {
        val prefsEstado = getSharedPreferences("estado_actividades", MODE_PRIVATE)
        val prefsObs = getSharedPreferences("observaciones_actividades", MODE_PRIVATE)
        val allEstados = prefsEstado.all
        val allObservaciones = prefsObs.all

        val builder = StringBuilder()
        val kmInicial = obtenerKilometraje()
        builder.append("Resumen del preoperacional:\n\n")
        builder.append("🚗 Kilometraje inicial: $kmInicial\n\n")

        // 🔽 Nuevo: Agrega los valores de los RadioGroups
        val portaDocumento = obtenerSeleccionRadio("porta_documento")
        val portaLicencia = obtenerSeleccionRadio("porta_licencia")
        val saludConductor = obtenerSeleccionRadio("salud_conductor")

        builder.append("🪪 Porta Documento de Identidad: $portaDocumento\n")
        builder.append("🚘 Porta Licencia de Conducción: $portaLicencia\n")
        builder.append("🩺 En condiciones de salud para conducir: $saludConductor\n\n")

        if (allEstados.isEmpty()) {
            Toast.makeText(this, "No se han seleccionado estados aún.", Toast.LENGTH_SHORT).show()
            return
        }

        for ((id, estado) in allEstados.toSortedMap(compareBy { it.toIntOrNull() })) {
            val obs = allObservaciones[id] ?: "Sin observación"
            val rutaFoto = prefs.getString("foto_${id}", null)

            builder.append("🛠 Actividad ID $id\n")
            builder.append("   Estado: $estado\n")
            builder.append("   Observación: $obs\n")

            if (!rutaFoto.isNullOrEmpty() && File(rutaFoto).exists()) {
                builder.append("   📷 Foto: $rutaFoto\n")
            } else {
                builder.append("   📷 Foto: No tomada\n")
            }

            builder.append("\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Actividades seleccionadas")
            .setMessage(builder.toString())
            .setPositiveButton("Aceptar", null)
            .show()
    }


    private fun enviarPreoperacional() {
        val service = RetrofitClient.instance

        val idVehi = intent.getIntExtra("idVehiculo", -1)
        Log.d("jeuuh", "$idVehi")


        // 📥 Datos individuales
        val kmInicial = obtenerKilometraje()
        val ObservacionInicial = obtenerObservacionInicial()
        val portaDocumento = obtenerSeleccionRadio("porta_documento")
        val portaLicencia = obtenerSeleccionRadio("porta_licencia")
        val saludConductor = obtenerSeleccionRadio("salud_conductor")

        Log.d("hdhdhd", kmInicial)

        if (kmInicial.isEmpty()) {
            Toast.makeText(
                this@FormularioActividadActivity,
                "Debe escribir el Kilometraje",
                Toast.LENGTH_SHORT
            ).show()
            return
        }


        if (portaDocumento == "No") {
            Toast.makeText(
                this@FormularioActividadActivity,
                "❌ Sin Documento: No Apto para conducir",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (portaLicencia == "No") {
            Toast.makeText(
                this@FormularioActividadActivity,
                "❌ Sin Licencia: No Apto para conducir",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (saludConductor == "No") {
            Toast.makeText(
                this@FormularioActividadActivity,
                "❌ No Apto para conducir por estado de salud.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        Log.d("pppppkknn", portaDocumento)

        if (portaDocumento == "No seleccionado") {
            Toast.makeText(
                this@FormularioActividadActivity,
                "❌ Sin Documento: No Apto para conducir",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (portaLicencia == "No seleccionado") {
            Toast.makeText(
                this@FormularioActividadActivity,
                "❌ Sin Licencia: No Apto para conducir",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (saludConductor == "No seleccionado") {
            Toast.makeText(
                this@FormularioActividadActivity,
                "❌ No Apto para conducir por estado de salud.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        // Obtener la placa y el idUsuario


        // ✅ SharedPreferences divididos como en mostrarEstadosSeleccionados
        val prefsEstado = getSharedPreferences("estado_actividades", MODE_PRIVATE)
        val prefsObs = getSharedPreferences("observaciones_actividades", MODE_PRIVATE)
        val prefsFotos = getSharedPreferences(
            "PreoperacionalPrefs",
            MODE_PRIVATE
        ) // Usar el mismo SharedPreferences que para guardar las fotos
        val estadosGuardados = prefsEstado.all


        // 🛠 Armar JSON de actividades
        val jsonActividades = JSONArray()
        for ((key, estado) in prefsEstado.all) {

            val id = key // El ID ya no tiene prefijo "estado_"
            val obs = prefsObs.getString("$id", "")
                ?: "" // Corregido: La clave para la observación es solo el ID

            if (estado == null || estado.toString().isBlank()) {
                Toast.makeText(this, "⚠️ Faltan actividades por seleccionar.", Toast.LENGTH_LONG)
                    .show()
                return
            }

            val obj = JSONObject().apply {
                put("id", id)
                put("estado", estado.toString())
                put("observacion", obs)
            }
            jsonActividades.put(obj)
        }
        if (estadosGuardados.size < totalActividades) {
            Toast.makeText(this, "⚠️ Favor selecionar todas las actividades", Toast.LENGTH_LONG)
                .show()
            return
        }
        // 📦 JSON principal
        val jsonCompleto = JSONObject().apply {
            put("idVehiculo", idVehi) //
            put("id_usuario", idUsuario) //
            put("km_inicial", kmInicial)
            put("porta_documento", portaDocumento)
            put("porta_licencia", portaLicencia)
            put("salud_conductor", saludConductor)
            put("observacion_inicial", ObservacionInicial)
            put("actividades", jsonActividades)
        }

        val jsonRequestBody = jsonCompleto.toString()
            .toRequestBody("text/plain".toMediaTypeOrNull())

        // 📷 Adjuntar imágenes desde el mismo SharedPreferences de estados
        val images = mutableListOf<MultipartBody.Part>()
        for ((key, _) in prefsEstado.all) {
            val id = key // El ID ya no tiene prefijo "estado_"
            val rutaFoto = prefsFotos.getString("foto_$id", null) // Leer desde prefsFotos

            Log.d("PREOPERACIONAL", "📂 Verificando foto para ID: $id")
            Log.d("PREOPERACIONAL", "📂 Ruta obtenida: $rutaFoto")

            if (!rutaFoto.isNullOrEmpty()) {
                val file = File(rutaFoto)
                Log.d("PREOPERACIONAL", "📂 Existe archivo: ${file.exists()}")
                if (file.exists()) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val body =
                        MultipartBody.Part.createFormData("imagenes[]", file.name, requestFile)
                    images.add(body)
                }
            }
        }

        Log.d("PREOPERACIONAL", "📷 Imágenes a enviar: ${images.size}")
        images.forEach {
            Log.d(
                "PREOPERACIONAL",
                "📷 Imagen: ${it.body?.contentType()} - ${it.headers?.get("Content-Disposition")}"
            )
        }

        // 🚀 Enviar solicitud
        service.iniciarPreoperacional(jsonRequestBody, images).enqueue(object : Callback<Void> {

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@FormularioActividadActivity,
                        "✅ Preoperacional enviado",
                        Toast.LENGTH_SHORT
                    ).show()
                    // 🧹 Limpiar solo los SharedPreferences usados en este proceso
                    getSharedPreferences("estado_actividades", MODE_PRIVATE).edit().clear().apply()
                    getSharedPreferences("observaciones_actividades", MODE_PRIVATE).edit().clear().apply()
                    getSharedPreferences("PreoperacionalPrefs", MODE_PRIVATE).edit().clear().apply()
                    getSharedPreferences("kilometraje_pref", MODE_PRIVATE).edit().clear().apply()


                    // 👉 Redirigir a otro activity
                    val intent = Intent(this@FormularioActividadActivity, iniciarPreoperacional::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // ⚠️ Intentar leer mensaje desde el cuerpo de error
                    val errorMsg = try {
                        response.errorBody()?.string()?.let {
                            val json = JSONObject(it)
                            json.optString("error", "Error desconocido")
                        } ?: "Error desconocido"
                    } catch (e: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        Log.e("PREOPERACIONAL", "❌ Error al parsear mensaje: ${e.localizedMessage}")
                        "Error inesperado"
                    }

                    Log.e("PREOPERACIONAL", "❌ Error al enviar: ${response.code()} - $errorMsg")
                    FirebaseCrashlytics.getInstance().log("Error al enviar preoperacional: ${response.code()} - $errorMsg")
                    Toast.makeText(
                        this@FormularioActividadActivity,
                        "❌ $errorMsg",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                FirebaseCrashlytics.getInstance().recordException(t)
                Log.e("PREOPERACIONAL", "❌ Fallo: ${t.localizedMessage}", t)
                Toast.makeText(
                    this@FormularioActividadActivity,
                    "❌ Fallo: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()

            }
        })
    }


}
