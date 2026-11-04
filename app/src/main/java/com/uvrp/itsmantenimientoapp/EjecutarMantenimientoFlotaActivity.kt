package com.uvrp.itsmantenimientoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.gson.Gson
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EjecutarMantenimientoFlotaActivity : AppCompatActivity() {

    private lateinit var item: ApiService.FlotaPendiente
    private val fotos = linkedMapOf<Int, MutableList<File>>()
    private val grupos = linkedMapOf<Int, RadioGroup>()
    private val notas = linkedMapOf<Int, EditText>()
    private val conteos = linkedMapOf<Int, TextView>()
    private var actividadFoto: Int = 0
    private var fotoActual: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ejecutar_mantenimiento_flota)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_home)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        item = Gson().fromJson(intent.getStringExtra("item"), ApiService.FlotaPendiente::class.java)
        supportActionBar?.title = item.placa ?: "Mantenimiento"
        findViewById<TextView>(R.id.tituloMantenimiento).text = item.nombre ?: "Mantenimiento"
        val contenedor = findViewById<LinearLayout>(R.id.contenedorActividades)
        item.actividades.orEmpty().forEach { actividad ->
            fotos[actividad.id] = mutableListOf()
            contenedor.addView(TextView(this).apply {
                text = actividad.descripcion + if (actividad.requiereFoto) " (foto)" else ""
                textSize = 16f
                setPadding(0, 24, 0, 8)
            })
            val grupo = RadioGroup(this)
            val cumple = RadioButton(this).apply { text = "Cumple"; id = android.view.View.generateViewId() }
            val noCumple = RadioButton(this).apply { text = "No cumple"; id = android.view.View.generateViewId() }
            grupo.addView(cumple)
            grupo.addView(noCumple)
            contenedor.addView(grupo)
            grupos[actividad.id] = grupo
            val nota = EditText(this).apply { hint = "Observación de la actividad" }
            contenedor.addView(nota)
            notas[actividad.id] = nota
            val conteo = TextView(this).apply { text = "Fotos: 0" }
            contenedor.addView(conteo)
            conteos[actividad.id] = conteo
            if (actividad.requiereFoto) {
                contenedor.addView(Button(this).apply {
                    text = "Tomar foto"
                    setOnClickListener { abrirCamara(actividad.id) }
                })
            }
        }
        fotos[0] = mutableListOf()
        findViewById<Button>(R.id.btnFotoGeneral).setOnClickListener { abrirCamara(0) }
        findViewById<Button>(R.id.btnEnviarMantenimiento).setOnClickListener { enviar() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun abrirCamara(idActividad: Int) {
        actividadFoto = idActividad
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 41)
            return
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val nombre = "FLOTA_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val archivo = File.createTempFile(nombre, ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        fotoActual = archivo
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", archivo)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, 42)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            abrirCamara(actividadFoto)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            fotoActual?.let { archivo ->
                if (archivo.exists() && archivo.length() > 0) {
                    fotos.getOrPut(actividadFoto) { mutableListOf() }.add(archivo)
                    if (actividadFoto == 0) {
                        findViewById<TextView>(R.id.conteoFotosGenerales).text = "Fotos generales: ${fotos[0]?.size ?: 0}"
                    } else {
                        conteos[actividadFoto]?.text = "Fotos: ${fotos[actividadFoto]?.size ?: 0}"
                    }
                }
            }
        }
    }

    private fun enviar() {
        val items = JSONArray()
        item.actividades.orEmpty().forEach { actividad ->
            val grupo = grupos[actividad.id]
            val elegido = grupo?.checkedRadioButtonId ?: -1
            if (elegido == -1) {
                Toast.makeText(this, "Marque la actividad: ${actividad.descripcion}", Toast.LENGTH_LONG).show()
                return
            }
            val resultado = if ((grupo?.findViewById<RadioButton>(elegido)?.text ?: "") == "Cumple") "cumple" else "no_cumple"
            if (actividad.requiereFoto && fotos[actividad.id].isNullOrEmpty()) {
                Toast.makeText(this, "Falta la foto de: ${actividad.descripcion}", Toast.LENGTH_LONG).show()
                return
            }
            items.put(JSONObject().apply {
                put("id_actividad", actividad.id)
                put("resultado", resultado)
                put("observacion", notas[actividad.id]?.text?.toString()?.trim().orEmpty())
            })
        }
        val archivos = mutableListOf<Pair<Int, File>>()
        fotos.forEach { (idActividad, lista) -> lista.forEach { archivos.add(idActividad to it) } }
        if (archivos.isEmpty()) {
            Toast.makeText(this, "Agregue al menos una foto de evidencia", Toast.LENGTH_LONG).show()
            return
        }
        val idUsuario = getSharedPreferences("Sesion", MODE_PRIVATE).getInt("idUser", -1)
        val json = JSONObject().apply {
            if (item.id > 0) {
                put("id_ciclo", item.id)
            } else {
                put("id_tipo", item.idTipo)
                put("id_vehiculo", item.idVehiculo)
            }
            put("id_usuario", idUsuario)
            put("observacion", findViewById<EditText>(R.id.observacionGeneral).text.toString().trim())
            put("items", items)
            put("foto_actividades", JSONArray(archivos.map { it.first }))
        }
        val partes = archivos.map { (_, archivo) ->
            val comprimida = comprimir(archivo)
            val body = comprimida.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("imagenes[]", comprimida.name, body)
        }
        val jsonBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        findViewById<Button>(R.id.btnEnviarMantenimiento).isEnabled = false
        RetrofitClient.instance.ejecutarMantenimientoFlota(jsonBody, partes)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EjecutarMantenimientoFlotaActivity, "Mantenimiento registrado", Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK, Intent().putExtra("id_ciclo", item.id))
                        finish()
                    } else {
                        findViewById<Button>(R.id.btnEnviarMantenimiento).isEnabled = true
                        val error = try {
                            JSONObject(response.errorBody()?.string() ?: "{}").optString("error", "No se pudo guardar")
                        } catch (_: Exception) {
                            "No se pudo guardar"
                        }
                        Toast.makeText(this@EjecutarMantenimientoFlotaActivity, error, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    findViewById<Button>(R.id.btnEnviarMantenimiento).isEnabled = true
                    Toast.makeText(this@EjecutarMantenimientoFlotaActivity, t.message, Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun comprimir(original: File): File {
        val bitmap = BitmapFactory.decodeFile(original.absolutePath) ?: return original
        val escala = 1024f / maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
        val destino = if (escala < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true) else bitmap
        val salida = File(original.parent, "COMP_${original.name}")
        FileOutputStream(salida).use { destino.compress(Bitmap.CompressFormat.JPEG, 70, it) }
        return salida
    }
}
