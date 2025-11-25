import SwiftUI

// MARK: - Ticket Detail View

public struct TicketDetailView: View {
    @EnvironmentObject var authStore: AuthStore
    @StateObject var ticketStore = TicketStore.shared
    @Environment(\.dismiss) var dismiss
    
    let ticketId: String
    
    @State private var ticket: Ticket?
    @State private var activities: [TicketActivityEntry] = []
    @State private var agents: [UserSummary] = []
    @State private var selectedAssigneeId: String?
    @State private var isLoading = true
    @State private var showEditSheet = false
    @State private var showAlert = false
    @State private var alertMessage = ""
    
    public init(ticketId: String) {
        self.ticketId = ticketId
    }
    
    private var user: AuthUser? { authStore.currentUser }
    private var isAdmin: Bool { user?.role == .admin }
    private var isAgent: Bool { user?.role == .agent }
    private var isTicketResolved: Bool { ticket?.status == .resolved }
    private var isTicketOwner: Bool { ticket?.creator.id == user?.id }
    
    private var canAssign: Bool { isAdmin && !isTicketResolved }
    private var canEdit: Bool {
        guard let ticket = ticket, !isTicketResolved else { return false }
        return isAdmin || (user?.role == .user && isTicketOwner)
    }
    private var canResolve: Bool { isAgent && ticket?.assignee?.id == user?.id && ticket?.status != .resolved }
    private var canRequestAssignment: Bool {
        isAgent && ticket?.assignee == nil && ticket?.status != .resolved
    }
    private var canReopen: Bool { isAdmin && isTicketResolved }
    
