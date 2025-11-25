import Foundation
import SwiftUI

// MARK: - Ticket Store

@MainActor
public final class TicketStore: ObservableObject {
    public static let shared = TicketStore()
    
    @Published public private(set) var tickets: [Ticket] = []
    @Published public private(set) var isLoading = false
    @Published public private(set) var error: String?
    
    @Published public var statusFilter: TicketStatus?
    @Published public var assignedToMe = false
    
    @Published public private(set) var recentActivity: [TicketActivityEntry] = []
    @Published public private(set) var statusSummary: TicketSummaryReport?
    
    private let ticketService = TicketService()
    
    private init() {
        setupSocketHandlers()
        setupNotificationObservers()
    }
    
    // MARK: - Fetch Methods
    
    public func fetchTickets() async {
        isLoading = true
        error = nil
        
        do {
            let filters = TicketFilters(
                status: statusFilter,
                assignedToMe: assignedToMe ? true : nil
            )
            tickets = try await ticketService.fetchTickets(filters: filters)
        } catch {
            self.error = error.localizedDescription
        }
        
        isLoading = false
    }
    
    public func fetchTicket(id: String) async -> Ticket? {
        do {
            return try await ticketService.fetchTicket(id: id)
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }
    
    public func fetchRecentActivity() async {
        do {
            recentActivity = try await ticketService.fetchRecentActivity(limit: 10)
        } catch {
            print("Failed to fetch recent activity: \(error.localizedDescription)")
        }
    }
    
    public func fetchStatusSummary() async {
        do {
            statusSummary = try await ticketService.fetchStatusSummary()
        } catch {
            print("Failed to fetch status summary: \(error.localizedDescription)")
        }
    }
    
    // MARK: - Ticket Actions
    
    public func createTicket(description: String, priority: TicketPriority, issueType: IssueType) async throws -> Ticket {
        let payload = CreateTicketPayload(
            description: description.trimmingCharacters(in: .whitespacesAndNewlines),
            priority: priority,
            issueType: issueType
        )
        let ticket = try await ticketService.createTicket(payload: payload)
        await fetchTickets()
        return ticket
    }
    
    public func updateTicket(id: String, description: String?, priority: TicketPriority?, issueType: IssueType?, status: TicketStatus?) async throws -> Ticket {
        let payload = UpdateTicketPayload(
            description: description?.trimmingCharacters(in: .whitespacesAndNewlines),
            priority: priority,
            issueType: issueType,
            status: status
        )
        let ticket = try await ticketService.updateTicket(id: id, payload: payload)
        await fetchTickets()
        return ticket
    }
    
    public func assignTicket(id: String, assigneeId: String?) async throws -> Ticket {
        let ticket = try await ticketService.assignTicket(id: id, assigneeId: assigneeId)
        await fetchTickets()
        return ticket
    }
    
    public func requestAssignment(id: String) async throws -> Ticket {
        let ticket = try await ticketService.requestAssignment(id: id)
        await fetchTickets()
        return ticket
    }
    
    public func declineAssignmentRequest(id: String) async throws -> Ticket {
        let ticket = try await ticketService.declineAssignmentRequest(id: id)
        await fetchTickets()
        return ticket
    }
    
    public func resolveTicket(id: String) async throws -> Ticket {
        let ticket = try await ticketService.resolveTicket(id: id)
        await fetchTickets()
        return ticket
    }
    
    public func fetchTicketActivity(ticketId: String, limit: Int = 100) async -> [TicketActivityEntry] {
        do {
            return try await ticketService.fetchTicketActivity(ticketId: ticketId, limit: limit)
        } catch {
            print("Failed to fetch ticket activity: \(error.localizedDescription)")
            return []
        }
    }
    
    // MARK: - Computed Properties
    
    public var counters: (open: Int, inProgress: Int, resolved: Int) {
        tickets.reduce((0, 0, 0)) { result, ticket in
            switch ticket.status {
            case .open: return (result.0 + 1, result.1, result.2)
            case .inProgress: return (result.0, result.1 + 1, result.2)
            case .resolved: return (result.0, result.1, result.2 + 1)
            }
        }
    }
    
    public var summaryTotals: (total: Int, open: Int, inProgress: Int, resolved: Int) {
        if let summary = statusSummary {
            let total = summary.statuses.reduce(0) { $0 + $1.count }
            let open = summary.statuses.first { $0.status == .open }?.count ?? 0
            let inProgress = summary.statuses.first { $0.status == .inProgress }?.count ?? 0
            let resolved = summary.statuses.first { $0.status == .resolved }?.count ?? 0
            return (total, open, inProgress, resolved)
        } else {
            let counts = counters
            return (tickets.count, counts.open, counts.inProgress, counts.resolved)
        }
    }
    
    // MARK: - Socket Handlers
    
    private func setupSocketHandlers() {
        TicketSocketManager.shared.onTicketCreated = { [weak self] ticket in
            Task { @MainActor in
                await self?.fetchTickets()
            }
        }
        
        TicketSocketManager.shared.onTicketUpdated = { [weak self] ticket in
            Task { @MainActor in
                await self?.fetchTickets()
            }
        }
        
        TicketSocketManager.shared.onTicketActivity = { [weak self] ticketId, activity in
            Task { @MainActor in
                await self?.fetchRecentActivity()
                // Notify observers about new activity
                NotificationStore.shared.addNotification(
                    id: activity.id,
                    ticketId: ticketId,
                    actor: activity.actor.name,
                    summary: activity.activityDescription,
                    createdAt: activity.createdAt,
                    type: .activity
                )
            }
        }
    }
    
    private func setupNotificationObservers() {
        NotificationCenter.default.addObserver(
            forName: .ticketRefreshNeeded,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                await self?.fetchTickets()
            }
        }
    }
}
