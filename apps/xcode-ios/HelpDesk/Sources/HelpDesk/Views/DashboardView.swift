import SwiftUI

// MARK: - Dashboard View

public struct DashboardView: View {
    @EnvironmentObject var authStore: AuthStore
    @StateObject var ticketStore = TicketStore.shared
    @StateObject var notificationStore = NotificationStore.shared
    
    @State private var showNotifications = false
    @State private var showNavDrawer = false
    @State private var selectedTicket: Ticket?
    
    public init() {}
    
    private var user: AuthUser? { authStore.currentUser }
    private var canCreate: Bool { user?.role == .user || user?.role == .admin }
    
    public var body: some View {
        NavigationStack {
            ZStack {
                Color.appBackground.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 16) {
                        // Hero Card
                        heroCard
                        
                        // Control Panel
                        controlPanel
                        
                        // Snapshot Row
                        snapshotSection
                        
                        // Tickets Section
                        ticketsSection
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, canCreate ? 100 : 60)
                }
                .refreshable {
                    await refreshData()
                }
                
                // FAB for creating tickets
                if canCreate {
                    VStack {
                        Spacer()
                        createTicketButton
                    }
                }
                
                // Nav Drawer Overlay
                if showNavDrawer {
                    navDrawerOverlay
                }
                
                // Notifications Overlay
                if showNotifications {
                    notificationsOverlay
                }
            }
            .navigationBarHidden(true)
            .task {
                await refreshData()
            }
            .sheet(item: $selectedTicket) { ticket in
                TicketDetailView(ticketId: ticket.id)
            }
        }
    }
    
    // MARK: - Hero Card
    
    private var heroCard: some View {
        VStack(spacing: 14) {
            HStack(alignment: .center, spacing: 12) {
                // Menu Button
                Button(action: { withAnimation { showNavDrawer = true } }) {
                    Image(systemName: "line.3.horizontal")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 46, height: 46)
                        .background(Color.cardBackground)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(Color.blue.opacity(0.4), lineWidth: 1))
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Command center")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .textCase(.uppercase)
                    
                    Text(user != nil ? "Hi, \(user!.name.components(separatedBy: " ").first ?? user!.name)" : "Help Desk")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                }
                
                Spacer()
                
                // Notification Button
                Button(action: { withAnimation { showNotifications = true } }) {
                    ZStack(alignment: .topTrailing) {
                        Image(systemName: "bell.fill")
                            .font(.title3)
                            .foregroundColor(.white)
                            .frame(width: 40, height: 40)
                            .background(Color.cardBackground)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(Color.borderColor, lineWidth: 1))
                        
                        if notificationStore.unreadCount > 0 {
                            Text(notificationStore.unreadCount > 9 ? "9+" : "\(notificationStore.unreadCount)")
                                .font(.caption2)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                                .padding(.horizontal, 4)
                                .background(Color.red)
                                .clipShape(Capsule())
                                .offset(x: 4, y: -4)
                        }
                    }
                }
                
                // Sign Out Button
                Button(action: { Task { await authStore.signOut() } }) {
                    Text("Sign out")
                        .font(.subheadline)
                        .foregroundColor(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 6)
                        .overlay(
                            RoundedRectangle(cornerRadius: 999)
                                .stroke(Color.borderColor, lineWidth: 1)
                        )
                }
            }
            
            Text("Monitor tickets, workload, and signals in one sleek view.")
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            // Stats Row
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                StatCard(label: "Open", value: ticketStore.summaryTotals.open, hint: "Active queue")
                StatCard(label: "In progress", value: ticketStore.summaryTotals.inProgress, hint: "Being handled")
                StatCard(label: "Resolved", value: ticketStore.summaryTotals.resolved, hint: "Closed")
                StatCard(label: "Total", value: ticketStore.summaryTotals.total, hint: "Tracked")
            }
        }
        .padding(20)
        .background(Color.cardBackground.opacity(0.85))
        .cornerRadius(26)
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(Color.blue.opacity(0.18), lineWidth: 1)
        )
    }
    
    // MARK: - Control Panel
    
    private var controlPanel: some View {
        VStack(spacing: 16) {
            // Status Filter
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    FilterChip(label: "All", isActive: ticketStore.statusFilter == nil) {
                        ticketStore.statusFilter = nil
                        Task { await ticketStore.fetchTickets() }
                    }
                    
                    ForEach(TicketStatus.allCases, id: \.self) { status in
                        FilterChip(label: status.displayName, isActive: ticketStore.statusFilter == status) {
                            ticketStore.statusFilter = status
                            Task { await ticketStore.fetchTickets() }
                        }
                    }
                }
            }
            
            // Assigned to me toggle (for agents/admins)
            if user?.role != .user {
                HStack {
                    Text("Assigned to me")
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    Toggle("", isOn: $ticketStore.assignedToMe)
                        .tint(Color.accentCyan)
                        .onChange(of: ticketStore.assignedToMe) { _ in
                            Task { await ticketStore.fetchTickets() }
                        }
                }
            }
        }
        .padding(16)
        .background(Color.black.opacity(0.8))
        .cornerRadius(22)
        .overlay(
            RoundedRectangle(cornerRadius: 22)
                .stroke(Color.secondary.opacity(0.2), lineWidth: 1)
        )
    }
    
    // MARK: - Snapshot Section
    
    private var snapshotSection: some View {
        VStack(spacing: 16) {
            // Status Snapshot
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Status snapshot")
                            .font(.headline)
                            .foregroundColor(.white)
                        Text(ticketStore.statusSummary != nil ? "Organization view" : "Personal view")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    NavigationLink(destination: ReportsView()) {
                        Text("Open reports")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(Color.accentCyan)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 6)
                            .overlay(
                                RoundedRectangle(cornerRadius: 999)
                                    .stroke(Color.accentCyan.opacity(0.4), lineWidth: 1)
                            )
                    }
                }
                
                // Metrics Grid
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    MetricChip(label: "Total", value: ticketStore.summaryTotals.total)
                    MetricChip(label: "Open", value: ticketStore.summaryTotals.open)
                    MetricChip(label: "In progress", value: ticketStore.summaryTotals.inProgress)
                }
            }
            .padding(18)
            .background(Color.cardBackground.opacity(0.82))
            .cornerRadius(22)
            .overlay(
                RoundedRectangle(cornerRadius: 22)
                    .stroke(Color.blue.opacity(0.25), lineWidth: 1)
            )
            
            // Live Activity Panel
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Live activity")
                            .font(.headline)
                            .foregroundColor(.white)
                        Text("Latest updates")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    Button(action: { withAnimation { showNotifications = true } }) {
                        Text("Inbox")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(Color.accentCyan)
                    }
                }
                
                if ticketStore.recentActivity.isEmpty {
                    Text("Real-time updates will appear as tickets evolve.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                } else {
                    ForEach(ticketStore.recentActivity.prefix(3)) { activity in
                        ActivityRow(activity: activity)
                    }
                }
            }
            .padding(18)
            .background(Color.black.opacity(0.75))
            .cornerRadius(22)
            .overlay(
                RoundedRectangle(cornerRadius: 22)
                    .stroke(Color.blue.opacity(0.2), lineWidth: 1)
            )
        }
    }
    
    // MARK: - Tickets Section
    
    private var ticketsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Tickets")
                .font(.headline)
                .foregroundColor(.white)
            
            if ticketStore.isLoading && ticketStore.tickets.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(40)
            } else if ticketStore.tickets.isEmpty {
                VStack(spacing: 8) {
                    Text("No tickets found")
                        .font(.headline)
                        .foregroundColor(.white)
                    Text(canCreate ? "Try a different filter or create a new ticket below." : "Try a different filter or request access from an admin.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(40)
            } else {
                ForEach(ticketStore.tickets) { ticket in
                    TicketCard(ticket: ticket) {
                        selectedTicket = ticket
                    }
                }
            }
        }
    }
    
    // MARK: - Create Ticket Button
    
    private var createTicketButton: some View {
        NavigationLink(destination: TicketFormView()) {
            Text("Create Ticket")
                .fontWeight(.bold)
                .foregroundColor(.black)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(Color.accentCyan)
                .cornerRadius(16)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 24)
    }
    
    // MARK: - Nav Drawer Overlay
    
    private var navDrawerOverlay: some View {
        ZStack(alignment: .leading) {
            Color.black.opacity(0.7)
                .ignoresSafeArea()
                .onTapGesture { withAnimation { showNavDrawer = false } }
            
            VStack(alignment: .leading, spacing: 12) {
                // Header
                HStack(spacing: 12) {
                    Button(action: { withAnimation { showNavDrawer = false } }) {
                        Image(systemName: "arrow.left")
                            .foregroundColor(.white)
                            .frame(width: 36, height: 36)
                            .overlay(Circle().stroke(Color.accentCyan.opacity(0.4), lineWidth: 1))
                    }
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Quick sections")
                            .font(.headline)
                            .foregroundColor(.white)
                        Text("Navigate rapidly")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.bottom, 12)
                
                // Nav Items
                NavDrawerItem(glyph: "🏠", title: "Dashboard overview", subtitle: "Scroll to activity") {
                    showNavDrawer = false
                }
                
                NavDrawerItem(glyph: "🧾", title: "My report", subtitle: "Personal ticket stats") {
                    showNavDrawer = false
                }
                
                NavDrawerItem(glyph: "📊", title: "Reports table", subtitle: "Filter + export") {
                    showNavDrawer = false
                }
                
                if user?.role != .user {
                    NavDrawerItem(glyph: "📈", title: "Agent workload", subtitle: "Assignments heatmap") {
                        showNavDrawer = false
                    }
                }
                
                if user?.role == .admin {
                    NavDrawerItem(glyph: "🏢", title: "Org snapshot", subtitle: "Status & escalations") {
                        showNavDrawer = false
                    }
                    
                    NavDrawerItem(glyph: "👥", title: "User management", subtitle: "Manage members") {
                        showNavDrawer = false
                    }
                }
                
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 48)
            .padding(.bottom, 24)
            .frame(width: 280)
            .background(Color.cardBackground.opacity(0.95))
            .cornerRadius(28)
            .overlay(
                RoundedRectangle(cornerRadius: 28)
                    .stroke(Color.blue.opacity(0.3), lineWidth: 1)
            )
        }
        .transition(.move(edge: .leading))
    }
    
    // MARK: - Notifications Overlay
    
    private var notificationsOverlay: some View {
        ZStack {
            Color.black.opacity(0.75)
                .ignoresSafeArea()
                .onTapGesture { withAnimation { showNotifications = false } }
            
            VStack(spacing: 0) {
                // Header
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Notifications")
                            .font(.headline)
                            .foregroundColor(.white)
                        Text(notificationStore.unreadCount > 0 ? "\(notificationStore.unreadCount) new notification\(notificationStore.unreadCount > 1 ? "s" : "")" : "You are all caught up.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    Button(action: { withAnimation { showNotifications = false } }) {
                        Text("Close")
                            .fontWeight(.semibold)
                            .foregroundColor(Color.accentCyan)
                    }
                }
                .padding(16)
                
                // Notifications List
                ScrollView {
                    if notificationStore.notifications.isEmpty {
                        Text("Real-time updates will appear here once new activity comes in.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .padding()
                    } else {
                        VStack(spacing: 12) {
                            ForEach(notificationStore.notifications) { notification in
                                NotificationRow(notification: notification) {
                                    notificationStore.markRead(notification.id)
                                    showNotifications = false
                                    // Navigate to ticket
                                    if let ticket = ticketStore.tickets.first(where: { $0.id == notification.ticketId }) {
                                        selectedTicket = ticket
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 16)
                    }
                }
            }
            .background(Color.cardBackground)
            .cornerRadius(20)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(Color.borderColor, lineWidth: 1)
            )
            .padding(.horizontal, 20)
            .padding(.top, 40)
            .frame(maxHeight: UIScreen.main.bounds.height * 0.7)
        }
        .transition(.opacity)
        .onAppear {
            notificationStore.markAllRead()
        }
    }
    
    // MARK: - Helpers
    
    private func refreshData() async {
        await ticketStore.fetchTickets()
        await ticketStore.fetchRecentActivity()
        if user?.role == .admin {
            await ticketStore.fetchStatusSummary()
        }
    }
}

// MARK: - Supporting Views

struct StatCard: View {
    let label: String
    let value: Int
    let hint: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label.uppercased())
                .font(.caption2)
                .foregroundColor(.secondary)
            
            Text("\(value)")
                .font(.title)
                .fontWeight(.bold)
                .foregroundColor(.white)
            
            Text(hint)
                .font(.caption2)
                .foregroundColor(Color.accentCyan)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(Color.cardBackground.opacity(0.9))
        .cornerRadius(18)
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(Color.blue.opacity(0.2), lineWidth: 1)
        )
    }
}