    public var body: some View {
        NavigationStack {
            ZStack {
                Color.appBackground.ignoresSafeArea()
                
                if isLoading {
                    ProgressView()
                        .tint(Color.accentCyan)
                } else if let ticket = ticket {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            // Ticket Header
                            ticketHeader(ticket)
                            
                            // Badges Row
                            badgesRow(ticket)
                            
                            // Creator Section
                            sectionView(label: "Creator", value: ticket.creator.name)
                            
                            // Pending Sync Banner
                            if ticket.pendingSync == true {
                                pendingSyncBanner
                            }
                            
                            // Assignee Section
                            assigneeSection(ticket)
                            
                            // Agent Assignment (Admin only)
                            if canAssign {
                                agentAssignmentSection
                            }
                            
                            // Attachments
                            if !ticket.attachments.isEmpty {
                                attachmentsSection(ticket.attachments)
                            }
                            
                            // Activity Section
                            activitySection
                            
                            // Resolved Notice
                            if isTicketResolved && !isAdmin {
                                resolvedNotice
                            }
                            
                            // Actions
                            actionsSection(ticket)
                        }
                        .padding(20)
                        .padding(.bottom, 60)
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .foregroundColor(.white)
                    }
                }
            }
            .task {
                await loadTicket()
            }
            .sheet(isPresented: $showEditSheet) {
                if let ticket = ticket {
                    TicketFormView(editingTicket: ticket)
                }
            }
            .alert("Action Failed", isPresented: $showAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(alertMessage)
            }
        }
    }
    
    // MARK: - Subviews
    
    private func ticketHeader(_ ticket: Ticket) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Ticket #\(String(ticket.id.prefix(8)))")
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text(ticket.description)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.white)
        }
    }
    
    private func badgesRow(_ ticket: Ticket) -> some View {
        HStack(spacing: 12) {
            badgeView(label: "Status", value: ticket.status.displayName)
            badgeView(label: "Priority", value: ticket.priority.rawValue.capitalized)
            badgeView(label: "Type", value: ticket.issueType.rawValue.capitalized)
        }
    }
    
    private func badgeView(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text(value)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.cardBackground)
        .cornerRadius(14)
    }
    
    private func sectionView(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text(value)
                .font(.subheadline)
                .foregroundColor(.white)
        }
    }
    
    private var pendingSyncBanner: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Pending sync")
                .font(.subheadline)
                .fontWeight(.bold)
                .foregroundColor(.yellow)
            
            Text("This change will be sent automatically once you are back online.")
                .font(.caption)
                .foregroundColor(.yellow.opacity(0.8))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.yellow.opacity(0.1))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.yellow, lineWidth: 1)
        )
    }
    
    private func assigneeSection(_ ticket: Ticket) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Assignee")
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text(ticket.assignee?.name ?? "Unassigned")
                .font(.subheadline)
                .foregroundColor(.white)
            
            if let request = ticket.assignmentRequest, ticket.assignee == nil {
                Text("Requested by \(request.name)")
                    .font(.caption)
                    .foregroundColor(.yellow)
                    .padding(.top, 4)
            }
        }
    }
    
    private var agentAssignmentSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Assign ticket")
                .font(.caption)
                .foregroundColor(.secondary)
            
            if agents.isEmpty {
                Text("Invite agents from the admin portal to assign tickets.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            } else {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(agents) { agent in
                        AgentChip(
                            agent: agent,
                            isSelected: selectedAssigneeId == agent.id
                        ) {
                            selectedAssigneeId = agent.id
                        }
                    }
                }
            }
            
            Text("Select an agent and tap Assign to re-route immediately.")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
    
    private func attachmentsSection(_ attachments: [String]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Attachments")
                .font(.caption)
                .foregroundColor(.secondary)
            
            ForEach(attachments, id: \.self) { attachment in
                HStack {
                    Text(attachment.components(separatedBy: "/").last ?? attachment)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    Text("Tap to open")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(12)
                .background(Color.cardBackground)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.blue, lineWidth: 1)
                )
            }
        }
    }
    
    private var activitySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Activity")
                .font(.caption)
                .foregroundColor(.secondary)
            
            if activities.isEmpty {
                Text("No recent changes yet.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            } else {
                VStack(spacing: 12) {
                    ForEach(activities) { entry in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(entry.activityDescription)
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundColor(.white)
                            
                            Text(formatDate(entry.createdAt))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(Color.cardBackground)
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.borderColor, lineWidth: 1)
                        )
                    }
                }
            }
        }
    }
    
    private var resolvedNotice: some View {
        Text("This ticket is resolved. Contact support if you need further changes.")
            .font(.subheadline)
            .foregroundColor(.blue.opacity(0.9))
            .padding(12)
            .frame(maxWidth: .infinity)
            .background(Color.blue.opacity(0.2))
            .cornerRadius(12)
    }
    
    private func actionsSection(_ ticket: Ticket) -> some View {
        VStack(spacing: 12) {
            if canEdit {
                Button(action: { showEditSheet = true }) {
                    Text("Edit ticket")
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.borderColor, lineWidth: 1)
                        )
                }
            }
            
            if canAssign {
                Button(action: handleAssign) {
                    Text(assignButtonText)
                        .fontWeight(.bold)
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(selectedAssigneeId != nil || ticket.assignmentRequest != nil ? Color.accentCyan : Color.accentCyan.opacity(0.5))
                        .cornerRadius(16)
                }
                .disabled(selectedAssigneeId == nil && ticket.assignmentRequest == nil)
            }
            
            if canRequestAssignment {
                Button(action: handleRequestAssignment) {
                    Text(requestAssignmentButtonText)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.borderColor, lineWidth: 1)
                        )
                }
                .disabled(ticket.assignmentRequest != nil)
            }
            
            if canResolve {
                Button(action: handleResolve) {
                    Text("Resolve")
                        .fontWeight(.bold)
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(16)
                }
            }
            
            if canReopen {
                Button(action: handleReopen) {
                    Text("Reopen ticket")
                        .fontWeight(.semibold)
                        .foregroundColor(Color.accentCyan)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.accentCyan, lineWidth: 1)
                        )
                }
            }
        }
        .padding(.top, 8)
    }
    
    // MARK: - Computed Properties
    
    private var assignButtonText: String {
        if let id = selectedAssigneeId, let agent = agents.first(where: { $0.id == id }) {
            return "Assign to \(agent.name)"
        }
        if let request = ticket?.assignmentRequest {
            return "Assign to \(request.name)"
        }
        return "Select an agent"
    }
    
    private var requestAssignmentButtonText: String {
        if ticket?.assignmentRequest?.id == user?.id {
            return "Request pending approval"
        }
        if ticket?.assignmentRequest != nil {
            return "Another agent requested"
        }
        return "Request assignment"
    }
    
    // MARK: - Actions
    
    private func loadTicket() async {
        isLoading = true
        
        ticket = await ticketStore.fetchTicket(id: ticketId)
        activities = await ticketStore.fetchTicketActivity(ticketId: ticketId)
        
        // Load agents if admin
        if isAdmin {
            let userService = UserService()
            agents = (try? await userService.fetchUsers(role: .agent)) ?? []
        }
        
        // Set initial selected assignee
        if let assigneeId = ticket?.assignee?.id {
            selectedAssigneeId = assigneeId
        } else if let requestId = ticket?.assignmentRequest?.id {
            selectedAssigneeId = requestId
        }
        
        // Mark notifications as read for this ticket
        NotificationStore.shared.markTicketRead(ticketId)
        
        isLoading = false
    }
    
    private func handleAssign() {
        let targetId = selectedAssigneeId ?? ticket?.assignmentRequest?.id
        guard let assigneeId = targetId else { return }
        
        Task {
            do {
                _ = try await ticketStore.assignTicket(id: ticketId, assigneeId: assigneeId)
                await loadTicket()
            } catch {
                alertMessage = "Please try again in a moment."
                showAlert = true
            }
        }
    }
    
    private func handleRequestAssignment() {
        Task {
            do {
                _ = try await ticketStore.requestAssignment(id: ticketId)
                await loadTicket()
            } catch {
                alertMessage = "Unable to request this ticket right now."
                showAlert = true
            }
        }
    }
    
    private func handleResolve() {
        Task {
            do {
                _ = try await ticketStore.resolveTicket(id: ticketId)
                await loadTicket()
            } catch {
                alertMessage = "Please try again in a moment."
                showAlert = true
            }
        }
    }
    
    private func handleReopen() {
        Task {
            do {
                _ = try await ticketStore.updateTicket(id: ticketId, description: nil, priority: nil, issueType: nil, status: .open)
                await loadTicket()
            } catch {
                alertMessage = "Please try again in a moment."
                showAlert = true
            }
        }
    }
    
    // MARK: - Helpers
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: dateString) {
            let dateFormatter = DateFormatter()
            dateFormatter.dateStyle = .medium
            dateFormatter.timeStyle = .short
            return dateFormatter.string(from: date)
        }
        return dateString
    }
}

// MARK: - Agent Chip

struct AgentChip: View {
    let agent: UserSummary
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 4) {
                Text(agent.name)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(isSelected ? Color.accentCyan : .white)
                
                Text(agent.email)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(isSelected ? Color.selectedBackground : Color.cardBackground)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Color.accentCyan : Color.borderColor, lineWidth: 1)
            )
        }
    }
}

// MARK: - Preview

#Preview {
    TicketDetailView(ticketId: "123")
        .environmentObject(AuthStore.shared)
}
