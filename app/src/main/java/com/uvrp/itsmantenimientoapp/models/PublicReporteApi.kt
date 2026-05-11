package com.uvrp.itsmantenimientoapp.models

import com.google.gson.annotations.SerializedName

data class PublicReporteCatalogosResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("areas") val areas: List<String>?,
    @SerializedName("tipos_solicitud") val tiposSolicitud: List<String>?,
    @SerializedName("max_attachments") val maxAttachments: Int?,
    @SerializedName("max_attachment_kb") val maxAttachmentKb: Int?,
)

data class PublicReporteSubmitResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: PublicReporteSubmitData?,
)

data class PublicReporteSubmitData(
    @SerializedName("id") val id: Int?,
    @SerializedName("ticket_number") val ticketNumber: String?,
)

data class PublicReporteLocacionResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: PublicReporteLocacionData?,
)

data class PublicReporteLocacionData(
    @SerializedName("locacion_id") val locacionId: Int?,
    @SerializedName("nombre") val nombre: String?,
)

data class PublicReporteLocacionesResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: List<PublicReporteLocacionItem>?,
)

data class PublicReporteLocacionItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String?,
)