struct FilterChip: View {
    let label: String
    let isActive: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.subheadline)
                .foregroundColor(isActive ? .black : .secondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(isActive ? Color.accentCyan : Color.clear)
                .cornerRadius(999)
                .overlay(
                    RoundedRectangle(cornerRadius: 999)
                        .stroke(isActive ? Color.accentCyan : Color.borderColor, lineWidth: 1)
                )
        }
    }
}

struct MetricChip: View {
    let label: String
    let value: Int
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text("\(value)")
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(Color.cardBackground)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.borderColor, lineWidth: 1)
        )
    }
}

struct ActivityRow: View {
    let activity: TicketActivityEntry
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(activity.actor.name)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(.white)
            
            Text(activity.activityDescription)
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text(formatTime(activity.createdAt))
                .font(.caption2)
                .foregroundColor(.gray)
        }
        .padding(.bottom, 10)
        .overlay(
            Rectangle()
                .fill(Color.secondary.opacity(0.12))
                .frame(height: 1),
            alignment: .bottom
        )
    }
    
    private func formatTime(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: dateString) {
            let timeFormatter = DateFormatter()
            timeFormatter.timeStyle = .short
            return timeFormatter.string(from: date)
        }
        return dateString
    }
}

struct TicketCard: View {
    let ticket: Ticket
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("#\(String(ticket.id.prefix(8)))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Text(ticket.status.displayName)
                        .font(.caption)
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(statusColor(ticket.status))
                        .cornerRadius(999)
                }
                
