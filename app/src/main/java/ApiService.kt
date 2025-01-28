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
import retrofit2.Call
import retrofit2.http.GET
interface ApiService {
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

}