import com.uvrp.itsmantenimientoapp.ActividadFormato
import com.uvrp.itsmantenimientoapp.Actividades
import com.uvrp.itsmantenimientoapp.Equipos
import com.uvrp.itsmantenimientoapp.Locaciones
import com.uvrp.itsmantenimientoapp.Periodicidad
import com.uvrp.itsmantenimientoapp.ProgramarMantenimiento
import com.uvrp.itsmantenimientoapp.RelRolesUsuarios
import com.uvrp.itsmantenimientoapp.RelSistemaLocacion
import com.uvrp.itsmantenimientoapp.RelSubsistemaSistema
import com.uvrp.itsmantenimientoapp.RelVehiculos
import com.uvrp.itsmantenimientoapp.Sistemas
import com.uvrp.itsmantenimientoapp.Subsistemas
import com.uvrp.itsmantenimientoapp.TipoEquipos
import com.uvrp.itsmantenimientoapp.Uf
import com.uvrp.itsmantenimientoapp.User
import com.uvrp.itsmantenimientoapp.UsuarioValidadoResponse
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query

interface ApiService {

    data class SincronizacionInspeccion(
        @SerializedName("inspecciones") // <-- ¡Esta es la clave!
        val usuarios: List<InspeccionUsuario>,

        @SerializedName("actividades")
        val actividades: List<RelInspeccionActividad>
    )


    data class ApiResponse(
        val messagedd2: String,
        val data2: Map<String, Any> ,
        val afirmativo: Integer
    )

    data class Vehiculo(
        val id: Int,
        val placa: String,
        val nombre: String
    )

    data class UsuarioVehiculo(
        val idUsuario: Int,
        val placa: String,
        val idVehiculo: Int,
        val nombre: String ,
        val estado: Int
    )

    data class PreoperacionalRequest(
        val idVehiculo: Int,
        val idUsuario: Int
    )




    data class ValidarVehiculoResponse(
        @SerializedName("vehiculo_con_preoperacional_abierto")
        val vehiculo_con_preoperacional_abierto: Boolean,

        @SerializedName("vehiculo_usuario_con_preoperacional_abierto")
        val vehiculo_usuario_con_preoperacional_abierto: Boolean,

        @SerializedName("licencia_vencida_estado")
        val licencia_vencida_estado: Boolean,

        @SerializedName("licencia_bloqueadas")
        val licencia_bloqueadas: Boolean,

        @SerializedName("licencia_vencida_fecha")
        val licencia_vencida_fecha: Boolean,

        @SerializedName("v_full_amparo")
        val v_full_amparo: Boolean,

        @SerializedName("v_impuesto")
        val v_impuesto: Boolean,

        @SerializedName("v_soat")
        val v_soat: Boolean,

        @SerializedName("v_tecnomecanica")
        val v_tecnomecanica: Boolean,

        @SerializedName("v_estado")
        val v_estado: Boolean,

        @SerializedName("aVehiculo")
        val aVehiculo: List<UsuarioVehiculo>,

        @SerializedName("aUsuario")
        val aUsuario: List<UsuarioVehiculo>
    )

    data class BitacoraMantenimiento(
        // Los nombres de las propiedades AHORA coinciden con las columnas de la BD
        @SerializedName("id") val id: Int,
        @SerializedName("FechaInicio") val FechaInicio: String,
        @SerializedName("FechaFin") val FechaFin: String,
        @SerializedName("idUsuario") val idUsuario: Int,
        @SerializedName("estado") val estado: Int,
        @SerializedName("Observacion") val Observacion: String?,
        @SerializedName("created_at") val created_at: String?,
        @SerializedName("updated_at") val updated_at: String?
    )

    data class ActividadBitacora(
        @SerializedName("id") val id: Int,
        @SerializedName("Descripcion") val Descripcion: String,
        @SerializedName("Estado") val Estado: Int,
        @SerializedName("TipoUnidad") val TipoUnidad: String,
        @SerializedName("Indicador") val Indicador: String,
        @SerializedName("created_at") val created_at: String?,
        @SerializedName("updated_at") val updated_at: String?
    )

