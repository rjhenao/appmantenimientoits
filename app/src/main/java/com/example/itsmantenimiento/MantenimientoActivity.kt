package com.example.itsmantenimiento

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
    private var currentActividad: Actividad? = null // Variable temporal para almacenar la actividad
    private var currentEstado: ActividadEstado? = null // Variable temporal para almacenar la actividad
    private var currentFotoButton: Button? = null
    private var currentIdEstado = 1
    private var currentIdMantenimiento = 0

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mantenimiento)

        // Verificar y solicitar permisos en tiempo de ejecución
        if (!allPermissionsGranted()) {
            requestPermissions()
        }

        val idMantenimiento = intent.getIntExtra("idmantenimiento", -1)
        currentIdMantenimiento = idMantenimiento
        val actividades = intent.getParcelableArrayListExtra<Actividad>("actividades") ?: arrayListOf()
        val estadomantenimientos = intent.getParcelableArrayListExtra<Actividad>("estadosmantenimientos") ?: arrayListOf()
        val dbHelper = DatabaseHelper(this)
        empleados = dbHelper.getEmpleados()

        Log.d("ITSDebug222", "$empleados")
        setupCheckboxes()

        // Obtener los valores de las columnas
        val column1f1 = intent.getStringExtra("column1f1")
        val column2f1 = intent.getStringExtra("column2f1")
        val column3f1 = intent.getStringExtra("column3f1")
        val column1f2 = intent.getStringExtra("column1f2")
        val column2f2 = intent.getStringExtra("column2f2")
        val column3f2 = intent.getStringExtra("column3f2")
        val column1f3 = intent.getStringExtra("column1f3")
        val column2f3 = intent.getStringExtra("column2f3")

        findViewById<TextView>(R.id.tLocacion).text = column1f1
        findViewById<TextView>(R.id.tSistema).text = column2f1
        findViewById<TextView>(R.id.tSubsistema).text = column3f1
        findViewById<TextView>(R.id.tTipo).text = column1f2
        findViewById<TextView>(R.id.tUf).text = column2f2
        findViewById<TextView>(R.id.tTag).text = column3f2
        findViewById<TextView>(R.id.tPeriodicidad).text = column1f3
        findViewById<TextView>(R.id.tFechaMantenimiento).text = column2f3

        Log.d("MantenimientoActivity", "📌 ID Mantenimiento: $idMantenimiento")
        Log.d("MantenimientoActivity", "📋 Actividades recibidas: ${actividades.size}")

        val container = findViewById<LinearLayout>(R.id.actividadesContainer)

        container.removeAllViews()


        actividades.forEach { actividad ->
            val cardView = layoutInflater.inflate(R.layout.item_actividad, container, false) as CardView
            cardView.findViewById<TextView>(R.id.txtActividad).text = actividad.descripcion

            val editObservaciones = cardView.findViewById<EditText>(R.id.editObservaciones)
            val btnFoto = cardView.findViewById<Button>(R.id.btnFoto)
            val btnAprobar = cardView.findViewById<Button>(R.id.btnAprobar)
            val btnRechazar = cardView.findViewById<Button>(R.id.btnRechazar)
            val linearLayout = cardView.findViewById<LinearLayout>(R.id.linearLayoutActividad)
            val btnFinalizarMantenimiento = findViewById<Button>(R.id.btnFinalizar)

            when (actividad.estado) {
                0 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                1 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.success))
                2 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.alert))
            }

            val tObservacion = dbHelper.getObservacionActividad(actividad.idEstado)
            Log.d("jjdd", tObservacion)

            if (tObservacion.isNotEmpty()) {
                editObservaciones.setText(tObservacion)
            } else {
                editObservaciones.setText("Ok")
            }

            var isSuccessPath = dbHelper.getHasPath(actividad.idEstado)

            if (isSuccessPath) {
                btnFoto.backgroundTintList = ColorStateList.valueOf(getColor(R.color.success))
            }

            btnFinalizarMantenimiento.setOnClickListener {
                val dbHelper = DatabaseHelper(this)

                // Verificar si el mantenimiento puede finalizarse
                val valEstadosMantenimiento = dbHelper.validarMantenimientosCompleto(currentIdMantenimiento)
                if (!valEstadosMantenimiento) {
                    Toast.makeText(
                        this,
                        "No se pudo finalizar el mantenimiento. Tiene actividades pendientes por gestionar.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                // Recopilar los IDs de los empleados seleccionados
                val idEmpleadosSeleccionados = checkBoxEmpleadoMap.filter { (checkBox, _) ->
                    checkBox.isChecked
                }.map { (_, empleado) ->
                    empleado.id
                }
                Log.d("jdjdjdj", "$idEmpleadosSeleccionados")

                // Insertar todos los empleados seleccionados en una sola transacción
                if (idEmpleadosSeleccionados.isNotEmpty()) {
                    val isSuccess = dbHelper.insertRelUserMantenimientoBatch(currentIdMantenimiento, idEmpleadosSeleccionados)
                    if (!isSuccess) {
                        Toast.makeText(
                            this,
                            "Error al insertar los empleados seleccionados en rel_user_mantenimiento",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Debe seleccionar al menos 1 Técnico.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                // Finalizar el mantenimiento
                val isSuccessMantenimiento = dbHelper.insertFinalizarMantenimiento(currentIdMantenimiento)
                if (!isSuccessMantenimiento) {
                    Toast.makeText(
                        this,
                        "No se pudo finalizar el mantenimiento, inténtelo nuevamente o comuníquese con un administrador.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val intent = Intent(this, Nivel1Activity::class.java)
                    startActivity(intent)
                    finish()
                    Toast.makeText(
                        this,
                        "Mantenimiento finalizado exitosamente.",
                        Toast.LENGTH_LONG
                    ).show()

                }
            }

            btnFoto.setOnClickListener {
                if (allPermissionsGranted()) {
                    currentActividad = actividad // Almacenar la actividad temporalmente
                    currentFotoButton = btnFoto
                    dispatchTakePictureIntent()
                } else {
                    requestPermissions()
                }
            }

            btnAprobar.setOnClickListener {
                val observacion = editObservaciones.text.toString().trim()
                if (observacion.isEmpty()) {
                    Toast.makeText(this, "Ingrese una observación", Toast.LENGTH_LONG).show()
                } else {
                    var uAprobado = dbHelper.insertObservacionActividad(actividad.idEstado, observacion, 1)

                    if (uAprobado) {
                        linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.success))
                    } else {
                        Toast.makeText(
                            this,
                            "Se produjo une error, intentelo nuevamente o contacte al administrador",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            btnRechazar.setOnClickListener {
                val observacion = editObservaciones.text.toString().trim()
                if (observacion.isEmpty()) {
                    Toast.makeText(this, "Ingrese una observación", Toast.LENGTH_LONG).show()
                } else {
                    var uAprobado = dbHelper.insertObservacionActividad(actividad.idEstado, observacion, 2)

                    if (uAprobado) {
                        linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.alert))
                    } else {
                        Toast.makeText(
                            this,
                            "Se produjo une error, intentelo nuevamente o contacte al administrador",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            container.addView(cardView)
        }

        estadomantenimientos.forEach { estados ->
            val cardView = layoutInflater.inflate(R.layout.item_actividad, container, false) as CardView
            cardView.findViewById<TextView>(R.id.txtActividad).text = estados.descripcion

            val linearLayout = cardView.findViewById<LinearLayout>(R.id.linearLayoutActividad)

            val editObservaciones1 = cardView.findViewById<EditText>(R.id.editObservaciones)
            val btnFoto1 = cardView.findViewById<Button>(R.id.btnFoto)
            val btnAprobar1 = cardView.findViewById<Button>(R.id.btnAprobar)
            val btnRechazar1 = cardView.findViewById<Button>(R.id.btnRechazar)

            val tObservacion1 = dbHelper.getObservacionActividadEstado(estados.idEstado)
            Log.d("jjdd", tObservacion1)

            if (tObservacion1.isNotEmpty()) {
                editObservaciones1.setText(tObservacion1)
            } else {
                if (estados.descripcion == "Herramientas Usadas") {
                    editObservaciones1.setText("Kit de Limpieza y Herramienta de mano")
                } else {
                    editObservaciones1.setText("Equipo en Buen Estado")
                }
            }

            when (estados.estado) {
                0 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                1 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.success))
                2 -> linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.alert))
            }

            var validarPathEstado = dbHelper.getHasPathEstado(estados.idEstado)

            if (validarPathEstado) {
                btnFoto1.backgroundTintList = ColorStateList.valueOf(getColor(R.color.success))
            }

            btnFoto1.setOnClickListener {
                if (allPermissionsGranted()) {
                    currentIdEstado = estados.idEstado // Almacenar la actividad temporalmente
                    currentFotoButton = btnFoto1
                    dispatchTakePictureIntent2()
                } else {
                    requestPermissions()
                }
            }

            btnAprobar1.setOnClickListener {
                val observacion = editObservaciones1.text.toString().trim()

                if (observacion.isEmpty()) {
                    Toast.makeText(this, "Ingrese una observación", Toast.LENGTH_LONG).show()
                } else {
                    val validarObservacion = dbHelper.getHasPathAll(currentIdMantenimiento)
                    Log.d("mxsxsxs", "$validarObservacion")
                    if (!validarObservacion) {
                        Toast.makeText(this, "Debe tomar al menos una Foto", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    var uAprobado = dbHelper.insertObservacionActividadEstado(estados.idEstado, observacion, 1)
                    if (uAprobado) {
                        linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.success))
                    } else {
                        Toast.makeText(
                            this,
                            "Se produjo une error, intentelo nuevamente o contacte al administrador",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            btnRechazar1.setOnClickListener {
                val observacion = editObservaciones1.text.toString().trim()
                if (observacion.isEmpty()) {
                    Toast.makeText(this, "Ingrese una observación", Toast.LENGTH_LONG).show()
                } else {
                    var uAprobado = dbHelper.insertObservacionActividadEstado(estados.idEstado, observacion, 2)
                    if (uAprobado) {
                        linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.alert))
                    } else {
                        Toast.makeText(
                            this,
                            "Se produjo une error, intentelo nuevamente o contacte al administrador",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            container.addView(cardView)
        }
    }

    private val checkBoxEmpleadoMap = mutableMapOf<CheckBox, Empleado>()

    private fun setupCheckboxes() {
        val checkboxContainer = findViewById<GridLayout>(R.id.checkboxContainer)
        checkboxContainer.removeAllViews()

        Log.d("setupCheckboxes", "Empleados: ${empleados.size}")

        empleados.forEach { empleado ->
            val checkBox = CheckBox(this)
            checkBox.text = empleado.nombre
            val params = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED)
            }
            checkBox.layoutParams = params
            checkboxContainer.addView(checkBox)

            // Almacenar la relación CheckBox -> Empleado
            checkBoxEmpleadoMap[checkBox] = empleado
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", // Prefijo
            ".jpg", // Sufijo
            storageDir // Directorio
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.e("CameraError", "Error al crear el archivo de la imagen", ex)
                    Toast.makeText(this, "Error al crear el archivo de la imagen", Toast.LENGTH_SHORT).show()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            } ?: run {
                Log.e("CameraError", "No se encontró una aplicación de cámara")
                Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dispatchTakePictureIntent2() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("MantenimientoActivity", "Error al crear archivo de imagen", ex)
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_2)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                if (resultCode == RESULT_OK) {
                    currentPhotoPath?.let { photoPath ->
                        // Rotar la imagen a vertical
                        val rotatedBitmap = rotateImageToVertical(photoPath)
                        // Comprimir la imagen
                        val compressedBitmap = compressImage(rotatedBitmap)
                        // Agregar marca de agua
                        val bitmapConMarca = agregarMarcaDeAgua(compressedBitmap)
                        // Guardar la imagen final
                        guardarImagenConMarca(bitmapConMarca, photoPath)

                        currentActividad?.let { actividad ->
                            val dbHelper = DatabaseHelper(this)
                            val isSuccess = dbHelper.insertarImagen(actividad.idEstado, photoPath)

                            if (isSuccess) {
                                currentFotoButton?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.success))
                            }

                            Toast.makeText(
                                this,
                                if (isSuccess) "Imagen guardada correctamente"
                                else "Error al guardar la imagen en la base de datos",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Captura de imagen cancelada", Toast.LENGTH_SHORT).show()
                }
            }

            REQUEST_IMAGE_CAPTURE_2 -> {
                if (resultCode == RESULT_OK) {
                    Log.d("kpppp", "jkakaka")
                    currentPhotoPath?.let { photoPath ->
                        // Rotar la imagen a vertical
                        val rotatedBitmap = rotateImageToVertical(photoPath)
                        // Comprimir la imagen
                        val compressedBitmap = compressImage(rotatedBitmap)
                        // Agregar marca de agua
                        val bitmapConMarca = agregarMarcaDeAgua(compressedBitmap)
                        // Guardar la imagen final
                        guardarImagenConMarca(bitmapConMarca, photoPath)

                        val dbHelper = DatabaseHelper(this)
                        val isSuccess = dbHelper.insertarImagen2(currentIdEstado, photoPath)

                        if (isSuccess) {
                            currentFotoButton?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.success))
                        }

                        Toast.makeText(
                            this,
                            if (isSuccess) "Imagen guardada correctamente"
                            else "Error al guardar la imagen en la base de datos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Captura de imagen cancelada", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
        val maxSize = 1024 // Tamaño máximo en píxeles
        val width = bitmap.width
        val height = bitmap.height

        val scale = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    private fun agregarMarcaDeAgua(bitmap: Bitmap): Bitmap {
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Configuración del texto
        paint.color = Color.WHITE  // Color del texto
        paint.textSize = 20f       // Tamaño del texto
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // Fuente en negrita
        paint.setShadowLayer(5f, 2f, 2f, Color.BLACK) // Sombra negra para más visibilidad

        // Obtener los valores de tLocacion y tTag
        val tLocacion = findViewById<TextView>(R.id.tLocacion).text.toString()
        val tTag = findViewById<TextView>(R.id.tTag).text.toString()

        // Obtener la fecha y hora actual
        val fechaHoraActual = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        // Construir el texto de la marca de agua (separado en líneas)
        val line1 = "$tLocacion - $tTag"
        val line2 = fechaHoraActual

        // Posición inicial del texto (10% desde la izquierda y 90% desde la parte superior)
        val x = bitmap.width * 0.05f
        var y = bitmap.height * 0.95f

        // Dibujar la primera línea
        canvas.drawText(line1, x, y, paint)

        // Ajustar la posición vertical para la segunda línea
        y += paint.textSize // Mover la posición Y hacia abajo

        // Dibujar la segunda línea
        canvas.drawText(line2, x, y, paint)

        return bitmap
    }

    private fun guardarImagenConMarca(bitmap: Bitmap, outputPath: String) {
        val file = File(outputPath)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream) // Comprimir al 70% de calidad
        outputStream.flush()
        outputStream.close()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permisos no concedidos. Algunas funcionalidades estarán limitadas.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        finish() // Forzar el cierre
    }

    override fun onStop() {
        super.onStop()
        finish() // También válido
    }

}