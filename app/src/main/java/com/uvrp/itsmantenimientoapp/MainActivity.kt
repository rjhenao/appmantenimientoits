package com.uvrp.itsmantenimientoapp

import ApiService
import ApiService.InspeccionUsuario
import ApiService.RelInspeccionActividad
import com.uvrp.itsmantenimientoapp.models.Ticket
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.http.GET


class MainActivity : AppCompatActivity() {

    lateinit var usernameInput : EditText
    lateinit var passswordInput : EditText
    lateinit var loginbtn : Button
    lateinit var sincronizarbtn : Button

    private val api: ApiService by lazy { RetrofitClient.instance }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RetrofitClient.init(applicationContext)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        val updater = AppUpdater(this)
        val versionName = BuildConfig.VERSION_NAME

        // Mostrar la versión en el TextView
        val tVersion = findViewById<TextView>(R.id.tVersion)
        tVersion.text = "Versión: $versionName"
        updater.checkForUpdate()

        val sharedPreferences = getSharedPreferences("Sesion", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // Si ya ha iniciado sesión, redirigir directamente a HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // Cierra MainActivity para que no pueda volver atrás
            return // Salir de onCreate para evitar la configuración de vistas innecesarias
        }


        usernameInput = findViewById(R.id.username_input)
        passswordInput = findViewById(R.id.password_input)
        loginbtn = findViewById(R.id.login_btn)
        val sincronizarbtn: FloatingActionButton = findViewById(R.id.sincronizar_btn)

        findViewById<FloatingActionButton>(R.id.fab_scan_reporte_qr).visibility = android.view.View.GONE

