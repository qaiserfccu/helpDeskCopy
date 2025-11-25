import Foundation

// MARK: - Ticket Service

public struct TicketService {
    private let apiClient: APIClient
    
    public init(apiClient: APIClient = .shared) {
        self.apiClient = apiClient
    }
    
    // MARK: - Fetch Methods
    
    public func fetchTickets(filters: TicketFilters = TicketFilters()) async throws -> [Ticket] {
        var queryItems: [URLQueryItem] = []
        
        if let status = filters.status {
            queryItems.append(URLQueryItem(name: "status", value: status.rawValue))
        }
        if let issueType = filters.issueType {
            queryItems.append(URLQueryItem(name: "issueType", value: issueType.rawValue))
        }
        if let assignedToMe = filters.assignedToMe, assignedToMe {
            queryItems.append(URLQueryItem(name: "assignedToMe", value: "true"))
        }
        if let limit = filters.limit {
            queryItems.append(URLQueryItem(name: "limit", value: String(limit)))
        }
        
        let response: TicketsResponse = try await apiClient.get(
            "/tickets",
            queryItems: queryItems.isEmpty ? nil : queryItems
        )
        return response.tickets
    }
    
    public func fetchTicket(id: String) async throws -> Ticket {
        let response: TicketResponse = try await apiClient.get("/tickets/\(id)")
        return response.ticket
    }
    
    // MARK: - Create/Update Methods
    
    public func createTicket(payload: CreateTicketPayload) async throws -> Ticket {
        let response: TicketResponse = try await apiClient.post("/tickets", body: payload)
        return response.ticket
    }
    
    public func updateTicket(id: String, payload: UpdateTicketPayload) async throws -> Ticket {
        let response: TicketResponse = try await apiClient.patch("/tickets/\(id)", body: payload)
        return response.ticket
    }
    
    // MARK: - Assignment Methods
    
    public func assignTicket(id: String, assigneeId: String?) async throws -> Ticket {
        let payload = AssignTicketPayload(assigneeId: assigneeId)
        let response: TicketResponse = try await apiClient.post("/tickets/\(id)/assign", body: payload)
        return response.ticket
    }
    
    public func requestAssignment(id: String) async throws -> Ticket {
        let response: TicketResponse = try await apiClient.post("/tickets/\(id)/request-assignment")
        return response.ticket
    }
    
    public func declineAssignmentRequest(id: String) async throws -> Ticket {
        let response: TicketResponse = try await apiClient.post("/tickets/\(id)/assignment-request/decline")
        return response.ticket
    }
    
    // MARK: - Status Methods
    
    public func resolveTicket(id: String) async throws -> Ticket {
        let response: TicketResponse = try await apiClient.post("/tickets/\(id)/resolve")
        return response.ticket
    }
    
    // MARK: - Activity Methods
    
    public func fetchTicketActivity(ticketId: String, limit: Int = 50) async throws -> [TicketActivityEntry] {
        let queryItems = [URLQueryItem(name: "limit", value: String(limit))]
        let response: ActivitiesResponse = try await apiClient.get(
            "/tickets/\(ticketId)/activity",
            queryItems: queryItems
        )
        return response.activities
    }
    
    public func fetchRecentActivity(limit: Int = 25) async throws -> [TicketActivityEntry] {
        let queryItems = [URLQueryItem(name: "limit", value: String(limit))]
        let response: ActivitiesResponse = try await apiClient.get(
            "/reports/tickets/activity",
            queryItems: queryItems
        )
        return response.activities
    }
    
    // MARK: - Reports
    
    public func fetchStatusSummary() async throws -> TicketSummaryReport {
        let response: SummaryResponse = try await apiClient.get("/reports/tickets/status-summary")
        return response.summary
    }
}
