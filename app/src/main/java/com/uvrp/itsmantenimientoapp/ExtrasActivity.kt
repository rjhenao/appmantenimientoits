package com.uvrp.itsmantenimientoapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.uvrp.itsmantenimientoapp.helpers.HeaderHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.round
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExtrasActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private lateinit var etFechaInicial: TextInputEditText
    private lateinit var etFechaFinal: TextInputEditText
    private lateinit var spTurno: Spinner
    private lateinit var cbAntes: CheckBox
    private lateinit var cbDespues: CheckBox
    private lateinit var layoutAntesDetalle: LinearLayout
    private lateinit var layoutDespuesDetalle: LinearLayout
    private lateinit var etHoraIniAntes: TextInputEditText
    private lateinit var etHoraFinAntes: TextInputEditText
    private lateinit var etHoraIniDespues: TextInputEditText
    private lateinit var etHoraFinDespues: TextInputEditText
    private lateinit var tvResumenAntes: TextView
    private lateinit var tvResumenDespues: TextView
    private lateinit var spAutorizo: Spinner
    private lateinit var etObservacion: TextInputEditText
    private lateinit var tvEstadoEnvio: TextView
    private lateinit var btnGuardarEnviar: MaterialButton

    private lateinit var dbHelper: DatabaseHelper

    private val watcherRecalculo = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            actualizarResumenAntes()
            actualizarResumenDespues()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(applicationContext)
        setContentView(R.layout.activity_extras)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        HeaderHelper.setupHeader(this, drawerLayout, navView)

        dbHelper = DatabaseHelper(this)

        etFechaInicial = findViewById(R.id.etFechaInicial)
        etFechaFinal = findViewById(R.id.etFechaFinal)
        spTurno = findViewById(R.id.spTurno)
        cbAntes = findViewById(R.id.cbAntes)
        cbDespues = findViewById(R.id.cbDespues)
        layoutAntesDetalle = findViewById(R.id.layoutAntesDetalle)
        layoutDespuesDetalle = findViewById(R.id.layoutDespuesDetalle)
        etHoraIniAntes = findViewById(R.id.etHoraIniAntes)
        etHoraFinAntes = findViewById(R.id.etHoraFinAntes)
        etHoraIniDespues = findViewById(R.id.etHoraIniDespues)
        etHoraFinDespues = findViewById(R.id.etHoraFinDespues)
        tvResumenAntes = findViewById(R.id.tvResumenAntes)
        tvResumenDespues = findViewById(R.id.tvResumenDespues)
        spAutorizo = findViewById(R.id.spAutorizo)
        etObservacion = findViewById(R.id.etObservacion)
        tvEstadoEnvio = findViewById(R.id.tvEstadoEnvio)
        btnGuardarEnviar = findViewById(R.id.btnGuardarYEnviarExtra)

        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etFechaInicial.setText(hoy)
        etFechaFinal.setText(hoy)

        configurarAutorizo()
        configurarTurnosBasico()

        listOf(etHoraIniAntes, etHoraFinAntes, etHoraIniDespues, etHoraFinDespues).forEach {
            it.addTextChangedListener(watcherRecalculo)
        }

        cbAntes.setOnCheckedChangeListener { _, _ -> syncInputsVisibility() }
        cbDespues.setOnCheckedChangeListener { _, _ -> syncInputsVisibility() }
        syncInputsVisibility()

        btnGuardarEnviar.setOnClickListener { ejecutarGuardarYEnviar() }
    }

    /** Vacía los campos para cargar un nuevo registro (tras envío exitoso al servidor). */
    private fun reiniciarCamposFormulario() {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etFechaInicial.setText(hoy)
        etFechaFinal.setText(hoy)
        spTurno.setSelection(0)
        cbAntes.isChecked = false
        cbDespues.isChecked = false
        etHoraIniAntes.text?.clear()
        etHoraFinAntes.text?.clear()
        etHoraIniDespues.text?.clear()
        etHoraFinDespues.text?.clear()
        etObservacion.text?.clear()
        spAutorizo.setSelection(0)
        syncInputsVisibility()
    }

    private fun ejecutarGuardarYEnviar() {
        val fechaInicial = etFechaInicial.text?.toString()?.trim().orEmpty()
        val fechaFinal = etFechaFinal.text?.toString()?.trim().orEmpty()
        val observacion = etObservacion.text?.toString()?.trim().orEmpty()

        if (!PATRON_FECHA.matches(fechaInicial) || !PATRON_FECHA.matches(fechaFinal)) {
            Toast.makeText(this, "Las fechas deben tener formato AAAA-MM-DD (ej. 2026-04-25).", Toast.LENGTH_LONG).show()
            return
        }
        if (!cbAntes.isChecked && !cbDespues.isChecked) {
            Toast.makeText(this, "Marque si aplica antes de la entrada, después de la salida, o ambas.", Toast.LENGTH_LONG).show()
            return
        }
        if (observacion.length < 3) {
            Toast.makeText(this, "La observación es obligatoria.", Toast.LENGTH_LONG).show()
            return
        }

        val mapping = (spTurno.tag as? List<*>)?.filterIsInstance<Pair<Int, String>>() ?: emptyList()
        if (mapping.isEmpty()) {
            Toast.makeText(this, "Espere a que carguen los turnos vigentes o inicie sesión con internet.", Toast.LENGTH_LONG).show()
            return
        }
        val turnoCodigo = mapping.getOrNull(spTurno.selectedItemPosition)?.first
        if (turnoCodigo == null) {
            Toast.makeText(this, "Seleccione un turno válido.", Toast.LENGTH_LONG).show()
            return
        }
        val autorizo = spAutorizo.selectedItem?.toString()?.trim().orEmpty()

        var hIniAntes: String? = null
        var hFinAntes: String? = null
        var hAntes: Double? = null
        if (cbAntes.isChecked) {
            val rawIni = etHoraIniAntes.text?.toString()?.trim().orEmpty()
            val rawFin = etHoraFinAntes.text?.toString()?.trim().orEmpty()
            hIniAntes = normalizarHora(rawIni)
            hFinAntes = normalizarHora(rawFin)
            hAntes = diferenciaHorasDecimal(rawIni, rawFin)
            if (hIniAntes == null || hFinAntes == null) {
                Toast.makeText(this, "Antes de entrada: indique hora inicial y hora final válidas (HH:mm).", Toast.LENGTH_LONG).show()
                return
            }
            if (hAntes == null || hAntes <= 0.0) {
                Toast.makeText(this, "Antes de entrada: la hora final debe ser posterior a la inicial (si cruza medianoche, ej. 22:00 a 06:00, también se admite).", Toast.LENGTH_LONG).show()
                return
            }
        }

        var hIniDespues: String? = null
        var hFinDespues: String? = null
        var hDespues: Double? = null
        if (cbDespues.isChecked) {
            val rawIni = etHoraIniDespues.text?.toString()?.trim().orEmpty()
            val rawFin = etHoraFinDespues.text?.toString()?.trim().orEmpty()
            hIniDespues = normalizarHora(rawIni)
            hFinDespues = normalizarHora(rawFin)
            hDespues = diferenciaHorasDecimal(rawIni, rawFin)
            if (hIniDespues == null || hFinDespues == null) {
                Toast.makeText(this, "Después de salida: indique hora inicial y hora final válidas (HH:mm).", Toast.LENGTH_LONG).show()
                return
            }
            if (hDespues == null || hDespues <= 0.0) {
                Toast.makeText(this, "Después de salida: revise el tramo horario (hora final posterior a la inicial).", Toast.LENGTH_LONG).show()
                return
            }
        }

        val hAntesRed = hAntes?.let { redondearDosDecimales(it) }
        val hDespuesRed = hDespues?.let { redondearDosDecimales(it) }

        btnGuardarEnviar.isEnabled = false
        tvEstadoEnvio.text = getString(R.string.extras_proceso_guardando_local)

        val clientUuid = UUID.randomUUID().toString()
        val cargadoEn = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        dbHelper.insertarExtraHourLocal(
            clientUuid = clientUuid,
            fechaInicial = fechaInicial,
            fechaFinal = fechaFinal,
            turnoCodigo = turnoCodigo,
            aplicaAntes = cbAntes.isChecked,
            horasAntes = if (cbAntes.isChecked) hAntesRed else null,
            horaInicioAntes = hIniAntes,
            horaFinAntes = hFinAntes,
            aplicaDespues = cbDespues.isChecked,
            horasDespues = if (cbDespues.isChecked) hDespuesRed else null,
            horaInicioDespues = hIniDespues,
            horaFinDespues = hFinDespues,
            autorizoNombre = autorizo,
            observacion = observacion,
            cargadoEn = cargadoEn
        )

        tvEstadoEnvio.text = buildString {
            append(getString(R.string.extras_proceso_guardando_local))
            append("\n")
            append(getString(R.string.extras_proceso_enviando))
        }

        FuncionesGenerales.sincronizarSoloExtras(this) { exito, detalle ->
            btnGuardarEnviar.isEnabled = true
            if (exito) {
                reiniciarCamposFormulario()
                tvEstadoEnvio.text = getString(R.string.extras_exito_final)
                Toast.makeText(this, getString(R.string.extras_toast_completo), Toast.LENGTH_LONG).show()
            } else {
                tvEstadoEnvio.text = getString(R.string.extras_proceso_envio_fallo, detalle)
                Toast.makeText(this, detalle, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun syncInputsVisibility() {
        layoutAntesDetalle.visibility = if (cbAntes.isChecked) View.VISIBLE else View.GONE
        layoutDespuesDetalle.visibility = if (cbDespues.isChecked) View.VISIBLE else View.GONE
        if (!cbAntes.isChecked) {
            tvResumenAntes.visibility = View.GONE
        }
        if (!cbDespues.isChecked) {
            tvResumenDespues.visibility = View.GONE
        }
        actualizarResumenAntes()
        actualizarResumenDespues()
    }

    private fun actualizarResumenAntes() {
        if (!cbAntes.isChecked) return
        val ini = etHoraIniAntes.text?.toString()?.trim().orEmpty()
        val fin = etHoraFinAntes.text?.toString()?.trim().orEmpty()
        val horas = diferenciaHorasDecimal(ini, fin)
        val iniN = normalizarHora(ini)
        val finN = normalizarHora(fin)
        if (horas != null && iniN != null && finN != null && horas > 0) {
            tvResumenAntes.text = getString(
                R.string.extras_resumen_tramo,
                iniN,
                finN,
                formatearHoras(horas)
            )
            tvResumenAntes.visibility = View.VISIBLE
        } else {
            tvResumenAntes.visibility = View.GONE
        }
    }

    private fun actualizarResumenDespues() {
        if (!cbDespues.isChecked) return
        val ini = etHoraIniDespues.text?.toString()?.trim().orEmpty()
        val fin = etHoraFinDespues.text?.toString()?.trim().orEmpty()
        val horas = diferenciaHorasDecimal(ini, fin)
        val iniN = normalizarHora(ini)
        val finN = normalizarHora(fin)
        if (horas != null && iniN != null && finN != null && horas > 0) {
            tvResumenDespues.text = getString(
                R.string.extras_resumen_tramo,
                iniN,
                finN,
                formatearHoras(horas)
            )
            tvResumenDespues.visibility = View.VISIBLE
        } else {
            tvResumenDespues.visibility = View.GONE
        }
    }

    private fun configurarAutorizo() {
        val items = listOf("Raul Henao", "Raul Hernandez", "Nestor Adrian")
        spAutorizo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
    }

    private fun configurarTurnosBasico() {
        val locales = dbHelper.obtenerExtrasTurnosCatalogo()
        val items = if (locales.isNotEmpty()) {
            locales.map { t ->
                val horario = listOfNotNull(t.startHora, t.endHora).joinToString(" - ")
                val suf = if (t.nextDayEnd) " (+1)" else ""
                val detail = if (horario.isNotEmpty()) " — $horario$suf" else ""
                Pair(t.codigo, "${t.codigo} — ${t.label}$detail")
            }
        } else {
            // Fallback mínimo mientras descarga; no usar catálogo histórico obsoleto
            emptyList()
        }
        if (items.isEmpty()) {
            spTurno.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Sincronizando turnos…")
            )
            spTurno.tag = emptyList<Pair<Int, String>>()
            tvEstadoEnvio.text = "Descargando catálogo de turnos vigentes…"
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                val ok = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ExtrasOfflineSync.sincronizarCatalogoTurnos(this@ExtrasActivity)
                }
                if (ok) {
                    configurarTurnosBasico()
                    tvEstadoEnvio.text = ""
                } else {
                    tvEstadoEnvio.text = "No se pudo cargar turnos. Inicie sesión con internet y abra Extras de nuevo."
                }
            }
            return
        }
        val labels = items.map { it.second }
        spTurno.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spTurno.tag = items
    }

    companion object {
        private val PATRON_FECHA = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        private val PATRON_HORA = Regex("^([01]?\\d|2[0-3])[:\\.]([0-5]\\d)$")

        fun normalizarHora(s: String): String? {
            val t = s.trim().replace('.', ':')
            val m = PATRON_HORA.find(t) ?: return null
            val hh = m.groupValues[1].toInt()
            val mm = m.groupValues[2].toInt()
            return String.format(Locale.US, "%02d:%02d", hh, mm)
        }

        /** Diferencia en horas decimales; si la hora final es menor que la inicial, se asume día siguiente. */
        fun diferenciaHorasDecimal(iniRaw: String, finRaw: String): Double? {
            val ini = normalizarHora(iniRaw) ?: return null
            val fin = normalizarHora(finRaw) ?: return null
            val a = minutosDesdeMedianoche(ini) ?: return null
            var b = minutosDesdeMedianoche(fin) ?: return null
            if (b == a) return null
            if (b < a) {
                b += 24 * 60
            }
            return (b - a) / 60.0
        }

        private fun minutosDesdeMedianoche(hhMm: String): Int? {
            val p = hhMm.split(':')
            if (p.size != 2) return null
            return p[0].toIntOrNull()?.times(60)?.plus(p[1].toIntOrNull() ?: return null) ?: return null
        }

        fun redondearDosDecimales(v: Double): Double = round(v * 100.0) / 100.0

        fun formatearHoras(v: Double): String = String.format(Locale.getDefault(), "%.2f h", v)
    }
}
