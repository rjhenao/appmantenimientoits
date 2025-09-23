package com.uvrp.itsmantenimientoapp

import ApiService
import ApiService.InspeccionUsuario
import ApiService.RelInspeccionActividad
import com.uvrp.itsmantenimientoapp.models.Ticket
import android.content.ContentValues
import android.content.Intent
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call

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
                editor.apply()


                // Redirigir a HomeActivity independientemente del rol (siempre que el rol sea válido para el login)
                // La lógica de qué mostrar dentro de HomeActivity se puede manejar allí basado en idRol
                if (idRol in 1..10) { // Asumiendo que los roles 1, 2, 3, 4 son válidos para ingresar
                    //val intent = Intent(this, HomeActivity::class.java)
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish() // Cierra la actividad de login para que no pueda volver atrás
                } else {
                    // Opcional: manejar roles no válidos para el login aquí, aunque si userId != -1, el rol debería ser válido.
                    // Esto podría ser un caso de error en la lógica de obtenerRolUsuario.
                    Toast.makeText(this, "Rol de usuario no válido para iniciar sesión.", Toast.LENGTH_SHORT).show()
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

        coroutineScope {
            val jobs = listOf(
                async { sincronizarTabla("users", api.getUsers()) },
                async { sincronizarTabla("rel_subsistema_sistema", api.getRelSubsistemaSistema()) },
                async { sincronizarTabla("rel_sistema_locacion", api.getRelSistemaLocacion()) },
                async { sincronizarTabla("periodicidad", api.getPeriodicidad()) },
                async { sincronizarTabla("locaciones", api.getLocaciones()) },
                async { sincronizarTabla("equipos", api.getEquipos()) },
                async { sincronizarTabla("actividades", api.getActividades()) },
                async { sincronizarTabla("sistemas", api.getSistemas()) },
                async { sincronizarTabla("subsistemas", api.getSubSistemas()) },
                async { sincronizarTabla("tipo_equipos", api.getTipoEquipos()) },
                async { sincronizarTabla("rel_roles_usuarios", api.relRolesUsuarios()) },
                async { sincronizarTabla("uf", api.getUf()) },
                async { sincronizarTabla("programar_mantenimientos", api.getProgramarMantenimientos()) }  ,
                async { sincronizarTabla("bitacora_mantenimientos", api.getBitacoraMantenimientos()) },
                async { sincronizarTabla("actividades_bitacoras", api.getActividadesBitacoras()) },
                async { sincronizarTabla("programar_actividades_bitacora", api.getProgramarActividadesBitacora()) },
                //async { sincronizarTabla("rel_bitacora_actividades", api.getRelBitacoraActividades()) },
                //async { sincronizarTabla("rel_fotos_bitacora_actividades", api.getRelFotosBitacoraActividades()) },
                async { sincronizarTabla("rel_cuadrillas_usuarios", api.getRelCuadrillasUsuarios()) },
                async { sincronizarTabla("cuadrillas", api.getCuadrillas()) },
                async { sincronizarTabla("actividades_inspeccion", api.getActividadesInspeccion()) },
                async { sincronizarTickets() },

            )

            val results = jobs.awaitAll()
            errorBD = results.count { !it }
        }

        return errorBD
    }


    val mutex = Mutex()

    suspend fun <T : Any> sincronizarTabla(nombreTabla: String, call: Call<List<T>>): Boolean {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase
        val tag = "API_SYNC_$nombreTabla" // Tag dinámico para filtrar fácil en Logcat

        return withContext(Dispatchers.IO) {
            try {
                // 1. Ejecutamos la llamada a la API
                val response = call.execute()

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
                                db.execSQL("DELETE FROM $nombreTabla") // Limpia la tabla local
                                datos.forEach { item ->
                                    val values = ContentValues().apply {
                                        item::class.java.declaredFields.forEach { field ->
                                            field.isAccessible = true
                                            // Corregimos el nombre del campo si es necesario, como en el caso de 'Observacion'
                                            val fieldName = if (field.name == "observación") "Observacion" else field.name
                                            put(fieldName, field.get(item)?.toString())
                                        }
                                    }
                                    // 4. Verificamos si la inserción en la BD fue exitosa
                                    val id = db.insert(nombreTabla, null, values)
                                    if (id == -1L) {
                                        Log.e(tag, "¡FALLÓ LA INSERCIÓN! -> Fila: $values")
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
                // 1. Ejecutamos la llamada a la API
                val response = api.getTickets().execute()

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