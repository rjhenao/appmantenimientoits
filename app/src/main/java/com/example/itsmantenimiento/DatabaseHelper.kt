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
import com.example.itsmantenimiento.models.ValidationRangesRaw
import com.google.gson.Gson
import com.uvrp.itsmantenimientoapp.models.ActividadInfo
import com.uvrp.itsmantenimientoapp.models.ActividadMantenimiento
import com.uvrp.itsmantenimientoapp.models.Activity
import com.uvrp.itsmantenimientoapp.models.BitacoraRecord
import com.uvrp.itsmantenimientoapp.models.Usuario
import com.uvrp.itsmantenimientoapp.models.ValidationParams
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "LocalDB", null, 35) {
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

        val createTableRelUsuarios = """
        CREATE TABLE rel_usuarios_bitacora_actividades (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            idRelProgramarActividadesBitacora INTEGER NOT NULL,
            idUsuario INTEGER NOT NULL,
            created_at TEXT,
            updated_at TEXT,
            sincronizado INTEGER DEFAULT 0
        )
    """.trimIndent()

        db?.execSQL(createTableRelUsuarios)

        // Tabla mantenimientos_correctivos
        val createMantenimientosCorrectivos = """
    CREATE TABLE mantenimientos_correctivos (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        idEquipo INTEGER NOT NULL,
        estado INTEGER NOT NULL, -- 1. Funcionando | 2. Reparación | 3. Fuera de Servicio
        diagnostico TEXT NOT NULL,
        descripcion_falla TEXT NOT NULL,
        acciones_correctivas TEXT NOT NULL,
        repuestos TEXT NOT NULL,
        causa TEXT NOT NULL,
        observacion_adicional TEXT,
        created_at TEXT,
        updated_at TEXT,
        sincronizado INTEGER DEFAULT 0
    )
"""
        db.execSQL(createMantenimientosCorrectivos)

// Tabla fotos_mantenimiento_correctivo
        val createFotosMantenimientoCorrectivo = """
    CREATE TABLE fotos_mantenimiento_correctivo (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        idMantenimientoCorrectivo INTEGER NOT NULL,
        url_foto TEXT NOT NULL,
        created_at TEXT,
        updated_at TEXT,
        sincronizado INTEGER DEFAULT 0
    )
"""
        db.execSQL(createFotosMantenimientoCorrectivo)
// -------------------------------------------------
// Tabla usuarios_mantenimiento_correctivo
        val createUsuariosMantenimientoCorrectivo = """
    CREATE TABLE usuarios_mantenimiento_correctivo (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        idMantenimientoCorrectivo INTEGER NOT NULL,
        idUsuario INTEGER NOT NULL,
        created_at TEXT,
        updated_at TEXT,
        sincronizado INTEGER DEFAULT 0
    )
"""
        db.execSQL(createUsuariosMantenimientoCorrectivo)

        // Tabla para almacenar las bitacoras de mantenimiento
        val createBitacoraMantenimientos = """
    CREATE TABLE bitacora_mantenimientos (
        id INTEGER PRIMARY KEY,
        FechaInicio TEXT NOT NULL,
        FechaFin TEXT NOT NULL,
        idUsuario INTEGER NOT NULL,
        estado INTEGER NOT NULL DEFAULT 0,
        Observacion TEXT,
        created_at TEXT,
        updated_at TEXT
    )
"""
        db.execSQL(createBitacoraMantenimientos)

// Tabla para el catálogo de actividades de las bitácoras
        val createActividadesBitacoras = """
    CREATE TABLE actividades_bitacoras (
        id INTEGER PRIMARY KEY,
        Descripcion TEXT NOT NULL,
        Estado INTEGER NOT NULL DEFAULT 0,
        TipoUnidad TEXT NOT NULL,
        Indicador TEXT NOT NULL,
        created_at TEXT,
        updated_at TEXT
    )
"""
        db.execSQL(createActividadesBitacoras)

// Tabla para la programación de actividades
        val createProgramarActividadesBitacora = """
    CREATE TABLE programar_actividades_bitacora (
        id INTEGER PRIMARY KEY,
        idBitacora INTEGER NOT NULL,
        idActividad INTEGER NOT NULL,
        PrInicial TEXT NOT NULL,
        PrFinal TEXT,
        IdCuadrilla INTEGER NOT NULL,
        UF INTEGER NOT NULL,
        Sentido TEXT NOT NULL,
        Lado TEXT NOT NULL,
        Cantidad REAL NOT NULL,
        Estado INTEGER NOT NULL DEFAULT 0,
        Observacion TEXT,
        supervisorResponsable INTEGER NOT NULL,
        created_at TEXT,
        updated_at TEXT
    )
"""
        db.execSQL(createProgramarActividadesBitacora)

// Tabla intermedia que relaciona la ejecución de una actividad programada
        val createRelBitacoraActividades = """
    CREATE TABLE rel_bitacora_actividades (
        id INTEGER PRIMARY KEY,
        idRelProgramarActividadesBitacora INTEGER NOT NULL,
        PrInicial TEXT NOT NULL,
        PrFinal TEXT NOT NULL,
        Cantidad REAL NOT NULL,
        Programada INTEGER NOT NULL DEFAULT 1,
        ObservacionInterna TEXT,
        sincronizado INTEGER NOT NULL DEFAULT 0,
        created_at TEXT,
        updated_at TEXT
    )
"""
        db.execSQL(createRelBitacoraActividades)

// Tabla para almacenar las rutas de las fotos de una actividad
        val createRelFotosBitacoraActividades = """
    CREATE TABLE rel_fotos_bitacora_actividades (
        id INTEGER PRIMARY KEY,
        idRelProgramarActividadesBitacora INTEGER NOT NULL,
        ruta TEXT NOT NULL,
        estado INTEGER NOT NULL DEFAULT 0,
        sincronizado INTEGER NOT NULL DEFAULT 0,
        created_at TEXT,
        updated_at TEXT
    )
"""
        db.execSQL(createRelFotosBitacoraActividades)

// Tabla para relacionar usuarios con cuadrillas
        val createRelCuadrillasUsuarios = """
    CREATE TABLE rel_cuadrillas_usuarios (
        id INTEGER PRIMARY KEY,
        IdCuadrilla INTEGER NOT NULL,
        IdUsuario INTEGER NOT NULL,
        created_at TEXT,
        updated_at TEXT
    )
"""
        db.execSQL(createRelCuadrillasUsuarios)


// Tabla para almacenar las cuadrillas
        val createCuadrillas = """
    CREATE TABLE cuadrillas (
        id INTEGER PRIMARY KEY,
        Nombre TEXT NOT NULL,
        Descripcion TEXT,
        created_at TEXT,
        updated_at TEXT
    )
"""
        db.execSQL(createCuadrillas)


        // Tabla para registrar las inspecciones por usuario y fecha
        val createInspeccionUsuarios = """
    CREATE TABLE inspeccion_usuarios (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        idUsuario INTEGER,
        fecha TEXT,
        sincronizado INTEGER,
        created_at TEXT,
        updated_at TEXT
    )
"""
        db.execSQL(createInspeccionUsuarios)
// Tabla para el catálogo de actividades de inspección
        val createActividadesInspeccion = """
    CREATE TABLE actividades_inspeccion (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        descripcion TEXT,
        estado INTEGER,
        sincronizado INTEGER,
        created_at TEXT,
        updated_at TEXT
    )
"""
        db.execSQL(createActividadesInspeccion)

// Tabla de relación para saber qué actividades se completaron en cada inspección
        val createRelInspeccionActividades = """
    CREATE TABLE rel_inspeccion_actividades (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        idInspeccionUsuarios INTEGER,
        idActividadInspeccion INTEGER,
        estado INTEGER,
        sincronizado INTEGER,
        created_at TEXT,
        updated_at TEXT
    )
"""
        db.execSQL(createRelInspeccionActividades)

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
        db.execSQL("DROP TABLE IF EXISTS mantenimientos_correctivos")
        db.execSQL("DROP TABLE IF EXISTS fotos_mantenimiento_correctivo")
        db.execSQL("DROP TABLE IF EXISTS usuarios_mantenimiento_correctivo")
        //-----
        db.execSQL("DROP TABLE IF EXISTS bitacora_mantenimientos")
        db.execSQL("DROP TABLE IF EXISTS actividades_bitacoras")
        db.execSQL("DROP TABLE IF EXISTS programar_actividades_bitacora")
        db.execSQL("DROP TABLE IF EXISTS rel_bitacora_actividades")
        db.execSQL("DROP TABLE IF EXISTS rel_fotos_bitacora_actividades")
        db.execSQL("DROP TABLE IF EXISTS rel_cuadrillas_usuarios")
        db.execSQL("DROP TABLE IF EXISTS cuadrillas")
        db.execSQL("DROP TABLE IF EXISTS inspeccion_usuarios")
        db.execSQL("DROP TABLE IF EXISTS actividades_inspeccion")
        db.execSQL("DROP TABLE IF EXISTS rel_inspeccion_actividades")
        db.execSQL("DROP TABLE IF EXISTS rel_usuarios_bitacora_actividades")

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

    fun obtenerNombreUsuario(idUser: Int): String {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT nombre FROM users WHERE id = ?",
            arrayOf(idUser.toString())
        )

        var nombreUsuario = ""

        if (cursor.moveToFirst()) {
            nombreUsuario = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
        }

        cursor.close()
        db.close()
        return nombreUsuario
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
            "SELECT id , password , documento , nombre FROM users WHERE documento = ? AND activo = 1",
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

        return if (isValid && cursor.moveToFirst()) { // Asegúrate de que isValid sea verdadero antes de intentar acceder a los datos del cursor
            val idUser = cursor.getInt(0)
            cursor.close()
            db.close()
            idUser // Retorna el idUser si el usuario es válido y se encontró
        } else {
            cursor.close()
            db.close()
            -1 // Retorna -1 si no encuentra el usuario o la contraseña no es válida
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

               /* (actividades + estados).forEach { item ->
                    val path = item["path"] as? String
                    if (!path.isNullOrEmpty()) {
                        val imageFile = File(path)
                        if (imageFile.exists()) {
                            val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)

                            val imagenPart = MultipartBody.Part.createFormData("imagen_actividad[]", imageFile.name, requestFile)
                            partesImagenes.add(imagenPart)
                        }
                    }
                } */

                actividades.forEach { item ->
                    val path = item["path"] as? String
                    if (!path.isNullOrEmpty()) {
                        val imageFile = File(path)
                        if (imageFile.exists()) {
                            val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)

                            // Usar el nombre de campo esperado por el backend para actividades
                            val imagenPart = MultipartBody.Part.createFormData("imagen_actividad[]", imageFile.name, requestFile)
                            partesImagenes.add(imagenPart)
                        }
                    }
                }

                // 2. Procesar imágenes de ESTADOS
                estados.forEach { item ->
                    val path = item["path"] as? String
                    if (!path.isNullOrEmpty()) {
                        val imageFile = File(path)
                        if (imageFile.exists()) {
                            val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)

                            // Usar el nombre de campo esperado por el backend para estados
                            val imagenPart = MultipartBody.Part.createFormData("imagen_estado[]", imageFile.name, requestFile)
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

    fun getLocaciones(): List<Pair<Int, String>> {
        return obtenerLista("""
        SELECT id, nombre
        FROM locaciones
        ORDER BY nombre ASC
    """)
    }



    fun getSistemas(locacionId: Int): List<Pair<Int, String>> {
        return obtenerLista("""
        SELECT s.id, s.nombre
        FROM rel_sistema_locacion rsl
        INNER JOIN sistemas s ON rsl.id_sistema = s.id
        WHERE rsl.id_locacion = ?
        ORDER BY s.nombre
    """, arrayOf(locacionId.toString()))
    }

    fun getSubsistemas(sistemaId: Int, locacionId: Int): List<Pair<Int, String>> {
        return obtenerLista("""
        SELECT ss.id, ss.nombre
        FROM rel_subsistema_sistema rsb
        INNER JOIN subsistemas ss ON rsb.id_subsistema = ss.id
        WHERE rsb.id_locacion = ? AND rsb.id_sistema = ?
        ORDER BY ss.nombre
    """, arrayOf(locacionId.toString(), sistemaId.toString()))
    }

    fun getTiposEquipo(subsistemaId: Int): List<Pair<Int, String>> {
        return obtenerLista("""
        SELECT DISTINCT te.id, te.nombre
        FROM equipos e
        INNER JOIN tipo_equipos te ON e.id_equipo = te.id
        WHERE e.id_subsistemas = ?
        ORDER BY te.nombre
    """, arrayOf(subsistemaId.toString()))
    }

    fun getTagsEquipo(locacionId: Int, sistemaId: Int, subsistemaId: Int, tipoEquipoId: Int): List<Pair<Int, String>> {
        return obtenerLista("""
        SELECT DISTINCT e.id, e.tag
        FROM equipos e
        WHERE e.id_locacion = ? 
          AND e.id_sistemas = ?
          AND e.id_subsistemas = ?
          AND e.id_equipo = ?   -- filtro agregado
        ORDER BY e.tag
    """, arrayOf(
            locacionId.toString(),
            sistemaId.toString(),
            subsistemaId.toString(),
            tipoEquipoId.toString()       // nuevo parámetro en el arreglo
        ))
    }


    private fun obtenerLista(query: String, args: Array<String>? = null): List<Pair<Int, String>> {
        val lista = mutableListOf<Pair<Int, String>>()
        val db = readableDatabase
        val cursor = db.rawQuery(query, args)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val nombre = cursor.getString(1)
            lista.add(id to nombre)
        }
        cursor.close()
        return lista
    }

    // Inserta mantenimiento correctivo en BD local
    fun insertMantenimientoCorrectivo(
        idEquipo: Int,
        descripcionFalla: String,
        diagnostico: String,
        acciones: String,
        repuestos: String,
        estadoFinal: String,
        causaRaiz: String,
        observaciones: String
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("idEquipo", idEquipo)
            put("descripcion_falla", descripcionFalla)
            put("diagnostico", diagnostico)
            put("acciones_correctivas", acciones)
            put("repuestos", repuestos)
            put("estado", estadoFinal) // lo guardamos como texto o número según tu spinner
            put("causa", causaRaiz)
            put("observacion_adicional", observaciones)
            put("created_at", System.currentTimeMillis().toString())
            put("updated_at", System.currentTimeMillis().toString())
            put("sincronizado", 0) // siempre pendiente
        }
        return db.insert("mantenimientos_correctivos", null, values)
    }

    // Inserta relación usuario-mantenimiento
    fun insertRelUserMantenimientoBatch(idMantenimiento: Long, usuarios: List<Int>): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            usuarios.forEach { idUsuario ->
                val values = ContentValues().apply {
                    put("idMantenimientoCorrectivo", idMantenimiento)
                    put("idUsuario", idUsuario)
                    put("created_at", System.currentTimeMillis().toString())
                    put("updated_at", System.currentTimeMillis().toString())
                    put("sincronizado", 0)
                }
                db.insert("usuarios_mantenimiento_correctivo", null, values)
            }
            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
        }
    }

    // Inserta fotos asociadas al mantenimiento
    fun insertFotosMantenimiento(idMantenimiento: Long, fotos: List<File>): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            fotos.forEach { foto ->
                val values = ContentValues().apply {
                    put("idMantenimientoCorrectivo", idMantenimiento)
                    put("url_foto", foto.absolutePath) // guardamos path local
                    put("created_at", System.currentTimeMillis().toString())
                    put("updated_at", System.currentTimeMillis().toString())
                    put("sincronizado", 0)
                }
                db.insert("fotos_mantenimiento_correctivo", null, values)
            }
            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
        }
    }

    fun insertarMantenimientoCompleto(
        idEquipo: Int,
        descripcionFalla: String,
        diagnostico: String,
        acciones: String,
        repuestos: String,
        estadoFinal: String,
        causa: String,
        observaciones: String,
        usuariosIds: List<Int>,
        fotos: List<File>
    ): Boolean {
        val db = writableDatabase
        var idMantenimiento: Long = -1

        db.beginTransaction()
        return try {
            // Insertar mantenimiento correctivo
            val valuesMantenimiento = ContentValues().apply {
                put("idEquipo", idEquipo)
                put("descripcion_falla", descripcionFalla)
                put("diagnostico", diagnostico)
                put("acciones_correctivas", acciones)
                put("repuestos", repuestos)
                put("estado", estadoFinal)
                put("causa", causa)
                put("observacion_adicional", observaciones)
                put("sincronizado", 0)
            }

            idMantenimiento = db.insert("mantenimientos_correctivos", null, valuesMantenimiento)

            if (idMantenimiento == -1L) throw Exception("Error insertando mantenimiento")

            // Insertar usuarios relacionados
            for (idUsuario in usuariosIds) {
                val valuesRel = ContentValues().apply {
                    put("idMantenimientoCorrectivo", idMantenimiento)
                    put("idUsuario", idUsuario)
                    put("sincronizado", 0)
                }
                val res = db.insert("usuarios_mantenimiento_correctivo", null, valuesRel)
                if (res == -1L) throw Exception("Error insertando usuario en relación")
            }

            // Insertar fotos
            for (foto in fotos) {
                val valuesFoto = ContentValues().apply {
                    put("idMantenimientoCorrectivo", idMantenimiento)
                    put("url_foto", foto.absolutePath) // Guarda path local
                    put("sincronizado", 0)
                }
                val res = db.insert("fotos_mantenimiento_correctivo", null, valuesFoto)
                if (res == -1L) throw Exception("Error insertando foto")
            }

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
        }
    }

    fun getMantenimientosPendientes(): List<String> {
        val pendientes = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
        SELECT e.tag
        FROM mantenimientos_correctivos mc
        JOIN equipos e ON mc.idEquipo = e.id
        WHERE mc.sincronizado = 0
        """, null
        )
        if (cursor.moveToFirst()) {
            do {
                pendientes.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return pendientes
    }


    fun getMantenimientosPendientesBicatacoras(): List<String> {
        val pendientes = mutableListOf<String>()
        val db = readableDatabase

        // Usamos .use para que el cursor se cierre automáticamente, es más seguro.
        db.rawQuery(
            """
        SELECT 
            CONCAT(
                SUBSTR(ab.Descripcion, 1, 10), 
                ' ', 
                CAST(rba.Cantidad AS TEXT)
            ) AS tag
        FROM 
            rel_bitacora_actividades rba 
        JOIN 
            programar_actividades_bitacora pab ON (pab.id = rba.idRelProgramarActividadesBitacora)
        JOIN 
            actividades_bitacoras ab ON (ab.id = pab.idActividad)
        WHERE
            rba.sincronizado = 0
        """, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    // El índice 0 corresponde a la primera (y única) columna: "tag"
                    pendientes.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
        }

        return pendientes
    }

    fun getMantenimientosPendientesActividad(): List<String> {
        val pendientes = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
        SELECT e.tag
        FROM rel_tecnico_mantenimiento rma
        join programar_mantenimientos as pro on (rma.idMantenimiento = pro.id )
        JOIN equipos e ON (pro.id_equipo = e.id)
        WHERE rma.sincronizado = 0
        """, null
        )

        if (cursor.moveToFirst()) {
            do {
                pendientes.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return pendientes
    }


    fun obtenerMantenimientosPendientes(): List<MantenimientoCorrectivo> {
        val lista = mutableListOf<MantenimientoCorrectivo>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM mantenimientos_correctivos WHERE sincronizado = 0",
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val idEquipo = cursor.getInt(cursor.getColumnIndexOrThrow("idEquipo"))
                val descripcionFalla = cursor.getString(cursor.getColumnIndexOrThrow("descripcion_falla"))
                val diagnostico = cursor.getString(cursor.getColumnIndexOrThrow("diagnostico"))
                val acciones = cursor.getString(cursor.getColumnIndexOrThrow("acciones_correctivas"))
                val repuestos = cursor.getString(cursor.getColumnIndexOrThrow("repuestos"))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
                val causa = cursor.getString(cursor.getColumnIndexOrThrow("causa"))
                val observaciones = cursor.getString(cursor.getColumnIndexOrThrow("observacion_adicional"))

                // 🔹 Obtener usuarios y fotos relacionadas
                val usuarios = obtenerUsuariosDeMantenimiento(id)
                val fotos = obtenerFotosDeMantenimiento(id)


                lista.add(
                    MantenimientoCorrectivo(
                        id = id,
                        idEquipo = idEquipo,
                        descripcionFalla = descripcionFalla,
                        diagnostico = diagnostico,
                        acciones = acciones,
                        repuestos = repuestos,
                        estadoFinal = estado,
                        causaRaiz = causa,
                        observaciones = observaciones,
                        usuarios = usuarios,
                        fotos = fotos
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun obtenerUsuariosDeMantenimiento(idMantenimiento: Int): List<Int> {
        val lista = mutableListOf<Int>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT idUsuario FROM usuarios_mantenimiento_correctivo WHERE idMantenimientoCorrectivo = ?",
            arrayOf(idMantenimiento.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                lista.add(cursor.getInt(cursor.getColumnIndexOrThrow("idUsuario")))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun obtenerFotosDeMantenimiento(idMantenimiento: Int): List<File> {
        val lista = mutableListOf<File>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT url_foto FROM fotos_mantenimiento_correctivo WHERE idMantenimientoCorrectivo = ?",
            arrayOf(idMantenimiento.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                val path = cursor.getString(cursor.getColumnIndexOrThrow("url_foto"))
                lista.add(File(path))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }


    fun marcarMantenimientoSincronizado(idMantenimiento: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("sincronizado", 1) // ✅ 1 = sincronizado, 0 = pendiente
        }

        db.update(
            "mantenimientos_correctivos",        // Nombre de la tabla
            values,                  // Valores a actualizar
            "id = ?",                // WHERE
            arrayOf(idMantenimiento.toString()) // Argumentos del WHERE
        )

        db.close()
    }

    // Dentro de la clase DatabaseHelper
    fun getTodasLasBitacoras(): List<Bitacora> {
        val listaBitacoras = mutableListOf<Bitacora>()
        val db = this.readableDatabase

        // Consulta SQL para unir bitacoras con usuarios y formatear el estado
        val query = """
        SELECT
            b.id,
            CASE b.estado
                WHEN 0 THEN 'Abierta'
                WHEN 1 THEN 'Programada'
                WHEN 2 THEN 'Cerrada'
                WHEN 3 THEN 'Anulada'
                ELSE 'Desconocido'
            END AS estado_texto,
            SUBSTR(u.nombre, 1, 12) AS nombre_responsable,
            b.FechaInicio,
            b.FechaFin
        FROM bitacora_mantenimientos AS b
        JOIN users AS u ON b.idUsuario = u.id
        ORDER BY b.FechaInicio DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                // Obtener los datos del cursor
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString()
                val estado = cursor.getString(cursor.getColumnIndexOrThrow("estado_texto"))
                val responsable = cursor.getString(cursor.getColumnIndexOrThrow("nombre_responsable"))
                val fechaInicioStr = cursor.getString(cursor.getColumnIndexOrThrow("FechaInicio"))
                val fechaFinStr = cursor.getString(cursor.getColumnIndexOrThrow("FechaFin"))

                // Formatear el rango de fechas
                val rangoFechas = try {
                    val formatoEntrada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es", "ES"))
                    val formatoSalida = SimpleDateFormat("dd MMM", Locale("es", "ES"))
                    val formatoAnio = SimpleDateFormat("yyyy", Locale("es", "ES"))

                    val fechaInicioDate = formatoEntrada.parse(fechaInicioStr)
                    val fechaFinDate = formatoEntrada.parse(fechaFinStr)

                    val fechaInicioFormateada = formatoSalida.format(fechaInicioDate)
                    val fechaFinFormateada = formatoSalida.format(fechaFinDate)
                    val anio = formatoAnio.format(fechaInicioDate) // Asumimos el año de la fecha de inicio

                    "$fechaInicioFormateada — $fechaFinFormateada $anio"
                } catch (e: Exception) {
                    // En caso de error de formato, mostrar las fechas originales
                    "$fechaInicioStr - $fechaFinStr"
                }

                // Crear el objeto Bitacora y añadirlo a la lista
                listaBitacoras.add(Bitacora(id, estado, responsable, rangoFechas))

            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return listaBitacoras
    }

    fun existeInspeccionHoy(context: Context): Boolean {
        // 1. Obtener el ID del usuario desde SharedPreferences
        val prefs = context.getSharedPreferences("Sesion", Context.MODE_PRIVATE)
        val idUsuario = prefs.getInt("idUser", -1) // Usamos -1 como valor por defecto si no se encuentra

        // Si no hay un usuario logueado, no puede haber registros
        if (idUsuario == -1) {
            Log.w("DatabaseHelper", "No se encontró un idUsuario en SharedPreferences.")
            return false
        }

        // 2. Obtener la fecha de hoy en formato YYYY-MM-DD
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaHoy = formatoFecha.format(Date())

        // 3. Realizar la consulta a la base de datos
        val db = this.readableDatabase
        var cursor: Cursor? = null
        var existeRegistro = false

        try {
            val query = "SELECT COUNT(*) FROM inspeccion_usuarios WHERE idUsuario = ? AND date(fecha) = ?"
            cursor = db.rawQuery(query, arrayOf(idUsuario.toString(), fechaHoy))

            if (cursor.moveToFirst()) {
                val count = cursor.getInt(0)
                if (count > 0) {
                    existeRegistro = true
                }
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error al verificar la inspección de hoy", e)
        } finally {
            cursor?.close()
            // Es buena práctica no cerrar la base de datos aquí si se va a usar
            // en múltiples lugares de la misma pantalla. La actividad se encargará de cerrarla.
        }

        return existeRegistro
    }

    fun getAllActivities(): List<Activity> {
        val activityList = mutableListOf<Activity>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT id, descripcion FROM actividades_inspeccion WHERE estado = 1", null)

        if (cursor.moveToFirst()) {
            do {
                val activity = Activity(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
                )
                activityList.add(activity)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return activityList
    }

    /**
     * Inserta una nueva cabecera de inspección y devuelve su ID.
     */
    fun addInspection(userId: Int): Long {
        val db = this.writableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val values = ContentValues().apply {
            put("idUsuario", userId)
            put("fecha", currentDate)
            put("sincronizado", 0) // 0 = no sincronizado
            put("created_at", currentDate)
            put("updated_at", currentDate)
        }

        Log.d("dddddfffgggbb", "ccc: $values")

        val id = db.insert("inspeccion_usuarios", null, values)
        db.close()
        return id
    }

    /**
     * Inserta el detalle de las actividades (marcadas y no marcadas) para una inspección.
     */
    fun addInspectionActivities(inspectionId: Long, activities: List<Activity>) {
        val db = this.writableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Usamos una transacción para asegurar que todas las inserciones se completen o ninguna.
        db.beginTransaction()
        try {
            for (activity in activities) {
                val values = ContentValues().apply {
                    put("idInspeccionUsuarios", inspectionId)
                    put("idActividadInspeccion", activity.id)
                    put("estado", if (activity.isChecked) 1 else 0) // 1 = completado, 0 = no
                    put("sincronizado", 0)
                    put("created_at", currentDate)
                    put("updated_at", currentDate)
                }
                Log.d("gghhhhhgtgfew", "ffdd: $values")

                db.insert("rel_inspeccion_actividades", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    /**
     * (Opcional) Método para añadir datos de prueba.
     */
    fun addSampleActivities() {
        val db = this.writableDatabase
        val countCursor = db.rawQuery("SELECT COUNT(*) FROM actividades_inspeccion", null)
        countCursor.moveToFirst()
        val count = countCursor.getInt(0)
        countCursor.close()

        if (count == 0) {
            val activities = listOf(
                "Revisar nivel de aceite", "Verificar presión de neumáticos", "Comprobar luces delanteras",
                "Inspeccionar frenos", "Revisar líquido limpiaparabrisas", "Verificar estado de espejos",
                "Comprobar claxon", "Inspeccionar cinturones de seguridad", "Verificar extintor",
                "Revisar documentación del vehículo"
            )
            activities.forEach { desc ->
                val values = ContentValues().apply {
                    put("descripcion", desc)
                    put("estado", 1) // 1 = activo
                    put("sincronizado", 0)
                }
                db.insert("actividades_inspeccion", null, values)
            }
        }
        db.close()
    }

    private fun formatCantidad(cantidad: Double): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }

        if (cantidad % 1.0 == 0.0) {
            return DecimalFormat("#,###", symbols).format(cantidad)
        } else {
            return DecimalFormat("#,##0.##", symbols).format(cantidad)
        }
    }

    fun getActividadesPorBitacora(bitacoraId: Int , idUser:Int): List<ActividadMantenimiento> {
        val actividadesList = mutableListOf<ActividadMantenimiento>()
        val db = this.readableDatabase

        val query = """
        SELECT
            pab.id,
            ab.Descripcion,
            c.Nombre,
            pab.UF,
            pab.Sentido,
            pab.Lado,
            pab.PrInicial,
            pab.PrFinal,
            pab.Cantidad,
            ab.TipoUnidad,
            pab.Observacion
        FROM programar_actividades_bitacora pab
        JOIN bitacora_mantenimientos bm ON (pab.idBitacora = bm.id)
        JOIN actividades_bitacoras ab ON (ab.id = pab.idActividad)
        JOIN cuadrillas c ON (c.id = pab.IdCuadrilla)
        LEFT JOIN rel_cuadrillas_usuarios rcu on (rcu.IdCuadrilla  = c.id)        
        WHERE bm.id = ? AND rcu.IdUsuario = ?
        GROUP BY pab.id
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(bitacoraId.toString(), idUser.toString()))


        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndexOrThrow("id")
                val descripcionIndex = it.getColumnIndexOrThrow("Descripcion")
                val cuadrillaIndex = it.getColumnIndexOrThrow("Nombre")
                val ufIndex = it.getColumnIndexOrThrow("UF")
                val sentidoIndex = it.getColumnIndexOrThrow("Sentido")
                val ladoIndex = it.getColumnIndexOrThrow("Lado")
                val prInicialIndex = it.getColumnIndexOrThrow("PrInicial")
                val prFinalIndex = it.getColumnIndexOrThrow("PrFinal")
                val cantidadIndex = it.getColumnIndexOrThrow("Cantidad")
                val tipoUnidadIndex = it.getColumnIndexOrThrow("TipoUnidad")
                val observacionIndex = it.getColumnIndexOrThrow("Observacion")

                do {
                    val rawCantidad = it.getDouble(cantidadIndex)
                    val tipoUnidad = it.getString(tipoUnidadIndex)
                    val formattedCantidad = formatCantidad(rawCantidad)
                    val cantidadYUnidad = "$formattedCantidad $tipoUnidad"

                    val actividad = ActividadMantenimiento(
                        id = it.getInt(idIndex),
                        descripcion = it.getString(descripcionIndex),
                        cuadrillaNombre = it.getString(cuadrillaIndex),
                        uf = it.getString(ufIndex),
                        sentido = it.getString(sentidoIndex),
                        lado = it.getString(ladoIndex),
                        prInicial = it.getString(prInicialIndex),
                        prFinal = it.getString(prFinalIndex),
                        cantidad = cantidadYUnidad,
                        tipoUnidad = tipoUnidad,
                        observacion = it.getString(observacionIndex)
                    )
                    actividadesList.add(actividad)
                } while (it.moveToNext())
            }
        }

        // Verifica si la lista quedó vacía después de procesar el cursor

            val noProgramada = ActividadMantenimiento(
                id = -1,
                descripcion = "No programada",
                cuadrillaNombre = "No programada",
                uf = "No programada",
                sentido = "No programada",
                lado = "No programada",
                prInicial = "No programada",
                prFinal = "No programada",
                cantidad = "0 kms",
                tipoUnidad = "kms",
                observacion = "No programada"
            )
            actividadesList.add(noProgramada)


        return actividadesList
    }

    /**
     * Obtiene la información detallada de una actividad de bitácora específica.
     * @param idActividad El ID de la actividad programada (de la tabla programar_actividades_bitacora).
     * @return Un objeto ActividadInfo si se encuentra, o null si no existe.
     */
    fun getActividadInfo(idActividad: Int): ActividadInfo? {
        val db = this.readableDatabase
        var actividadInfo: ActividadInfo? = null

        val query = """
            SELECT pab.id, ab.Descripcion, ab.TipoUnidad, pab.IdCuadrilla
            FROM programar_actividades_bitacora pab
            JOIN actividades_bitacoras ab ON ab.id = pab.idActividad
            WHERE pab.id = ?
        """.trimIndent()

        // El bloque .use se encarga de cerrar el cursor automáticamente
        db.rawQuery(query, arrayOf(idActividad.toString())).use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow("Descripcion"))
                val tipoUnidad = cursor.getString(cursor.getColumnIndexOrThrow("TipoUnidad"))
                val idCuadrilla = cursor.getInt(cursor.getColumnIndexOrThrow("IdCuadrilla"))

                actividadInfo = ActividadInfo(id, descripcion, tipoUnidad, idCuadrilla)
            }
        }
        return actividadInfo
    }

    /**
     * Obtiene la lista de usuarios (empleados) asignados a una actividad a través de su cuadrilla.
     * @param idActividad El ID de la actividad programada.
     * @return Una lista de objetos Usuario.
     */
    fun getUsuariosPorActividad(idActividad: Int): List<Usuario> {
        val db = this.readableDatabase
        val listaUsuarios = mutableListOf<Usuario>()

        val query = """
            SELECT u.id, u.nombre
            FROM programar_actividades_bitacora pab
            JOIN rel_cuadrillas_usuarios rcu ON rcu.IdCuadrilla = pab.IdCuadrilla
            JOIN users u ON u.id = rcu.IdUsuario
            WHERE pab.id = ?
        """.trimIndent()

        // Usamos el bloque .use para seguridad y limpieza automática del cursor
        db.rawQuery(query, arrayOf(idActividad.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))

                listaUsuarios.add(Usuario(id, nombre))
            }
        }
        return listaUsuarios
    }


    // ...

    fun getAllUsers(): List<Usuario> {
        val userList = mutableListOf<Usuario>()
        val db = this.readableDatabase
        val query = "SELECT id, nombre FROM users ORDER BY nombre ASC" // Asume que tu tabla se llama 'users'

        db.rawQuery(query, null).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
                userList.add(Usuario(id, nombre))
            }
        }
        return userList
    }

    fun getUsuarioPorId(usuarioId: Int): Usuario? {
        val db = this.readableDatabase
        var usuario: Usuario? = null
        val query = "SELECT id, nombre FROM users WHERE id = ?"

        db.rawQuery(query, arrayOf(usuarioId.toString())).use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
                usuario = Usuario(id, nombre)
            }
        }
        return usuario
    }


    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }



    fun insertarRegistroBitacora(
        numeroActividad: Int,
        prInicial: String,
        prFinal: String,
        cantidad: Double,
        observacion: String,
        idUsuarios: List<Int>, // <-- Ahora se recibe la lista de usuarios
        fotos: List<File>       // <-- Y la lista de fotos
    ): Boolean {
        val db = this.writableDatabase
        var exito = false

        // INICIAMOS LA TRANSACCIÓN
        db.beginTransaction()
        try {
            val timestamp = getCurrentTimestamp()

            // --- PASO 1: Insertar el registro principal ---
            val valuesRegistro = ContentValues().apply {
                put("idRelProgramarActividadesBitacora", numeroActividad)
                put("PrInicial", prInicial)
                put("PrFinal", prFinal)
                put("Cantidad", cantidad)
                put("Programada", 1)
                put("ObservacionInterna", observacion)
                put("created_at", timestamp)
                put("updated_at", timestamp)
                put("sincronizado", 0)
            }

            val idNuevoRegistro = db.insert("rel_bitacora_actividades", null, valuesRegistro)

            // Si el registro principal falla, detenemos todo.
            if (idNuevoRegistro == -1L) {
                return false
            }

            // --- PASO 2: Insertar todas las fotos ---
            fotos.forEach { file ->
                val valuesFoto = ContentValues().apply {
                    put("idRelProgramarActividadesBitacora", idNuevoRegistro)
                    put("ruta", file.absolutePath)
                    put("estado", 0)
                    put("created_at", timestamp)
                    put("updated_at", timestamp)
                    put("sincronizado", 0)
                }
                db.insert("rel_fotos_bitacora_actividades", null, valuesFoto)
            }

            // --- PASO 3: Insertar todos los usuarios ---
            idUsuarios.forEach { usuarioId ->
                val valuesUsuario = ContentValues().apply {
                    put("idRelProgramarActividadesBitacora", idNuevoRegistro)
                    put("idUsuario", usuarioId)
                    put("created_at", timestamp)
                    put("updated_at", timestamp)
                    put("sincronizado", 0)
                }
                db.insert("rel_usuarios_bitacora_actividades", null, valuesUsuario)
            }

            // Si todos los inserts fueron exitosos, marcamos la transacción como completada
            db.setTransactionSuccessful()
            exito = true

        } finally {
            // Finalizamos la transacción. Si fue exitosa, se guardan los cambios. Si no, se revierten.
            db.endTransaction()
        }

        db.close()
        return exito
    }

    private fun parseRangeString(rangeStr: String?): Pair<Double, Double> {
        // Si el texto de la BD es nulo o vacío, devuelve un rango que no se puede cumplir.
        if (rangeStr.isNullOrBlank()) {
            return Pair(0.0, 0.0)
        }

        // Divide el texto usando el "+" como separador. "456+1231" se convierte en ["456", "1231"]
        val parts = rangeStr.split('+')

        // Si el formato no es correcto (no hay un '+'), devuelve un rango inválido.
        if (parts.size != 2) {
            return Pair(0.0, 0.0)
        }

        // Convierte cada parte a número, eliminando espacios. Si falla, usa 0.0.
        val min = parts[0].trim().toDoubleOrNull() ?: 0.0
        val max = parts[1].trim().toDoubleOrNull() ?: 0.0

        return Pair(min, max)
    }

    // Dentro de DatabaseHelper.kt
    fun getValidationRanges(numeroActividad: Int): ValidationRangesRaw? {
        val db = this.readableDatabase
        var ranges: ValidationRangesRaw? = null

        // Usamos TU consulta para traer los textos crudos
        val query = """
        SELECT PrInicial, PrFinal, Cantidad
        FROM programar_actividades_bitacora
        WHERE id = ?
    """.trimIndent()

        db.rawQuery(query, arrayOf(numeroActividad.toString())).use { cursor ->
            if (cursor.moveToFirst()) {
                ranges = ValidationRangesRaw(
                    prInicialStr = cursor.getString(cursor.getColumnIndexOrThrow("PrInicial")),
                    prFinalStr = cursor.getString(cursor.getColumnIndexOrThrow("PrFinal")),
                    cantidadStr = cursor.getString(cursor.getColumnIndexOrThrow("Cantidad"))
                )
            }
        }
        return ranges
    }

    fun getValidationParams(numeroActividad: Int): ValidationParams? {
        val db = this.readableDatabase
        var params: ValidationParams? = null

        // USAMOS TU CONSULTA EXACTA
        val query = """
        SELECT PrInicial, PrFinal, Cantidad
        FROM programar_actividades_bitacora
        WHERE id = ?
    """.trimIndent()

        db.rawQuery(query, arrayOf(numeroActividad.toString())).use { cursor ->
            if (cursor.moveToFirst()) {
                // 1. Obtenemos los textos tal como vienen de la BD (ej: "456+1231")
                val prInicialStr = cursor.getString(cursor.getColumnIndexOrThrow("PrInicial"))
                val prFinalStr = cursor.getString(cursor.getColumnIndexOrThrow("PrFinal"))
                val cantidadStr = cursor.getString(cursor.getColumnIndexOrThrow("Cantidad"))

                // 2. Usamos nuestra función auxiliar para "traducir" cada texto a un rango
                val prInicialRange = parseRangeString(prInicialStr)
                val prFinalRange = parseRangeString(prFinalStr)
                val cantidadRange = parseRangeString(cantidadStr)

                // 3. Creamos el objeto de parámetros con los valores ya separados
                params = ValidationParams(
                    prInicialMin = prInicialRange.first,  // El número antes del '+'
                    prInicialMax = prInicialRange.second, // El número después del '+'
                    prFinalMin = prFinalRange.first,
                    prFinalMax = prFinalRange.second,
                    cantidadMin = cantidadRange.first,
                    cantidadMax = cantidadRange.second
                )
            }
        }
        return params
    }


    // Dentro de la clase DatabaseHelper.kt

    /**
     * Obtiene todos los registros de bitácora pendientes de sincronizar (sincronizado = 0),
     * incluyendo sus listas de usuarios y fotos asociadas.
     */
    fun obtenerBitacorasPendientes(): List<BitacoraRecord> {
        val listaBitacoras = mutableListOf<BitacoraRecord>()
        val db = this.readableDatabase

        // 1. Obtenemos los registros principales de bitácora pendientes
        val queryPrincipal = "SELECT * FROM rel_bitacora_actividades WHERE sincronizado = 0 and Programada = 1"
        db.rawQuery(queryPrincipal, null).use { cursor ->
            while (cursor.moveToNext()) {
                val idRegistro = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val idProgramada = cursor.getInt(cursor.getColumnIndexOrThrow("id"))

                // 2. Por cada registro, buscamos sus usuarios asociados
                val listaUsuarios = mutableListOf<Int>()
                val queryUsuarios = "SELECT idUsuario FROM rel_usuarios_bitacora_actividades WHERE idRelProgramarActividadesBitacora = ?"
                db.rawQuery(queryUsuarios, arrayOf(idProgramada.toString())).use { userCursor ->
                    while (userCursor.moveToNext()) {
                        listaUsuarios.add(userCursor.getInt(userCursor.getColumnIndexOrThrow("idUsuario")))
                    }
                }
                Log.d("DepuracionUsuarios", "IDs de usuarios encontrados: $idRegistro " + listaUsuarios.toString())

                // 3. Por cada registro, buscamos sus fotos asociadas
                val listaFotos = mutableListOf<File>()
                val queryFotos = "SELECT ruta FROM rel_fotos_bitacora_actividades WHERE idRelProgramarActividadesBitacora = ?"
                db.rawQuery(queryFotos, arrayOf(idProgramada.toString())).use { photoCursor ->
                    while (photoCursor.moveToNext()) {
                        val path = photoCursor.getString(photoCursor.getColumnIndexOrThrow("ruta"))
                        listaFotos.add(File(path))
                    }
                }

                // 4. Creamos el objeto completo y lo añadimos a la lista
                listaBitacoras.add(
                    BitacoraRecord(
                        id = idRegistro,
                        idRelProgramarActividadesBitacora = idProgramada,
                        prInicial = cursor.getString(cursor.getColumnIndexOrThrow("PrInicial")),
                        prFinal = cursor.getString(cursor.getColumnIndexOrThrow("PrFinal")),
                        cantidad = cursor.getDouble(cursor.getColumnIndexOrThrow("Cantidad")),
                        observacion = cursor.getString(cursor.getColumnIndexOrThrow("ObservacionInterna")),
                        usuarios = listaUsuarios,
                        fotos = listaFotos
                    )
                )
            }
        }
        return listaBitacoras
    }

    /**
     * Actualiza el estado de un registro de bitácora a sincronizado (sincronizado = 1).
     */
    fun marcarBitacoraSincronizada(idRegistro: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("sincronizado", 1)
        }
        db.update("rel_bitacora_actividades", values, "id = ?", arrayOf(idRegistro.toString()))
        db.close()
    }


}