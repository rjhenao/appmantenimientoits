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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        cargarFotosDePrefs()

        if (savedInstanceState != null) {
            savedInstanceState.getString("currentPhotoPath")?.let { currentPhotoFile = File(it) }
        }

        btnAgregarFoto.setOnClickListener { verificarPermisos() }

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
                    "fecha_finalizacion": "${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())}",
                    "kmFinal": $kmFinal,
                    "observacionFinal": "${observacion.replace("\"", "\\\"")}"
                }
            """.trimIndent()

            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)
            val compressedFiles = mutableListOf<File>()

            val imagenes = fotosList.mapNotNull { file ->
                try {
                    if (!file.exists() || file.length() == 0L) return@mapNotNull null
                    val compressed = comprimirImagen(file)
                    compressedFiles.add(compressed)
                    val requestFile = compressed.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("imagenes[]", compressed.name, requestFile)
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                    null
                }
            }

            RetrofitClient.instance.finalizarPreoperacional(requestBody, imagenes)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        compressedFiles.forEach { it.delete() }
                        if (response.isSuccessful) {
                            // ✅ Limpieza de fotos
                            fotosList.clear()
                            currentPhotoFile = null
                            fotoAdapter.notifyDataSetChanged()
                            limpiarFotosEnPrefs()

                            Toast.makeText(this@FinalizarPreoperacionalActivity, "✅ Enviado correctamente", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@FinalizarPreoperacionalActivity, iniciarPreoperacional::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        } else {
                            val errorMessage = try {
                                val errorJson = response.errorBody()?.string() ?: "{}"
                                JSONObject(errorJson).optString("error", "⚠️ Error inesperado")
                            } catch (e: Exception) {
                                FirebaseCrashlytics.getInstance().recordException(e)
                                "Error al parsear JSON de error: ${e.message}"
                            }
                            Toast.makeText(this@FinalizarPreoperacionalActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        compressedFiles.forEach { it.delete() }
                        FirebaseCrashlytics.getInstance().recordException(t)
                        Toast.makeText(this@FinalizarPreoperacionalActivity, "❌ Falló la conexión: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun setupRecyclerView() {
        fotoAdapter = FotoAdapter(fotosList) { file ->
            fotosList.remove(file)
            guardarFotosEnPrefs()
            fotoAdapter.notifyDataSetChanged()
        }

        recyclerFotos.apply {
            layoutManager = LinearLayoutManager(this@FinalizarPreoperacionalActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = fotoAdapter
        }
    }

    private fun verificarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
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
                FirebaseCrashlytics.getInstance().recordException(e)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            currentPhotoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    fotosList.add(file)
                    guardarFotosEnPrefs()
                    fotoAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "❌ Error al guardar la foto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            abrirCamara()
        } else {
            Toast.makeText(this, "❌ Se requieren permisos de cámara y almacenamiento", Toast.LENGTH_SHORT).show()
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
        val prefs = getSharedPreferences("FotosPrefs", MODE_PRIVATE)
        val editor = prefs.edit()
        val paths = fotosList.map { it.absolutePath }.toSet()
        editor.putStringSet("fotos_guardadas", paths)
        editor.apply()
    }

    private fun cargarFotosDePrefs() {
        val prefs = getSharedPreferences("FotosPrefs", MODE_PRIVATE)
        val paths = prefs.getStringSet("fotos_guardadas", emptySet())
        fotosList.clear()
        paths?.forEach {
            val file = File(it)
            if (file.exists()) fotosList.add(file)
        }
        fotoAdapter.notifyDataSetChanged()
    }

    private fun limpiarFotosEnPrefs() {
        val prefs = getSharedPreferences("FotosPrefs", MODE_PRIVATE)
        prefs.edit().remove("fotos_guardadas").apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList("fotosPaths", ArrayList(fotosList.map { it.absolutePath }))
        currentPhotoFile?.let { outState.putString("currentPhotoPath", it.absolutePath) }
    }
}
