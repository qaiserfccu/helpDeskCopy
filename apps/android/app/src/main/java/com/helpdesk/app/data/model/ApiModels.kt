package com.helpdesk.app.data.model

import com.google.gson.annotations.SerializedName
import com.helpdesk.app.domain.model.*

// Auth DTOs
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String? = null
)

data class RefreshRequest(
    val refreshToken: String
)

data class AuthResponse(
    val user: UserDto,
    val tokens: TokensDto
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String
) {
    fun toDomain(): User = User(
        id = id,
        name = name,
        email = email,
        role = UserRole.fromString(role)
    )
}

data class TokensDto(
    val accessToken: String,
    val refreshToken: String
)

// Ticket DTOs
data class TicketUserDto(
    val id: String,
    val name: String,
    val email: String
) {
    fun toDomain(): TicketUser = TicketUser(id, name, email)
}

data class TicketDto(
    val id: String,
    val description: String,
    val priority: String,
    val issueType: String,
    val status: String,
    val attachments: List<String>? = null,
    val createdAt: String,
    val updatedAt: String,
    val resolvedAt: String? = null,
    val creator: TicketUserDto,
    val assignee: TicketUserDto? = null,
    val assignmentRequest: TicketUserDto? = null
) {
    fun toDomain(): Ticket = Ticket(
        id = id,
        description = description,
        priority = TicketPriority.fromString(priority),
        issueType = IssueType.fromString(issueType),
        status = TicketStatus.fromString(status),
        attachments = attachments ?: emptyList(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        resolvedAt = resolvedAt,
        creator = creator.toDomain(),
        assignee = assignee?.toDomain(),
        assignmentRequest = assignmentRequest?.toDomain()
    )
}

data class TicketListResponse(
    val tickets: List<TicketDto>
)

data class TicketResponse(
    val ticket: TicketDto
)

data class CreateTicketRequest(
    val description: String,
    val priority: String,
    val issueType: String,
    val attachments: List<String>? = null
)

data class UpdateTicketRequest(
    val description: String? = null,
    val priority: String? = null,
    val issueType: String? = null,
    val status: String? = null
)

data class AssignTicketRequest(
    val assigneeId: String? = null
)

// Activity DTOs
data class TicketActivityActorDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String
) {
    fun toDomain(): TicketActivityActor = TicketActivityActor(
        id = id,
        name = name,
        email = email,
        role = UserRole.fromString(role)
    )
}

data class TicketActivityDto(
    val id: String,
    val ticketId: String,
    val type: String,
    val createdAt: String,
    val actor: TicketActivityActorDto,
    val fromStatus: String? = null,
    val toStatus: String? = null,
    val fromAssignee: TicketUserDto? = null,
    val toAssignee: TicketUserDto? = null
) {
    fun toDomain(): TicketActivityEntry = TicketActivityEntry(
        id = id,
        ticketId = ticketId,
        type = TicketActivityType.fromString(type),
        createdAt = createdAt,
        actor = actor.toDomain(),
        fromStatus = fromStatus?.let { TicketStatus.fromString(it) },
        toStatus = toStatus?.let { TicketStatus.fromString(it) },
        fromAssignee = fromAssignee?.toDomain(),
        toAssignee = toAssignee?.toDomain()
    )
}

data class TicketActivityListResponse(
    val activities: List<TicketActivityDto>
)

// Report DTOs
data class StatusCountsDto(
    val open: Int? = null,
    @SerializedName("in_progress")
    val inProgress: Int? = null,
    val resolved: Int? = null
) {
    fun toDomain(): StatusCounts = StatusCounts(
        open = open ?: 0,
        inProgress = inProgress ?: 0,
        resolved = resolved ?: 0
    )
}

data class AgentAssignmentDto(
    val agentId: String,
    val count: Int,
    val agent: TicketUserDto? = null
) {
    fun toDomain(): AgentAssignment = AgentAssignment(
        agentId = agentId,
        count = count,
        agent = agent?.toDomain()
    )
}

data class StatusBucketDto(
    val status: String,
    val count: Int
) {
    fun toDomain(): StatusBucket = StatusBucket(
        status = TicketStatus.fromString(status),
        count = count
    )
}

data class TicketSummaryReportDto(
    val statuses: List<StatusBucketDto>? = null,
    val assignments: List<AgentAssignmentDto>? = null
) {
    fun toDomain(): TicketSummaryReport = TicketSummaryReport(
        statuses = statuses?.map { it.toDomain() } ?: emptyList(),
        assignments = assignments?.map { it.toDomain() } ?: emptyList()
    )
}

data class StatusSummaryResponse(
    val summary: TicketSummaryReportDto
)

data class UserTicketReportDto(
    val statusCounts: StatusCountsDto,
    val tickets: List<TicketDto>
) {
    fun toDomain(): UserTicketReport = UserTicketReport(
        statusCounts = statusCounts.toDomain(),
        tickets = tickets.map { it.toDomain() }
    )
}

data class UserTicketReportResponse(
    val report: UserTicketReportDto
)

data class AgentWorkloadReportDto(
    val statusCounts: StatusCountsDto,
    val assigned: List<TicketDto>,
    val pendingRequests: List<TicketDto>,
    val escalations: List<TicketDto>
) {
    fun toDomain(): AgentWorkloadReport = AgentWorkloadReport(
        statusCounts = statusCounts.toDomain(),
        assigned = assigned.map { it.toDomain() },
        pendingRequests = pendingRequests.map { it.toDomain() },
        escalations = escalations.map { it.toDomain() }
    )
}

data class AgentWorkloadReportResponse(
    val report: AgentWorkloadReportDto
)

data class AdminOverviewReportDto(
    val statusCounts: StatusCountsDto,
    val assignmentLoad: List<AgentAssignmentDto>,
    val oldestOpen: List<TicketDto>
) {
    fun toDomain(): AdminOverviewReport = AdminOverviewReport(
        statusCounts = statusCounts.toDomain(),
        assignmentLoad = assignmentLoad.map { it.toDomain() },
        oldestOpen = oldestOpen.map { it.toDomain() }
    )
}

data class AdminOverviewReportResponse(
    val report: AdminOverviewReportDto
)

data class AdminEscalationReportDto(
    val highPriority: List<TicketDto>,
    val staleTickets: List<TicketDto>
) {
    fun toDomain(): AdminEscalationReport = AdminEscalationReport(
        highPriority = highPriority.map { it.toDomain() },
        staleTickets = staleTickets.map { it.toDomain() }
    )
}

data class AdminEscalationReportResponse(
    val report: AdminEscalationReportDto
)

// User management DTOs
data class UserSummaryDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String
) {
    fun toDomain(): User = User(
        id = id,
        name = name,
        email = email,
        role = UserRole.fromString(role)
    )
}

data class UserListResponse(
    val users: List<UserSummaryDto>
)

data class UserResponse(
    val user: UserSummaryDto
)

data class CreateUserRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String
)

data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val role: String? = null
)
