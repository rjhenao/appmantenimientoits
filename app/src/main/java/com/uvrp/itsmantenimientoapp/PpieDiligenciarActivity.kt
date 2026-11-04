package com.uvrp.itsmantenimientoapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class PpieDiligenciarActivity : AppCompatActivity() {

    private data class LineUi(
        val activity: DatabaseHelper.PpieActividadLocal,
        val root: View,
        val spCorrecto: Spinner,
        val spInc: Spinner,
        val etObs: EditText
    )

    private val lineUis = mutableListOf<LineUi>()
    private lateinit var db: DatabaseHelper
    private var formatId = 0
    private var formatCode = ""
    private val opts = listOf("—" to "", "SI" to "si", "NO" to "no", "N/A" to "na")
    /** Un solo UUID por sesión del formulario (evita duplicados por doble toque). */
    private lateinit var clientUuid: String
    private var enviando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(applicationContext)
        setContentView(R.layout.activity_ppie_diligenciar)
        clientUuid = savedInstanceState?.getString(KEY_CLIENT_UUID) ?: UUID.randomUUID().toString()

        val toolbar = findViewById<Toolbar>(R.id.toolbarPpie)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        formatId = intent.getIntExtra("format_id", 0)
        formatCode = intent.getStringExtra("format_code").orEmpty()
        val title = intent.getStringExtra("format_title").orEmpty()
        findViewById<TextView>(R.id.tvPpieFormatTitle).text = "$formatCode — $title"

        db = DatabaseHelper(this)
        val etDate = findViewById<TextInputEditText>(R.id.etPpieDate)
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        etDate.setText(sdf.format(cal.time))
        etDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                etDate.setText(sdf.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        findViewById<TextView>(R.id.btnPpieLeyenda).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Claves")
                .setMessage(
                    "GRUPO\n1 Preliminar al proceso\n2 Materiales a incorporar\n3 Control de proceso\n4 Control final\n\n" +
                        "D.C: Dígito de control\nN°: Orden de la inspección\n\n" +
                        "PC Punto Crítico · PE Espera · PP Parada\n\n" +
                        "INC: Incumplimiento / NC No Conformidad\n(Si es fallo inmediato, márcalo y explica en Observaciones)."
                )
                .setPositiveButton("Entendido", null)
                .show()
        }

        val container = findViewById<LinearLayout>(R.id.containerPpieLines)
        val inflater = LayoutInflater.from(this)
        for (act in db.obtenerPpieActividades(formatId)) {
            val row = inflater.inflate(R.layout.item_ppie_line, container, false)
            row.findViewById<TextView>(R.id.tvPpieChips).text =
                "G${act.grupo.orEmpty()} · DC ${act.dc.orEmpty()} · PC ${act.pc.orEmpty()}"
            row.findViewById<TextView>(R.id.tvPpieItem).text = "N° ${act.itemNumber.orEmpty()}"
            val desc = row.findViewById<TextView>(R.id.tvPpieDesc)
            desc.text = act.description.orEmpty()
            desc.setOnClickListener {
                desc.maxLines = if (desc.maxLines == 2) 20 else 2
            }
            val spC = row.findViewById<Spinner>(R.id.spPpieCorrecto)
            val spI = row.findViewById<Spinner>(R.id.spPpieInc)
            val etObs = row.findViewById<EditText>(R.id.etPpieObs)
            val labels = opts.map { it.first }
            spC.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
            spI.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
            spC.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    when (opts[pos].second) {
                        "si" -> {
                            spI.setSelection(2) // NO
                            spI.isEnabled = false
                            etObs.visibility = View.GONE
                        }
                        "no" -> {
                            spI.setSelection(1) // SI
                            spI.isEnabled = false
                            etObs.visibility = View.VISIBLE
                        }
                        else -> {
                            spI.isEnabled = true
                            etObs.visibility = if (opts[spI.selectedItemPosition].second == "si") View.VISIBLE else View.GONE
                        }
                    }
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            })
            spI.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (spI.isEnabled) {
                        etObs.visibility = if (opts[pos].second == "si") View.VISIBLE else View.GONE
                    }
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            })
            container.addView(row)
            lineUis.add(LineUi(act, row, spC, spI, etObs))
        }

        findViewById<MaterialButton>(R.id.btnPpieEnviar).setOnClickListener { enviar() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CLIENT_UUID, clientUuid)
    }

    private fun enviar() {
        if (enviando) return
        val btn = findViewById<MaterialButton>(R.id.btnPpieEnviar)
        val location = findViewById<TextInputEditText>(R.id.etPpieLocation).text?.toString()?.trim().orEmpty()
        val date = findViewById<TextInputEditText>(R.id.etPpieDate).text?.toString()?.trim().orEmpty()
        if (location.isEmpty()) {
            Toast.makeText(this, "Indique la locación", Toast.LENGTH_SHORT).show()
            return
        }
        if (date.isEmpty()) {
            Toast.makeText(this, "Indique la fecha", Toast.LENGTH_SHORT).show()
            return
        }
        val lines = mutableListOf<ApiService.PpieLineSubmit>()
        for (ui in lineUis) {
            val c = opts[ui.spCorrecto.selectedItemPosition].second
            var inc = opts[ui.spInc.selectedItemPosition].second
            if (c.isEmpty() || inc.isEmpty()) {
                Toast.makeText(this, "Complete Correcto e INC en todas las filas", Toast.LENGTH_LONG).show()
                return
            }
            if (c == "si") inc = "no"
            if (c == "no") inc = "si"
            lines.add(
                ApiService.PpieLineSubmit(
                    activityId = ui.activity.id,
                    correcto = c,
                    incHoy = inc,
                    observaciones = ui.etObs.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }

        val uuid = clientUuid
        val req = ApiService.PpieSubmitRequest(
            clientUuid = uuid,
            formatId = formatId,
            location = location,
            inspectionDate = date,
            lines = lines
        )
        val json = Gson().toJson(req)
        // Solo insertar pendiente si aún no existe este UUID (reintento reusa el mismo)
        val yaPendiente = db.obtenerPpiePendientes().any { it.clientUuid == uuid }
        if (!yaPendiente) {
            db.insertarPpiePendiente(uuid, formatId, formatCode, json)
        }

        enviando = true
        btn.isEnabled = false
        val progress = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        progress.show()

        lifecycleScope.launch {
            var errMsg = "Sin conexión o error. Quedó pendiente en Inicio para reenviar."
            val ok = withContext(Dispatchers.IO) {
                try {
                    val r = RetrofitClient.instance.ppieSubmit(req).execute()
                    val body = r.body()
                    if (r.isSuccessful && body?.ok == true) {
                        db.obtenerPpiePendientes().firstOrNull { it.clientUuid == uuid }?.let {
                            db.marcarPpiePendienteSincronizado(it.id.toLong())
                        }
                        true
                    } else {
                        val fromBody = body?.message
                        val fromErr = try {
                            r.errorBody()?.string()?.let { raw ->
                                Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
                            }
                        } catch (_: Exception) {
                            null
                        }
                        errMsg = fromBody ?: fromErr ?: "Error HTTP ${r.code()}. Quedó pendiente en Inicio."
                        android.util.Log.e("PPIE", "submit fail code=${r.code()} msg=$errMsg")
                        false
                    }
                } catch (e: Exception) {
                    errMsg = "Error de red: ${e.message ?: "sin detalle"}. Quedó pendiente en Inicio."
                    android.util.Log.e("PPIE", "submit exception", e)
                    false
                }
            }
            progress.dismiss()
            if (ok) {
                Toast.makeText(this@PpieDiligenciarActivity, "Ficha enviada a jefe", Toast.LENGTH_LONG).show()
                finish()
            } else {
                enviando = false
                btn.isEnabled = true
                Toast.makeText(this@PpieDiligenciarActivity, errMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val KEY_CLIENT_UUID = "ppie_client_uuid"
    }
}
