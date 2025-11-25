package com.helpdesk.app.data.repository

import com.helpdesk.app.data.api.TicketsApi
import com.helpdesk.app.data.model.*
import com.helpdesk.app.domain.model.*
import com.helpdesk.app.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TicketRepository @Inject constructor(
    private val ticketsApi: TicketsApi
) {
    suspend fun getTickets(filters: TicketFilters = TicketFilters()): Result<List<Ticket>> {
        return try {
            val response = ticketsApi.getTickets(
                status = filters.status?.toApiValue(),
                issueType = filters.issueType?.toApiValue(),
                assignedToMe = filters.assignedToMe,
                limit = filters.limit
            )
            if (response.isSuccessful && response.body() != null) {
                val tickets = response.body()!!.tickets.map { it.toDomain() }
                Result.Success(tickets)
            } else {
                Result.Error(Exception("Failed to fetch tickets: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getTicket(ticketId: String): Result<Ticket> {
        return try {
            val response = ticketsApi.getTicket(ticketId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.ticket.toDomain())
            } else {
                Result.Error(Exception("Failed to fetch ticket: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createTicket(payload: CreateTicketPayload): Result<Ticket> {
        return try {
            val response = ticketsApi.createTicket(
                CreateTicketRequest(
                    description = payload.description,
                    priority = payload.priority.toApiValue(),
                    issueType = payload.issueType.toApiValue(),
                    attachments = payload.attachments.ifEmpty { null }
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.ticket.toDomain())
            } else {
                Result.Error(Exception("Failed to create ticket: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateTicket(ticketId: String, payload: UpdateTicketPayload): Result<Ticket> {
        return try {
            val response = ticketsApi.updateTicket(
                ticketId = ticketId,
                request = UpdateTicketRequest(
                    description = payload.description,
                    priority = payload.priority?.toApiValue(),
                    issueType = payload.issueType?.toApiValue(),
                    status = payload.status?.toApiValue()
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.ticket.toDomain())
            } else {
                Result.Error(Exception("Failed to update ticket: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun assignTicket(ticketId: String, assigneeId: String?): Result<Ticket> {
        return try {
            val response = ticketsApi.assignTicket(
                ticketId = ticketId,
                request = AssignTicketRequest(assigneeId)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.ticket.toDomain())
            } else {
                Result.Error(Exception("Failed to assign ticket: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun resolveTicket(ticketId: String): Result<Ticket> {
        return try {
            val response = ticketsApi.resolveTicket(ticketId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.ticket.toDomain())
            } else {
                Result.Error(Exception("Failed to resolve ticket: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun requestAssignment(ticketId: String): Result<Ticket> {
        return try {
            val response = ticketsApi.requestAssignment(ticketId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.ticket.toDomain())
            } else {
                Result.Error(Exception("Failed to request assignment: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun declineAssignmentRequest(ticketId: String): Result<Ticket> {
        return try {
            val response = ticketsApi.declineAssignmentRequest(ticketId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.ticket.toDomain())
            } else {
                Result.Error(Exception("Failed to decline assignment request: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getTicketActivity(ticketId: String, limit: Int = 50): Result<List<TicketActivityEntry>> {
        return try {
            val response = ticketsApi.getTicketActivity(ticketId, limit)
            if (response.isSuccessful && response.body() != null) {
                val activities = response.body()!!.activities.map { it.toDomain() }
                Result.Success(activities)
            } else {
                Result.Error(Exception("Failed to fetch activity: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
