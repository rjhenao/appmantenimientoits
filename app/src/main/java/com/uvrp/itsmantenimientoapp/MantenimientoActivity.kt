package com.uvrp.itsmantenimientoapp

import Actividad
import ActividadEstado
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MantenimientoActivity : AppCompatActivity() {

    private lateinit var empleados: List<Empleado>
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_CAPTURE_2 = 2
    private var currentPhotoPath: String? = null
    private var currentActividad: Actividad? = null
    private var currentIdEstado = 1
    private var currentIdMantenimiento = 0
    private var currentFotoButton: Button? = null

    // Mapa para guardar temporalmente las observaciones usando claves únicas
    private val observacionesTemporales = mutableMapOf<String, String>()

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    private val checkBoxEmpleadoMap = mutableMapOf<CheckBox, Empleado>()
    private val dbHelper: DatabaseHelper by lazy { DatabaseHelper(this) }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mantenimiento)

        if (!allPermissionsGranted()) {
            requestPermissions()
        }

        val idMantenimiento = intent.getIntExtra("idmantenimiento", -1)
        currentIdMantenimiento = idMantenimiento
        val actividades = intent.getParcelableArrayListExtra<Actividad>("actividades") ?: arrayListOf()
        val estadomantenimientos = intent.getParcelableArrayListExtra<Actividad>("estadosmantenimientos") ?: arrayListOf()

        empleados = dbHelper.getEmpleados()
        setupCheckboxes()

        findViewById<TextView>(R.id.tLocacion).text = intent.getStringExtra("column1f1")
        findViewById<TextView>(R.id.tSistema).text = intent.getStringExtra("column2f1")
        findViewById<TextView>(R.id.tSubsistema).text = intent.getStringExtra("column3f1")
        findViewById<TextView>(R.id.tTipo).text = intent.getStringExtra("column1f2")
        findViewById<TextView>(R.id.tUf).text = intent.getStringExtra("column2f2")
        findViewById<TextView>(R.id.tTag).text = intent.getStringExtra("column3f2")
        findViewById<TextView>(R.id.tPeriodicidad).text = intent.getStringExtra("column1f3")
        findViewById<TextView>(R.id.tFechaMantenimiento).text = intent.getStringExtra("column2f3")

        val container = findViewById<LinearLayout>(R.id.actividadesContainer)
        container.removeAllViews()

        val btnFinalizarMantenimiento = findViewById<Button>(R.id.btnFinalizar)
        btnFinalizarMantenimiento.setOnClickListener {
            finalizarMantenimiento()
        }

        // --- BUCLE PARA ACTIVIDADES ---
        actividades.forEach { actividad ->
            val uniqueKey = "actividad_${actividad.idEstado}"
            val cardView = layoutInflater.inflate(R.layout.item_actividad, container, false) as CardView
            val txtActividad = cardView.findViewById<TextView>(R.id.txtActividad)
            val editObservaciones = cardView.findViewById<EditText>(R.id.editObservaciones)
            val btnFoto = cardView.findViewById<Button>(R.id.btnFoto)
            val btnAprobar = cardView.findViewById<Button>(R.id.btnAprobar)
            val btnRechazar = cardView.findViewById<Button>(R.id.btnRechazar)
            val linearLayout = cardView.findViewById<LinearLayout>(R.id.linearLayoutActividad)

            txtActividad.text = actividad.descripcion

            if (observacionesTemporales.containsKey(uniqueKey)) {
                editObservaciones.setText(observacionesTemporales[uniqueKey])
            } else {
                val tObservacion = dbHelper.getObservacionActividad(actividad.idEstado)
                editObservaciones.setText(if (tObservacion.isNotEmpty()) tObservacion else "Ok")
            }

            editObservaciones.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    observacionesTemporales[uniqueKey] = s.toString()
                }
            })

            when (actividad.estado) {
                0 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                1 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.success))
                2 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.alert))
            }

            if (dbHelper.getHasPath(actividad.idEstado)) {
                btnFoto.backgroundTintList = ColorStateList.valueOf(getColor(R.color.success))
            }

            btnFoto.setOnClickListener {
                if (allPermissionsGranted()) {
                    currentActividad = actividad
                    currentFotoButton = btnFoto
                    dispatchTakePictureIntent()
                } else {
                    requestPermissions()
                }
            }

            btnAprobar.setOnClickListener {
                val observacion = editObservaciones.text.toString().trim()
                if (observacion.isNotEmpty()) {
                    if (dbHelper.insertObservacionActividad(actividad.idEstado, observacion, 1)) {
                        observacionesTemporales.remove(uniqueKey)
                        linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.success))
                    } else {
                        Toast.makeText(this, "Error al guardar.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Ingrese una observación.", Toast.LENGTH_SHORT).show()
                }
            }

            btnRechazar.setOnClickListener {
                val observacion = editObservaciones.text.toString().trim()
                if (observacion.isNotEmpty()) {
                    if (dbHelper.insertObservacionActividad(actividad.idEstado, observacion, 2)) {
                        observacionesTemporales.remove(uniqueKey)
                        linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.alert))
                    } else {
                        Toast.makeText(this, "Error al guardar.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Ingrese una observación.", Toast.LENGTH_SHORT).show()
                }
            }
            container.addView(cardView)
        }

        // --- BUCLE PARA ESTADOS DE MANTENIMIENTO ---
        estadomantenimientos.forEach { estados ->
            val uniqueKey = "estado_${estados.idEstado}"
            val cardView = layoutInflater.inflate(R.layout.item_actividad, container, false) as CardView
            val txtActividad = cardView.findViewById<TextView>(R.id.txtActividad)
            val editObservaciones = cardView.findViewById<EditText>(R.id.editObservaciones)
            val btnFoto = cardView.findViewById<Button>(R.id.btnFoto)
            val btnAprobar = cardView.findViewById<Button>(R.id.btnAprobar)
            val btnRechazar = cardView.findViewById<Button>(R.id.btnRechazar)
            val linearLayout = cardView.findViewById<LinearLayout>(R.id.linearLayoutActividad)

            txtActividad.text = estados.descripcion

            if (observacionesTemporales.containsKey(uniqueKey)) {
                editObservaciones.setText(observacionesTemporales[uniqueKey])
            } else {
                val tObservacion = dbHelper.getObservacionActividadEstado(estados.idEstado)
                if (tObservacion.isNotEmpty()) {
                    editObservaciones.setText(tObservacion)
                } else {
                    editObservaciones.setText(
                        if (estados.descripcion == "Herramientas Usadas") "Kit de Limpieza y Herramienta de mano"
                        else "Equipo en Buen Estado"
                    )
                }
            }

            editObservaciones.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    observacionesTemporales[uniqueKey] = s.toString()
                }
            })

            when (estados.estado) {
                0 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                1 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.success))
                2 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.alert))
            }

            if (dbHelper.getHasPathEstado(estados.idEstado)) {
                btnFoto.backgroundTintList = ColorStateList.valueOf(getColor(R.color.success))
            }

            btnFoto.setOnClickListener {
                if (allPermissionsGranted()) {
                    currentIdEstado = estados.idEstado
                    currentFotoButton = btnFoto
                    dispatchTakePictureIntent2()
                } else {
                    requestPermissions()
                }
            }

            btnAprobar.setOnClickListener {
                val observacion = editObservaciones.text.toString().trim()
                if (observacion.isNotEmpty()) {
                    if (!dbHelper.getHasPathAll(currentIdMantenimiento)) {
                        Toast.makeText(this, "Debe tomar al menos una foto.", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    if (dbHelper.insertObservacionActividadEstado(estados.idEstado, observacion, 1)) {
                        observacionesTemporales.remove(uniqueKey)
                        linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.success))
                    } else {
                        Toast.makeText(this, "Error al guardar.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Ingrese una observación.", Toast.LENGTH_SHORT).show()
                }
            }

            btnRechazar.setOnClickListener {
                val observacion = editObservaciones.text.toString().trim()
                if (observacion.isNotEmpty()) {
                    if (dbHelper.insertObservacionActividadEstado(estados.idEstado, observacion, 2)) {
                        observacionesTemporales.remove(uniqueKey)
                        linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.alert))
                    } else {
                        Toast.makeText(this, "Error al guardar.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Ingrese una observación.", Toast.LENGTH_SHORT).show()
                }
            }
            container.addView(cardView)
        }
    }

    private fun finalizarMantenimiento() {
        if (!dbHelper.validarMantenimientosCompleto(currentIdMantenimiento)) {
            Toast.makeText(this, "Tiene actividades pendientes por gestionar.", Toast.LENGTH_LONG).show()
            return
        }

        val idEmpleadosSeleccionados = checkBoxEmpleadoMap.filterValues { it in empleados }.keys
            .filter { it.isChecked }
            .map { checkBoxEmpleadoMap[it]!!.id }

        if (idEmpleadosSeleccionados.isEmpty()) {
            Toast.makeText(this, "Debe seleccionar al menos 1 Técnico.", Toast.LENGTH_LONG).show()
            return
        }

        dbHelper.insertRelUserMantenimientoBatch(currentIdMantenimiento, idEmpleadosSeleccionados)

        if (dbHelper.insertFinalizarMantenimiento(currentIdMantenimiento)) {
            Toast.makeText(this, "Mantenimiento finalizado exitosamente.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, Nivel1Activity::class.java))
            finish()
        } else {
            Toast.makeText(this, "No se pudo finalizar el mantenimiento.", Toast.LENGTH_LONG).show()
        }
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
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                currentPhotoPath = absolutePath
            }
        } catch (ex: IOException) {
            Log.e("CameraError", "Error al crear archivo de imagen", ex)
            Toast.makeText(this, "No se pudo crear el archivo para la foto.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            if (takePictureIntent.resolveActivity(packageManager) == null) {
                Toast.makeText(this, "No se encontró aplicación de cámara.", Toast.LENGTH_SHORT).show()
                return@also
            }
            createImageFile()?.also {
                val photoURI: Uri = FileProvider.getUriForFile(this, "com.uvrp.itsmantenimientoapp.provider", it)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun dispatchTakePictureIntent2() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            if (takePictureIntent.resolveActivity(packageManager) == null) {
                Toast.makeText(this, "No se encontró aplicación de cámara.", Toast.LENGTH_SHORT).show()
                return@also
            }
            createImageFile()?.also {
                val photoURI: Uri = FileProvider.getUriForFile(this, "com.uvrp.itsmantenimientoapp.provider", it)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_2)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Captura de imagen cancelada.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        currentPhotoPath?.let { photoPath ->
            try {
                val finalBitmap = processImage(photoPath)
                guardarImagenConMarca(finalBitmap, photoPath)

                val isSuccess = when (requestCode) {
                    REQUEST_IMAGE_CAPTURE -> currentActividad?.let { dbHelper.insertarImagen(it.idEstado, photoPath) } ?: false
                    REQUEST_IMAGE_CAPTURE_2 -> dbHelper.insertarImagen2(currentIdEstado, photoPath)
                    else -> false
                }

                if (isSuccess) {
                    currentFotoButton?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.success))
                    Toast.makeText(this, "Imagen guardada.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al guardar imagen en BD.", Toast.LENGTH_SHORT).show()
                }
            } catch(e: Exception) {
                Log.e("ImageProcessingError", "Error al procesar la imagen: ${e.message}", e)
                Toast.makeText(this, "Error al procesar la imagen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processImage(imagePath: String): Bitmap {
        val rotatedBitmap = rotateImageToVertical(imagePath)
        val compressedBitmap = compressImage(rotatedBitmap)
        return agregarMarcaDeAgua(compressedBitmap)
    }

    private fun rotateImageToVertical(imagePath: String): Bitmap {
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun compressImage(bitmap: Bitmap): Bitmap {
        val maxSize = 1024
        val width = bitmap.width
        val height = bitmap.height
        val scale = if (width > height) maxSize.toFloat() / width else maxSize.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    private fun agregarMarcaDeAgua(bitmap: Bitmap): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(5f, 2f, 2f, Color.BLACK)
        }
        val tLocacion = findViewById<TextView>(R.id.tLocacion).text.toString()
        val tTag = findViewById<TextView>(R.id.tTag).text.toString()
        val fechaHoraActual = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val line1 = "$tLocacion - $tTag"
        val line2 = fechaHoraActual
        val x = mutableBitmap.width * 0.05f
        var y = mutableBitmap.height * 0.95f
        canvas.drawText(line1, x, y, paint)
        y += paint.textSize
        canvas.drawText(line2, x, y, paint)
        return mutableBitmap
    }

    private fun guardarImagenConMarca(bitmap: Bitmap, outputPath: String) {
        try {
            FileOutputStream(outputPath).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
        } catch (e: IOException) {
            Log.e("ImageSaveError", "Error al guardar la imagen", e)
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "Permiso de cámara denegado. La función de fotos no estará disponible.", Toast.LENGTH_LONG).show()
            }
        }
    }
}