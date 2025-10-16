package com.uvrp.itsmantenimientoapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.* // <-- NUEVO IMPORT
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.uvrp.itsmantenimientoapp.adapters.ParticipantesAdapter
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import com.uvrp.itsmantenimientoapp.models.Usuario
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RegistrarMantenimientoBitacora : AppCompatActivity() {

    // --- Vistas de la UI ---
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var textViewActividad: TextView
    private lateinit var textViewUnidad: TextView
    private lateinit var inputPrInicial1: EditText
    private lateinit var inputPrInicial2: EditText
    private lateinit var inputPrFinal1: EditText
    private lateinit var inputPrFinal2: EditText
    private lateinit var inputCantidad: EditText
    private lateinit var inputObservacion: EditText
    private lateinit var buttonTakePhoto: ImageButton
    private lateinit var rvPhotos: RecyclerView
    private lateinit var buttonRegister: Button
    private lateinit var buttonAgregarUsuario: Button
    private lateinit var rvParticipantes: RecyclerView

    // --- Lógica y Datos ---
    private lateinit var participantesAdapter: ParticipantesAdapter
    private val participantesList = mutableListOf<Usuario>()
    private lateinit var fotosAdapter: FotoAdapter
    private val fotosList = mutableListOf<File>()
    private var currentPhotoFile: File? = null
    private var numeroActividad: Int = -1
    private var idUser: Int = -1
    private lateinit var dbHelper: DatabaseHelper

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_CAMERA_PERMISSION = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_mantenimiento)

        // 1. Obtenemos los datos necesarios del Intent y SharedPreferences
        numeroActividad = intent.getIntExtra("numero_actividad", -1)
        idUser = getSharedPreferences("Sesion", Context.MODE_PRIVATE).getInt("idUser", -1)

        if (numeroActividad == -1 || idUser == -1) {
            Toast.makeText(this, "Error: Faltan datos críticos para continuar.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 2. Centralizamos la inicialización de vistas y helpers
        initViews()
        dbHelper = DatabaseHelper(this)
        HeaderHelper.setupHeader(this, drawerLayout, navView)
        supportActionBar?.title = "Registrar Mantenimiento"

        // 3. Configuramos todos los listeners y adapters
        setupListeners()
        setupRecyclerViewFotos()
        setupParticipantesRecyclerView()

        supportFragmentManager.setFragmentResultListener("seleccion_usuarios_request", this) { _, bundle ->
            val idsSeleccionados = bundle.getIntegerArrayList("ids_seleccionados")
            idsSeleccionados?.let {
                agregarNuevosUsuarios(it)
            }
        }

        // 4. Cargamos TODOS los datos necesarios para la vista
        cargarInfoActividad()
        cargarDatosDePrefs()
        cargarFotosDePrefs()
        fotosAdapter.notifyDataSetChanged()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        textViewActividad = findViewById(R.id.text_view_actividad)
        textViewUnidad = findViewById(R.id.text_view_unidad)
        inputPrInicial1 = findViewById(R.id.input_pr_inicial_1)
        inputPrInicial2 = findViewById(R.id.input_pr_inicial_2)
        inputPrFinal1 = findViewById(R.id.input_pr_final_1)
        inputPrFinal2 = findViewById(R.id.input_pr_final_2)
        inputCantidad = findViewById(R.id.input_cantidad)
        inputObservacion = findViewById(R.id.input_observacion)
        buttonTakePhoto = findViewById(R.id.button_take_photo)
        rvPhotos = findViewById(R.id.rv_photos)
        buttonRegister = findViewById(R.id.button_register)
        buttonAgregarUsuario = findViewById(R.id.button_agregar_usuario)
        rvParticipantes = findViewById(R.id.recycler_view_participantes)
    }

    private fun setupListeners() {
        buttonTakePhoto.setOnClickListener { verificarPermisosCamara() }
        buttonRegister.setOnClickListener { registrarMantenimiento() }
        buttonAgregarUsuario.setOnClickListener {
            val idsActuales = participantesList.map { it.id }
            val dialogo = SeleccionarUsuarioDialogFragment.newInstance(ArrayList(idsActuales))
            dialogo.show(supportFragmentManager, "SeleccionarUsuarioDialog")
        }

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDatosEnPrefs()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        listOf(
            inputPrInicial1,
            inputPrInicial2,
            inputPrFinal1,
            inputPrFinal2,
            inputCantidad,
            inputObservacion
        ).forEach { editText ->
            editText.addTextChangedListener(textWatcher)
        }
    }

    private fun setupParticipantesRecyclerView() {
        val empleadosSeleccionados = cargarSeleccionUsuariosDePrefs()

        participantesList.clear()
        participantesList.addAll(dbHelper.getUsuariosPorActividad(numeroActividad))

        participantesAdapter = ParticipantesAdapter(participantesList, empleadosSeleccionados) {
            guardarDatosEnPrefs()
        }

        rvParticipantes.layoutManager = LinearLayoutManager(this)
        rvParticipantes.adapter = participantesAdapter
    }

    private fun agregarNuevosUsuarios(nuevosIds: List<Int>) {
        val startIndex = participantesList.size
        var itemsAdded = 0

        nuevosIds.forEach { id ->
            if (participantesList.none { it.id == id }) {
                val usuario = dbHelper.getUsuarioPorId(id)
                usuario?.let {
                    participantesList.add(it)
                    itemsAdded++
                }
            }
        }

        if (itemsAdded > 0) {
            participantesAdapter.notifyItemRangeInserted(startIndex, itemsAdded)
        }
    }

    private fun cargarInfoActividad() {
        val actividadInfo = dbHelper.getActividadInfo(numeroActividad)
        actividadInfo?.let {
            textViewActividad.text = it.descripcion
            textViewUnidad.text = "(${it.tipoUnidad})"
        } ?: run {
            textViewActividad.text = "Actividad no encontrada"
        }
    }

    private fun setupRecyclerViewFotos() {
        fotosAdapter = FotoAdapter(fotosList) { file ->
            fotosAdapter.eliminarFoto(file)
            if (!file.delete()) {
                Log.e("FileDeletionError", "No se pudo borrar el archivo: ${file.absolutePath}")
            }
            guardarFotosEnPrefs()
        }
        rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPhotos.adapter = fotosAdapter
    }

    private fun verificarPermisosCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            abrirCamara()
        }
    }

    private fun abrirCamara() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                try {
                    val photoFile: File = crearArchivoImagen()
                    currentPhotoFile = photoFile
                    val photoURI: Uri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                } catch (ex: IOException) {
                    Toast.makeText(this, "Error al crear el archivo de la foto.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun crearArchivoImagen(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("BITACORA_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            currentPhotoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    val compressedFile = comprimirImagen(file)
                    fotosList.add(compressedFile)
                    guardarFotosEnPrefs()
                    fotosAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error al guardar la foto.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- INICIO DE LA MODIFICACIÓN ---
    private fun comprimirImagen(originalFile: File): File {
        // 1. Decodificar y escalar la imagen como ya lo hacías
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

        // 2. Para poder dibujar, necesitamos una copia mutable del bitmap
        val bitmapMutable = bitmapEscalado.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmapMutable)

        // 3. Preparar el "pincel" (Paint) para el texto
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            // El tamaño del texto será relativo a la altura de la imagen
            textSize = bitmapMutable.height / 35f
            // Añadimos una sombra sutil para que el texto sea visible en fondos claros
            setShadowLayer(5f, 2f, 2f, Color.BLACK)
        }

        // 4. Obtener los textos para la marca de agua
        val sdf = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale("es", "ES"))
        val fechaHoraTexto = sdf.format(Date())
        val actividadTexto = textViewActividad.text.toString()

        // 5. Definir la posición (esquina inferior izquierda con un pequeño margen)
        val margen = 25f
        val yPosFecha = bitmapMutable.height - margen
        val yPosActividad = yPosFecha - paint.textSize - (margen / 2) // Una línea arriba de la fecha

        // 6. Dibujar los textos en el canvas (que está vinculado a nuestro bitmap)
        canvas.drawText(fechaHoraTexto, margen, yPosFecha, paint)
        canvas.drawText(actividadTexto, margen, yPosActividad, paint)

        // 7. Guardar el bitmap ya modificado con la marca de agua
        val compressedFile = File(originalFile.parent, "COMP_${originalFile.name}")
        FileOutputStream(compressedFile).use { out ->
            bitmapMutable.compress(Bitmap.CompressFormat.JPEG, 80, out) // Calidad 80 para buen balance
            out.flush()
        }

        // 8. Borrar el archivo original sin marca de agua y devolver el nuevo
        originalFile.delete()
        return compressedFile
    }
    // --- FIN DE LA MODIFICACIÓN ---

    private fun getPrefKey(fieldName: String): String = "${fieldName}_${numeroActividad}_${idUser}"

    private fun guardarDatosEnPrefs() {
        val prefs = getSharedPreferences("DatosBitacora", MODE_PRIVATE).edit()
        prefs.putString(getPrefKey("pr_inicial_1"), inputPrInicial1.text.toString())
        prefs.putString(getPrefKey("pr_inicial_2"), inputPrInicial2.text.toString())
        prefs.putString(getPrefKey("pr_final_1"), inputPrFinal1.text.toString())
        prefs.putString(getPrefKey("pr_final_2"), inputPrFinal2.text.toString())
        prefs.putString(getPrefKey("cantidad"), inputCantidad.text.toString())
        prefs.putString(getPrefKey("observacion"), inputObservacion.text.toString())

        if (::participantesAdapter.isInitialized) {
            val selectedUserIds = participantesAdapter.getSelectedUserIds().map { it.toString() }.toSet()
            prefs.putStringSet(getPrefKey("usuarios_seleccionados"), selectedUserIds)
        }

        prefs.apply()
    }

    private fun cargarDatosDePrefs() {
        val prefs = getSharedPreferences("DatosBitacora", MODE_PRIVATE)
        inputPrInicial1.setText(prefs.getString(getPrefKey("pr_inicial_1"), ""))
        inputPrInicial2.setText(prefs.getString(getPrefKey("pr_inicial_2"), ""))
        inputPrFinal1.setText(prefs.getString(getPrefKey("pr_final_1"), ""))
        inputPrFinal2.setText(prefs.getString(getPrefKey("pr_final_2"), ""))
        inputCantidad.setText(prefs.getString(getPrefKey("cantidad"), ""))
        inputObservacion.setText(prefs.getString(getPrefKey("observacion"), ""))
    }

    private fun cargarSeleccionUsuariosDePrefs(): Set<String> {
        val prefs = getSharedPreferences("DatosBitacora", MODE_PRIVATE)
        return prefs.getStringSet(getPrefKey("usuarios_seleccionados"), emptySet()) ?: emptySet()
    }

    private fun guardarFotosEnPrefs() {
        val prefs = getSharedPreferences("FotosBitacora", MODE_PRIVATE).edit()
        val photoPaths = fotosList.map { it.absolutePath }.toSet()
        prefs.putStringSet(getPrefKey("fotos_guardadas"), photoPaths).apply()
    }

    private fun cargarFotosDePrefs() {
        val prefs = getSharedPreferences("FotosBitacora", MODE_PRIVATE)
        val paths = prefs.getStringSet(getPrefKey("fotos_guardadas"), emptySet())
        fotosList.clear()
        paths?.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                fotosList.add(file)
            }
        }
    }

    private fun limpiarPrefs() {
        getSharedPreferences("DatosBitacora", MODE_PRIVATE).edit()
            .remove(getPrefKey("pr_inicial_1"))
            .remove(getPrefKey("pr_inicial_2"))
            .remove(getPrefKey("pr_final_1"))
            .remove(getPrefKey("pr_final_2"))
            .remove(getPrefKey("cantidad"))
            .remove(getPrefKey("observacion"))
            .remove(getPrefKey("usuarios_seleccionados"))
            .apply()

        getSharedPreferences("FotosBitacora", MODE_PRIVATE).edit()
            .remove(getPrefKey("fotos_guardadas"))
            .apply()
    }

    private fun registrarMantenimiento() {
        if (!validarCampos()) return

        val ranges = dbHelper.getValidationRanges(numeroActividad)
        if (ranges == null) {
            Toast.makeText(this, "Error: No se encontraron los rangos de validación.", Toast.LENGTH_LONG).show()
            return
        }

        val rangoInicial = parseStationingToComparableLong(ranges.prInicialStr)
        val rangoFinal = parseStationingToComparableLong(ranges.prFinalStr)

        val prInicialUsuario = parseStationingToComparableLong(
            "${inputPrInicial1.text.toString()}+${inputPrInicial2.text.toString()}"
        )
        val prFinalUsuario = parseStationingToComparableLong(
            "${inputPrFinal1.text.toString()}+${inputPrFinal2.text.toString()}"
        )
        val cantidadUsuario = inputCantidad.text.toString().toDoubleOrNull() ?: 0.0

        val errores = mutableListOf<String>()

        if (prInicialUsuario !in rangoInicial..rangoFinal) {
            errores.add("• El Pr. Inicial (${inputPrInicial1.text}+${inputPrInicial2.text}) está fuera del rango de trabajo (${ranges.prInicialStr} - ${ranges.prFinalStr}).")
        }
        if (prFinalUsuario !in rangoInicial..rangoFinal) {
            errores.add("• El Pr. Final (${inputPrFinal1.text}+${inputPrFinal2.text}) está fuera del rango de trabajo (${ranges.prInicialStr} - ${ranges.prFinalStr}).")
        }

        val cantidadMaxPermitida = ranges.cantidadStr?.toDoubleOrNull() ?: Double.MAX_VALUE
        val cantidadMinPermitida = 0.0

        if (cantidadUsuario < cantidadMinPermitida || cantidadUsuario > cantidadMaxPermitida) {
            errores.add("• La Cantidad (${cantidadUsuario}) está fuera del rango permitido (${cantidadMinPermitida} - ${cantidadMaxPermitida}).")
        }

        if (errores.isNotEmpty()) {
            mostrarErrorDeValidacion(errores.joinToString("\n\n"))
            return
        }

        val prInicialFusionado = "${inputPrInicial1.text}+${inputPrInicial2.text}"
        val prFinalFusionado = "${inputPrFinal1.text}+${inputPrFinal2.text}"

        val usuariosSeleccionadosIds = participantesAdapter.getSelectedUserIds().toList()
        val exito = dbHelper.insertarRegistroBitacora(
            numeroActividad = numeroActividad,
            prInicial = prInicialFusionado,
            prFinal= prFinalFusionado,
            cantidad = cantidadUsuario,
            observacion = inputObservacion.text.toString(),
            idUsuarios = usuariosSeleccionadosIds,
            fotos = fotosList
        )

        if (exito) {
            Toast.makeText(this, "Mantenimiento registrado localmente", Toast.LENGTH_LONG).show()
            limpiarPrefs()
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Error al guardar en la base de datos local", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseStationingToComparableLong(stationingStr: String?): Long {
        if (stationingStr.isNullOrBlank()) return 0L

        val parts = stationingStr.split('+')
        if (parts.size != 2) return 0L

        val mainPart = parts[0].trim().toLongOrNull() ?: 0L
        val subPart = parts[1].trim().toLongOrNull() ?: 0L

        return (mainPart * 1000) + subPart
    }

    private fun mostrarErrorDeValidacion(mensaje: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Datos fuera de rango")
            .setMessage(mensaje)
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun validarCampos(): Boolean {
        var esValido = true
        var primeraVistaConError: View? = null

        fun marcarError(editText: EditText, mensaje: String) {
            editText.error = mensaje
            if (primeraVistaConError == null) {
                primeraVistaConError = editText
            }
            esValido = false
        }

        // Validar PR Inicial Km (obligatorio, máximo 4 dígitos)
        if (inputPrInicial1.text.isBlank()) {
            marcarError(inputPrInicial1, "Este campo es obligatorio")
        }
        
        // Validar PR Inicial m (obligatorio, máximo 4 dígitos)
        if (inputPrInicial2.text.isBlank()) {
            marcarError(inputPrInicial2, "Este campo es obligatorio")
        }
        
        // Validar PR Final Km (obligatorio, máximo 4 dígitos)
        if (inputPrFinal1.text.isBlank()) {
            marcarError(inputPrFinal1, "Este campo es obligatorio")
        }
        
        // Validar PR Final m (obligatorio, máximo 4 dígitos)
        if (inputPrFinal2.text.isBlank()) {
            marcarError(inputPrFinal2, "Este campo es obligatorio")
        }
        if (inputCantidad.text.isBlank()) {
            marcarError(inputCantidad, "La cantidad es obligatoria")
        }
        if (inputObservacion.text.isBlank()) {
            marcarError(inputObservacion, "Por favor, ingrese una observación")
        }

        if (!::participantesAdapter.isInitialized || participantesAdapter.getSelectedUserIds().isEmpty()) {
            Toast.makeText(this, "Debes seleccionar al menos un participante", Toast.LENGTH_SHORT).show()
            esValido = false
        }

        if (fotosList.isEmpty()) {
            Toast.makeText(this, "Debes agregar al menos una foto de evidencia", Toast.LENGTH_SHORT).show()
            esValido = false
        }

        primeraVistaConError?.requestFocus()
        return esValido
    }
}