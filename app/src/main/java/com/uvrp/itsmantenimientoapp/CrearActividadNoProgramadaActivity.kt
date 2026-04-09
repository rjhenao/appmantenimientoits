package com.uvrp.itsmantenimientoapp



import android.content.Intent

import android.os.Bundle

import android.util.Log

import android.util.TypedValue

import android.view.View

import android.text.Editable

import android.text.TextWatcher

import android.widget.ArrayAdapter

import android.widget.AutoCompleteTextView

import android.widget.EditText

import android.widget.Spinner

import android.widget.TextView

import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.button.MaterialButton

import com.google.android.material.textfield.TextInputLayout

import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper

import com.uvrp.itsmantenimientoapp.DatabaseHelper.ActividadBitacora

import com.uvrp.itsmantenimientoapp.DatabaseHelper.Cuadrilla



/**

 * Crea la fila en [programar_actividades_bitacora] (Estado = NP) **sin** fotos.

 * La evidencia se registra después en [RegistrarMantenimientoBitacora] al pulsar «Registrar actividad».

 */

class CrearActividadNoProgramadaActivity : AppCompatActivity() {



    private lateinit var dbHelper: DatabaseHelper

    private lateinit var actividadesList: List<ActividadBitacora>

    private lateinit var cuadrillasList: List<Cuadrilla>

    private var idBitacora: Int = -1

    private var idUsuarioLogueado: Int = -1



    private lateinit var tilActividad: TextInputLayout

    private lateinit var autoCompleteActividad: AutoCompleteTextView

    /** Actividad elegida desde la lista (evita ambigüedad si el usuario edita el texto a mano). */
    private var actividadSeleccionada: ActividadBitacora? = null

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



        idBitacora = intent.getIntExtra("id_bitacora", -1)