        loginbtn.setOnClickListener {

            val username = usernameInput.text.toString() // Cédula
            val password = passswordInput.text.toString() // Contraseña

            // Valida las credenciales con la base de datos local
            val dbHelper = DatabaseHelper(this)

            val userId = dbHelper.obtenerIdUsuario(username, password) // Obtiene el ID del usuario

            Log.i("Test Credenciales2", "Documento: $username y Password: $password y $userId")

            if (userId != -1) { // Credenciales válidas
                val nombreUsu = dbHelper.obtenerNombreUsuario(userId)
                val idRol = dbHelper.obtenerRolUsuario(username, password) // O dbHelper.obtenerRolUsuarioPorId(userId) si tienes esa función

                val editor = getSharedPreferences("Sesion", MODE_PRIVATE).edit()
                editor.putBoolean("isLoggedIn", true)
                editor.putInt("idUser", userId)
                editor.putInt("idRol", idRol)
                editor.putString("nombre", nombreUsu)
                editor.putString("documento", username)
                editor.remove("api_token")
                editor.putBoolean("puede_inventario", idRol == 1 || idRol == 2)
                editor.apply()

                if (idRol !in 1..10) {
                    Toast.makeText(this, "Rol de usuario no válido para iniciar sesión.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val loginResp = api.mobileLogin(
                                ApiService.MobileLoginRequest(username, password)
                            ).execute()
                            if (loginResp.isSuccessful) {
                                val body = loginResp.body()
                                val token = body?.token?.trim().orEmpty()
                                if (token.isNotEmpty()) {
                                    getSharedPreferences("Sesion", MODE_PRIVATE).edit().apply {
                                        putString("api_token", token)
                                        putBoolean("puede_inventario", body?.puedeInventario == true)
                                        putBoolean("puede_ppie", body?.puedePpie == true)
                                        putBoolean("puede_compras_seguimiento", body?.puedeComprasSeguimiento == true)
                                        apply()
                                    }
                                    InventarioOfflineSync.sincronizarCatalogo(this@MainActivity)
                                    PpieOfflineSync.sincronizarCatalogo(this@MainActivity)
                                    ExtrasOfflineSync.sincronizarCatalogoTurnos(this@MainActivity)
                                }
                            }
                        } catch (_: Exception) {
                            // Sin red: credenciales locales
                        }
                    }

                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    finish()
                }
            } else {
                Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            }

            Log.i("Test Credenciales", "Documento: $username y Password: $password")

        }

        sincronizarbtn.setOnClickListener {
            val progressDialog = AlertDialog.Builder(this)
                .setView(R.layout.dialog_loading) // XML con el ProgressBar
                .setCancelable(false)
                .create()

            progressDialog.show()

            CoroutineScope(Dispatchers.Main).launch {
                val errorBD = async(Dispatchers.IO) {
                    sincronizarDatos()
                }.await()

                progressDialog.dismiss()

                Log.d("Sincronizacion", "Errores en BD: $errorBD")

                if (errorBD > 0) {
                    Toast.makeText(this@MainActivity, "Sincronización con errores", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Sincronización completa", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    suspend fun sincronizarDatos(): Int {
        var errorBD = 0
        // Secuencial + pequeña pausa: evita HTTP 429 (Too Many Requests) al disparar ~20 llamadas en paralelo.
        val pasos: List<suspend () -> Boolean> = listOf(
            { sincronizarTabla("users", api.getUsers()) },
            { sincronizarTabla("rel_subsistema_sistema", api.getRelSubsistemaSistema()) },
            { sincronizarTabla("rel_sistema_locacion", api.getRelSistemaLocacion()) },
            { sincronizarTabla("periodicidad", api.getPeriodicidad()) },
            { sincronizarTabla("locaciones", api.getLocaciones()) },
            { sincronizarTabla("equipos", api.getEquipos()) },
            { sincronizarTabla("actividades", api.getActividades()) },
            { sincronizarTabla("sistemas", api.getSistemas()) },
            { sincronizarTabla("subsistemas", api.getSubSistemas()) },
            { sincronizarTabla("tipo_equipos", api.getTipoEquipos()) },
            { sincronizarTabla("rel_roles_usuarios", api.relRolesUsuarios()) },
            { sincronizarTabla("uf", api.getUf()) },
            { sincronizarTabla("sentidos_catalogo", api.getSentidosCatalogo()) },
            { sincronizarTabla("lados_catalogo", api.getLadosCatalogo()) },
            { sincronizarTabla("programar_mantenimientos", api.getProgramarMantenimientos()) },
            { sincronizarTabla("bitacora_mantenimientos", api.getBitacoraMantenimientos()) },
            { sincronizarTabla("actividades_bitacoras", api.getActividadesBitacoras()) },
            { sincronizarTabla("programar_actividades_bitacora", api.getProgramarActividadesBitacora()) },
            { sincronizarTabla("rel_cuadrillas_usuarios", api.getRelCuadrillasUsuarios()) },
            { sincronizarTabla("cuadrillas", api.getCuadrillas()) },
            { sincronizarTabla("actividades_inspeccion", api.getActividadesInspeccion()) },
            { sincronizarTickets() },
        )
        for (paso in pasos) {
            try {
                if (!paso()) errorBD++
            } catch (e: Exception) {
                Log.e("Sincronizacion", "Error en paso de sync: ${e.message}", e)
                errorBD++
            }
            delay(80)
        }
        try {
            if (!PpieOfflineSync.sincronizarCatalogo(this@MainActivity)) {
                // Sin token o sin permiso: no cuenta como error de catálogo general
            }
        } catch (e: Exception) {
            Log.e("Sincronizacion", "PPIE catalogo: ${e.message}")
        }
        return errorBD
    }


    val mutex = Mutex()

    /**
     * El servidor puede responder 429 si hay demasiadas peticiones; se reintenta con espera progresiva.
     */
    private suspend fun <T> ejecutarCallConReintentoGenerico(call: Call<T>, tag: String): Response<T> {
        var ultima: Response<T>? = null
        repeat(4) { intento ->
            val c = if (intento == 0) call else call.clone()
            ultima = c.execute()
            if (ultima!!.code() != 429) return ultima!!
            Log.w(tag, "HTTP 429 Too Many Requests — reintento ${intento + 1}/4 tras espera...")
            delay(1000L * (intento + 1))
        }
        return ultima!!
    }

    suspend fun <T : Any> sincronizarTabla(nombreTabla: String, call: Call<List<T>>): Boolean {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase
        val tag = "API_SYNC_$nombreTabla" // Tag dinámico para filtrar fácil en Logcat

        return withContext(Dispatchers.IO) {
            try {
                // 1. Llamada con reintentos si el servidor limita por tasa (429)
                val response = ejecutarCallConReintentoGenerico(call, tag)

                // 2. Verificamos si la respuesta del servidor fue exitosa (código 200-299)
                if (response.isSuccessful) {
                    val datos = response.body()

                    // 3. LOG CLAVE: Mostramos lo que la API nos entregó.
                    // Si 'datos' es null o una lista vacía, lo veremos aquí.
                    Log.d(tag, "Respuesta recibida: $datos")

                    if (datos != null && datos.isNotEmpty()) {
                        Log.d(tag, "Procesando ${datos.size} registros.")
                        mutex.withLock {
                            db.beginTransaction()
                            try {
                                // PROTECCIÓN NP: conservar TODAS las filas Estado=2 (no programadas).
                                // Antes se borraban las NP ya sincronizadas (sincronizado=1) y al reinsertar desde la API
                                // el modelo no trae "sincronizado", quedaba 0 y volvían a salir "por sincronizar".
                                if (nombreTabla == "programar_actividades_bitacora") {
                                    db.execSQL("DELETE FROM $nombreTabla WHERE IFNULL(Estado, 0) != 2")
                                    Log.d(tag, "Tabla $nombreTabla: eliminadas solo programadas; NP (Estado=2) preservadas")
                                } else {
                                    db.execSQL("DELETE FROM $nombreTabla") // Limpia la tabla local
                                }
                                
                                datos.forEach { item ->
                                    val values = ContentValues().apply {
                                        item::class.java.declaredFields.forEach { field ->
                                            field.isAccessible = true
                                            // Corregimos el nombre del campo si es necesario, como en el caso de 'Observacion'
                                            val fieldName = if (field.name == "observación") "Observacion" else field.name
                                            put(fieldName, field.get(item)?.toString())
                                        }
                                    }
                                    // Filas descargadas del servidor: no son "pendientes de subir" (la API no envía sincronizado).
                                    if (nombreTabla == "programar_actividades_bitacora") {
                                        values.put("sincronizado", 1)
                                    }

                                    // 4. Verificamos si la inserción en la BD fue exitosa
                                    // Para programar_actividades_bitacora, usamos INSERT OR IGNORE para no sobrescribir IDs locales
                                    if (nombreTabla == "programar_actividades_bitacora") {
                                        val id = db.insertWithOnConflict(nombreTabla, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                                        if (id == -1L) {
                                            Log.d(tag, "Registro ya existe (ID del servidor), ignorado correctamente")
                                        }
                                    } else {
                                        val id = db.insert(nombreTabla, null, values)
                                        if (id == -1L) {
                                            Log.e(tag, "¡FALLÓ LA INSERCIÓN! -> Fila: $values")
                                        }
                                    }
                                }
                                db.setTransactionSuccessful()
                            } finally {
                                db.endTransaction()
                            }
                        }
                        true // La sincronización fue exitosa
                    } else {
                        Log.w(tag, "La respuesta fue exitosa pero no contiene datos (lista nula o vacía).")
                        true // Técnicamente no es un error, solo no había nada que sincronizar.
                    }
                } else {
                    // 5. Si la API devolvió un error (ej. 404, 500), lo mostramos.
                    val errorBody = response.errorBody()?.string()
                    Log.e(tag, "Error en la respuesta de la API: ${response.code()} - $errorBody")
                    false // La sincronización falló
                }
            } catch (e: Exception) {
                // 6. Si hubo un problema de red o de conversión de datos, lo capturamos.
                Log.e(tag, "Excepción durante la sincronización: ${e.message}", e)
                false // La sincronización falló
            }
        }
    }

    // Función específica para sincronizar tickets
    suspend fun sincronizarTickets(): Boolean {
        val dbHelper = DatabaseHelper(this)
        val tag = "API_SYNC_TICKETS"

        return withContext(Dispatchers.IO) {
            try {
                val response = ejecutarCallConReintentoGenerico(api.getTickets(), tag)

                // 2. Verificamos si la respuesta del servidor fue exitosa
                if (response.isSuccessful) {
                    val ticketResponse = response.body()

                    Log.d(tag, "Respuesta recibida: $ticketResponse")

                    if (ticketResponse != null && ticketResponse.success && ticketResponse.data.isNotEmpty()) {
                        Log.d(tag, "Procesando ${ticketResponse.data.size} tickets.")
                        
                        // Usar la función de extensión para insertar tickets
                        dbHelper.insertarOActualizarTickets(ticketResponse.data)
                        
                        true // La sincronización fue exitosa
                    } else {
                        Log.w(tag, "La respuesta fue exitosa pero no contiene datos de tickets.")
                        true // Técnicamente no es un error, solo no había nada que sincronizar.
                    }
                } else {
                    // Si la API devolvió un error
                    val errorBody = response.errorBody()?.string()
                    Log.e(tag, "Error en la respuesta de la API: ${response.code()} - $errorBody")
                    false // La sincronización falló
                }
            } catch (e: Exception) {
                // Si hubo un problema de red o de conversión de datos
                Log.e(tag, "Excepción durante la sincronización de tickets: ${e.message}", e)
                false // La sincronización falló
            }
        }
    }


}