                Text(ticket.description)
                    .font(.subheadline)
                    .foregroundColor(.white)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                
                HStack {
                    Text("Priority: \(ticket.priority.rawValue)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Text("Type: \(ticket.issueType.rawValue)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Text(ticket.assignee != nil ? "Assigned to \(ticket.assignee!.name)" : "Unassigned")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                if ticket.pendingSync == true {
                    Text("Pending sync")
                        .font(.caption2)
                        .fontWeight(.semibold)
                        .foregroundColor(Color.yellow)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.yellow.opacity(0.2))
                        .cornerRadius(999)
                        .overlay(
                            RoundedRectangle(cornerRadius: 999)
                                .stroke(Color.yellow, lineWidth: 1)
                        )
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(Color.cardBackground)
            .cornerRadius(18)
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(Color.borderColor, lineWidth: 1)
            )
        }
    }
    
    private func statusColor(_ status: TicketStatus) -> Color {
        switch status {
        case .open: return Color.cardBackground
        case .inProgress: return Color.blue.opacity(0.8)
        case .resolved: return Color.teal.opacity(0.8)
        }
    }
}

struct NavDrawerItem: View {
    let glyph: String
    let title: String
    let subtitle: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Text(glyph)
                    .font(.title2)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                    
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
            .padding(.vertical, 10)
            .overlay(
                Rectangle()
                    .fill(Color.borderColor.opacity(0.6))
                    .frame(height: 1),
                alignment: .bottom
            )
        }
    }
}

struct NotificationRow: View {
    let notification: NotificationEntry
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(notification.actor)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                    
                    Text(notification.summary)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    if !notification.isRead {
                        Circle()
                            .fill(Color.accentCyan)
                            .frame(width: 6, height: 6)
                    }
                    
                    Text(formatTime(notification.createdAt))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.vertical, 6)
            .padding(.horizontal, 8)
            .background(notification.isRead ? Color.clear : Color.accentCyan.opacity(0.08))
            .cornerRadius(12)
        }
    }
    
    private func formatTime(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: dateString) {
            let timeFormatter = DateFormatter()
            timeFormatter.timeStyle = .short
            return timeFormatter.string(from: date)
        }
        return dateString
    }
}

// MARK: - Placeholder Views

struct ReportsView: View {
    var body: some View {
        ZStack {
            Color.appBackground.ignoresSafeArea()
            Text("Reports")
                .foregroundColor(.white)
        }
        .navigationTitle("Reports")
    }
}

// MARK: - Preview

#Preview {
    DashboardView()
        .environmentObject(AuthStore.shared)
}
