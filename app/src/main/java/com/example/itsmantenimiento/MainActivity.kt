package com.example.itsmantenimiento

import ApiService
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback  // Asegúrate de importar Callback
import retrofit2.Response

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

        usernameInput = findViewById(R.id.username_input)
        passswordInput = findViewById(R.id.password_input)
        loginbtn = findViewById(R.id.login_btn)
        val sincronizarbtn: FloatingActionButton = findViewById(R.id.sincronizar_btn)


        loginbtn.setOnClickListener {

            val username = usernameInput.text.toString() // Cédula
            val password = passswordInput.text.toString() // Contraseña

            // Valida las credenciales con la base de datos local
            val dbHelper = DatabaseHelper(this)
            val isValidUser = dbHelper.validateUser(username, password)

            if (isValidUser) {
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, programacion_diaria::class.java)
                startActivity(intent)
                // Realizar cualquier acción adicional (por ejemplo, iniciar otra actividad)
            } else {
                Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            }
            Log.i("Test Credenciales" , "username: $username and Password: $password")
        }


        val api = RetrofitClient.instance
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

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