        if (idBitacora == -1) {

            Toast.makeText(this, "Error: No se especificó la bitácora", Toast.LENGTH_SHORT).show()

            finish()

            return

        }



        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)

        idUsuarioLogueado = sharedPreferences.getInt("idUser", -1)

        val idRol = sharedPreferences.getInt("idRol", -1)

        if (idUsuarioLogueado == -1) {

            Toast.makeText(this, "Error: Usuario no logueado", Toast.LENGTH_SHORT).show()

            finish()

            return

        }



        if (idRol != 1 && idRol != 5 && idRol != 6) {

            Toast.makeText(this, "No tienes permisos para crear actividades no programadas.", Toast.LENGTH_LONG).show()

            finish()

            return

        }



        dbHelper = DatabaseHelper(this)

        initViews()



        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerLayout)

        val navView = findViewById<com.google.android.material.navigation.NavigationView>(R.id.navView)

        HeaderHelper.setupHeader(this, drawerLayout, navView)

        supportActionBar?.title = "Crear Actividad No Programada"



        setupSpinners()



        btnGuardarActividad.setOnClickListener {

            guardarActividad()

        }

    }



    private fun initViews() {

        tilActividad = findViewById(R.id.tilActividad)

        autoCompleteActividad = findViewById(R.id.autoCompleteActividad)

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

            actividadesList = dbHelper.obtenerActividadesBitacoras()

            if (actividadesList.isEmpty()) {

                Toast.makeText(this, "No hay actividades disponibles", Toast.LENGTH_SHORT).show()

                return

            }



            cuadrillasList = dbHelper.obtenerCuadrillas()

            if (cuadrillasList.isEmpty()) {

                Toast.makeText(this, "No hay cuadrillas disponibles", Toast.LENGTH_SHORT).show()

                return

            }



            val actividadesDropdownAdapter = ActividadBitacoraDropdownAdapter(this, actividadesList)

            autoCompleteActividad.setAdapter(actividadesDropdownAdapter)

            autoCompleteActividad.threshold = 0

            autoCompleteActividad.dropDownHeight = (resources.displayMetrics.heightPixels * 0.45).toInt()

            val dm = resources.displayMetrics

            val anchoPantalla = dm.widthPixels

            // MATCH_PARENT no siempre fuerza el ancho real en todos los fabricantes; usar píxeles explícitos.
            autoCompleteActividad.dropDownWidth = anchoPantalla

            val margenTexto = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, dm).toInt()

            actividadesDropdownAdapter.setAnchoDisponibleParaTexto((anchoPantalla - margenTexto * 2).coerceAtLeast(160))

            autoCompleteActividad.post {

                autoCompleteActividad.dropDownWidth = resources.displayMetrics.widthPixels

            }

            autoCompleteActividad.setOnItemClickListener { parent, _, position, _ ->

                val adapt = parent.adapter as ActividadBitacoraDropdownAdapter

                actividadSeleccionada = adapt.getItem(position)

                tvUnidadMedida.text = actividadSeleccionada?.tipoUnidad ?: "--"

                Log.d("ACTIVIDAD_NO_PROGRAMADA", "Unidad: ${actividadSeleccionada?.tipoUnidad}")

            }

            autoCompleteActividad.setOnClickListener { autoCompleteActividad.showDropDown() }

            autoCompleteActividad.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {

                    val t = s?.toString()?.trim().orEmpty()

                    if (actividadSeleccionada != null && actividadSeleccionada?.descripcion != t) {

                        actividadSeleccionada = null

                        tvUnidadMedida.text = "--"

                    }

                }

            })



            val cuadrillasAdapter = ArrayAdapter(

                this,

                android.R.layout.simple_spinner_item,

                listOf("Seleccione una cuadrilla") + cuadrillasList.map { it.nombre }

            )

            cuadrillasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerCuadrilla.adapter = cuadrillasAdapter



            val ufAdapter = ArrayAdapter(

                this,

                android.R.layout.simple_spinner_item,

                listOf("Seleccione...", "UF 1", "UF 2", "UF 3", "UF 4", "UF 5", "UF 6")

            )

            ufAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerUF.adapter = ufAdapter



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

            if (!validarCampos()) {

                return

            }



            val selectedCuadrilla = spinnerCuadrilla.selectedItemPosition - 1

            val selectedUF = spinnerUF.selectedItemPosition



            if (selectedCuadrilla < 0) {

                Toast.makeText(this, "Por favor seleccione cuadrilla", Toast.LENGTH_SHORT).show()

                return

            }



            if (selectedUF == 0) {

                Toast.makeText(this, "Por favor seleccione UF", Toast.LENGTH_SHORT).show()

                return

            }



            val textoAct = autoCompleteActividad.text.toString().trim()

            val actividad = actividadSeleccionada

                ?: actividadesList.firstOrNull {

                    it.descripcion.trim().equals(textoAct, ignoreCase = true)

                }

            if (actividad == null) {

                tilActividad.error = "Seleccione una actividad de la lista o escriba el texto exacto"

                Toast.makeText(

                    this,

                    "Elija una actividad de la lista desplegable (texto largo: use búsqueda con 3+ letras)",

                    Toast.LENGTH_LONG

                ).show()

                return

            }

            tilActividad.error = null

            val cuadrilla = cuadrillasList[selectedCuadrilla]

            val uf = selectedUF

            val sentido = spinnerSentido.selectedItem?.toString().orEmpty()

            val lado = spinnerLado.selectedItem?.toString().orEmpty()



            if (sentido.isBlank() || lado.isBlank()) {

                Toast.makeText(this, "Por favor seleccione Sentido y Lado", Toast.LENGTH_SHORT).show()

                return

            }



            val prInicialKm = etPrInicialKm.text.toString().trim()

            val prInicialM = etPrInicialM.text.toString().trim()

            val prInicial = "$prInicialKm+$prInicialM"



            val prFinalKm = etPrFinalKm.text.toString().trim()

            val prFinalM = etPrFinalM.text.toString().trim()

            val prFinal = "$prFinalKm+$prFinalM"



            val cantidad = etCantidad.text.toString().trim().toDoubleOrNull() ?: 0.0

            val observacion = etObservacion.text.toString().trim()



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

                fotos = emptyList()

            )



            if (resultado > 0) {

                Toast.makeText(

                    this,

                    "Actividad creada. Registra la evidencia al pulsar «Registrar actividad» en el listado.",

                    Toast.LENGTH_LONG

                ).show()

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



        if (etPrInicialKm.text.toString().trim().isEmpty()) {

            tilPrInicialKm.error = "Km obligatorio"

            esValido = false

        } else {

            tilPrInicialKm.error = null

        }



        if (etPrInicialM.text.toString().trim().isEmpty()) {

            tilPrInicialM.error = "m obligatorio"

            esValido = false

        } else {

            tilPrInicialM.error = null

        }



        if (etPrFinalKm.text.toString().trim().isEmpty()) {

            tilPrFinalKm.error = "Km obligatorio"

            esValido = false

        } else {

            tilPrFinalKm.error = null

        }



        if (etPrFinalM.text.toString().trim().isEmpty()) {

            tilPrFinalM.error = "m obligatorio"

            esValido = false

        } else {

            tilPrFinalM.error = null

        }



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


