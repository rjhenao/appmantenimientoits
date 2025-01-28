package com.example.itsmantenimiento
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "LocalDB", null, 20) {

    override fun onCreate(db: SQLiteDatabase) {

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
        onCreate(db)
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




}