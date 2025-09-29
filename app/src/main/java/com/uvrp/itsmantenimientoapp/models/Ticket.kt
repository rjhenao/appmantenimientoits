package com.uvrp.itsmantenimientoapp.models

import com.google.gson.annotations.SerializedName

data class Ticket(
    @SerializedName("id") val id: Int,
    @SerializedName("ticket_number") val ticketNumber: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("status") val status: String,
    @SerializedName("status_text") val statusText: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("priority_text") val priorityText: String,
    @SerializedName("category") val category: String,
    @SerializedName("category_text") val categoryText: String,
    @SerializedName("reported_at") val reportedAt: String,
    @SerializedName("reported_at_formatted") val reportedAtFormatted: String,
    @SerializedName("resolved_at") val resolvedAt: String?,
    @SerializedName("resolution") val resolution: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("reported_by") val reportedBy: TicketUser,
    @SerializedName("assigned_to") val assignedTo: TicketUser?,
    @SerializedName("location") val location: TicketLocation,
    @SerializedName("system") val system: TicketSystem?,
    @SerializedName("subsystem") val subsystem: TicketSubsystem?,
    @SerializedName("equipment") val equipment: TicketEquipment?,
    @SerializedName("comments") val comments: List<TicketComment>,
    @SerializedName("comments_count") val commentsCount: Int
)

data class TicketUser(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String
)

data class TicketLocation(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class TicketSystem(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class TicketSubsystem(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class TicketEquipment(
    @SerializedName("id") val id: Int,
    @SerializedName("tag") val tag: String,
    @SerializedName("type") val type: String?
)

data class TicketComment(
    @SerializedName("id") val id: Int,
    @SerializedName("comment") val comment: String,
    @SerializedName("is_internal") val isInternal: Int, // Cambiado a Int para manejar 0/1
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("created_at_formatted") val createdAtFormatted: String,
    @SerializedName("user") val user: TicketUser
) {
    // Función helper para convertir Int a Boolean
    fun isInternalBoolean(): Boolean = isInternal == 1
}

data class TicketResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<Ticket>,
    @SerializedName("total") val total: Int,
    @SerializedName("timestamp") val timestamp: String
)

data class TicketDetailResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: Ticket,
    @SerializedName("timestamp") val timestamp: String
)

data class TicketStatsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: TicketStats,
    @SerializedName("timestamp") val timestamp: String
)

data class TicketStats(
    @SerializedName("total_tickets") val totalTickets: Int,
    @SerializedName("open_tickets") val openTickets: Int,
    @SerializedName("in_progress_tickets") val inProgressTickets: Int,
    @SerializedName("resolved_tickets") val resolvedTickets: Int,
    @SerializedName("closed_tickets") val closedTickets: Int
)
