package com.uvrp.itsmantenimientoapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import com.uvrp.itsmantenimientoapp.DatabaseHelper.ActividadBitacora
import com.uvrp.itsmantenimientoapp.DatabaseHelper.Cuadrilla

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
        if (idUsuarioLogueado == -1) {
            Toast.makeText(this, "Error: Usuario no logueado", Toast.LENGTH_SHORT).show()
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

            // Configurar spinner de sentido (PACU, CUPA y PACU / CUPA como en el formulario web)
            val sentidoAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("PACU", "CUPA", "PACU / CUPA")
            )
            sentidoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSentido.adapter = sentidoAdapter

            // Configurar spinner de lado (exactamente como en el formulario web)
            val ladoAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("Der", "Izq", "Eje", "Der / Izq", "Der N1", "Der N2", "Der N3", "Der N4", "Der N5", "Izq N1", "Izq N2", "Izq N3", "Izq N4", "Izq N5", "Der / Eje", "Retorno")
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
            val selectedSentido = spinnerSentido.selectedItemPosition
            val selectedLado = spinnerLado.selectedItemPosition

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
            val sentidoOpciones = listOf("PACU", "CUPA", "PACU / CUPA")
            val sentido = sentidoOpciones[selectedSentido]
            val ladoOpciones = listOf("Der", "Izq", "Eje", "Der / Izq", "Der N1", "Der N2", "Der N3", "Der N4", "Der N5", "Izq N1", "Izq N2", "Izq N3", "Izq N4", "Izq N5", "Der / Eje", "Retorno")
            val lado = ladoOpciones[selectedLado]
            
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
                supervisorResponsable = idUsuarioLogueado
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

        // Validar PR Inicial Km (obligatorio, permite 1-4 dígitos)
        if (etPrInicialKm.text.toString().trim().isEmpty()) {
            tilPrInicialKm.error = "Km obligatorio"
            esValido = false
        } else {
            tilPrInicialKm.error = null
        }

        // Validar PR Inicial m (obligatorio, permite 1-4 dígitos)
        if (etPrInicialM.text.toString().trim().isEmpty()) {
            tilPrInicialM.error = "m obligatorio"
            esValido = false
        } else {
            tilPrInicialM.error = null
        }

        // Validar PR Final Km (obligatorio, permite 1-4 dígitos)
        if (etPrFinalKm.text.toString().trim().isEmpty()) {
            tilPrFinalKm.error = "Km obligatorio"
            esValido = false
        } else {
            tilPrFinalKm.error = null
        }

        // Validar PR Final m (obligatorio, permite 1-4 dígitos)
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

        return esValido
    }
}
