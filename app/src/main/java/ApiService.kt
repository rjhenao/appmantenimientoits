import com.example.itsmantenimiento.Actividades
import com.example.itsmantenimiento.Equipos
import com.example.itsmantenimiento.Locaciones
import com.example.itsmantenimiento.Periodicidad
import com.example.itsmantenimiento.ProgramarMantenimiento
import com.example.itsmantenimiento.RelSistemaLocacion
import com.example.itsmantenimiento.RelSubsistemaSistema
import com.example.itsmantenimiento.Sistemas
import com.example.itsmantenimiento.Subsistemas
import com.example.itsmantenimiento.TipoEquipos
import com.example.itsmantenimiento.Uf
import com.example.itsmantenimiento.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    data class ApiResponse(
        val messagedd2: String,
        val data2: Map<String, Any> ,
        val afirmativo: Integer
    )




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

    @GET("/api/ufs")
    fun getUf(): Call<List<Uf>>

    @Multipart
    @POST("api/enviarMantenimientosTerminados")
    fun enviarMantenimientosTerminados(
        @Part("json") json: RequestBody,
        @Part imagenes: List<MultipartBody.Part>
    ): Call<ApiResponse>



}