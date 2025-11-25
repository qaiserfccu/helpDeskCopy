package com.helpdesk.app.domain.model

data class StatusCounts(
    val open: Int = 0,
    val inProgress: Int = 0,
    val resolved: Int = 0
) {
    val total: Int get() = open + inProgress + resolved
}

data class TicketSummaryReport(
    val statuses: List<StatusBucket>,
    val assignments: List<AgentAssignment>
)

data class StatusBucket(
    val status: TicketStatus,
    val count: Int
)

data class AgentAssignment(
    val agentId: String,
    val count: Int,
    val agent: TicketUser?
)

data class UserTicketReport(
    val statusCounts: StatusCounts,
    val tickets: List<Ticket>
)

data class AgentWorkloadReport(
    val statusCounts: StatusCounts,
    val assigned: List<Ticket>,
    val pendingRequests: List<Ticket>,
    val escalations: List<Ticket>
)

data class AdminOverviewReport(
    val statusCounts: StatusCounts,
    val assignmentLoad: List<AgentAssignment>,
    val oldestOpen: List<Ticket>
)

data class AdminEscalationReport(
    val highPriority: List<Ticket>,
    val staleTickets: List<Ticket>
)

data class AdminProductivityReport(
    val resolutionTrend: List<ResolutionTrendPoint>
)

data class ResolutionTrendPoint(
    val date: String,
    val count: Int
)