    data class ProgramarActividadBitacora(
        @SerializedName("id") val id: Int,
        @SerializedName("idBitacora") val idBitacora: Int,
        @SerializedName("idActividad") val idActividad: Int,
        @SerializedName("PrInicial") val PrInicial: String,
        @SerializedName("PrFinal") val PrFinal: String?,
        @SerializedName("IdCuadrilla") val IdCuadrilla: Int,
        @SerializedName("UF") val UF: Int,
        @SerializedName("Sentido") val Sentido: String,
        @SerializedName("Lado") val Lado: String,
        @SerializedName("Cantidad") val Cantidad: String, // Cambiado de Double a String para coincidir con el JSON
        @SerializedName("Estado") val Estado: Int,
        @SerializedName("Observacion") val Observacion: String?,
        @SerializedName("supervisorResponsable") val supervisorResponsable: Int,
        @SerializedName("created_at") val created_at: String?,
        @SerializedName("updated_at") val updated_at: String?
    )

    data class RelBitacoraActividad(
        @SerializedName("id") val id: Int,
        @SerializedName("idRelProgramarActividadesBitacora") val idRelProgramarActividadesBitacora: Int,
        @SerializedName("PrInicial") val PrInicial: String,
        @SerializedName("PrFinal") val PrFinal: String,
        @SerializedName("Cantidad") val Cantidad: Double, // Ver nota abajo
        @SerializedName("Programada") val Programada: Int,
        @SerializedName("ObservacionInterna") val ObservacionInterna: String?,
        @SerializedName("sincronizado") val sincronizado: Int,
        @SerializedName("created_at") val created_at: String?,
        @SerializedName("updated_at") val updated_at: String?
    )

    data class RelFotosBitacoraActividad(
        @SerializedName("id") val id: Int,
        @SerializedName("idRelProgramarActividadesBitacora") val idRelProgramarActividadesBitacora: Int,
        @SerializedName("ruta") val ruta: String,
        @SerializedName("estado") val estado: Int,
        @SerializedName("sincronizado") val sincronizado: Int,
        @SerializedName("created_at") val created_at: String?,
        @SerializedName("updated_at") val updated_at: String?
    )

    data class RelCuadrillaUsuario(
        @SerializedName("id") val id: Int,
        @SerializedName("IdCuadrilla") val IdCuadrilla: Int,
        @SerializedName("IdUsuario") val IdUsuario: Int,
        @SerializedName("created_at") val created_at: String?,
        @SerializedName("updated_at") val updated_at: String?
    )


    data class Cuadrilla(
        @SerializedName("id") val id: Int,
        @SerializedName("Nombre") val Nombre: String,
        @SerializedName("Descripcion") val Descripcion: String?,
        @SerializedName("created_at") val created_at: String?,
        @SerializedName("updated_at") val updated_at: String?
    )

    data class InspeccionUsuario(
        @SerializedName("id") val id: Int,
        @SerializedName("idUsuario") val idUsuario: Int,
        @SerializedName("fecha") val fecha: String,
        @SerializedName("created_at") val created_at: String?,
        @SerializedName("updated_at") val updated_at: String?
    )

    data class ActividadInspeccion(
        @SerializedName("id") val id: Int,
        @SerializedName("descripcion") val descripcion: String,
        @SerializedName("estado") val estado: Int,
        @SerializedName("created_at") val created_at: String?,
        @SerializedName("updated_at") val updated_at: String?
    )

    data class RelInspeccionActividad(
        @SerializedName("id") val id: Int,
        @SerializedName("idInspeccionUsuarios") val idInspeccionUsuarios: Int,
        @SerializedName("idActividadInspeccion") val idActividadInspeccion: Int,
        @SerializedName("estado") val estado: Int,
        @SerializedName("created_at") val created_at: String?,
        @SerializedName("updated_at") val updated_at: String?
    )

    data class ValidarVehiculoRequest(
        val idusuario: Int,
        val idvehiculo: Int
    )

    data class PreoperacionalVehiculo(
        val idUsuario: Int,
        val placa: String,
        val idVehiculo: Int
    )




    @GET("/api/validar-vehiculos-licencia")
    fun validarVehiculoLicencia(
        @Query("idusuario") idUsuario: Int,
        @Query("idvehiculo") idVehiculo: Int
    ): Call<ValidarVehiculoResponse>



    @GET("/api/programar_mantenimientos")
    fun getProgramarMantenimientos(): Call<List<ProgramarMantenimiento>>

    @GET("/api/users")
    fun getUsers(): Call<List<User>>

    @GET("/api/actividades")
    fun getActividades(): Call<List<Actividades>>

