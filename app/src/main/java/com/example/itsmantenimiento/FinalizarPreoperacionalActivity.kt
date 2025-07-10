package com.uvrp.itsmantenimientoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uvrp.itsmantenimientoapp.iniciarPreoperacional
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.java

class FinalizarPreoperacionalActivity : AppCompatActivity() {

    private lateinit var recyclerFotos: RecyclerView
    private lateinit var btnAgregarFoto: Button
    private lateinit var btnIniciarPreoperacional: Button
    private lateinit var fotoAdapter: FotoAdapter
    private val REQUEST_IMAGE_CAPTURE = 1


    private val fotosList = mutableListOf<File>()
    private var currentPhotoFile: File? = null

    private val REQUEST_CAMERA_PERMISSION = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finalizar_preopercional)

        recyclerFotos = findViewById(R.id.recyclerFotos)
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto)
        btnIniciarPreoperacional = findViewById(R.id.btnIniciarPreoperacional)

        val inputKmFinal = findViewById<EditText>(R.id.inputKmFinal)
        val observacionFinal = findViewById<EditText>(R.id.inputObservacionFinal)
        setupRecyclerView()

        btnAgregarFoto.setOnClickListener {
            verificarPermisos()
        }

        btnIniciarPreoperacional.setOnClickListener {
            if (fotosList.isEmpty()) {
                Toast.makeText(this, "Debes tomar al menos una foto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val kmFinal = inputKmFinal.text.toString().trim()
            val observacion = observacionFinal.text.toString().trim()

            val idVehi = intent.getIntExtra("idVehiculo", -1)
            val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
            val idUsuario = sharedPreferences.getInt("idUser", -1)

            if (kmFinal.isEmpty()) {
                Toast.makeText(this, "Debes ingresar el km final", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val jsonBody = """
                    {
                        "id_vehiculo": $idVehi,
                        "id_usuario": $idUsuario,
                        "fecha_finalizacion": "${
                                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(
                                        Date()
                                    )
                                }",
                        "kmFinal": $kmFinal,
                        "observacionFinal": "${observacion.replace("\"", "\\\"")}"
                    }
                    """.trimIndent()


            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)

            val compressedFiles = mutableListOf<File>()

            val imagenes = fotosList.mapNotNull { file ->
                try {
                    if (!file.exists() || file.length() == 0L) {
                        Log.e("IMAGEN", "Archivo inválido: ${file.absolutePath}")
                        return@mapNotNull null
                    }
                    val compressed = comprimirImagen(file)
                    compressedFiles.add(compressed)

                    val requestFile = compressed.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("imagenes[]", compressed.name, requestFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            RetrofitClient.instance.finalizarPreoperacional(requestBody, imagenes)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        compressedFiles.forEach { it.delete() }

                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@FinalizarPreoperacionalActivity,
                                "✅ Enviado correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                            // 👉 Ir al siguiente Activity
                            val intent = Intent(this@FinalizarPreoperacionalActivity, iniciarPreoperacional::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Opcional: limpia la pila
                            startActivity(intent)
                            finish() // Finaliza el activity actual si ya no lo necesitas
                        } else {
                            // Leer el mensaje de error del backend
                            val errorMessage = try {
                                val errorJson = response.errorBody()?.string()
                                JSONObject(errorJson).optString("error", "⚠️ Error inesperado")
                            } catch (e: Exception) {
                                "⚠️ Error inesperado al procesar la respuesta"
                            }

                            Toast.makeText(
                                this@FinalizarPreoperacionalActivity,
                                errorMessage,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }


                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        compressedFiles.forEach { it.delete() }
                        t.printStackTrace()
                        Toast.makeText(
                            this@FinalizarPreoperacionalActivity,
                            "❌ Falló la conexión: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }


    }

    private fun setupRecyclerView() {
        fotoAdapter = FotoAdapter(fotosList) { file ->
            fotosList.remove(file)
            fotoAdapter.notifyDataSetChanged()
        }

        recyclerFotos.apply {
            layoutManager = LinearLayoutManager(
                this@FinalizarPreoperacionalActivity, LinearLayoutManager.HORIZONTAL, false
            )
            adapter = fotoAdapter
        }
    }

    private fun verificarPermisos() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION
            )
        } else {
            abrirCamara()
        }
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
                e.printStackTrace()
                Toast.makeText(this, "❌ Error creando archivo de imagen", Toast.LENGTH_SHORT).show()
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

    @Deprecated("Usa ActivityResultLauncher en versiones nuevas")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            currentPhotoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    fotosList.add(file)
                    fotoAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "❌ Error al guardar la foto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                abrirCamara()
            } else {
                Toast.makeText(
                    this, "❌ Se requieren permisos de cámara y almacenamiento", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun comprimirImagen(originalFile: File): File {
        // Cargar el bitmap original
        val bitmapOriginal = BitmapFactory.decodeFile(originalFile.absolutePath)

        // Tamaño máximo permitido (por ancho o alto)
        val maxLado = 1024

        // Dimensiones actuales
        val width = bitmapOriginal.width
        val height = bitmapOriginal.height

        // Calcular proporción escalada
        val scale = if (width >= height) {
            maxLado.toFloat() / width
        } else {
            maxLado.toFloat() / height
        }

        // Si ya es más pequeña, no escales
        val nuevoAncho = if (scale < 1) (width * scale).toInt() else width
        val nuevoAlto = if (scale < 1) (height * scale).toInt() else height

        val bitmapEscalado = Bitmap.createScaledBitmap(bitmapOriginal, nuevoAncho, nuevoAlto, true)

        // Crear archivo comprimido
        val compressedFile = File(originalFile.parent, "COMP_${originalFile.name}")
        val outputStream = FileOutputStream(compressedFile)

        bitmapEscalado.compress(Bitmap.CompressFormat.JPEG, 70, outputStream) // calidad 70%
        outputStream.flush()
        outputStream.close()

        return compressedFile
    }


}
