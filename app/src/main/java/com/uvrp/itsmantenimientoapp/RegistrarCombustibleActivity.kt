package com.uvrp.itsmantenimientoapp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class RegistrarCombustibleActivity : AppCompatActivity() {

    private var idPreoperacional: Int = -1
    private var idVehiculo: Int = -1
    private var idUsuario: Int = -1
    private var placa: String = ""
    private var currentPhotoPath: String? = null
    private val REQUEST_IMAGE_CAPTURE = 1001
    private val REQUEST_CAMERA_PERMISSION = 2001
    private lateinit var dbHelper: DatabaseHelper

    // Views
    private lateinit var inputKmInicial: EditText
    private lateinit var inputCantidadGalones: EditText
    private lateinit var inputValorGalon: EditText
    private lateinit var inputValorTotal: EditText
    private lateinit var inputObservacion: EditText
    private lateinit var btnTomarFoto: Button
    private lateinit var imgFotoTicket: ImageView
    private lateinit var btnRegistrarTanqueo: Button

    // SharedPreferences para persistencia temporal
    private lateinit var prefs: android.content.SharedPreferences

    // Mapa para controlar el formateo en proceso por EditText (evitar loops infinitos)
    private val formatoEnProceso = mutableMapOf<EditText, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_combustible)

        // Obtener datos del Intent
        idPreoperacional = intent.getIntExtra("idPreoperacional", -1)
        idVehiculo = intent.getIntExtra("idVehiculo", -1)
        idUsuario = intent.getIntExtra("idUsuario", -1)
        placa = intent.getStringExtra("placa") ?: ""

        if (idPreoperacional == -1 || idVehiculo == -1 || idUsuario == -1) {
            Toast.makeText(this, "Error: Datos incompletos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)
        prefs = getSharedPreferences("CombustiblePrefs_$idUsuario", MODE_PRIVATE)

        // Inicializar views
        inputKmInicial = findViewById(R.id.inputKmInicial)
        inputCantidadGalones = findViewById(R.id.inputCantidadGalones)
        inputValorGalon = findViewById(R.id.inputValorGalon)
        inputValorTotal = findViewById(R.id.inputValorTotal)
        inputObservacion = findViewById(R.id.inputObservacion)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        imgFotoTicket = findViewById(R.id.imgFotoTicket)
        btnRegistrarTanqueo = findViewById(R.id.btnRegistrarTanqueo)

        // Configurar toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_home)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Registrar Combustible - $placa"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Restaurar datos guardados
        restaurarDatos()

        // Configurar TextWatchers para formatos numéricos
        configurarFormatosNumericos()

        // Configurar listeners
        btnTomarFoto.setOnClickListener {
            verificarPermisosCamara()
        }

        btnRegistrarTanqueo.setOnClickListener {
            validarYRegistrar()
        }

        // Guardar datos en tiempo real
        inputKmInicial.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDato("kmInicial", s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        inputCantidadGalones.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDato("cantidadGalones", s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        inputValorGalon.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDato("valorGalon", s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        inputValorTotal.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDato("valorTotal", s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        inputObservacion.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDato("observacion", s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun configurarFormatosNumericos() {
        // Formato para kilometraje (124.567,89) - con miles y decimales
        configurarFormatoNumericoConMiles(inputKmInicial, true)
        
        // Formato para galones (15.000,87) - con miles y decimales
        configurarFormatoNumericoConMiles(inputCantidadGalones, true)
        
        // Formato para valores (16.400,00) - con miles y decimales
        configurarFormatoNumericoConMiles(inputValorGalon, true)
        configurarFormatoNumericoConMiles(inputValorTotal, true)
    }

    private fun configurarFormatoNumericoConMiles(editText: EditText, permitirDecimales: Boolean) {
        // Usar inputType TEXT para permitir comas desde el teclado
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT
        
        // Configurar KeyListener personalizado para teclado numérico con coma
        editText.keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789,")
        
        val textWatcher = object : TextWatcher {
            private var textoAnterior = ""
            private var cursorAnterior = 0
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                textoAnterior = s?.toString() ?: ""
                cursorAnterior = editText.selectionStart
            }
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // Verificar si ya estamos formateando este campo
                if (formatoEnProceso[editText] == true) return
                
                val textoActual = s?.toString() ?: ""
                
                // Si está vacío, no hacer nada
                if (textoActual.isEmpty()) {
                    formatoEnProceso[editText] = false
                    return
                }
                
                // Separar parte entera y decimal usando la coma como separador decimal
                val tieneComa = textoActual.contains(",")
                val partes = textoActual.split(",", limit = 2)
                
                // Extraer solo los números de la parte entera (eliminar puntos y otros caracteres)
                val parteEnteraSinFormato = partes[0].replace(Regex("[^0-9]"), "")
                
                // Extraer solo números de la parte decimal (máximo 2 dígitos)
                val parteDecimal = if (partes.size > 1 && permitirDecimales) {
                    partes[1].replace(Regex("[^0-9]"), "").take(2)
                } else {
                    ""
                }
                
                // Si no hay parte entera y no hay coma, no formatear aún
                if (parteEnteraSinFormato.isEmpty() && !tieneComa) {
                    formatoEnProceso[editText] = false
                    return
                }
                
                // Si solo hay coma sin parte entera, permitir (usuario puede estar escribiendo 0,XX)
                if (parteEnteraSinFormato.isEmpty() && tieneComa) {
                    formatoEnProceso[editText] = false
                    return
                }
                
                // Formatear parte entera con puntos de miles automáticamente
                val parteEnteraFormateada = if (parteEnteraSinFormato.isNotEmpty()) {
                    formatearConPuntosMiles(parteEnteraSinFormato)
                } else {
                    ""
                }
                
                // Construir el texto formateado final
                val textoFormateado = when {
                    parteDecimal.isNotEmpty() -> "$parteEnteraFormateada,$parteDecimal"
                    tieneComa && parteEnteraFormateada.isNotEmpty() -> "$parteEnteraFormateada,"
                    parteEnteraFormateada.isNotEmpty() -> parteEnteraFormateada
                    else -> ""
                }
                
                // Verificar si necesita actualizar
                if (textoFormateado != textoActual && textoFormateado.isNotEmpty()) {
                    // Marcar que estamos formateando
                    formatoEnProceso[editText] = true
                    
                    // Remover listener temporalmente para evitar loop infinito
                    editText.removeTextChangedListener(this)
                    
                    // Calcular posición del cursor
                    val posicionCursor = calcularPosicionCursor(
                        textoAnterior = textoAnterior,
                        textoNuevo = textoFormateado,
                        cursorAnterior = cursorAnterior
                    )
                    
                    // Actualizar el texto
                    s?.replace(0, s.length, textoFormateado)
                    
                    // Establecer posición del cursor
                    try {
                        val posicionFinal = maxOf(0, minOf(posicionCursor, textoFormateado.length))
                        editText.setSelection(posicionFinal)
                    } catch (e: Exception) {
                        try {
                            editText.setSelection(textoFormateado.length)
                        } catch (e2: Exception) {
                            // Si falla, no establecer selección
                        }
                    }
                    
                    // Volver a agregar el listener
                    editText.addTextChangedListener(this)
                    
                    // Desmarcar que terminamos de formatear
                    editText.post {
                        formatoEnProceso[editText] = false
                    }
                } else {
                    formatoEnProceso[editText] = false
                }
            }
        }
        
        editText.addTextChangedListener(textWatcher)
    }
    
    /**
     * Calcula la posición correcta del cursor después de formatear el texto
     */
    private fun calcularPosicionCursor(
        textoAnterior: String,
        textoNuevo: String,
        cursorAnterior: Int
    ): Int {
        // Si el cursor estaba al final, mantenerlo al final
        if (cursorAnterior >= textoAnterior.length) {
            return textoNuevo.length
        }
        
        // Contar dígitos antes del cursor en el texto anterior
        val textoAntesCursor = textoAnterior.substring(0, minOf(cursorAnterior, textoAnterior.length))
        val digitosAntesCursor = textoAntesCursor.count { it.isDigit() }
        val hayComa = textoNuevo.contains(",")
        val comaAntesCursor = textoAntesCursor.contains(",")
        
        // Si el cursor estaba después de la coma (parte decimal)
        if (comaAntesCursor && hayComa) {
            val posicionComa = textoNuevo.indexOf(",")
            val digitosDecimalesAntesCursor = textoAntesCursor.substringAfter(",").count { it.isDigit() }
            return minOf(posicionComa + 1 + digitosDecimalesAntesCursor, textoNuevo.length)
        }
        
        // El cursor está en la parte entera
        // Encontrar la posición después de N dígitos en el texto nuevo
        var digitosEncontrados = 0
        for (i in textoNuevo.indices) {
            if (textoNuevo[i].isDigit()) {
                digitosEncontrados++
                if (digitosEncontrados >= digitosAntesCursor) {
                    return i + 1
                }
            }
        }
        
        return textoNuevo.length
    }
    
    // Función auxiliar para formatear números con puntos de miles
    private fun formatearConPuntosMiles(numero: String): String {
        if (numero.isEmpty()) return ""
        
        val reverso = numero.reversed()
        val resultado = StringBuilder()
        
        for (i in reverso.indices) {
            if (i > 0 && i % 3 == 0) {
                resultado.append(".")
            }
            resultado.append(reverso[i])
        }
        
        return resultado.toString().reversed()
    }


    private fun restaurarDatos() {
        inputKmInicial.setText(prefs.getString("kmInicial", ""))
        inputCantidadGalones.setText(prefs.getString("cantidadGalones", ""))
        inputValorGalon.setText(prefs.getString("valorGalon", ""))
        inputValorTotal.setText(prefs.getString("valorTotal", ""))
        inputObservacion.setText(prefs.getString("observacion", ""))

        val fotoPath = prefs.getString("fotoTicket", null)
        if (!fotoPath.isNullOrEmpty() && File(fotoPath).exists()) {
            currentPhotoPath = fotoPath
            mostrarFoto()
        }
    }

    private fun guardarDato(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    private fun validarYRegistrar() {
        // Validar campos obligatorios
        val kmInicialStr = inputKmInicial.text.toString().trim()
        val cantidadGalonesStr = inputCantidadGalones.text.toString().trim()
        val valorGalonStr = inputValorGalon.text.toString().trim()
        val valorTotalStr = inputValorTotal.text.toString().trim()

        if (kmInicialStr.isEmpty()) {
            inputKmInicial.error = "Campo obligatorio"
            inputKmInicial.requestFocus()
            return
        }

        if (cantidadGalonesStr.isEmpty()) {
            inputCantidadGalones.error = "Campo obligatorio"
            inputCantidadGalones.requestFocus()
            return
        }

        if (valorGalonStr.isEmpty()) {
            inputValorGalon.error = "Campo obligatorio"
            inputValorGalon.requestFocus()
            return
        }

        if (valorTotalStr.isEmpty()) {
            inputValorTotal.error = "Campo obligatorio"
            inputValorTotal.requestFocus()
            return
        }

        // Validar formato numérico
        val kmInicial = convertirANumero(kmInicialStr)
        val cantidadGalones = convertirANumero(cantidadGalonesStr)
        val valorGalon = convertirANumero(valorGalonStr)
        val valorTotal = convertirANumero(valorTotalStr)

        if (kmInicial == null || cantidadGalones == null || valorGalon == null || valorTotal == null) {
            Toast.makeText(this, "Error: Verifique que los valores numéricos sean correctos", Toast.LENGTH_SHORT).show()
            return
        }

        // Confirmación antes de registrar
        AlertDialog.Builder(this)
            .setTitle("Confirmar Registro")
            .setMessage("¿Desea registrar el tanqueo de combustible?")
            .setPositiveButton("Sí, Registrar") { _, _ ->
                registrarTanqueo(kmInicial, cantidadGalones, valorGalon, valorTotal)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun convertirANumero(valor: String): Double? {
        return try {
            // Reemplazar punto de miles y convertir coma decimal
            valor.replace(".", "").replace(",", ".").toDouble()
        } catch (e: Exception) {
            null
        }
    }

    private fun registrarTanqueo(
        kmInicial: Double,
        cantidadGalones: Double,
        valorGalon: Double,
        valorTotal: Double
    ) {
        val observacion = inputObservacion.text.toString().trim()
        val fechaTanqueo = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Guardar en BD local
        val id = dbHelper.insertarCombustible(
            idPreoperacional = idPreoperacional,
            idVehiculo = idVehiculo,
            idUsuario = idUsuario,
            kilometrajeInicial = kmInicial,
            cantidadGalones = cantidadGalones,
            valorGalon = valorGalon,
            valorTotal = valorTotal,
            rutaFotoTicket = currentPhotoPath,
            observacion = if (observacion.isEmpty()) null else observacion,
            fechaTanqueo = fechaTanqueo
        )

        if (id > 0) {
            // Limpiar SharedPreferences después de registrar exitosamente
            prefs.edit().clear().apply()

            Toast.makeText(this, "✅ Combustible registrado exitosamente", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "❌ Error al registrar el combustible", Toast.LENGTH_SHORT).show()
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
            abrirCamara()
        }
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                crearArchivoImagen()
            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                null
            }

            photoFile?.let {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "$packageName.provider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun crearArchivoImagen(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_ticket_combustible_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
            guardarDato("fotoTicket", absolutePath)
        }
    }

    private fun mostrarFoto() {
        currentPhotoPath?.let {
            val bitmap = BitmapFactory.decodeFile(it)
            imgFotoTicket.setImageBitmap(bitmap)
            imgFotoTicket.visibility = android.view.View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            mostrarFoto()
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
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}









