import Foundation

// MARK: - Ticket Types

public enum TicketPriority: String, Codable, CaseIterable {
    case low
    case medium
    case high
}

public enum TicketStatus: String, Codable, CaseIterable {
    case open
    case inProgress = "in_progress"
    case resolved
    
    public var displayName: String {
        switch self {
        case .open: return "Open"
        case .inProgress: return "In Progress"
        case .resolved: return "Resolved"
        }
    }
}

public enum IssueType: String, Codable, CaseIterable {
    case hardware
    case software
    case network
    case access
    case other
}

public struct TicketUser: Codable, Identifiable, Equatable {
    public let id: String
    public let name: String
    public let email: String
    
    public init(id: String, name: String, email: String) {
        self.id = id
        self.name = name
        self.email = email
    }
}

public struct Ticket: Codable, Identifiable, Equatable {
    public let id: String
    public let description: String
    public let priority: TicketPriority
    public let issueType: IssueType
    public let status: TicketStatus
    public let attachments: [String]
    public let createdAt: String
    public let updatedAt: String
    public let resolvedAt: String?
    public let creator: TicketUser
    public let assignee: TicketUser?
    public let assignmentRequest: TicketUser?
    public var pendingSync: Bool?
    public var pendingAction: String?
    public var isLocalOnly: Bool?
    
    public init(
        id: String,
        description: String,
        priority: TicketPriority,
        issueType: IssueType,
        status: TicketStatus,
        attachments: [String] = [],
        createdAt: String,
        updatedAt: String,
        resolvedAt: String? = nil,
        creator: TicketUser,
        assignee: TicketUser? = nil,
        assignmentRequest: TicketUser? = nil,
        pendingSync: Bool? = nil,
        pendingAction: String? = nil,
        isLocalOnly: Bool? = nil
    ) {
        self.id = id
        self.description = description
        self.priority = priority
        self.issueType = issueType
        self.status = status
        self.attachments = attachments
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.resolvedAt = resolvedAt
        self.creator = creator
        self.assignee = assignee
        self.assignmentRequest = assignmentRequest
        self.pendingSync = pendingSync
        self.pendingAction = pendingAction
        self.isLocalOnly = isLocalOnly
    }
}

// MARK: - Ticket Payloads

public struct CreateTicketPayload: Codable {
    public let description: String
    public let priority: TicketPriority
    public let issueType: IssueType
    public let attachments: [String]?
    
    public init(description: String, priority: TicketPriority, issueType: IssueType, attachments: [String]? = nil) {
        self.description = description
        self.priority = priority
        self.issueType = issueType
        self.attachments = attachments
    }
}

public struct UpdateTicketPayload: Codable {
    public let description: String?
    public let priority: TicketPriority?
    public let issueType: IssueType?
    public let status: TicketStatus?
    
    public init(description: String? = nil, priority: TicketPriority? = nil, issueType: IssueType? = nil, status: TicketStatus? = nil) {
        self.description = description
        self.priority = priority
        self.issueType = issueType
        self.status = status
    }
}

public struct AssignTicketPayload: Codable {
    public let assigneeId: String?
    
    public init(assigneeId: String? = nil) {
        self.assigneeId = assigneeId
    }
}

// MARK: - Ticket Filters

public struct TicketFilters: Codable {
    public let status: TicketStatus?
    public let issueType: IssueType?
    public let assignedToMe: Bool?
    public let limit: Int?
    
    public init(status: TicketStatus? = nil, issueType: IssueType? = nil, assignedToMe: Bool? = nil, limit: Int? = nil) {
        self.status = status
        self.issueType = issueType
        self.assignedToMe = assignedToMe
        self.limit = limit
    }
}

// MARK: - Ticket Activity

public enum TicketActivityType: String, Codable {
    case statusChange = "status_change"
    case assignmentChange = "assignment_change"
    case assignmentRequest = "assignment_request"
    case ticketUpdate = "ticket_update"
}

public struct TicketActivityActor: Codable, Equatable {
    public let id: String
    public let name: String
    public let email: String
    public let role: UserRole
}

public struct TicketActivityEntry: Codable, Identifiable, Equatable {
    public let id: String
    public let ticketId: String
    public let type: TicketActivityType
    public let createdAt: String
    public let actor: TicketActivityActor
    public let fromStatus: TicketStatus?
    public let toStatus: TicketStatus?
    public let fromAssignee: TicketUser?
    public let toAssignee: TicketUser?
    
    public var activityDescription: String {
        let ticketLabel = "#\(String(ticketId.prefix(6)))"
        
        switch type {
        case .assignmentRequest:
            return "\(actor.name) requested assignment on \(ticketLabel)"
        case .ticketUpdate:
            return "\(actor.name) updated details on \(ticketLabel)"
        case .assignmentChange:
            if let toAssignee = toAssignee {
                return "\(actor.name) assigned \(ticketLabel) to \(toAssignee.name)"
            }
            if let fromAssignee = fromAssignee {
                return "\(actor.name) cleared \(fromAssignee.name)'s assignment on \(ticketLabel)"
            }
            return "\(actor.name) updated assignments for \(ticketLabel)"
        case .statusChange:
            if let fromStatus = fromStatus, let toStatus = toStatus {
                return "\(actor.name) moved \(ticketLabel) from \(fromStatus.displayName) to \(toStatus.displayName)"
            }
            if let toStatus = toStatus {
                return "\(actor.name) moved \(ticketLabel) to \(toStatus.displayName)"
            }
            if let fromStatus = fromStatus {
                return "\(actor.name) updated \(ticketLabel) from \(fromStatus.displayName)"
            }
            return "\(actor.name) updated \(ticketLabel)"
        }
    }
}

// MARK: - Report Types

public struct StatusCounts: Codable {
    public let open: Int
    public let inProgress: Int
    public let resolved: Int
    
    enum CodingKeys: String, CodingKey {
        case open
        case inProgress = "in_progress"
        case resolved
    }
}

public struct TicketSummaryReport: Codable {
    public struct StatusBucket: Codable {
        public let status: TicketStatus
        public let count: Int
    }
    
    public struct AssignmentBucket: Codable {
        public let agentId: String
        public let count: Int
        public let agent: TicketUser?
    }
    
    public let statuses: [StatusBucket]
    public let assignments: [AssignmentBucket]
}

// MARK: - API Response Wrappers

public struct TicketsResponse: Codable {
    public let tickets: [Ticket]
}

public struct TicketResponse: Codable {
    public let ticket: Ticket
}

public struct ActivitiesResponse: Codable {
    public let activities: [TicketActivityEntry]
}

public struct SummaryResponse: Codable {
    public let summary: TicketSummaryReport
}

public struct UsersResponse: Codable {
    public let users: [UserSummary]
}
