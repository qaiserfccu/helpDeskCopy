package com.helpdesk.app

import com.helpdesk.app.domain.model.*
import org.junit.Test
import org.junit.Assert.*

class DomainModelTest {

    @Test
    fun `UserRole fromString returns correct role`() {
        assertEquals(UserRole.USER, UserRole.fromString("user"))
        assertEquals(UserRole.AGENT, UserRole.fromString("agent"))
        assertEquals(UserRole.ADMIN, UserRole.fromString("admin"))
        assertEquals(UserRole.USER, UserRole.fromString("unknown"))
    }

    @Test
    fun `UserRole toApiValue returns lowercase string`() {
        assertEquals("user", UserRole.USER.toApiValue())
        assertEquals("agent", UserRole.AGENT.toApiValue())
        assertEquals("admin", UserRole.ADMIN.toApiValue())
    }

    @Test
    fun `TicketPriority fromString returns correct priority`() {
        assertEquals(TicketPriority.LOW, TicketPriority.fromString("low"))
        assertEquals(TicketPriority.MEDIUM, TicketPriority.fromString("medium"))
        assertEquals(TicketPriority.HIGH, TicketPriority.fromString("high"))
        assertEquals(TicketPriority.MEDIUM, TicketPriority.fromString("unknown"))
    }

    @Test
    fun `TicketStatus fromString returns correct status`() {
        assertEquals(TicketStatus.OPEN, TicketStatus.fromString("open"))
        assertEquals(TicketStatus.IN_PROGRESS, TicketStatus.fromString("in_progress"))
        assertEquals(TicketStatus.RESOLVED, TicketStatus.fromString("resolved"))
        assertEquals(TicketStatus.OPEN, TicketStatus.fromString("unknown"))
    }

    @Test
    fun `TicketStatus displayName returns human readable name`() {
        assertEquals("Open", TicketStatus.OPEN.displayName())
        assertEquals("In Progress", TicketStatus.IN_PROGRESS.displayName())
        assertEquals("Resolved", TicketStatus.RESOLVED.displayName())
    }

    @Test
    fun `IssueType fromString returns correct type`() {
        assertEquals(IssueType.HARDWARE, IssueType.fromString("hardware"))
        assertEquals(IssueType.SOFTWARE, IssueType.fromString("software"))
        assertEquals(IssueType.NETWORK, IssueType.fromString("network"))
        assertEquals(IssueType.ACCESS, IssueType.fromString("access"))
        assertEquals(IssueType.OTHER, IssueType.fromString("other"))
        assertEquals(IssueType.OTHER, IssueType.fromString("unknown"))
    }

    @Test
    fun `User data class creates correctly`() {
        val user = User(
            id = "123",
            name = "Test User",
            email = "test@example.com",
            role = UserRole.AGENT
        )
        assertEquals("123", user.id)
        assertEquals("Test User", user.name)
        assertEquals("test@example.com", user.email)
        assertEquals(UserRole.AGENT, user.role)
    }

    @Test
    fun `AuthSession data class creates correctly`() {
        val user = User("1", "Test", "test@test.com", UserRole.USER)
        val session = AuthSession(
            user = user,
            accessToken = "access123",
            refreshToken = "refresh456"
        )
        assertEquals(user, session.user)
        assertEquals("access123", session.accessToken)
        assertEquals("refresh456", session.refreshToken)
    }

    @Test
    fun `Ticket data class creates correctly`() {
        val creator = TicketUser("1", "Creator", "creator@test.com")
        val ticket = Ticket(
            id = "ticket123",
            description = "Test issue",
            priority = TicketPriority.HIGH,
            issueType = IssueType.SOFTWARE,
            status = TicketStatus.OPEN,
            attachments = listOf("file1.pdf"),
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            resolvedAt = null,
            creator = creator,
            assignee = null,
            assignmentRequest = null
        )
        assertEquals("ticket123", ticket.id)
        assertEquals("Test issue", ticket.description)
        assertEquals(TicketPriority.HIGH, ticket.priority)
        assertEquals(TicketStatus.OPEN, ticket.status)
        assertEquals(1, ticket.attachments.size)
        assertNull(ticket.assignee)
    }

    @Test
    fun `StatusCounts total is calculated correctly`() {
        val counts = StatusCounts(open = 5, inProgress = 3, resolved = 2)
        assertEquals(10, counts.total)
    }

    @Test
    fun `TicketActivityEntry describe returns correct message for status change`() {
        val actor = TicketActivityActor("1", "John", "john@test.com", UserRole.AGENT)
        val activity = TicketActivityEntry(
            id = "act1",
            ticketId = "ticket1",
            type = TicketActivityType.STATUS_CHANGE,
            createdAt = "2024-01-01T00:00:00Z",
            actor = actor,
            fromStatus = TicketStatus.OPEN,
            toStatus = TicketStatus.IN_PROGRESS,
            fromAssignee = null,
            toAssignee = null
        )
        assertEquals("John changed status from Open to In Progress", activity.describe())
    }

    @Test
    fun `TicketActivityEntry describe returns correct message for assignment`() {
        val actor = TicketActivityActor("1", "Admin", "admin@test.com", UserRole.ADMIN)
        val toAssignee = TicketUser("2", "Agent", "agent@test.com")
        val activity = TicketActivityEntry(
            id = "act2",
            ticketId = "ticket1",
            type = TicketActivityType.ASSIGNMENT_CHANGE,
            createdAt = "2024-01-01T00:00:00Z",
            actor = actor,
            fromStatus = null,
            toStatus = null,
            fromAssignee = null,
            toAssignee = toAssignee
        )
        assertEquals("Admin assigned ticket to Agent", activity.describe())
    }
}
