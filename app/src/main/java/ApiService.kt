import com.example.itsmantenimiento.ActividadFormato
import com.example.itsmantenimiento.Actividades
import com.example.itsmantenimiento.Equipos
import com.example.itsmantenimiento.Locaciones
import com.example.itsmantenimiento.Periodicidad
import com.example.itsmantenimiento.ProgramarMantenimiento
import com.example.itsmantenimiento.RelRolesUsuarios
import com.example.itsmantenimiento.RelSistemaLocacion
import com.example.itsmantenimiento.RelSubsistemaSistema
import com.example.itsmantenimiento.RelVehiculos
import com.example.itsmantenimiento.Sistemas
import com.example.itsmantenimiento.Subsistemas
import com.example.itsmantenimiento.TipoEquipos
import com.example.itsmantenimiento.Uf
import com.example.itsmantenimiento.User
import com.example.itsmantenimiento.UsuarioValidadoResponse
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

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




}