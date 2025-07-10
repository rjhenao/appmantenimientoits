package com.uvrp.itsmantenimientoapp

import ApiService
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
                if (idRol in 1..4) { // Asumiendo que los roles 1, 2, 3, 4 son válidos para ingresar
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
                async { sincronizarTabla("programar_mantenimientos", api.getProgramarMantenimientos()) }
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
        return withContext(Dispatchers.IO) {
            try {
                mutex.withLock { // Asegura que el acceso a la base de datos sea sincronizado
                    val response = call.execute()
                    if (response.isSuccessful) {
                        val datos = response.body()
                        datos?.let {
                            db.execSQL("DELETE FROM $nombreTabla") // Limpia la tabla local
                            it.forEach { item ->
                                val values = ContentValues().apply {
                                    item::class.java.declaredFields.forEach { field ->
                                        field.isAccessible = true
                                        put(field.name, field.get(item)?.toString())
                                    }
                                }
                                db.insert(nombreTabla, null, values)
                            }
                        }
                        true
                    } else {
                        Log.d("API Error", "Error al sincronizar $nombreTabla: ${response.errorBody()?.string()}")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.d("API Error", "Fallo en $nombreTabla: ${e.message}")
                false
            }
        }
    }


}