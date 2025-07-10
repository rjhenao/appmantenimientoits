package com.uvrp.itsmantenimientoapp
import Actividad
import ActividadEstado
import ApiService
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.BoringLayout
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "LocalDB", null, 29) {
    private val api: ApiService by lazy { RetrofitClient.instance }
    override fun onCreate(db: SQLiteDatabase) {

        val createRelUserMantenimiento = """
            CREATE TABLE rel_user_mantenimiento (
                id INTEGER PRIMARY KEY,
                id_mantenimiento INTEGER,
                id_empleado INTEGER,
                sincronizado INTEGER
            )
        """
        db.execSQL(createRelUserMantenimiento)

        val createRelRolesUsuarios = """
            CREATE TABLE rel_roles_usuarios (
                id INTEGER PRIMARY KEY,
                idRol INTEGER,
                idUsuario INTEGER
            )
        """
        db.execSQL(createRelRolesUsuarios)

        val createRelManteninimientoEstado = """
            CREATE TABLE rel_mantenimiento_estado (
                id INTEGER PRIMARY KEY,
                id_mantenimiento INTEGER,
                descripcion TEXT,
                path TEXT,                
                observacion TEXT,
                estado INTEGER,
                sincronizado INTEGER
            )
        """
        db.execSQL(createRelManteninimientoEstado)

        val creteRelMantenimientoActividadTableQuery = """
            CREATE TABLE rel_mantenimiento_actividad (
                id INTEGER PRIMARY KEY,
                id_mantenimiento INTEGER,
                id_actividad INTEGER,
                id_mantenimiento_usuario INTEGER,
                path TEXT,
                estado INTEGER,
                observacion TEXT,
                sincronizado INTEGER
            )
        """
        db.execSQL(creteRelMantenimientoActividadTableQuery)

        val createRelTecnicoMantenimientoTableQuery = """
            CREATE TABLE rel_tecnico_mantenimiento (
                id INTEGER PRIMARY KEY,
                idUser INTEGER,
                idMantenimiento INTEGER,
                fechaInicial TEXT,
                fechaFinal TEXT,
                estado INTEGER,
                sincronizado INTEGER
            )
        """
        db.execSQL(createRelTecnicoMantenimientoTableQuery)

        val createActividadesTableQuery = """
            CREATE TABLE actividades (
                id INTEGER PRIMARY KEY,
                id_equipo INTEGER,
                id_periodicidad INTEGER,
                descripcion_actividad TEXT,
                created_at TEXT,
                updated_at TEXT
            )
        """
        db.execSQL(createActividadesTableQuery)

        val createUsersTableQuery  = """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY,
                name TEXT,
                documento INTEGER,
                nombre TEXT,
                email TEXT,
                activo INTEGER,
                password TEXT,
                created_at TEXT,
                updated_at TEXT
            )
        """
        db.execSQL(createUsersTableQuery )

        val createProgramarMantenimientosTableQuery = """
            CREATE TABLE programar_mantenimientos (
                id INTEGER PRIMARY KEY,
                id_equipo INTEGER,
                id_periodicidad INTEGER,
                fecha_programado TEXT,
                fecha_reprogramado TEXT,
                fecha_realizado TEXT,
                created_at TEXT,
                updated_at TEXT
            )
        """
        db.execSQL(createProgramarMantenimientosTableQuery)

        val createEquiposTableQuery = """
            CREATE TABLE equipos (
                id INTEGER PRIMARY KEY,
                id_locacion INTEGER,
                id_sistemas INTEGER,
                id_subsistemas INTEGER,
                id_equipo INTEGER,
                id_uf INTEGER,
                tag  TEXT,
                coordenada TEXT,
                mantenimiento_programado INTEGER,
                activo INTEGER
            )
            """
        db.execSQL(createEquiposTableQuery)

        val createLocacionesTableQuery = """
            CREATE TABLE locaciones (
                id INTEGER PRIMARY KEY,
                nombre TEXT,
                created_at TEXT,
                updated_at TEXT
            )
            """
        db.execSQL(createLocacionesTableQuery)

        val createPeriodicidadTableQuery = """
            CREATE TABLE periodicidad (
                id INTEGER PRIMARY KEY,
                nombre TEXT,
                dias INTEGER,
                created_at TEXT,
                updated_at TEXT
            )
            """
        db.execSQL(createPeriodicidadTableQuery)

        val createRelSistemaLocacionTableQuery = """
            CREATE TABLE rel_sistema_locacion (
                id INTEGER PRIMARY KEY,
                id_locacion INTEGER,
                id_sistema INTEGER,
                created_at TEXT,
                updated_at TEXT
            )
            """
        db.execSQL(createRelSistemaLocacionTableQuery)

        val createRelSubsistemaSistemaTableQuery = """
            CREATE TABLE rel_subsistema_sistema (
                id INTEGER PRIMARY KEY,
                id_subsistema INTEGER,
                id_sistema INTEGER,
                id_locacion INTEGER,
                created_at TEXT,
                updated_at TEXT
            )
            """
        db.execSQL(createRelSubsistemaSistemaTableQuery)

        val crateSistemasTableQuery = """
            CREATE TABLE sistemas (
                id INTEGER PRIMARY KEY,
                nombre TEXT,
                created_at TEXT,
                updated_at TEXT
            )
            """
        db.execSQL(crateSistemasTableQuery)

        val createSubsitemasTableQuery = """
            CREATE TABLE subsistemas (
                id INTEGER PRIMARY KEY,
                nombre TEXT,
                created_at TEXT,
                updated_at TEXT
            )
            """
        db.execSQL(createSubsitemasTableQuery)

        val createTipoEquiposTableQuery = """
            CREATE TABLE tipo_equipos (
                id INTEGER PRIMARY KEY,
                nombre TEXT,
                created_at TEXT,
                updated_at TEXT
            )
            """
        db.execSQL(createTipoEquiposTableQuery)

        val createUfTableQuery = """
            CREATE TABLE uf (
                id INTEGER PRIMARY KEY,
                nombre TEXT,
                created_at TEXT,
                updated_at TEXT
            )
            """
        db.execSQL(createUfTableQuery)


    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS programar_mantenimientos")
        db.execSQL("DROP TABLE IF EXISTS actividades")
        db.execSQL("DROP TABLE IF EXISTS equipos")
        db.execSQL("DROP TABLE IF EXISTS locaciones")
        db.execSQL("DROP TABLE IF EXISTS periodicidad")
        db.execSQL("DROP TABLE IF EXISTS rel_sistema_locacion")
        db.execSQL("DROP TABLE IF EXISTS rel_subsistema_sistema")
        db.execSQL("DROP TABLE IF EXISTS sistemas")
        db.execSQL("DROP TABLE IF EXISTS subsistemas")
        db.execSQL("DROP TABLE IF EXISTS tipo_equipos")
        db.execSQL("DROP TABLE IF EXISTS uf")
        db.execSQL("DROP TABLE IF EXISTS rel_tecnico_mantenimiento")
        db.execSQL("DROP TABLE IF EXISTS rel_mantenimiento_actividad")
        db.execSQL("DROP TABLE IF EXISTS rel_mantenimiento_estado")
        db.execSQL("DROP TABLE IF EXISTS rel_user_mantenimiento")
        db.execSQL("DROP TABLE IF EXISTS rel_roles_usuarios")

        onCreate(db)
    }

    fun getObservacionActividadEstado(idEstado: Int): String {
        val db = this.readableDatabase
        var observacion = "" // Valor por defecto en caso de no encontrar resultados
        Log.d("kkk", idEstado.toString())

        // Consulta SQL para obtener la observación basada en el id
        val cursor = db.rawQuery(
            """
        SELECT observacion FROM rel_mantenimiento_estado WHERE id = ?
        """.trimIndent(),
            arrayOf(idEstado.toString())
        )

        // Verificar si el cursor tiene resultados
        if (cursor.moveToFirst()) {
            Log.d("fff", "hola!")
            val columnIndex = cursor.getColumnIndex("observacion")
            Log.d("fff", "bgbgbgbg!=$columnIndex")

            if (columnIndex != -1) {
                if (!cursor.isNull(columnIndex)) {
                    observacion = cursor.getString(columnIndex)
                    Log.d("fff", "Observación: $observacion")
                } else {
                    Log.e("DatabaseError", "La columna 'observacion' es nula")
                }
            } else {
                Log.e("DatabaseError", "La columna 'observacion' no existe en el resultado de la consulta")
            }
            Log.d("fff", "gbgbgb!")
        } else {
            Log.d("fff", "tttt")
            Log.d("DatabaseDebug", "No se encontraron resultados para id = $idEstado")
        }
        Log.d("fff", "xxxx")

        // Cerrar el cursor para liberar recursos
        cursor.close()
        db.close()

        return observacion
    }

    fun getObservacionActividad(idEstado: Int): String {
        val db = this.readableDatabase
        var observacion = "" // Valor por defecto en caso de no encontrar resultados
        Log.d("kkk", idEstado.toString())

        // Consulta SQL para obtener la observación basada en el id
        val cursor = db.rawQuery(
            """
        SELECT observacion FROM rel_mantenimiento_actividad WHERE id = ?
        """.trimIndent(),
            arrayOf(idEstado.toString())
        )

        // Verificar si el cursor tiene resultados
        if (cursor.moveToFirst()) {
            Log.d("fff", "hola!")
            val columnIndex = cursor.getColumnIndex("observacion")
            Log.d("fff", "bgbgbgbg!=$columnIndex")

            if (columnIndex != -1) {
                if (!cursor.isNull(columnIndex)) {
                    observacion = cursor.getString(columnIndex)
                    Log.d("fff", "Observación: $observacion")
                } else {
                    Log.e("DatabaseError", "La columna 'observacion' es nula")
                }
            } else {
                Log.e("DatabaseError", "La columna 'observacion' no existe en el resultado de la consulta")
            }
            Log.d("fff", "gbgbgb!")
        } else {
            Log.d("fff", "tttt")
            Log.d("DatabaseDebug", "No se encontraron resultados para id = $idEstado")
        }
        Log.d("fff", "xxxx")

        // Cerrar el cursor para liberar recursos
        cursor.close()
        db.close()

        return observacion
    }

    fun insertObservacionActividad (idEstado : Int , observacion : String , estado : Int) : Boolean {

        val db = this.writableDatabase
        var isSuccess = false

        try {
            db.beginTransaction()

            // Definir los valores a actualizar
            val values = ContentValues().apply {
                put("observacion", observacion)
                put("estado", estado)
            }

            // Definir la cláusula WHERE
            val whereClause = "id = ?"
            val whereArgs = arrayOf(idEstado.toString())

            // Ejecutar el UPDATE
            val rowsAffected = db.update(
                "rel_mantenimiento_actividad", // Nombre de la tabla
                values, // Valores a actualizar
                whereClause, // Cláusula WHERE
                whereArgs // Argumentos para la cláusula WHERE
            )

            // Verificar si se actualizó al menos una fila
            if (rowsAffected > 0) {
                isSuccess = true // La operación fue exitosa
                db.setTransactionSuccessful() // Marcar la transacción como exitosa
            } else {
                Log.e("DatabaseError", "No se encontró el idActividadTecnico: $idEstado")
            }

        } catch (e:Exception) {
            e.printStackTrace()
        }finally {
            db.endTransaction()
            db.close()
        }
        return isSuccess
    }

    fun insertObservacionActividadEstado (idEstado : Int , observacion : String , estado : Int) : Boolean {

        val db = this.writableDatabase
        var isSuccess = false

        try {
            db.beginTransaction()

            // Definir los valores a actualizar
            val values = ContentValues().apply {
                put("observacion", observacion)
                put("estado", estado)
            }

            // Definir la cláusula WHERE
            val whereClause = "id = ?"
            val whereArgs = arrayOf(idEstado.toString())

            // Ejecutar el UPDATE
            val rowsAffected = db.update(
                "rel_mantenimiento_estado", // Nombre de la tabla
                values, // Valores a actualizar
                whereClause, // Cláusula WHERE
                whereArgs // Argumentos para la cláusula WHERE
            )

            // Verificar si se actualizó al menos una fila
            if (rowsAffected > 0) {
                isSuccess = true // La operación fue exitosa
                db.setTransactionSuccessful() // Marcar la transacción como exitosa
            } else {
                Log.e("DatabaseError", "No se encontró el idActividadTecnico: $idEstado")
            }

        } catch (e:Exception) {
            e.printStackTrace()
        }finally {
            db.endTransaction()
            db.close()
        }
        return isSuccess
    }

    fun getEmpleados(): List<Empleado> {
        val empleados = mutableListOf<Empleado>()
        val db = this.readableDatabase
        val query = "SELECT u.id, u.nombre as name \n" +
                "FROM users u\n" +
                "join rel_roles_usuarios rru on (rru.idUsuario  = u.id)\n" +
                "WHERE u.activo = 1 and rru.idRol  in (1,2)" // Ajusta la consulta según tu esquema de base de datos
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                empleados.add(Empleado(id, nombre))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return empleados
    }



    fun insertRelUserMantenimientoBatch(idMantenimiento: Int, idEmpleados: List<Int>): Boolean {
        val db = this.writableDatabase
        db.beginTransaction() // Iniciar transacción
        try {
            for (idEmpleado in idEmpleados) {
                val values = ContentValues().apply {
                    put("id_mantenimiento", idMantenimiento)
                    put("id_empleado", idEmpleado)
                }
                val result = db.insert("rel_user_mantenimiento", null, values)
                if (result == -1L) {
                    // Si una inserción falla, deshacer la transacción
                    db.endTransaction()
                    return false
                }
            }
            db.setTransactionSuccessful() // Marcar la transacción como exitosa
            return true
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error en la transacción: ${e.message}")
            return false
        } finally {
            db.endTransaction() // Finalizar la transacción
        }
    }

    fun validarMantenimientosCompleto(idMantenimiento: Int): Boolean {
        val db = this.readableDatabase
        var mantenimientoFinalizado = true // Valor por defecto: no hay path

        val query = db.rawQuery(
            """
        SELECT estado FROM rel_mantenimiento_actividad WHERE id_mantenimiento = ? and estado = 0
        UNION ALL
        SELECT estado FROM rel_mantenimiento_estado WHERE id_mantenimiento = ? and estado = 0
        """.trimIndent(),
            arrayOf(idMantenimiento.toString(), idMantenimiento.toString())
        )

        Log.d("DatabaseQuery", "Query executed: $query")

        // Verificar si el cursor tiene resultados
        if (query.moveToFirst()) {
            mantenimientoFinalizado = false
        }

        Log.d("HasPathResult", "hasPath: $mantenimientoFinalizado")

        // Cerrar el cursor y la base de datos para liberar recursos
        query.close()
        db.close()

        return mantenimientoFinalizado
    }

    fun getHasPathAll(idMantenimiento: Int): Boolean {
        val db = this.readableDatabase
        var hasPath = false // Valor por defecto: no hay path

        val query = db.rawQuery(
            """
        SELECT path FROM rel_mantenimiento_actividad WHERE id_mantenimiento = ? 
        UNION ALL
        SELECT path FROM rel_mantenimiento_estado WHERE id_mantenimiento = ?
        """.trimIndent(),
            arrayOf(idMantenimiento.toString(), idMantenimiento.toString())
        )

        Log.d("DatabaseQuery", "Query executed: $query")

        // Verificar si el cursor tiene resultados
        if (query.moveToFirst()) {
            val columnIndex = query.getColumnIndex("path")
            if (columnIndex != -1) {
                do {
                    // Obtener el valor de "path" en el registro actual
                    val path = query.getString(columnIndex)
                    if (!path.isNullOrEmpty()) {
                        // Si al menos un path tiene información, establecer hasPath como true y salir del bucle
                        hasPath = true
                        break
                    }
                } while (query.moveToNext()) // Iterar sobre todos los registros
            }
        }

        Log.d("HasPathResult", "hasPath: $hasPath")

        // Cerrar el cursor y la base de datos para liberar recursos
        query.close()
        db.close()

        return hasPath
    }

    fun getHasPath(idActividadEstado: Int): Boolean {
        val db = this.readableDatabase
        var hasPath = false // Valor por defecto: no hay path

        val query = db.rawQuery(
            """
        SELECT path FROM rel_mantenimiento_actividad WHERE id = ?
        """.trimIndent(),
            arrayOf(idActividadEstado.toString())
        )
        Log.d("ñññ" , "$query")

        // Verificar si el cursor tiene resultados
        if (query.moveToFirst()) {
            // Obtener el índice de la columna "path"
            val columnIndex = query.getColumnIndex("path")
            if (columnIndex != -1) {
                // Verificar si el valor de "path" no es nulo y no está vacío
                val path = query.getString(columnIndex)
                hasPath = !path.isNullOrEmpty() // true si tiene información, false si está vacío o es null
            }
        }
        Log.d("ppññ" , "$hasPath")

        // Cerrar el cursor para liberar recursos
        query.close()
        db.close()

        return hasPath
    }


    fun getHasPathEstado(idActividadEstado: Int): Boolean {
        val db = this.readableDatabase
        var hasPath = false // Valor por defecto: no hay path

        val query = db.rawQuery(
            """
        SELECT path FROM rel_mantenimiento_estado WHERE id = ?
        """.trimIndent(),
            arrayOf(idActividadEstado.toString())
        )
        Log.d("ñññ" , "$query")

        // Verificar si el cursor tiene resultados
        if (query.moveToFirst()) {
            // Obtener el índice de la columna "path"
            val columnIndex = query.getColumnIndex("path")
            if (columnIndex != -1) {
                // Verificar si el valor de "path" no es nulo y no está vacío
                val path = query.getString(columnIndex)
                hasPath = !path.isNullOrEmpty() // true si tiene información, false si está vacío o es null
            }
        }
        Log.d("ppññ" , "$hasPath")

        // Cerrar el cursor para liberar recursos
        query.close()
        db.close()

        return hasPath
    }


    fun insertFinalizarMantenimiento(idMantenimiento: Int): Boolean {

        val db = this.writableDatabase
        var isSuccess = false // Variable para indicar si la operación fue exitosa

        val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val ahora = formatoFecha.format(Date()) // Formato: "2023-10-05 14:30:45"

        try {
            db.beginTransaction()

            // Definir los valores a actualizar
            val values = ContentValues().apply {
                put("fechaFinal", ahora)
                put("estado", 1)
            }



            // Definir la cláusula WHERE
            val whereClause = "idMantenimiento = ?"
            val whereArgs = arrayOf(idMantenimiento.toString())

            // Ejecutar el UPDATE
            val rowsAffected = db.update(
                "rel_tecnico_mantenimiento", // Nombre de la tabla
                values, // Valores a actualizar
                whereClause, // Cláusula WHERE
                whereArgs // Argumentos para la cláusula WHERE
            )

            val values1 = ContentValues().apply {
                put("fecha_realizado", ahora)
            }

            // Definir la cláusula WHERE
            val whereClause1 = "id = ?"
            val whereArgs1 = arrayOf(idMantenimiento.toString())

            // Ejecutar el UPDATE
            val rowsAffected1 = db.update(
                "programar_mantenimientos", // Nombre de la tabla
                values1, // Valores a actualizar
                whereClause1, // Cláusula WHERE
                whereArgs1 // Argumentos para la cláusula WHERE
            )

            // Verificar si se actualizó al menos una fila
            if (rowsAffected > 0 && rowsAffected1 >0) {
                isSuccess = true // La operación fue exitosa
                db.setTransactionSuccessful() // Marcar la transacción como exitosa
            } else {
                Log.e("DatabaseError", "No se encontró el idActividadTecnico: $")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DatabaseError", "Error al actualizar la imagen en la base de datos", e)
        } finally {
            db.endTransaction() // Finalizar la transacción
            db.close() // Cerrar la base de datos
        }

        return isSuccess // Devolver si la operación fue exitosa
    }


    fun insertarImagen2(idActividadTecnico: Int, photoPath: String): Boolean {
        Log.d("kkiiuu" , "dd2d2d2d2")
        val db = this.writableDatabase
        var isSuccess = false // Variable para indicar si la operación fue exitosa

        Log.d("jjj" , "$idActividadTecnico")

        try {
            db.beginTransaction()

            // Definir los valores a actualizar
            val values = ContentValues().apply {
                put("path", photoPath) // Actualizar el campo "path" con el valor de photoPath
            }

            // Definir la cláusula WHERE
            val whereClause = "id = ?"
            val whereArgs = arrayOf(idActividadTecnico.toString())

            // Ejecutar el UPDATE
            val rowsAffected = db.update(
                "rel_mantenimiento_estado", // Nombre de la tabla
                values, // Valores a actualizar
                whereClause, // Cláusula WHERE
                whereArgs // Argumentos para la cláusula WHERE
            )

            // Verificar si se actualizó al menos una fila
            if (rowsAffected > 0) {
                isSuccess = true // La operación fue exitosa
                db.setTransactionSuccessful() // Marcar la transacción como exitosa
            } else {
                Log.e("DatabaseError", "No se encontró el idActividadTecnico: $idActividadTecnico")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DatabaseError", "Error al actualizar la imagen en la base de datos", e)
        } finally {
            db.endTransaction() // Finalizar la transacción
            db.close() // Cerrar la base de datos
        }

        return isSuccess // Devolver si la operación fue exitosa
    }

    fun insertarImagen(idActividadTecnico: Int, photoPath: String): Boolean {
        val db = this.writableDatabase
        var isSuccess = false // Variable para indicar si la operación fue exitosa

        Log.d("jjj" , "$idActividadTecnico")

        try {
            db.beginTransaction()

            // Definir los valores a actualizar
            val values = ContentValues().apply {
                put("path", photoPath) // Actualizar el campo "path" con el valor de photoPath
            }

            // Definir la cláusula WHERE
            val whereClause = "id = ?"
            val whereArgs = arrayOf(idActividadTecnico.toString())

            // Ejecutar el UPDATE
            val rowsAffected = db.update(
                "rel_mantenimiento_actividad", // Nombre de la tabla
                values, // Valores a actualizar
                whereClause, // Cláusula WHERE
                whereArgs // Argumentos para la cláusula WHERE
            )

            // Verificar si se actualizó al menos una fila
            if (rowsAffected > 0) {
                isSuccess = true // La operación fue exitosa
                db.setTransactionSuccessful() // Marcar la transacción como exitosa
            } else {
                Log.e("DatabaseError", "No se encontró el idActividadTecnico: $idActividadTecnico")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DatabaseError", "Error al actualizar la imagen en la base de datos", e)
        } finally {
            db.endTransaction() // Finalizar la transacción
            db.close() // Cerrar la base de datos
        }

        return isSuccess // Devolver si la operación fue exitosa
    }



    fun getActividadesMantenimiento(idMantenimiento: Int): Pair<List<Actividad>, List<ActividadEstado>> {
        val db = this.readableDatabase
        val actividades = mutableListOf<Actividad>()
        val estados = mutableListOf<ActividadEstado>()

        val query = db.rawQuery(
            """
        SELECT a.id AS id_actividad, a.descripcion_actividad AS descripcion, rma.id AS id_estado, rma.estado
        FROM rel_mantenimiento_actividad AS rma
        JOIN actividades a ON a.id = rma.id_actividad
        WHERE rma.id_mantenimiento = ?
        """.trimIndent(),
            arrayOf(idMantenimiento.toString())
        )

        if (query.moveToFirst()) {
            do {
                val idActividad = query.getInt(query.getColumnIndexOrThrow("id_actividad"))
                val descripcion = query.getString(query.getColumnIndexOrThrow("descripcion"))
                val idEstado = query.getInt(query.getColumnIndexOrThrow("id_estado"))
                val estado = query.getInt(query.getColumnIndexOrThrow("estado"))

                actividades.add(Actividad(idActividad, descripcion, idEstado, estado))
            } while (query.moveToNext())
        }
        query.close()

        val query2 = db.rawQuery(
            """
        SELECT rme.descripcion, rme.estado, rme.id AS id_estado
        FROM rel_mantenimiento_estado AS rme
        WHERE rme.id_mantenimiento = ?
        """.trimIndent(),
            arrayOf(idMantenimiento.toString())
        )

        if (query2.moveToFirst()) {
            do {
                val descripcion = query2.getString(query2.getColumnIndexOrThrow("descripcion"))
                val idEstado = query2.getInt(query2.getColumnIndexOrThrow("id_estado"))
                val estado = query2.getInt(query2.getColumnIndexOrThrow("estado"))

                estados.add(ActividadEstado(1, descripcion, idEstado, estado))
            } while (query2.moveToNext())
        }
        query2.close()

        return Pair(actividades, estados)
    }

    fun getActividadesMantenimiento1(idMantenimiento: Int): List<TableItem> {
        val db = this.readableDatabase
        val actividades = mutableListOf<TableItem>()

        val query = db.rawQuery(
            """
        SELECT rma.id, a.id AS id_actividad, a.descripcion_actividad AS descripcion 
        FROM rel_mantenimiento_actividad AS rma
        JOIN actividades a ON a.id = rma.id_actividad
        WHERE rma.id_mantenimiento = ?
        """.trimIndent(),
            arrayOf(idMantenimiento.toString())
        )

        query.close() // Cerrar cursor para liberar memoria
        return actividades
    }




    fun buscarPorID(id: Int): List<Pair<Int, String>> {
        val db = this.readableDatabase
        val query = """
        SELECT a.id AS id_actividad, a.descripcion_actividad AS descripcion
        FROM programar_mantenimientos pm
        JOIN equipos e ON pm.id_equipo = e.id
        JOIN actividades a ON a.id_equipo = e.id_equipo and a.id_periodicidad = pm.id_periodicidad
        WHERE pm.id = ?
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(id.toString()))
        val actividades = mutableListOf<Pair<Int, String>>() // Lista de pares (ID, Descripción)

        if (cursor.moveToFirst()) {
            do {
                val idActividad = cursor.getInt(cursor.getColumnIndexOrThrow("id_actividad"))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
                actividades.add(Pair(idActividad, descripcion)) // Agregar (ID, Descripción)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return actividades
    }




    fun validateUser(documento: String, password: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM users WHERE documento = ?"
        val cursor = db.rawQuery(query, arrayOf(documento))

        var isValid = false
        if (cursor.moveToFirst()) {
            val storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"))

            val parts = storedPassword.split("$")
            if (parts.size == 2) {
                val storedHash = parts[0]
                val storedSalt = parts[1]

                val passwordWithSalt = password + storedSalt
                val hashedPassword = hashSHA256(passwordWithSalt)

                isValid = hashedPassword == storedHash
            }
        }
        cursor.close() // 🔹 ¡IMPORTANTE! Cerrar el cursor
        return isValid
    }

    fun hashSHA256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray())
            // Convertimos los bytes en un String hexadecimal
            val stringBuilder = StringBuilder()
            for (byte in hashBytes) {
                stringBuilder.append(String.format("%02x", byte))
            }
            stringBuilder.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            ""
        }
    }

    fun obtenerRolUsuario(documento: String, password: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT u.id, u.password, ru.idRol FROM users AS u " +
                    "JOIN rel_roles_usuarios ru ON ru.idUsuario = u.id " +
                    "WHERE u.documento = ? AND u.activo = 1",
            arrayOf(documento)
        )

        var isValid = false
        var idRol = -1

        if (cursor.moveToFirst()) {
            val storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"))

            val parts = storedPassword.split("$")
            if (parts.size == 2) {
                val storedHash = parts[0]
                val storedSalt = parts[1]

                val passwordWithSalt = password + storedSalt
                val hashedPassword = hashSHA256(passwordWithSalt)

                isValid = hashedPassword == storedHash

                if (isValid) {
                    idRol = cursor.getInt(cursor.getColumnIndexOrThrow("idRol"))
                }
            }
        }

        cursor.close()
        db.close()
        return idRol // Retorna el idRol si es válido, o -1 si no
    }


    fun obtenerIdUsuario(documento: String, password: String): Int {

        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id , password , documento FROM users WHERE documento = ? AND activo = 1",
            arrayOf(documento)
        )

        var isValid = false
        if (cursor.moveToFirst()) {
            val storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"))

            val parts = storedPassword.split("$")
            if (parts.size == 2) {
                val storedHash = parts[0]
                val storedSalt = parts[1]

                val passwordWithSalt = password + storedSalt
                val hashedPassword = hashSHA256(passwordWithSalt)

                isValid = hashedPassword == storedHash
            }
        }

        return if (cursor.moveToFirst() and isValid) {
            val idUser = cursor.getInt(0)
            cursor.close()
            db.close()
            idUser
        } else {
            cursor.close()
            db.close()
            -1 // Retorna -1 si no encuentra el usuario
        }
    }

    fun validarManteninientoActividad (idUser: Int , idMantenimiento: Int):Boolean {
        val db = this.readableDatabase
        var isValid = false //No encuentra información

       val cursor = db.rawQuery(
           "SELECT id FROM rel_tecnico_mantenimiento WHERE idUser = ? and idMantenimiento = ? ",
           arrayOf(idUser.toString() ,idMantenimiento.toString()) )

        if (cursor.moveToFirst()) {
            isValid = true
        }
        db.close()
        return isValid
    }

    fun insertarRelTecnicoMantenimiento(
        idUser: Int,
        idMantenimiento: Int,
        fechaInicial: String,
        fechaFinal: String,
        estado: Int,
        sincronizado: Int
    ): Long {
        val db = this.writableDatabase
        val dbb = this.readableDatabase

        var idRelTecnicoMantenimiento: Long = -1 // Declaración única

        try {
            db.beginTransaction()

            val values = ContentValues().apply {
                put("idUser", idUser)
                put("idMantenimiento", idMantenimiento)
                put("fechaInicial", fechaInicial)
                put("fechaFinal", fechaFinal)
                put("estado", estado)
                put("sincronizado", sincronizado)
            }





            idRelTecnicoMantenimiento = db.insert("rel_tecnico_mantenimiento", null, values) // Asignación en la misma variable
            Log.d("DebugITS24", "$values ||| $idRelTecnicoMantenimiento")

            if (idRelTecnicoMantenimiento != -1L) {
                val query = dbb.rawQuery(
                    """
                SELECT a.id AS id_actividad, a.descripcion_actividad AS descripcion
                FROM programar_mantenimientos pm
                JOIN equipos e ON pm.id_equipo = e.id
                JOIN actividades a ON a.id_equipo = e.id_equipo AND a.id_periodicidad = pm.id_periodicidad
                WHERE pm.id = ?
                """.trimIndent(),
                    arrayOf(idMantenimiento.toString())
                )

                var insertMantenimientoEstado1 = ContentValues().apply {
                    put("id_mantenimiento" , idMantenimiento)
                    put("descripcion" , "Estado")
                    put("estado" , 0)
                    put("sincronizado" , 0)
                }

                var insertMantenimientoEstado2 = ContentValues().apply {
                    put("id_mantenimiento" , idMantenimiento)
                    put("descripcion" , "Herramientas Usadas")
                    put("estado" , 0)
                    put("sincronizado" , 0)
                }

                val resultado2 = db.insert("rel_mantenimiento_estado", null, insertMantenimientoEstado1)
                val resultado3 = db.insert("rel_mantenimiento_estado", null, insertMantenimientoEstado2)

                if (resultado2 == -1L) {
                    throw Exception("Error al insertar la actividad")
                }

                if (resultado3 == -1L) {
                    throw Exception("Error al insertar la actividad")
                }

                while (query.moveToNext()) {
                    val idActividad = query.getInt(query.getColumnIndexOrThrow("id_actividad"))

                    val insertActividades = ContentValues().apply {
                        put("id_mantenimiento", idMantenimiento)
                        put("id_actividad", idActividad)
                        put("id_mantenimiento_usuario", idRelTecnicoMantenimiento)
                        put("estado", 0) // 0 = Creado e Iniciado | 1 = Chequeado | 2 = Rechazado
                        put("sincronizado", 0)
                    }
                    val resultado1 = db.insert("rel_mantenimiento_actividad", null, insertActividades)

                    if (resultado1 == -1L) {
                        throw Exception("Error al insertar la actividad: $idActividad")
                    }
                }
                query.close()
                Log.d("DebugITS2456", "$idRelTecnicoMantenimiento")

                db.setTransactionSuccessful() // ✅ Marca la transacción como exitosa
            } else {
                throw Exception("Error al insertar rel_tecnico_mantenimiento: $idMantenimiento")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
            db.close()
        }

        return idRelTecnicoMantenimiento // Ahora devolverá el ID correcto
    }

    fun getNivel1 (): List<TableItemLv1> {

        val lista = mutableListOf<TableItemLv1>()
        val db = this.readableDatabase


        val query = """
            SELECT 
                l.nombre AS locacion ,
                l.id as idLocacion
                    FROM programar_mantenimientos pm
                    JOIN equipos e ON pm.id_equipo = e.id
                    JOIN locaciones l ON l.id = e.id_locacion
                    JOIN sistemas s ON s.id = e.id_sistemas
                    JOIN subsistemas s2 ON s2.id = e.id_subsistemas
                    JOIN tipo_equipos te ON te.id = e.id_equipo
                    JOIN uf u ON u.id = e.id_uf
                    JOIN periodicidad p ON p.id = pm.id_periodicidad
                     WHERE date(pm.fecha_programado) <= date('now')
                    AND pm.fecha_realizado IS NULL
                    GROUP BY l.nombre                    
                    ORDER BY pm.fecha_programado ASC
                    """
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val locacion = cursor.getString(cursor.getColumnIndexOrThrow("locacion"))
                val idlocacion = cursor.getString(cursor.getColumnIndexOrThrow("idLocacion"))


                lista.add(TableItemLv1(locacion , idlocacion))


            } while (cursor.moveToNext())
        } else {
            Log.d("DB_RESULT", "No se encontraron resultados en la consulta.")
        }

        cursor.close()
        db.close()
        return lista
    }


    fun getNivel2 (idLocacion:Int): List<TableItemLv2> {

        val lista = mutableListOf<TableItemLv2>()
        val db = this.readableDatabase


        val query = """
            SELECT 
                l.nombre AS locacion ,
                l.id as idLocacion,
                s.nombre as sistema,
                s.id as idSistema
                    FROM programar_mantenimientos pm
                    JOIN equipos e ON pm.id_equipo = e.id
                    JOIN locaciones l ON l.id = e.id_locacion
                    JOIN sistemas s ON s.id = e.id_sistemas
                    JOIN subsistemas s2 ON s2.id = e.id_subsistemas
                    JOIN tipo_equipos te ON te.id = e.id_equipo
                    JOIN uf u ON u.id = e.id_uf
                    JOIN periodicidad p ON p.id = pm.id_periodicidad
                     WHERE date(pm.fecha_programado) <= date('now')
                     AND l.id = ?
                    AND pm.fecha_realizado IS NULL
                    GROUP BY s.nombre                    
                    ORDER BY pm.fecha_programado ASC
                    """
        val cursor = db.rawQuery(query, arrayOf(idLocacion.toString()))

        if (cursor.moveToFirst()) {
            do {
                val locacion = cursor.getString(cursor.getColumnIndexOrThrow("locacion"))
                val idLocacion = cursor.getString(cursor.getColumnIndexOrThrow("idLocacion"))
                val sistema = cursor.getString(cursor.getColumnIndexOrThrow("sistema"))
                val idSistema = cursor.getString(cursor.getColumnIndexOrThrow("idSistema"))

                lista.add(TableItemLv2(locacion , idLocacion , sistema , idSistema))

            } while (cursor.moveToNext())
        } else {
            Log.d("DB_RESULT", "No se encontraron resultados en la consulta.")
        }
Log.d("jdudud" , "$lista")
        cursor.close()
        db.close()
        return lista
    }


    fun getNivel3 (idLocacion1 : Int , idSistema1 : Int): List<TableItemLv3> {

        val lista = mutableListOf<TableItemLv3>()
        val db = this.readableDatabase

        val query = """
            SELECT 
                l.nombre AS locacion ,
                l.id as idLocacion,
                s.nombre as sistema,
                s.id as idSistema,
                s2.nombre as subsistema,
                s2.id as idSubsistema
                    FROM programar_mantenimientos pm
                    JOIN equipos e ON pm.id_equipo = e.id
                    JOIN locaciones l ON l.id = e.id_locacion
                    JOIN sistemas s ON s.id = e.id_sistemas
                    JOIN subsistemas s2 ON s2.id = e.id_subsistemas
                    JOIN tipo_equipos te ON te.id = e.id_equipo
                    JOIN uf u ON u.id = e.id_uf
                    JOIN periodicidad p ON p.id = pm.id_periodicidad
                     WHERE date(pm.fecha_programado) <= date('now')
                     AND l.id = ? AND s.id = ?
                    AND pm.fecha_realizado IS NULL
                    GROUP BY s2.nombre                    
                    ORDER BY pm.fecha_programado ASC
                    """
        val cursor = db.rawQuery(query, arrayOf(idLocacion1.toString() , idSistema1.toString()))

        if (cursor.moveToFirst()) {
            do {
                val locacion = cursor.getString(cursor.getColumnIndexOrThrow("locacion"))
                val idLocacion = cursor.getString(cursor.getColumnIndexOrThrow("idLocacion"))
                val sistema = cursor.getString(cursor.getColumnIndexOrThrow("sistema"))
                val idSistema = cursor.getString(cursor.getColumnIndexOrThrow("idSistema"))
                val subsistema = cursor.getString(cursor.getColumnIndexOrThrow("subsistema"))
                val idSubsistema = cursor.getString(cursor.getColumnIndexOrThrow("idSubsistema"))

                lista.add(TableItemLv3(locacion , idLocacion , sistema , idSistema , subsistema , idSubsistema))


            } while (cursor.moveToNext())
        } else {
            Log.d("DB_RESULT", "No se encontraron resultados en la consulta.")
        }
        Log.d("d2d33dawsdasdfsa" , "$lista")
        cursor.close()
        db.close()
        return lista
    }

    fun getProgramaciones(idLocacionInt : Int , idSistemaInt : Int , idSubsistemaInt : Int): List<TableItem> {
        val lista = mutableListOf<TableItem>()
        val db = this.readableDatabase


        val query = """
            SELECT 
                l.nombre AS locacion,
                s.nombre AS sistema,
                s2.nombre AS id_subsistemas,
                te.nombre AS equipo,
                u.nombre AS uf,
                e.tag,
                p.nombre AS periocididad,
                pm.fecha_programado,
                CASE 
                    WHEN pm.fecha_realizado IS NOT NULL THEN 'Realizado'
                    ELSE 'Pendiente'
                END AS Estado ,
                pm.id AS idmantenimiento
                    FROM programar_mantenimientos pm
                    JOIN equipos e ON pm.id_equipo = e.id
                    JOIN locaciones l ON l.id = e.id_locacion
                    JOIN sistemas s ON s.id = e.id_sistemas
                    JOIN subsistemas s2 ON s2.id = e.id_subsistemas
                    JOIN tipo_equipos te ON te.id = e.id_equipo
                    JOIN uf u ON u.id = e.id_uf
                    JOIN periodicidad p ON p.id = pm.id_periodicidad
                     WHERE date(pm.fecha_programado) <= date('now')
                     AND l.id = ? AND s.id = ? AND s2.id = ?
                    AND pm.fecha_realizado IS NULL
                    ORDER BY pm.fecha_programado ASC
                    """

        val cursor = db.rawQuery(query, arrayOf(idLocacionInt.toString() , idSistemaInt.toString() , idSubsistemaInt.toString()))

        if (cursor.moveToFirst()) {
            do {
                val locacion = cursor.getString(cursor.getColumnIndexOrThrow("locacion"))
                val sistema = cursor.getString(cursor.getColumnIndexOrThrow("sistema"))
                val idSubSistemas = cursor.getString(cursor.getColumnIndexOrThrow("id_subsistemas"))
                val equipo = cursor.getString(cursor.getColumnIndexOrThrow("equipo"))
                val uf = cursor.getString(cursor.getColumnIndexOrThrow("uf"))
                val tag = cursor.getString(cursor.getColumnIndexOrThrow("tag"))
                val periocididad = cursor.getString(cursor.getColumnIndexOrThrow("periocididad"))

                val fechaOriginal = cursor.getString(cursor.getColumnIndexOrThrow("fecha_programado"))
                val formatoEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Mantén el formato de entrada
                val formatoSalida = SimpleDateFormat("dd-MMM-yy", Locale("es", "ES")) // Cambiado a español para días y meses en español

                val fechaFormateada = try {
                    val date = formatoEntrada.parse(fechaOriginal)
                    formatoSalida.format(date)
                } catch (e: Exception) {
                    fechaOriginal // En caso de error, devuelve la fecha original
                }
                val idmantenimiento = cursor.getInt(cursor.getColumnIndexOrThrow("idmantenimiento"))

                lista.add(TableItem(locacion, sistema, idSubSistemas, equipo, uf, tag, periocididad, fechaFormateada , idmantenimiento))

                // Mostrar en logs
                Log.d("DB_RESULT", "Locacion: $locacion, Sistema: $sistema, SubSistema: $idSubSistemas, Equipo: $equipo, UF: $uf, Tag: $tag, Periocididad: $periocididad, Fecha: $fechaFormateada")
            } while (cursor.moveToNext())
        } else {
            Log.d("DB_RESULT", "No se encontraron resultados en la consulta.")
        }

        /*if (cursor.moveToFirst()) {
            do {
                val locacion = cursor.getString(cursor.getColumnIndexOrThrow("locacion"))
                val equipo = cursor.getString(cursor.getColumnIndexOrThrow("equipo"))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
                lista.add(TableItem(locacion, equipo, estado))
            } while (cursor.moveToNext())
        }*/

        cursor.close()
        db.close()
        return lista
    }







    // Función para agregar imágenes si existen
    fun agregarImagenSiExiste(path: String?, key: String, partesImagenes: MutableList<MultipartBody.Part>) {
        if (!path.isNullOrEmpty()) {
            val imageFile = File(path)
            if (imageFile.exists()) {
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagenPart = MultipartBody.Part.createFormData(key, imageFile.name, requestFile)
                partesImagenes.add(imagenPart)
            } else {
                Log.d("IMAGEN_ERROR", "No se encontró la imagen en la ruta: $path")
            }
        }
    }

    // Función para agregar imágenes si existen
    fun agregarImagenSiExiste111(path: String?, key: String, partesImagenes: MutableList<MultipartBody.Part>) {
        if (!path.isNullOrEmpty()) {
            val imageFile = File(path)
            if (imageFile.exists()) {
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagenPart = MultipartBody.Part.createFormData(key, imageFile.name, requestFile)
                partesImagenes.add(imagenPart)
            } else {
                Log.d("IMAGEN_ERROR", "No se encontró la imagen en la ruta: $path")
            }
        }
    }


    suspend fun sincronizarManteninimientosTerminados(): Int {
        val deferredResult = CompletableDeferred<Int>()
        val db = this.readableDatabase
        val dbb = this.writableDatabase
        var insertOk = 0

        val query = """
        SELECT id, idMantenimiento 
        FROM rel_tecnico_mantenimiento
        WHERE estado = 1 AND sincronizado <> 1              
    """
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val idCodigoMantenimiento = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val idMantenimiento = cursor.getInt(cursor.getColumnIndexOrThrow("idMantenimiento"))

                val tecnicosMantenimiento = cursorToList(db.rawQuery("""
                SELECT idUser, idMantenimiento, fechaInicial, fechaFinal, estado, sincronizado 
                FROM rel_tecnico_mantenimiento
                WHERE (sincronizado <> 1 OR sincronizado IS NULL) AND idMantenimiento = ?       
            """, arrayOf(idMantenimiento.toString())))

                val actividades = cursorToList(db.rawQuery("""
                SELECT id_mantenimiento, id_actividad, id_mantenimiento_usuario, path, estado, observacion, sincronizado 
                FROM rel_mantenimiento_actividad
                WHERE (sincronizado <> 1 OR sincronizado IS NULL) AND id_mantenimiento_usuario = ?       
            """, arrayOf(idCodigoMantenimiento.toString())))

                val estados = cursorToList(db.rawQuery("""
                SELECT id_mantenimiento, descripcion, path, observacion, estado, sincronizado 
                FROM rel_mantenimiento_estado
                WHERE (sincronizado <> 1 OR sincronizado IS NULL) AND id_mantenimiento = ?       
            """, arrayOf(idMantenimiento.toString())))

                val tecnicosActividad = cursorToList(db.rawQuery("""
                SELECT id_mantenimiento, id_empleado, sincronizado 
                FROM rel_user_mantenimiento
                WHERE (sincronizado <> 1 OR sincronizado IS NULL) AND id_mantenimiento = ?       
            """, arrayOf(idMantenimiento.toString())))

                if (tecnicosMantenimiento.isEmpty()) {
                    Log.d("ERROR", "No hay datos en tecnicosMantenimiento para idMantenimiento: $idMantenimiento")
                    continue
                }

                val data = mapOf(
                    "actividades" to mapOf("data" to actividades),
                    "estados" to mapOf("data" to estados),
                    "tecnicos_actividad" to mapOf("data" to tecnicosActividad),
                    "tecnicos_mantenimiento" to mapOf("data" to tecnicosMantenimiento)
                )

                val jsonString = Gson().toJson(data)
                Log.d("JSON_ENVIADO", jsonString)

                val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
                val partesImagenes = mutableListOf<MultipartBody.Part>()

                (actividades + estados).forEach { item ->
                    val path = item["path"] as? String
                    if (!path.isNullOrEmpty()) {
                        val imageFile = File(path)
                        if (imageFile.exists()) {
                            val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)

                            val imagenPart = MultipartBody.Part.createFormData("imagen_actividad[]", imageFile.name, requestFile)
                            partesImagenes.add(imagenPart)
                        }
                    }
                }

                Log.d("IMAGENES_ENVIADAS", "Total de imágenes enviadas: ${partesImagenes.size}")

                val call = api.enviarMantenimientosTerminados(requestBody, partesImagenes.ifEmpty { emptyList() })
                call.enqueue(object : Callback<ApiService.ApiResponse> {
                    override fun onResponse(call: Call<ApiService.ApiResponse>, response: Response<ApiService.ApiResponse>) {
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse?.afirmativo as? Int == 1) {
                                val values = ContentValues().apply {
                                    put("sincronizado", 1)
                                    put("estado", 2)
                                }
                                dbb.update("rel_tecnico_mantenimiento", values, "idMantenimiento = ?", arrayOf(idMantenimiento.toString()))
                                insertOk = 1
                            }
                            Log.d("API_RESPONSE", "Mensaje del servidor: ${apiResponse?.messagedd2}")
                        } else {
                            Log.d("API_ERROR", "Error al enviar el JSON: ${response.errorBody()?.string()}")
                        }
                        deferredResult.complete(insertOk)
                    }
                    override fun onFailure(call: Call<ApiService.ApiResponse>, t: Throwable) {
                        Log.d("API_FAILURE", "Error en la solicitud: ${t.message}")
                        deferredResult.complete(0)
                    }
                })
            } while (cursor.moveToNext())
        } else {
            Log.d("DB_RESULT", "No se encontraron resultados en la consulta.")
            deferredResult.complete(0)
        }

        return deferredResult.await()
    }



    suspend fun sincronizarManteninimientosTerminados44(): Int {
        val deferredResult = CompletableDeferred<Int>() // Espera el resultado
        val db = this.readableDatabase
        val dbb = this.writableDatabase
        var insertOk = 0

        val query = """
        SELECT id, idMantenimiento 
        FROM rel_tecnico_mantenimiento
        WHERE estado = 1 AND sincronizado <> 1              
    """

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val idCodigoMantenimiento = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val idMantenimiento = cursor.getInt(cursor.getColumnIndexOrThrow("idMantenimiento"))

                val qTecnicoMantenimiento = """
                SELECT idUser, idMantenimiento, fechaInicial, fechaFinal, estado, sincronizado 
                FROM rel_tecnico_mantenimiento
                WHERE (sincronizado <> 1 OR sincronizado IS NULL) AND idMantenimiento = ?       
            """
                val cursor4 = db.rawQuery(qTecnicoMantenimiento, arrayOf(idMantenimiento.toString()))

                val tecnicosMantenimiento = cursorToList(cursor4)

                if (tecnicosMantenimiento.isEmpty()) {
                    Log.d("ERROR", "No hay datos en tecnicosMantenimiento para idMantenimiento: $idMantenimiento")
                    continue
                }

                val data = mapOf(
                    "tecnicos_mantenimiento" to mapOf("data" to tecnicosMantenimiento)
                )

                val gson = Gson()
                val jsonString = gson.toJson(data)
                Log.d("JSON_ENVIADO", jsonString)

                val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)

                val call = api.enviarMantenimientosTerminados(requestBody, emptyList())

                call.enqueue(object : Callback<ApiService.ApiResponse> {
                    override fun onResponse(call: Call<ApiService.ApiResponse>, response: Response<ApiService.ApiResponse>) {
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            val validaInsert = apiResponse?.afirmativo as? Int

                            if (validaInsert == 1) {
                                val values1 = ContentValues().apply {
                                    put("sincronizado", 1)
                                    put("estado", 2)
                                }

                                dbb.update(
                                    "rel_tecnico_mantenimiento",
                                    values1,
                                    "idMantenimiento = ?",
                                    arrayOf(idMantenimiento.toString())
                                )
                                insertOk = 1
                            }
                            Log.d("API_RESPONSE", "Mensaje del servidor: ${apiResponse?.messagedd2}")
                        } else {
                            Log.d("API_ERROR", "Error al enviar el JSON: ${response.errorBody()?.string()}")
                        }

                        deferredResult.complete(insertOk) // Completa la espera con el resultado
                    }

                    override fun onFailure(call: Call<ApiService.ApiResponse>, t: Throwable) {
                        Log.d("API_FAILURE", "Error en la solicitud: ${t.message}")
                        deferredResult.complete(0) // Completa con error
                    }
                })
            } while (cursor.moveToNext())
        } else {
            Log.d("DB_RESULT", "No se encontraron resultados en la consulta.")
            deferredResult.complete(0)
        }

        return deferredResult.await() // Espera la respuesta antes de devolver el resultado
    }



    fun sincronizarManteninimientosTerminados2() : Int  {
        val db = this.readableDatabase
        val dbb = this.writableDatabase

        var insertOk = 0

        val query = """
        SELECT id, idMantenimiento 
        FROM rel_tecnico_mantenimiento
        WHERE estado = 1 AND sincronizado <> 1              
    """

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val idCodigoMantenimiento = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val idMantenimiento = cursor.getInt(cursor.getColumnIndexOrThrow("idMantenimiento"))

                // Consulta para actividades
                val qActividades = """
                SELECT id_mantenimiento, id_actividad, id_mantenimiento_usuario, path, estado, observacion, sincronizado 
                FROM rel_mantenimiento_actividad
                WHERE (sincronizado <> 1 OR sincronizado IS NULL) AND id_mantenimiento_usuario = ?       
            """
                val cursor1 = db.rawQuery(qActividades, arrayOf(idCodigoMantenimiento.toString()))

                // Consulta para estados
                val qEstado = """
                SELECT id_mantenimiento, descripcion, path, observacion, estado, sincronizado 
                FROM rel_mantenimiento_estado
                WHERE (sincronizado <> 1 OR sincronizado IS NULL) AND id_mantenimiento = ?       
            """
                val cursor2 = db.rawQuery(qEstado, arrayOf(idMantenimiento.toString()))

                // Consulta para técnicos de actividad
                val qTenicosActividad = """
                SELECT id_mantenimiento, id_empleado, sincronizado 
                FROM rel_user_mantenimiento
                WHERE (sincronizado <> 1 OR sincronizado IS NULL) AND id_mantenimiento = ?       
            """
                val cursor3 = db.rawQuery(qTenicosActividad, arrayOf(idMantenimiento.toString()))

                // Consulta para técnicos de mantenimiento
                val qTecnicoManteninimiento = """
                SELECT idUser, idMantenimiento, fechaInicial, fechaFinal, estado, sincronizado 
                FROM rel_tecnico_mantenimiento
                WHERE (sincronizado <> 1 OR sincronizado IS NULL) AND idMantenimiento = ?       
            """
                val cursor4 = db.rawQuery(qTecnicoManteninimiento, arrayOf(idMantenimiento.toString()))

                // Convertir los resultados a listas de mapas
                val actividades = cursorToList(cursor1)
                val estados = cursorToList(cursor2)
                val tecnicosActividad = cursorToList(cursor3)
                val tecnicosMantenimiento = cursorToList(cursor4)

                // Verificar si hay datos en tecnicosMantenimiento
                if (tecnicosMantenimiento.isEmpty()) {
                    Log.d("ERROR", "No hay datos en tecnicosMantenimiento")
                    continue // Saltar a la siguiente iteración si no hay datos
                }

                if (tecnicosMantenimiento.isEmpty()) {
                    Log.d("ERRORd2d2d2w", "No hay datos en tecnicosMantenimiento para idMantenimiento: $idMantenimiento")
                    continue // Saltar a la siguiente iteración si no hay datos
                }

                // Convertir los datos a JSON
                val data = mapOf(
                    "actividades" to mapOf("data" to actividades),
                    "estados" to mapOf("data" to estados),
                    "tecnicos_actividad" to mapOf("data" to tecnicosActividad),
                    "tecnicos_mantenimiento" to mapOf("data" to tecnicosMantenimiento)
                )

                val gson = Gson()
                val jsonString = gson.toJson(data)
                Log.d("JSON_ENVIADO", jsonString) // Verificar el JSON antes de enviarlo

                /*

                // Crear RequestBody para los datos JSON
                val mediaType = MediaType.parse("application/json")
                val requestBody = RequestBody.create(mediaType, jsonString)

                // Convertir el RequestBody en un MultipartBody.Part
                val jsonPart = MultipartBody.Part.createFormData("json", "data.json", requestBody) */

                val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)



                // Crear MultipartBody.Part para las imágenes (si existen)
                val partesImagenes = mutableListOf<MultipartBody.Part>()

                // Procesar imágenes de actividades
                for (actividad in actividades) {
                    val path = actividad["path"] as? String
                    if (!path.isNullOrEmpty()) {
                        val imageFile = File(path)
                        if (imageFile.exists()) {
                            val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)
                            val imagenPart = MultipartBody.Part.createFormData("imagen_actividad[]", imageFile.name, requestFile)

                            //val imagenPart = MultipartBody.Part.createFormData("imagen_actividad", imageFile.name, requestFile)
                            partesImagenes.add(imagenPart)
                        }
                    }
                }

                // Procesar imágenes de estados
                for (estado in estados) {
                    val path = estado["path"] as? String
                    if (!path.isNullOrEmpty()) {
                        val imageFile = File(path)
                        if (imageFile.exists()) {
                            val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)
                            val imagenPart = MultipartBody.Part.createFormData("imagen_estado[]", imageFile.name, requestFile)
                            //val imagenPart = MultipartBody.Part.createFormData("imagen_estado", imageFile.name, requestFile)
                            partesImagenes.add(imagenPart)
                        }
                    }
                }
                Log.d("IMAGENES_ENVIADAS", "Total de imágenes enviadas: ${partesImagenes.size}")

                // Llamar al método de la API
                //val call = api.enviarMantenimientosTerminados(requestBody)
                //val call = api.enviarMantenimientosTerminados(jsonPart, partesImagenes)
                val call = api.enviarMantenimientosTerminados(requestBody, partesImagenes.ifEmpty { emptyList() })


                call.enqueue(object : Callback<ApiService.ApiResponse> {
                    override fun onResponse(call: Call<ApiService.ApiResponse>, response: Response<ApiService.ApiResponse>) {
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            val validaInsert = apiResponse?.afirmativo as? Int

                            if (validaInsert == 1) {

                                // Actualizar el campo fecha_realizado en programar_mantenimientos
                                val values = ContentValues().apply {
                                    put("fecha_realizado", System.currentTimeMillis()) // Guardar fecha actual en formato UNIX timestamp
                                }

                                dbb.update(
                                    "programar_mantenimientos",  // Nombre de la tabla
                                    values,                      // Valores a actualizar
                                    "id = ?",                    // WHERE condición
                                    arrayOf(idMantenimiento.toString())  // Parámetros
                                )

                                val values1 = ContentValues().apply {
                                    put("sincronizado", 1)
                                    put("estado", 2)
                                }

                                dbb.update(
                                    "rel_tecnico_mantenimiento",  // Nombre de la tabla
                                    values1,                      // Valores a actualizar
                                    "idMantenimiento = ?",                    // WHERE condición
                                    arrayOf(idMantenimiento.toString())  // Parámetros
                                )
                                insertOk = 1
                            }
                            Log.d("API_RESPONSE", "Mensaje del servidor: ${apiResponse?.messagedd2}")
                            Log.d("API_RESPONSE", "Datos recibidos: ${apiResponse?.data2}")
                            Log.d("API_RESPONSE", "Datos recibidos: ${apiResponse?.afirmativo}")
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.d("API_ERROR", "Error al enviar el JSON: $errorBody")
                        }
                    }

                    override fun onFailure(call: Call<ApiService.ApiResponse>, t: Throwable) {
                        Log.d("API_FAILURE", "Error en la solicitud: ${t.message}")
                    }
                })

            } while (cursor.moveToNext())
        } else {
            Log.d("DB_RESULT", "No se encontraron resultados en la consulta.")
        }
        Log.d("djdjdjd2892d9029d"  , "$insertOk")
return insertOk
    }



    fun cursorToList(cursor: Cursor): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        if (cursor.moveToFirst()) {
            do {
                val row = mutableMapOf<String, Any?>()
                for (i in 0 until cursor.columnCount) {
                    val columnName = cursor.getColumnName(i)
                    when (cursor.getType(i)) {
                        Cursor.FIELD_TYPE_NULL -> row[columnName] = null
                        Cursor.FIELD_TYPE_INTEGER -> row[columnName] = cursor.getLong(i)
                        Cursor.FIELD_TYPE_FLOAT -> row[columnName] = cursor.getDouble(i)
                        Cursor.FIELD_TYPE_STRING -> row[columnName] = cursor.getString(i)
                        Cursor.FIELD_TYPE_BLOB -> row[columnName] = cursor.getBlob(i)
                    }
                }
                result.add(row)
            } while (cursor.moveToNext())
        }
        cursor.close()

        // Ajustar la estructura para mantener el campo "data"
        val adjustedResult = mutableListOf<Map<String, Any?>>()
        for (row in result) {
            val adjustedRow = mutableMapOf<String, Any?>()

            for ((key, value) in row) {
                // Verificar si el campo tiene "data" y envolver en una lista si es necesario
                if (key == "actividades" || key == "estados" || key == "tecnicos_actividad") {
                    // Si ya es una lista, la envuelvo dentro de un campo "data"
                    adjustedRow[key] = mapOf("data" to value)
                } else if (key == "tecnicos_mantenimiento") {
                    // Asegurar que "tecnicos_mantenimiento" esté envuelto en "data"
                    adjustedRow[key] = mapOf("data" to value)
                } else {
                    // De lo contrario, dejamos el campo tal como está
                    adjustedRow[key] = value
                }
            }
            adjustedResult.add(adjustedRow)
        }

        return adjustedResult
    }





    fun cursorToList1(cursor: Cursor): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        if (cursor.moveToFirst()) {
            do {
                val row = mutableMapOf<String, Any?>()
                for (i in 0 until cursor.columnCount) {
                    val columnName = cursor.getColumnName(i)
                    when (cursor.getType(i)) {
                        Cursor.FIELD_TYPE_NULL -> row[columnName] = null
                        Cursor.FIELD_TYPE_INTEGER -> row[columnName] = cursor.getLong(i)
                        Cursor.FIELD_TYPE_FLOAT -> row[columnName] = cursor.getDouble(i)
                        Cursor.FIELD_TYPE_STRING -> row[columnName] = cursor.getString(i)
                        Cursor.FIELD_TYPE_BLOB -> row[columnName] = cursor.getBlob(i)
                    }
                }
                result.add(row)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }




}