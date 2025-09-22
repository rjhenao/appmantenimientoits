package com.uvrp.itsmantenimientoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
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

class MantenimientoCorrectivoActivity : AppCompatActivity() {

    private lateinit var recyclerFotos: RecyclerView
    private lateinit var btnAgregarFoto: Button
    private lateinit var btnGuardar: Button
    private lateinit var fotoAdapter: FotoAdapter

    private lateinit var etDescripcionFalla: EditText
    private lateinit var etDiagnostico: EditText
    private lateinit var etAcciones: EditText
    private lateinit var etRepuestos: EditText
    private lateinit var spEstadoFinal: MaterialAutoCompleteTextView
    private lateinit var etCausaRaiz: EditText
    private lateinit var etObservaciones: EditText

    private val fotosList = mutableListOf<File>()
    private var currentPhotoFile: File? = null
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_CAMERA_PERMISSION = 123

    private lateinit var dbHelper: DatabaseHelper
    private var empleados = listOf<Empleado>()
    private val checkBoxEmpleadoMap = mutableMapOf<CheckBox, Empleado>()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mantenimiento_correctivo)

        // ==== INICIALIZAR COMPONENTES DEL HEADER ====
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // Configurar toolbar + menú hamburguesa + NavigationView
        HeaderHelper.setupHeader(
            activity = this,
            drawerLayout = drawerLayout,
            navView = navView
        )

        // ==== VISTAS DEL FORMULARIO ====
        recyclerFotos = findViewById(R.id.recyclerFotos)
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto)
        btnGuardar = findViewById(R.id.btnGuardar)

        etDescripcionFalla = findViewById(R.id.etDescripcionFalla)
        etDiagnostico = findViewById(R.id.etDiagnostico)
        etAcciones = findViewById(R.id.etAcciones)
        etRepuestos = findViewById(R.id.etRepuestos)
        etCausaRaiz = findViewById(R.id.etCausaRaiz)
        etObservaciones = findViewById(R.id.etObservaciones)
        spEstadoFinal = findViewById(R.id.spEstadoFinal)

        // Inicializar adaptador para spEstadoFinal desde recursos
        val estados = resources.getStringArray(R.array.estado_final)
        val adapterEstado = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estados)
        spEstadoFinal.setAdapter(adapterEstado)
        spEstadoFinal.setOnItemClickListener { _, _, _, _ -> guardarDatosEnPrefs() }

        cargarDatosDePrefs()
        cargarFotosDePrefs()

        // Guardar cuando cambien los textos
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDatosEnPrefs()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etDescripcionFalla.addTextChangedListener(textWatcher)
        etDiagnostico.addTextChangedListener(textWatcher)
        etAcciones.addTextChangedListener(textWatcher)
        etRepuestos.addTextChangedListener(textWatcher)
        etCausaRaiz.addTextChangedListener(textWatcher)
        etObservaciones.addTextChangedListener(textWatcher)

        // DB y empleados
        dbHelper = DatabaseHelper(this)
        empleados = dbHelper.getEmpleados()
        setupCheckboxes()

        // Botones
        btnAgregarFoto.setOnClickListener { verificarPermisos() }
        btnGuardar.setOnClickListener { GuardarMantenimiento() }

        setupRecyclerView()

        // ==== CARGAR DATOS DE INTENT EN TEXTVIEWS ====
        val locacion = intent.getStringExtra("locacionDescripcion") ?: "--"
        val sistema = intent.getStringExtra("sistemaDescripcion") ?: "--"
        val subsistema = intent.getStringExtra("subsistemaDescripcion") ?: "--"
        val tipoEquipo = intent.getStringExtra("tipoEquipoDescripcion") ?: "--"
        val tagEquipo = intent.getStringExtra("tagEquipoDescripcion") ?: "--"

        findViewById<TextView>(R.id.tvLocacion).text = "Locación: $locacion"
        findViewById<TextView>(R.id.tvSistema).text = "Sistema: $sistema"
        findViewById<TextView>(R.id.tvSubsistema).text = "Subsistema: $subsistema"
        findViewById<TextView>(R.id.tvTipoEquipo).text = "Tipo de equipo: $tipoEquipo"
        findViewById<TextView>(R.id.tvTagEquipo).text = "Tag del equipo: $tagEquipo"
    }


    private fun setupRecyclerView() {
        fotoAdapter = FotoAdapter(fotosList) { file ->
            fotosList.remove(file)
            guardarFotosEnPrefs()
            fotoAdapter.notifyDataSetChanged()
        }
        recyclerFotos.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerFotos.adapter = fotoAdapter
    }

    private fun verificarPermisos() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else abrirCamara()
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            try {
                val photoFile = crearArchivoImagen()
                currentPhotoFile = photoFile
                val photoURI = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            } catch (e: IOException) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "Error creando archivo de imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Throws(IOException::class)
    private fun crearArchivoImagen(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_${timeStamp}"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDir!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            currentPhotoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    fotosList.add(file)
                    guardarFotosEnPrefs()
                    fotoAdapter.notifyDataSetChanged()
                } else Toast.makeText(this, "Error al guardar la foto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun comprimirImagen(originalFile: File): File {
        val bitmapOriginal = BitmapFactory.decodeFile(originalFile.absolutePath)
        val maxLado = 1024
        val scale = if (bitmapOriginal.width >= bitmapOriginal.height) {
            maxLado.toFloat() / bitmapOriginal.width
        } else {
            maxLado.toFloat() / bitmapOriginal.height
        }
        val nuevoAncho = (bitmapOriginal.width * scale).toInt()
        val nuevoAlto = (bitmapOriginal.height * scale).toInt()
        val bitmapEscalado = Bitmap.createScaledBitmap(bitmapOriginal, nuevoAncho, nuevoAlto, true)
        val compressedFile = File(originalFile.parent, "COMP_${originalFile.name}")
        FileOutputStream(compressedFile).use { outputStream ->
            bitmapEscalado.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.flush()
        }
        return compressedFile
    }

    private fun guardarFotosEnPrefs() {
        val prefs = getSharedPreferences("FotosMantenimiento", MODE_PRIVATE)
        prefs.edit().putStringSet("fotos_guardadas", fotosList.map { it.absolutePath }.toSet())
            .apply()
    }

    private fun cargarFotosDePrefs() {
        val prefs = getSharedPreferences("FotosMantenimiento", MODE_PRIVATE)
        val paths = prefs.getStringSet("fotos_guardadas", emptySet())
        fotosList.clear()
        paths?.forEach { if (File(it).exists()) fotosList.add(File(it)) }
    }

    private fun limpiarFotosEnPrefs() {
        getSharedPreferences("FotosMantenimiento", MODE_PRIVATE).edit().remove("fotos_guardadas")
            .apply()
    }

    private fun setupCheckboxes() {
        val checkboxContainer = findViewById<GridLayout>(R.id.checkboxContainer)
        checkboxContainer.removeAllViews()
        empleados.forEach { empleado ->
            val checkBox = CheckBox(this).apply {
                text = empleado.nombre
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                textSize = 12f
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
            }
            checkboxContainer.addView(checkBox)
            checkBoxEmpleadoMap[checkBox] = empleado
            checkBox.setOnCheckedChangeListener { _, _ -> guardarDatosEnPrefs() }
        }
    }

    private fun guardarDatosEnPrefs() {
        val idEquipo = intent.getStringExtra("tagId") ?: "0"
        val prefs = getSharedPreferences("DatosMantenimiento", MODE_PRIVATE)
        with(prefs.edit()) {
            putString("descripcion_falla_$idEquipo", etDescripcionFalla.text.toString())
            putString("diagnostico_$idEquipo", etDiagnostico.text.toString())
            putString("acciones_$idEquipo", etAcciones.text.toString())
            putString("repuestos_$idEquipo", etRepuestos.text.toString())
            putString("estado_final_$idEquipo", spEstadoFinal.text.toString())
            putString("causa_raiz_$idEquipo", etCausaRaiz.text.toString())
            putString("observaciones_$idEquipo", etObservaciones.text.toString())
            putStringSet(
                "empleados_seleccionados_$idEquipo",
                checkBoxEmpleadoMap.filter { it.key.isChecked }.map { it.value.id.toString() }
                    .toSet()
            )
            apply()
        }
    }

    private fun cargarDatosDePrefs() {
        val idEquipo = intent.getStringExtra("tagId") ?: "0"
        val prefs = getSharedPreferences("DatosMantenimiento", MODE_PRIVATE)
        etDescripcionFalla.setText(prefs.getString("descripcion_falla_$idEquipo", ""))
        etDiagnostico.setText(prefs.getString("diagnostico_$idEquipo", ""))
        etAcciones.setText(prefs.getString("acciones_$idEquipo", ""))
        etRepuestos.setText(prefs.getString("repuestos_$idEquipo", ""))
        etCausaRaiz.setText(prefs.getString("causa_raiz_$idEquipo", ""))
        etObservaciones.setText(prefs.getString("observaciones_$idEquipo", ""))
        spEstadoFinal.setText(prefs.getString("estado_final_$idEquipo", ""), false)

        val empleadosSel = prefs.getStringSet("empleados_seleccionados_$idEquipo", emptySet())
        checkBoxEmpleadoMap.forEach { (checkBox, empleado) ->
            checkBox.isChecked = empleadosSel?.contains(empleado.id.toString()) == true
        }
    }


    private fun GuardarMantenimiento3() {
        if (fotosList.isEmpty()) {
            Toast.makeText(this, "Debes tomar al menos una foto", Toast.LENGTH_SHORT).show()
            return
        }

        // 1️⃣ Obtener IDs de usuarios chequeados
        val usuariosSeleccionadosIds = checkBoxEmpleadoMap
            .filter { it.key.isChecked }
            .map { it.value.id } // Asegúrate de que Empleado tenga la propiedad "id"

        // 2️⃣ Construir JSON incluyendo usuarios_checkeados
        val jsonBody = JSONObject().apply {
            put("descripcion_falla", etDescripcionFalla.text.toString())
            put("diagnostico", etDiagnostico.text.toString())
            put("acciones", etAcciones.text.toString())
            put("repuestos", etRepuestos.text.toString())
            put("estado_final", spEstadoFinal.text.toString())
            put("causa_raiz", etCausaRaiz.text.toString())
            put("observaciones", etObservaciones.text.toString())
            put("usuarios_checkeados", JSONArray(usuariosSeleccionadosIds))
            put("id_tag_equipo", intent.getIntExtra("tagId", -1)) // Añadido para enviar el tagId


        }.toString()

        // 3️⃣ Convertir a RequestBody
        val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

        // 4️⃣ Preparar imágenes
        val imagenes = fotosList.map { file ->
            val compressed = comprimirImagen(file)
            MultipartBody.Part.createFormData(
                "imagenes[]",
                compressed.name,
                compressed.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
        }

        // 5️⃣ Enviar a la API
        RetrofitClient.instance.finalizarMantenimiento(requestBody, imagenes)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@MantenimientoCorrectivoActivity,
                            "Mantenimiento enviado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        limpiarDatosEnPrefs()
                        limpiarFotosEnPrefs()
                        finish()
                    } else {
                        // Leer el cuerpo del error y parsearlo como JSON si es posible
                        val errorBody = response.errorBody()?.string()
                        val mensajeError = if (errorBody.isNullOrEmpty()) {
                            "Error del servidor (código: ${response.code()})"
                        } else {
                            try {
                                val jsonError = JSONObject(errorBody)
                                jsonError.optString("error", "Error del servidor: $errorBody")
                            } catch (e: Exception) {
                                "Error del servidor: $errorBody" // Si no es JSON, mostrar el cuerpo tal cual
                            }
                            "Error del servidor: $errorBody"
                        }
                        Toast.makeText(
                            this@MantenimientoCorrectivoActivity,
                            mensajeError,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(
                        this@MantenimientoCorrectivoActivity,
                        "Error de conexión: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun GuardarMantenimiento() {
        if (fotosList.isEmpty()) {
            Toast.makeText(this, "Debes tomar al menos una foto", Toast.LENGTH_SHORT).show()
            return
        }

        val estadoFinal = spEstadoFinal.text.toString().trim()
        if (estadoFinal.isEmpty()) {
            Toast.makeText(this, "Debes seleccionar un estado final", Toast.LENGTH_SHORT).show()
            return
        }

        val dbHelper = DatabaseHelper(this)
        val idEquipo = intent.getStringExtra("tagId")?.toIntOrNull() ?: -1

        // 1️⃣ Usuarios seleccionados
        val usuariosSeleccionadosIds = checkBoxEmpleadoMap
            .filter { it.key.isChecked }
            .map { it.value.id }

        if (usuariosSeleccionadosIds.isEmpty()) {
            Toast.makeText(this, "Debes seleccionar al menos un usuario", Toast.LENGTH_SHORT).show()
            return
        }

        // 2️⃣ Ejecutar en transacción
        val exito = dbHelper.insertarMantenimientoCompleto(
            idEquipo,
            etDescripcionFalla.text.toString(),
            etDiagnostico.text.toString(),
            etAcciones.text.toString(),
            etRepuestos.text.toString(),
            estadoFinal,
            etCausaRaiz.text.toString(),
            etObservaciones.text.toString(),
            usuariosSeleccionadosIds,
            fotosList
        )

        if (exito) {
            Toast.makeText(this, "Mantenimiento guardado localmente", Toast.LENGTH_SHORT).show()

            // 🔹 Limpiar SharedPreferences
            limpiarDatosEnPrefs()
            limpiarFotosEnPrefs()

            // 🔹 Limpiar campos visuales
            etDescripcionFalla.text.clear()
            etDiagnostico.text.clear()
            etAcciones.text.clear()
            etRepuestos.text.clear()
            etCausaRaiz.text.clear()
            etObservaciones.text.clear()
            spEstadoFinal.setText("", false)
            checkBoxEmpleadoMap.keys.forEach { it.isChecked = false }
            fotosList.clear()
            fotoAdapter.notifyDataSetChanged()

            // 🔹 Volver a HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish() // cerrar esta activity
        } else {
            Toast.makeText(this, "Error al guardar mantenimiento en BD local", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun GuardarMantenimientoApi() {
        if (fotosList.isEmpty()) {
            Toast.makeText(this, "Debes tomar al menos una foto", Toast.LENGTH_SHORT).show()
            return
        }

        // 1️⃣ Obtener IDs de usuarios chequeados
        val usuariosSeleccionadosIds = checkBoxEmpleadoMap
            .filter { it.key.isChecked }
            .map { it.value.id } // Asegúrate que "Empleado" tenga la propiedad id

        // 2️⃣ Construir JSON como string
        val jsonString = JSONObject().apply {
            put("descripcion_falla", etDescripcionFalla.text.toString())
            put("diagnostico", etDiagnostico.text.toString())
            put("acciones", etAcciones.text.toString())
            put("repuestos", etRepuestos.text.toString())
            put("estado_final", spEstadoFinal.text.toString())
            put("causa_raiz", etCausaRaiz.text.toString())
            put("observaciones", etObservaciones.text.toString())
            put("usuarios_checkeados", JSONArray(usuariosSeleccionadosIds))
            put("id_tag_equipo", intent.getStringExtra("tagId"))
        }.toString()

        // 3️⃣ Convertir JSON a RequestBody (manteniendo tu firma)
        val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

        // 4️⃣ Preparar imágenes como "imagenes[]"
        val imagenesParts = fotosList.map { file ->
            val compressed = comprimirImagen(file)
            MultipartBody.Part.createFormData(
                "imagenes[]",
                compressed.name,
                compressed.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
        }

        // 5️⃣ Llamar a Retrofit
        RetrofitClient.instance.finalizarMantenimiento(requestBody, imagenesParts)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@MantenimientoCorrectivoActivity,
                            "Mantenimiento enviado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        limpiarDatosEnPrefs()
                        limpiarFotosEnPrefs()
                        finish()
                    } else {
                        // Leer el cuerpo del error si está disponible
                        val errorBody = response.errorBody()?.string()
                        val mensajeError = if (errorBody.isNullOrEmpty()) {
                            "Error del servidor (código: ${response.code()})"
                        } else {
                            "Error del servidor: $errorBody"
                        }
                        Toast.makeText(
                            this@MantenimientoCorrectivoActivity,
                            mensajeError,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(
                        this@MantenimientoCorrectivoActivity,
                        "Error de conexión: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }


    private fun limpiarDatosEnPrefs() {
        val idEquipo = intent.getStringExtra("tagId") ?: "0"
        val prefs = getSharedPreferences("DatosMantenimiento", MODE_PRIVATE).edit()
        prefs.remove("descripcion_falla_$idEquipo")
        prefs.remove("diagnostico_$idEquipo")
        prefs.remove("acciones_$idEquipo")
        prefs.remove("repuestos_$idEquipo")
        prefs.remove("estado_final_$idEquipo")
        prefs.remove("causa_raiz_$idEquipo")
        prefs.remove("observaciones_$idEquipo")
        prefs.remove("empleados_seleccionados_$idEquipo")
        prefs.apply()
    }

}