    @GET("/api/equipos")
    fun getEquipos(): Call<List<Equipos>>

    @GET("/api/locaciones")
    fun getLocaciones(): Call<List<Locaciones>>

    @GET("/api/periodicidad")
    fun getPeriodicidad(): Call<List<Periodicidad>>

    @GET("/api/relsistemalocacion")
    fun getRelSistemaLocacion(): Call<List<RelSistemaLocacion>>

    @GET("/api/relsubsistemasistema")
    fun getRelSubsistemaSistema(): Call<List<RelSubsistemaSistema>>

    @GET("/api/sistemas")
    fun getSistemas(): Call<List<Sistemas>>

    @GET("/api/subsistemas")
    fun getSubSistemas(): Call<List<Subsistemas>>

    @GET("/api/tipoequipos")
    fun getTipoEquipos(): Call<List<TipoEquipos>>

    @GET("/api/relrolesusuarios")
    fun relRolesUsuarios(): Call<List<RelRolesUsuarios>>

    @GET("/api/vehiculos")
    suspend fun getVehiculos(): List<Vehiculo>

    @GET("api/validarUsuario")
    fun validarUsuario(@Query("idUsuario") idUsuario: Int): Call<UsuarioValidadoResponse>


    @GET("/api/actividadesinspeccion")
    fun getActividadesInspeccion(): Call<List<ActividadInspeccion>>

    @GET("/api/inspeccionusuario")
    fun getInspeccionUsuarios(): Call<List<InspeccionUsuario>>

    @GET("/api/relinspeccionactividades")
    fun getRelInspeccionActividad(): Call<List<RelInspeccionActividad>>

    // ----
    @GET("/api/bitacoras")
    fun getBitacoraMantenimientos(): Call<List<BitacoraMantenimiento>>

    @GET("/api/actividadesbitacora")
    fun getActividadesBitacoras(): Call<List<ActividadBitacora>>

    @GET("/api/programaractividadesbitacora")
    fun getProgramarActividadesBitacora(): Call<List<ProgramarActividadBitacora>>

    @GET("/api/relbitacoraactividades")
    fun getRelBitacoraActividades(): Call<List<RelBitacoraActividad>>

    @GET("/api/relfotobitacoraactividades")
    fun getRelFotosBitacoraActividades(): Call<List<RelFotosBitacoraActividad>>

    @GET("/api/relcuadrillausuarios")
    fun getRelCuadrillasUsuarios(): Call<List<RelCuadrillaUsuario>>


    @GET("/api/cuadrillas")
    fun getCuadrillas(): Call<List<Cuadrilla>>



    @GET("api/actividades-formato")
    fun getActividadesFormato(@Query("idVehiculo") idVehiculo: Int): Call<List<ActividadFormato>>

    @POST("api/abrirpreoperacional")
    fun abrirPreoperacional(@Body datos: PreoperacionalRequest): Call<Void>




    @GET("/api/ufs")
    fun getUf(): Call<List<Uf>>

    @Multipart
    @POST("api/enviarMantenimientosTerminados")
    fun enviarMantenimientosTerminados(
        @Part("json") json: RequestBody,
        @Part imagenes: List<MultipartBody.Part>
    ): Call<ApiResponse>


    @Multipart
    @POST("api/iniciarpreoperacional")
    fun iniciarPreoperacional(
        @Part("json") json: RequestBody,
        @Part imagenes: List<MultipartBody.Part>
    ): Call<Void>

    @Multipart
    @POST("api/finalizarpreoperacional")
    fun finalizarPreoperacional(
        @Part("json") json: RequestBody,
        @Part imagenes: List<MultipartBody.Part>
    ): Call<Void>

    @Multipart
    @POST("api/finalizarMantenimiento")
    fun finalizarMantenimiento(
        @Part("json") json: RequestBody,
        @Part imagenes: List<MultipartBody.Part>
    ): Call<Void>




    @Multipart
    @POST("api/finalizaractividadbitacora")
    fun finalizarMantenimientoBitacora(
        @Part("json") json: RequestBody, // <-- CAMBIA @PartMap POR @Part("json")
        @Part imagenes: List<MultipartBody.Part>
    ): Call<Void>



    @POST("api/sincronizarInspeccionCompleta")
    fun sincronizarInspeccionCompleta(@Body data: SincronizacionInspeccion): Call<Void>







}