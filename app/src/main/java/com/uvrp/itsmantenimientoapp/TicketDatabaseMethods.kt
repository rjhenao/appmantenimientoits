package com.uvrp.itsmantenimientoapp

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.uvrp.itsmantenimientoapp.models.Ticket
import com.uvrp.itsmantenimientoapp.models.TicketComment
import com.uvrp.itsmantenimientoapp.models.TicketUser
import com.uvrp.itsmantenimientoapp.models.TicketLocation
import com.uvrp.itsmantenimientoapp.models.TicketSystem
import com.uvrp.itsmantenimientoapp.models.TicketSubsystem
import com.uvrp.itsmantenimientoapp.models.TicketEquipment

// Extensiones para DatabaseHelper - Métodos de Tickets
fun DatabaseHelper.obtenerTickets(): List<Ticket> {
    val lista = mutableListOf<Ticket>()
    val db = this.readableDatabase
    val cursor = db.rawQuery("SELECT * FROM tickets ORDER BY created_at DESC", null)

    if (cursor.moveToFirst()) {
        do {
            val ticket = Ticket(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                ticketNumber = cursor.getString(cursor.getColumnIndexOrThrow("ticket_number")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                statusText = cursor.getString(cursor.getColumnIndexOrThrow("status_text")),
                priority = cursor.getString(cursor.getColumnIndexOrThrow("priority")),
                priorityText = cursor.getString(cursor.getColumnIndexOrThrow("priority_text")),
                category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                categoryText = cursor.getString(cursor.getColumnIndexOrThrow("category_text")),
                reportedAt = cursor.getString(cursor.getColumnIndexOrThrow("reported_at")),
                reportedAtFormatted = cursor.getString(cursor.getColumnIndexOrThrow("reported_at_formatted")),
                resolvedAt = cursor.getString(cursor.getColumnIndexOrThrow("resolved_at")),
                resolution = cursor.getString(cursor.getColumnIndexOrThrow("resolution")),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at")),
                reportedBy = TicketUser(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("reported_by_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("reported_by_name")),
                    email = cursor.getString(cursor.getColumnIndexOrThrow("reported_by_email"))
                ),
                assignedTo = if (cursor.getInt(cursor.getColumnIndexOrThrow("assigned_to_id")) != 0) {
                    TicketUser(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("assigned_to_id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("assigned_to_name")),
                        email = cursor.getString(cursor.getColumnIndexOrThrow("assigned_to_email"))
                    )
                } else null,
                location = TicketLocation(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("location_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("location_name"))
                ),
                system = if (cursor.getInt(cursor.getColumnIndexOrThrow("system_id")) != 0) {
                    TicketSystem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("system_id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("system_name"))
                    )
                } else null,
                subsystem = if (cursor.getInt(cursor.getColumnIndexOrThrow("subsystem_id")) != 0) {
                    TicketSubsystem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("subsystem_id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("subsystem_name"))
                    )
                } else null,
                equipment = if (cursor.getInt(cursor.getColumnIndexOrThrow("equipment_id")) != 0) {
                    TicketEquipment(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("equipment_id")),
                        tag = cursor.getString(cursor.getColumnIndexOrThrow("equipment_tag")),
                        type = cursor.getString(cursor.getColumnIndexOrThrow("equipment_type"))
                    )
                } else null,
                comments = this.obtenerComentariosTicket(cursor.getInt(cursor.getColumnIndexOrThrow("id"))),
                commentsCount = cursor.getInt(cursor.getColumnIndexOrThrow("comments_count"))
            )
            lista.add(ticket)
        } while (cursor.moveToNext())
    }
    cursor.close()
    db.close()
    return lista
}

fun DatabaseHelper.obtenerComentariosTicket(ticketId: Int): List<TicketComment> {
    val lista = mutableListOf<TicketComment>()
    val db = this.readableDatabase
    val cursor = db.rawQuery("SELECT * FROM ticket_comments WHERE ticket_id = ? ORDER BY created_at DESC", arrayOf(ticketId.toString()))

    if (cursor.moveToFirst()) {
        do {
            val comment = TicketComment(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                comment = cursor.getString(cursor.getColumnIndexOrThrow("comment")),
                isInternal = cursor.getInt(cursor.getColumnIndexOrThrow("is_internal")),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                createdAtFormatted = cursor.getString(cursor.getColumnIndexOrThrow("created_at_formatted")),
                user = TicketUser(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("user_name")),
                    email = ""
                )
            )
            lista.add(comment)
        } while (cursor.moveToNext())
    }
    cursor.close()
    db.close()
    return lista
}

fun DatabaseHelper.insertarOActualizarTickets(tickets: List<Ticket>) {
    val db = this.writableDatabase
    db.beginTransaction()
    try {
        // Limpiar tabla de tickets
        db.execSQL("DELETE FROM tickets")
        db.execSQL("DELETE FROM ticket_comments")

        for (ticket in tickets) {
            val values = ContentValues().apply {
                put("id", ticket.id)
                put("ticket_number", ticket.ticketNumber)
                put("title", ticket.title)
                put("description", ticket.description)
                put("status", ticket.status)
                put("status_text", ticket.statusText)
                put("priority", ticket.priority)
                put("priority_text", ticket.priorityText)
                put("category", ticket.category)
                put("category_text", ticket.categoryText)
                put("reported_at", ticket.reportedAt)
                put("reported_at_formatted", ticket.reportedAtFormatted)
                put("resolved_at", ticket.resolvedAt)
                put("resolution", ticket.resolution)
                put("created_at", ticket.createdAt)
                put("updated_at", ticket.updatedAt)
                put("reported_by_id", ticket.reportedBy.id)
                put("reported_by_name", ticket.reportedBy.name)
                put("reported_by_email", ticket.reportedBy.email)
                put("assigned_to_id", ticket.assignedTo?.id ?: 0)
                put("assigned_to_name", ticket.assignedTo?.name ?: "")
                put("assigned_to_email", ticket.assignedTo?.email ?: "")
                put("location_id", ticket.location.id)
                put("location_name", ticket.location.name)
                put("system_id", ticket.system?.id ?: 0)
                put("system_name", ticket.system?.name ?: "")
                put("subsystem_id", ticket.subsystem?.id ?: 0)
                put("subsystem_name", ticket.subsystem?.name ?: "")
                put("equipment_id", ticket.equipment?.id ?: 0)
                put("equipment_tag", ticket.equipment?.tag ?: "")
                put("equipment_type", ticket.equipment?.type ?: "")
                put("comments_count", ticket.commentsCount)
            }
            db.insert("tickets", null, values)

            // Insertar comentarios del ticket
            for (comment in ticket.comments) {
                val commentValues = ContentValues().apply {
                    put("id", comment.id)
                    put("ticket_id", ticket.id)
                    put("comment", comment.comment)
                    put("is_internal", comment.isInternal)
                    put("created_at", comment.createdAt)
                    put("created_at_formatted", comment.createdAtFormatted)
                    put("user_id", comment.user.id)
                    put("user_name", comment.user.name)
                }
                db.insert("ticket_comments", null, commentValues)
            }
        }
        db.setTransactionSuccessful()
    } catch (e: Exception) {
        Log.e("DatabaseHelper", "Error al insertar tickets", e)
    } finally {
        db.endTransaction()
        db.close()
    }
}

fun DatabaseHelper.obtenerTicketsPorEstado(estados: List<String>): List<Ticket> {
    val lista = mutableListOf<Ticket>()
    val db = this.readableDatabase
    val placeholders = estados.joinToString(",") { "?" }
    val cursor = db.rawQuery("SELECT * FROM tickets WHERE status IN ($placeholders) ORDER BY created_at DESC", estados.toTypedArray())

    if (cursor.moveToFirst()) {
        do {
            val ticket = Ticket(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                ticketNumber = cursor.getString(cursor.getColumnIndexOrThrow("ticket_number")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                statusText = cursor.getString(cursor.getColumnIndexOrThrow("status_text")),
                priority = cursor.getString(cursor.getColumnIndexOrThrow("priority")),
                priorityText = cursor.getString(cursor.getColumnIndexOrThrow("priority_text")),
                category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                categoryText = cursor.getString(cursor.getColumnIndexOrThrow("category_text")),
                reportedAt = cursor.getString(cursor.getColumnIndexOrThrow("reported_at")),
                reportedAtFormatted = cursor.getString(cursor.getColumnIndexOrThrow("reported_at_formatted")),
                resolvedAt = cursor.getString(cursor.getColumnIndexOrThrow("resolved_at")),
                resolution = cursor.getString(cursor.getColumnIndexOrThrow("resolution")),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at")),
                reportedBy = TicketUser(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("reported_by_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("reported_by_name")),
                    email = cursor.getString(cursor.getColumnIndexOrThrow("reported_by_email"))
                ),
                assignedTo = if (cursor.getInt(cursor.getColumnIndexOrThrow("assigned_to_id")) != 0) {
                    TicketUser(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("assigned_to_id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("assigned_to_name")),
                        email = cursor.getString(cursor.getColumnIndexOrThrow("assigned_to_email"))
                    )
                } else null,
                location = TicketLocation(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("location_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("location_name"))
                ),
                system = if (cursor.getInt(cursor.getColumnIndexOrThrow("system_id")) != 0) {
                    TicketSystem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("system_id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("system_name"))
                    )
                } else null,
                subsystem = if (cursor.getInt(cursor.getColumnIndexOrThrow("subsystem_id")) != 0) {
                    TicketSubsystem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("subsystem_id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("subsystem_name"))
                    )
                } else null,
                equipment = if (cursor.getInt(cursor.getColumnIndexOrThrow("equipment_id")) != 0) {
                    TicketEquipment(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("equipment_id")),
                        tag = cursor.getString(cursor.getColumnIndexOrThrow("equipment_tag")),
                        type = cursor.getString(cursor.getColumnIndexOrThrow("equipment_type"))
                    )
                } else null,
                comments = this.obtenerComentariosTicket(cursor.getInt(cursor.getColumnIndexOrThrow("id"))),
                commentsCount = cursor.getInt(cursor.getColumnIndexOrThrow("comments_count"))
            )
            lista.add(ticket)
        } while (cursor.moveToNext())
    }
    cursor.close()
    db.close()
    return lista
}
