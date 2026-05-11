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
import com.uvrp.itsmantenimientoapp.models.Ticket
import com.uvrp.itsmantenimientoapp.models.TicketResponse
import com.uvrp.itsmantenimientoapp.models.TicketDetailResponse
import com.uvrp.itsmantenimientoapp.models.TicketStatsResponse
import com.uvrp.itsmantenimientoapp.models.TicketCreateRequest
import com.uvrp.itsmantenimientoapp.models.TicketCreateResponse
import com.uvrp.itsmantenimientoapp.models.PublicReporteCatalogosResponse
import com.uvrp.itsmantenimientoapp.models.PublicReporteLocacionResponse
import com.uvrp.itsmantenimientoapp.models.PublicReporteLocacionesResponse
import com.uvrp.itsmantenimientoapp.models.PublicReporteSubmitResponse
import com.uvrp.itsmantenimientoapp.models.FotosMasivasRequest
import com.uvrp.itsmantenimientoapp.models.FotosMasivasResponse
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
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

    data class PreoperacionalResponse(
        @SerializedName("success")
        val success: Boolean,
        @SerializedName("id_preoperacional")
        val idPreoperacional: Int?
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

    // Catálogos para campos Sentido y Lado (evitar hardcode en la app)
    data class SentidoCatalogo(
        @SerializedName("nombre")
        val nombre: String
    )

    data class LadoCatalogo(
        @SerializedName("nombre")
        val nombre: String
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

    @POST("/api/abrirpreoperacional")
    fun abrirPreoperacional(@Body datos: PreoperacionalRequest): Call<PreoperacionalResponse>




    @GET("/api/ufs")
    fun getUf(): Call<List<Uf>>

    @GET("/api/sentidos-catalogo")
    fun getSentidosCatalogo(): Call<List<SentidoCatalogo>>

    @GET("/api/lados-catalogo")
    fun getLadosCatalogo(): Call<List<LadoCatalogo>>

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
    ): Call<ResponseBody>



    @POST("api/sincronizarInspeccionCompleta")
    fun sincronizarInspeccionCompleta(@Body data: SincronizacionInspeccion): Call<Void>







    // ===== ENDPOINTS DE TICKETS =====
    
    @GET("/api/tickets")
    fun getTickets(): Call<TicketResponse>
    
    @GET("/api/tickets/{id}")
    fun getTicket(@retrofit2.http.Path("id") id: Int): Call<TicketDetailResponse>
    
    @GET("/api/tickets-stats")
    fun getTicketStats(): Call<TicketStatsResponse>

    @POST("/api/tickets")
    fun createTicket(@retrofit2.http.Body body: TicketCreateRequest): Call<TicketCreateResponse>

    @GET("api/public/reporte-qr/catalogos")
    fun publicReporteCatalogos(): Call<PublicReporteCatalogosResponse>

    @GET("api/public/reporte-qr/locaciones")
    fun publicReporteLocaciones(): Call<PublicReporteLocacionesResponse>

    @GET("api/public/reporte-qr/locacion")
    fun publicReporteLocacion(
        @Query("locacion_id") locacionId: Int,
        @Query("qr_token") qrToken: String,
    ): Call<PublicReporteLocacionResponse>

    @Multipart
    @POST("api/public/reporte-qr/enviar")
    fun publicReporteEnviar(
        @Part("locacion_id") locacionId: RequestBody,
        @Part("qr_token") qrToken: RequestBody,
        @Part("sin_escaneo_qr") sinEscaneoQr: RequestBody,
        @Part("solicitante_nombre") solicitanteNombre: RequestBody,
        @Part("solicitante_area") solicitanteArea: RequestBody,
        @Part("tipo_solicitud") tipoSolicitud: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("email") email: RequestBody,
        @Part attachments: List<MultipartBody.Part>,
    ): Call<PublicReporteSubmitResponse>

    // ===== ENDPOINTS DE FOTOS MASIVAS =====
    
    @POST("/api/sincronizar-fotos-masivas")
    fun sincronizarFotosMasivas(@Body requestBody: FotosMasivasRequest): Call<FotosMasivasResponse>

    // ===== ENDPOINTS DE COMBUSTIBLE =====
    
    data class CombustibleResponse(
        @SerializedName("success")
        val success: Boolean,
        @SerializedName("message")
        val message: String?,
        @SerializedName("data")
        val data: CombustibleData?
    )

    data class CombustibleData(
        @SerializedName("id")
        val id: Int?,
        @SerializedName("id_preoperacional")
        val idPreoperacional: Int?,
        @SerializedName("fecha_tanqueo")
        val fechaTanqueo: String?
    )

    @Multipart
    @POST("/api/sincronizar-combustible")
    fun sincronizarCombustible(
        @Part("json") json: RequestBody,
        @Part foto_ticket: MultipartBody.Part?
    ): Call<CombustibleResponse>

    // ===== LOGIN MÓVIL (SANCTUM) + INVENTARIO =====

    data class MobileLoginRequest(
        val documento: String,
        val password: String
    )

    data class MobileLoginResponse(
        val token: String?,
        @SerializedName("token_type") val tokenType: String?,
        val user: MobileUserInfo?,
        val roles: List<Int>?,
        @SerializedName("puede_inventario") val puedeInventario: Boolean?
    )

    data class MobileUserInfo(
        val id: Int,
        val nombre: String?,
        val documento: Long?
    )

    data class InvUnidadDto(
        val id: Int,
        val codigo: String,
        val nombre: String
    )

    data class InvProductoDto(
        val id: Int,
        @SerializedName("codigo_etiqueta") val codigoEtiqueta: String,
        val nombre: String,
        val tipo: String,
        @SerializedName("inv_unidad_id") val invUnidadId: Int,
        @SerializedName("unidad_codigo") val unidadCodigo: String?,
        val descripcion: String?
    )

    data class InvUbicacionDto(
        val id: Int,
        @SerializedName("codigo_unico_global") val codigoUnicoGlobal: String,
        val label: String
    )

    data class InventarioCatalogoResponse(
        val unidades: List<InvUnidadDto>?,
        val productos: List<InvProductoDto>?,
        val ubicaciones: List<InvUbicacionDto>?,
        val meta: Map<String, Any>?
    )

    data class ExistenciaItemDto(
        @SerializedName("inv_ubicacion_id") val invUbicacionId: Int,
        val label: String,
        val cantidad: String
    )

    data class ExistenciasInventarioResponse(
        val data: List<ExistenciaItemDto>?
    )

    data class UbicacionExistenciaProductoDto(
        @SerializedName("inv_producto_id") val invProductoId: Int,
        @SerializedName("codigo_etiqueta") val codigoEtiqueta: String,
        val nombre: String,
        val tipo: String,
        @SerializedName("unidad_codigo") val unidadCodigo: String?,
        val cantidad: String
    )

    data class UbicacionExistenciasUbicacionDto(
        val id: Int,
        @SerializedName("codigo_unico_global") val codigoUnicoGlobal: String,
        val label: String,
        @SerializedName("bodega_codigo") val bodegaCodigo: String?,
        @SerializedName("bodega_nombre") val bodegaNombre: String?,
        @SerializedName("estante_codigo") val estanteCodigo: String?,
        val fila: Int?,
        val columna: Int?
    )

    data class UbicacionExistenciasResumenDto(
        @SerializedName("lineas_con_stock") val lineasConStock: Int?,
        @SerializedName("productos_distintos") val productosDistintos: Int?,
        @SerializedName("por_tipo") val porTipo: Map<String, Int>?
    )

    data class UbicacionExistenciasResponse(
        val ubicacion: UbicacionExistenciasUbicacionDto?,
        val resumen: UbicacionExistenciasResumenDto?,
        val data: List<UbicacionExistenciaProductoDto>?
    )

    data class InventarioMensajeResponse(
        val message: String?
    )

    data class InventarioAjusteRequest(
        @SerializedName("inv_producto_id") val invProductoId: Int,
        @SerializedName("inv_ubicacion_id") val invUbicacionId: Int,
        val cantidad: String,
        val nota: String?
    )

    data class InventarioSalidaRequest(
        @SerializedName("inv_producto_id") val invProductoId: Int,
        @SerializedName("inv_ubicacion_id") val invUbicacionId: Int,
        val cantidad: String,
        @SerializedName("tipo_movimiento") val tipoMovimiento: String,
        @SerializedName("responsable_recibe_nombre") val responsableRecibeNombre: String? = null,
        @SerializedName("responsable_recibe_documento") val responsableRecibeDocumento: String? = null,
        @SerializedName("destino_uso") val destinoUso: String? = null,
        @SerializedName("prestado_a_nombre") val prestadoANombre: String? = null,
        @SerializedName("prestado_a_documento") val prestadoADocumento: String? = null,
        @SerializedName("observacion_salida") val observacionSalida: String? = null
    )

    @POST("api/mobile/login")
    fun mobileLogin(@Body body: MobileLoginRequest): Call<MobileLoginResponse>

    @GET("api/inventario/sync/catalogo")
    fun inventarioCatalogoOffline(): Call<InventarioCatalogoResponse>

    @GET("api/inventario/productos/{id}/existencias")
    fun inventarioExistencias(@retrofit2.http.Path("id") productoId: Int): Call<ExistenciasInventarioResponse>

    @GET("api/inventario/ubicaciones/{id}/existencias")
    fun inventarioUbicacionExistencias(@retrofit2.http.Path("id") ubicacionId: Int): Call<UbicacionExistenciasResponse>

    @POST("api/inventario/stock/ajuste-entrada")
    fun inventarioAjusteEntrada(@Body body: InventarioAjusteRequest): Call<InventarioMensajeResponse>

    @POST("api/inventario/movimientos/salida")
    fun inventarioSalida(@Body body: InventarioSalidaRequest): Call<InventarioMensajeResponse>

    // ===== EXTRAS (HORAS EXTRAS) — OFFLINE FIRST =====

    data class ExtraTurnoDto(
        val codigo: Int,
        val label: String,
        val start: String?,
        val end: String?,
        @SerializedName("next_day_end") val nextDayEnd: Boolean?
    )

    data class ExtrasTurnosCatalogoResponse(
        val data: List<ExtraTurnoDto>?
    )

    data class ExtraHourSyncItem(
        @SerializedName("client_uuid") val clientUuid: String,
        @SerializedName("fecha_inicial") val fechaInicial: String,
        @SerializedName("fecha_final") val fechaFinal: String,
        @SerializedName("turno_codigo") val turnoCodigo: Int,
        @SerializedName("aplica_antes") val aplicaAntes: Boolean,
        @SerializedName("horas_antes") val horasAntes: Double?,
        @SerializedName("hora_inicio_antes") val horaInicioAntes: String? = null,
        @SerializedName("hora_fin_antes") val horaFinAntes: String? = null,
        @SerializedName("aplica_despues") val aplicaDespues: Boolean,
        @SerializedName("horas_despues") val horasDespues: Double?,
        @SerializedName("hora_inicio_despues") val horaInicioDespues: String? = null,
        @SerializedName("hora_fin_despues") val horaFinDespues: String? = null,
        @SerializedName("autorizo_nombre") val autorizoNombre: String,
        val observacion: String,
        @SerializedName("cargado_en") val cargadoEn: String?
    )

    data class ExtrasSyncRequest(
        val extras: List<ExtraHourSyncItem>
    )

    data class ExtrasSyncResponse(
        val created: Int?,
        val ignored: Int?,
        val ids: List<Int>?
    )

    @GET("api/extras/catalogo/turnos")
    fun extrasCatalogoTurnos(): Call<ExtrasTurnosCatalogoResponse>

    @POST("api/extras/sync")
    fun extrasSync(@Body body: ExtrasSyncRequest): Call<ExtrasSyncResponse>

}