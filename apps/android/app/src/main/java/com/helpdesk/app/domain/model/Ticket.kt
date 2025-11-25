package com.helpdesk.app.domain.model

enum class TicketPriority {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun fromString(value: String): TicketPriority {
            return when (value.lowercase()) {
                "low" -> LOW
                "medium" -> MEDIUM
                "high" -> HIGH
                else -> MEDIUM
            }
        }
    }

    fun toApiValue(): String = name.lowercase()
}

enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED;

    companion object {
        fun fromString(value: String): TicketStatus {
            return when (value.lowercase()) {
                "open" -> OPEN
                "in_progress" -> IN_PROGRESS
                "resolved" -> RESOLVED
                else -> OPEN
            }
        }
    }

    fun toApiValue(): String = name.lowercase()

    fun displayName(): String {
        return when (this) {
            OPEN -> "Open"
            IN_PROGRESS -> "In Progress"
            RESOLVED -> "Resolved"
        }
    }
}

enum class IssueType {
    HARDWARE,
    SOFTWARE,
    NETWORK,
    ACCESS,
    OTHER;

    companion object {
        fun fromString(value: String): IssueType {
            return when (value.lowercase()) {
                "hardware" -> HARDWARE
                "software" -> SOFTWARE
                "network" -> NETWORK
                "access" -> ACCESS
                "other" -> OTHER
                else -> OTHER
            }
        }
    }

    fun toApiValue(): String = name.lowercase()
}

data class TicketUser(
    val id: String,
    val name: String,
    val email: String
)

data class Ticket(
    val id: String,
    val description: String,
    val priority: TicketPriority,
    val issueType: IssueType,
    val status: TicketStatus,
    val attachments: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val resolvedAt: String?,
    val creator: TicketUser,
    val assignee: TicketUser?,
    val assignmentRequest: TicketUser?,
    val pendingSync: Boolean = false,
    val pendingAction: String? = null,
    val isLocalOnly: Boolean = false
)

data class CreateTicketPayload(
    val description: String,
    val priority: TicketPriority,
    val issueType: IssueType,
    val attachments: List<String> = emptyList()
)

data class UpdateTicketPayload(
    val description: String? = null,
    val priority: TicketPriority? = null,
    val issueType: IssueType? = null,
    val status: TicketStatus? = null
)

enum class TicketActivityType {
    STATUS_CHANGE,
    ASSIGNMENT_CHANGE,
    ASSIGNMENT_REQUEST,
    TICKET_UPDATE;

    companion object {
        fun fromString(value: String): TicketActivityType {
            return when (value.lowercase()) {
                "status_change" -> STATUS_CHANGE
                "assignment_change" -> ASSIGNMENT_CHANGE
                "assignment_request" -> ASSIGNMENT_REQUEST
                "ticket_update" -> TICKET_UPDATE
                else -> TICKET_UPDATE
            }
        }
    }
}

data class TicketActivityActor(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole
)

data class TicketActivityEntry(
    val id: String,
    val ticketId: String,
    val type: TicketActivityType,
    val createdAt: String,
    val actor: TicketActivityActor,
    val fromStatus: TicketStatus?,
    val toStatus: TicketStatus?,
    val fromAssignee: TicketUser?,
    val toAssignee: TicketUser?
) {
    fun describe(): String {
        return when (type) {
            TicketActivityType.STATUS_CHANGE -> {
                val from = fromStatus?.displayName() ?: "Unknown"
                val to = toStatus?.displayName() ?: "Unknown"
                "${actor.name} changed status from $from to $to"
            }
            TicketActivityType.ASSIGNMENT_CHANGE -> {
                val to = toAssignee?.name ?: "Unassigned"
                "${actor.name} assigned ticket to $to"
            }
            TicketActivityType.ASSIGNMENT_REQUEST -> {
                "${actor.name} requested assignment"
            }
            TicketActivityType.TICKET_UPDATE -> {
                "${actor.name} updated the ticket"
            }
        }
    }
}

data class TicketFilters(
    val status: TicketStatus? = null,
    val issueType: IssueType? = null,
    val assignedToMe: Boolean? = null,
    val limit: Int? = null
)
