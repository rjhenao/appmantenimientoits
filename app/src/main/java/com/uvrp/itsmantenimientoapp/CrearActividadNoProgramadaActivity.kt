package com.uvrp.itsmantenimientoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import com.uvrp.itsmantenimientoapp.DatabaseHelper.ActividadBitacora
import com.uvrp.itsmantenimientoapp.DatabaseHelper.Cuadrilla
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrearActividadNoProgramadaActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var actividadesList: List<ActividadBitacora>
    private lateinit var cuadrillasList: List<Cuadrilla>
    private var idBitacora: Int = -1
    private var idUsuarioLogueado: Int = -1
    
    // Views
    private lateinit var spinnerActividad: Spinner
    private lateinit var spinnerCuadrilla: Spinner
    private lateinit var spinnerUF: Spinner
    private lateinit var spinnerSentido: Spinner
    private lateinit var spinnerLado: Spinner
    private lateinit var etPrInicialKm: EditText
    private lateinit var etPrInicialM: EditText
    private lateinit var etPrFinalKm: EditText
    private lateinit var etPrFinalM: EditText
    private lateinit var etCantidad: EditText
    private lateinit var etObservacion: EditText
    private lateinit var btnGuardarActividad: MaterialButton
    private lateinit var tilPrInicialKm: TextInputLayout
    private lateinit var tilPrInicialM: TextInputLayout
    private lateinit var tilPrFinalKm: TextInputLayout
    private lateinit var tilPrFinalM: TextInputLayout
    private lateinit var tilCantidad: TextInputLayout
    private lateinit var tvUnidadMedida: TextView
    private lateinit var btnTomarFotoNoProgramada: FloatingActionButton
    private lateinit var btnGaleriaNoProgramada: FloatingActionButton
    private lateinit var rvFotosNoProgramada: RecyclerView
    private lateinit var fotosAdapter: FotoAdapter
    private val fotosList = mutableListOf<File>()
    private var currentPhotoFile: File? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 11
        private const val REQUEST_IMAGE_GALLERY = 12
        private const val REQUEST_CAMERA_PERMISSION = 131
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_actividad_no_programada)

        // Obtener ID de bitácora desde el Intent
        idBitacora = intent.getIntExtra("id_bitacora", -1)
        if (idBitacora == -1) {
            Toast.makeText(this, "Error: No se especificó la bitácora", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Obtener ID del usuario logueado
        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
        idUsuarioLogueado = sharedPreferences.getInt("idUser", -1)
        val idRol = sharedPreferences.getInt("idRol", -1)
        if (idUsuarioLogueado == -1) {
            Toast.makeText(this, "Error: Usuario no logueado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Permiso: cualquiera con acceso a bitácoras puede crear no programadas
        if (idRol != 1 && idRol != 5 && idRol != 6) {
            Toast.makeText(this, "No tienes permisos para crear actividades no programadas.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Inicializar DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Inicializar views
        initViews()

        // Configurar header
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerLayout)
        val navView = findViewById<com.google.android.material.navigation.NavigationView>(R.id.navView)
        HeaderHelper.setupHeader(this, drawerLayout, navView)
        supportActionBar?.title = "Crear Actividad No Programada"

        // Configurar spinners
        setupSpinners()

        // Configurar botón guardar
        btnGuardarActividad.setOnClickListener {
            guardarActividad()
        }

        setupFotos()
    }

    private fun initViews() {
        spinnerActividad = findViewById(R.id.spinnerActividad)
        spinnerCuadrilla = findViewById(R.id.spinnerCuadrilla)
        spinnerUF = findViewById(R.id.spinnerUF)
        spinnerSentido = findViewById(R.id.spinnerSentido)
        spinnerLado = findViewById(R.id.spinnerLado)
        etPrInicialKm = findViewById(R.id.etPrInicialKm)
        etPrInicialM = findViewById(R.id.etPrInicialM)
        etPrFinalKm = findViewById(R.id.etPrFinalKm)
        etPrFinalM = findViewById(R.id.etPrFinalM)
        etCantidad = findViewById(R.id.etCantidad)
        etObservacion = findViewById(R.id.etObservacion)
        btnGuardarActividad = findViewById(R.id.btnGuardarActividad)
        tilPrInicialKm = findViewById(R.id.tilPrInicialKm)
        tilPrInicialM = findViewById(R.id.tilPrInicialM)
        tilPrFinalKm = findViewById(R.id.tilPrFinalKm)
        tilPrFinalM = findViewById(R.id.tilPrFinalM)
        tilCantidad = findViewById(R.id.tilCantidad)
        tvUnidadMedida = findViewById(R.id.tvUnidadMedida)
        btnTomarFotoNoProgramada = findViewById(R.id.btnTomarFotoNoProgramada)
        btnGaleriaNoProgramada = findViewById(R.id.btnGaleriaNoProgramada)
        rvFotosNoProgramada = findViewById(R.id.rvFotosNoProgramada)
    }

    private fun setupFotos() {
        fotosAdapter = FotoAdapter(fotosList) { file ->
            fotosAdapter.eliminarFoto(file)
            if (!file.delete()) {
                Log.e("ACTIVIDAD_NO_PROGRAMADA", "No se pudo borrar foto: ${file.absolutePath}")
            }
        }
        rvFotosNoProgramada.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvFotosNoProgramada.adapter = fotosAdapter

        btnTomarFotoNoProgramada.setOnClickListener { verificarPermisosCamara() }
        btnGaleriaNoProgramada.setOnClickListener { abrirGaleria() }
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

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
    }

    @Throws(IOException::class)
    private fun crearArchivoImagen(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("BITACORA_NP_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Toast.makeText(this, "Permiso denegado para acceder a imágenes.", Toast.LENGTH_SHORT).show()
            return
        }
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> abrirCamara()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            currentPhotoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    val compressedFile = comprimirImagen(file)
                    fotosList.add(compressedFile)
                    fotosAdapter.notifyDataSetChanged()
                }
            }
        } else if (requestCode == REQUEST_IMAGE_GALLERY && resultCode == RESULT_OK) {
            val selectedUris = mutableListOf<Uri>()
            data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i)?.uri?.let { selectedUris.add(it) }
                }
            }
            data?.data?.let { selectedUris.add(it) }
            selectedUris.forEach { uri ->
                try {
                    val tempFile = copiarUriAArchivo(uri)
                    if (tempFile.exists() && tempFile.length() > 0) {
                        val compressedFile = comprimirImagen(tempFile)
                        fotosList.add(compressedFile)
                    }
                } catch (e: Exception) {
                    Log.e("ACTIVIDAD_NO_PROGRAMADA", "Error procesando imagen de galería: ${e.message}", e)
                }
            }
            fotosAdapter.notifyDataSetChanged()
        }
    }

    private fun copiarUriAArchivo(uri: Uri): File {
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(storageDir, "GALLERY_NP_${timeStamp}_.jpg")

        contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IOException("No se pudo abrir la imagen seleccionada.")
            FileOutputStream(outputFile).use { output -> input.copyTo(output) }
        }
        return outputFile
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
        FileOutputStream(compressedFile).use { out ->
            bitmapEscalado.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.flush()
        }
        originalFile.delete()
        return compressedFile
    }

    private fun setupSpinners() {
        try {
            // Obtener actividades
            actividadesList = dbHelper.obtenerActividadesBitacoras()
            if (actividadesList.isEmpty()) {
                Toast.makeText(this, "No hay actividades disponibles", Toast.LENGTH_SHORT).show()
                return
            }

            // Obtener cuadrillas
            cuadrillasList = dbHelper.obtenerCuadrillas()
            if (cuadrillasList.isEmpty()) {
                Toast.makeText(this, "No hay cuadrillas disponibles", Toast.LENGTH_SHORT).show()
                return
            }

            // Configurar spinner de actividades
            val actividadesAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("Seleccione una actividad") + actividadesList.map { it.descripcion }
            )
            actividadesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerActividad.adapter = actividadesAdapter

            // Listener para actualizar la unidad de medida
            spinnerActividad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0) { // Ignorar el placeholder
                        val actividadSeleccionada = actividadesList[position - 1]
                        tvUnidadMedida.text = actividadSeleccionada.tipoUnidad
                        Log.d("ACTIVIDAD_NO_PROGRAMADA", "Unidad de medida actualizada: ${actividadSeleccionada.tipoUnidad}")
                    } else {
                        tvUnidadMedida.text = "--"
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    tvUnidadMedida.text = "--"
                }
            }

            // Configurar spinner de cuadrillas
            val cuadrillasAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("Seleccione una cuadrilla") + cuadrillasList.map { it.nombre }
            )
            cuadrillasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCuadrilla.adapter = cuadrillasAdapter

            // Configurar spinner de UF (valores 1-6 como en el formulario web)
            val ufAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("Seleccione...", "UF 1", "UF 2", "UF 3", "UF 4", "UF 5", "UF 6")
            )
            ufAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerUF.adapter = ufAdapter

            // Configurar spinner de sentido (CUPA y PACU como en el formulario web)
            val sentidosCatalogo = dbHelper.obtenerSentidosCatalogo()
            if (sentidosCatalogo.isEmpty()) {
                Toast.makeText(this, "No hay sentidos configurados. Ejecuta sincronización.", Toast.LENGTH_LONG).show()
                return
            }
            val sentidoAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                sentidosCatalogo
            )
            sentidoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSentido.adapter = sentidoAdapter

            // Configurar spinner de lado (exactamente como en el formulario web)
            val ladosCatalogo = dbHelper.obtenerLadosCatalogo()
            if (ladosCatalogo.isEmpty()) {
                Toast.makeText(this, "No hay lados configurados. Ejecuta sincronización.", Toast.LENGTH_LONG).show()
                return
            }
            val ladoAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                ladosCatalogo
            )
            ladoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerLado.adapter = ladoAdapter

            Log.d("ACTIVIDAD_NO_PROGRAMADA", "Spinners configurados correctamente")

        } catch (e: Exception) {
            Log.e("ACTIVIDAD_NO_PROGRAMADA", "Error configurando spinners: ${e.message}", e)
            Toast.makeText(this, "Error configurando formulario: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarActividad() {
        try {
            // Validar campos obligatorios
            if (!validarCampos()) {
                return
            }

            // Obtener valores de los spinners
            val selectedActividad = spinnerActividad.selectedItemPosition - 1
            val selectedCuadrilla = spinnerCuadrilla.selectedItemPosition - 1
            val selectedUF = spinnerUF.selectedItemPosition

            if (selectedActividad < 0 || selectedCuadrilla < 0) {
                Toast.makeText(this, "Por favor seleccione actividad y cuadrilla", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (selectedUF == 0) {
                Toast.makeText(this, "Por favor seleccione UF", Toast.LENGTH_SHORT).show()
                return
            }

            // Obtener datos
            val actividad = actividadesList[selectedActividad]
            val cuadrilla = cuadrillasList[selectedCuadrilla]
            val uf = selectedUF // Ya viene como 1, 2, 3, 4, 5, 6
            val sentido = spinnerSentido.selectedItem?.toString().orEmpty()
            val lado = spinnerLado.selectedItem?.toString().orEmpty()

            if (sentido.isBlank() || lado.isBlank()) {
                Toast.makeText(this, "Por favor seleccione Sentido y Lado", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Combinar Km + m en formato "XX+YYY" para PR Inicial y PR Final
            val prInicialKm = etPrInicialKm.text.toString().trim()
            val prInicialM = etPrInicialM.text.toString().trim()
            val prInicial = "$prInicialKm+$prInicialM"
            
            val prFinalKm = etPrFinalKm.text.toString().trim()
            val prFinalM = etPrFinalM.text.toString().trim()
            val prFinal = "$prFinalKm+$prFinalM"
            
            val cantidad = etCantidad.text.toString().trim().toDoubleOrNull() ?: 0.0
            val observacion = etObservacion.text.toString().trim()

            Log.d("ACTIVIDAD_NO_PROGRAMADA", "Datos a insertar:")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "ID Bitácora: $idBitacora")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "ID Actividad: ${actividad.id}")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "ID Cuadrilla: ${cuadrilla.id}")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "UF: $uf")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "Sentido: $sentido")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "Lado: $lado")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "PR Inicial: $prInicial")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "PR Final: $prFinal")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "Cantidad: $cantidad")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "Observación: $observacion")
            Log.d("ACTIVIDAD_NO_PROGRAMADA", "Supervisor: $idUsuarioLogueado")

            // Insertar en la base de datos
            val resultado = dbHelper.insertarActividadNoProgramada(
                idBitacora = idBitacora,
                idActividad = actividad.id,
                idCuadrilla = cuadrilla.id,
                uf = uf,
                sentido = sentido,
                lado = lado,
                prInicial = prInicial,
                prFinal = prFinal,
                cantidad = cantidad,
                observacion = observacion,
                supervisorResponsable = idUsuarioLogueado,
                fotos = fotosList
            )

            if (resultado > 0) {
                Toast.makeText(this, "Actividad no programada creada exitosamente", Toast.LENGTH_SHORT).show()
                
                // Retornar resultado exitoso
                val resultIntent = Intent()
                resultIntent.putExtra("actividad_creada", true)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Error al crear la actividad", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("ACTIVIDAD_NO_PROGRAMADA", "Error guardando actividad: ${e.message}", e)
            Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validarCampos(): Boolean {
        var esValido = true

        // Validar PR Inicial Km
        if (etPrInicialKm.text.toString().trim().isEmpty()) {
            tilPrInicialKm.error = "Km obligatorio"
            esValido = false
        } else {
            tilPrInicialKm.error = null
        }

        // Validar PR Inicial m
        if (etPrInicialM.text.toString().trim().isEmpty()) {
            tilPrInicialM.error = "m obligatorio"
            esValido = false
        } else {
            tilPrInicialM.error = null
        }

        // Validar PR Final Km
        if (etPrFinalKm.text.toString().trim().isEmpty()) {
            tilPrFinalKm.error = "Km obligatorio"
            esValido = false
        } else {
            tilPrFinalKm.error = null
        }

        // Validar PR Final m
        if (etPrFinalM.text.toString().trim().isEmpty()) {
            tilPrFinalM.error = "m obligatorio"
            esValido = false
        } else {
            tilPrFinalM.error = null
        }

        // Validar Cantidad
        val cantidad = etCantidad.text.toString().trim()
        if (cantidad.isEmpty()) {
            tilCantidad.error = "Cantidad es obligatoria"
            esValido = false
        } else if (cantidad.toDoubleOrNull() == null || cantidad.toDouble() <= 0) {
            tilCantidad.error = "Cantidad debe ser un número positivo"
            esValido = false
        } else {
            tilCantidad.error = null
        }

        if (fotosList.isEmpty()) {
            Toast.makeText(this, "Debes agregar al menos una foto de evidencia", Toast.LENGTH_SHORT).show()
            esValido = false
        }

        return esValido
    }
}
