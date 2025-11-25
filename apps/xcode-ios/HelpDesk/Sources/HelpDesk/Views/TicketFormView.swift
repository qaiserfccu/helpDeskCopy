import SwiftUI

// MARK: - Ticket Form View

public struct TicketFormView: View {
    @EnvironmentObject var authStore: AuthStore
    @StateObject var ticketStore = TicketStore.shared
    @Environment(\.dismiss) var dismiss
    
    let editingTicket: Ticket?
    
    @State private var description = ""
    @State private var priority: TicketPriority = .medium
    @State private var issueType: IssueType = .other
    @State private var isSubmitting = false
    @State private var showAlert = false
    @State private var alertTitle = ""
    @State private var alertMessage = ""
    
    private var isEdit: Bool { editingTicket != nil }
    private var isLockedFromEditing: Bool { isEdit && editingTicket?.status == .resolved }
    private var headerTitle: String { isEdit ? "Update ticket" : "Create ticket" }
    
    public init(editingTicket: Ticket? = nil) {
        self.editingTicket = editingTicket
    }
    
    public var body: some View {
        NavigationStack {
            ZStack {
                Color.appBackground.ignoresSafeArea()
                
                if isLockedFromEditing {
                    lockedView
                } else {
                    formView
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
            .onAppear {
                if let ticket = editingTicket {
                    description = ticket.description
                    priority = ticket.priority
                    issueType = ticket.issueType
                }
            }
            .alert(alertTitle, isPresented: $showAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(alertMessage)
            }
        }
    }
    
    // MARK: - Locked View
    
    private var lockedView: some View {
        VStack(spacing: 16) {
            Text("Ticket is resolved")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.white)
            
            Text("Reopen the ticket from the detail screen before making changes. Contact support if you need additional help.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            Button(action: { dismiss() }) {
                Text("Back to ticket")
                    .fontWeight(.semibold)
                    .foregroundColor(Color.accentCyan)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(Color.accentCyan, lineWidth: 1)
                    )
            }
        }
        .padding(24)
        .background(Color.cardBackground)
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.borderColor, lineWidth: 1)
        )
        .padding(24)
    }
    
    // MARK: - Form View
    
    private var formView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                Text(headerTitle)
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                // Description
                VStack(alignment: .leading, spacing: 8) {
                    Text("Description")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    TextEditor(text: $description)
                        .frame(minHeight: 120)
                        .padding(14)
                        .background(Color.inputBackground)
                        .foregroundColor(.white)
                        .cornerRadius(16)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.borderColor, lineWidth: 1)
                        )
                }
                
                // Priority
                VStack(alignment: .leading, spacing: 8) {
                    Text("Priority")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    HStack(spacing: 8) {
                        ForEach(TicketPriority.allCases, id: \.self) { p in
                            OptionChip(
                                label: p.rawValue.capitalized,
                                isSelected: priority == p
                            ) {
                                priority = p
                            }
                        }
                    }
                }
                
                // Issue Type
                VStack(alignment: .leading, spacing: 8) {
                    Text("Issue type")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    FlexibleGrid(data: IssueType.allCases, spacing: 8) { type in
                        OptionChip(
                            label: type.rawValue.capitalized,
                            isSelected: issueType == type
                        ) {
                            issueType = type
                        }
                    }
                }
                
                // Existing Attachments (Edit mode)
                if isEdit, let attachments = editingTicket?.attachments, !attachments.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Current files")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        ForEach(attachments, id: \.self) { attachment in
                            Text(attachment.components(separatedBy: "/").last ?? attachment)
                                .font(.subheadline)
                                .foregroundColor(.white)
                        }
                    }
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.cardBackground)
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.borderColor, lineWidth: 1)
                    )
                }
                
                // Attachments Placeholder
                VStack(alignment: .leading, spacing: 8) {
                    Text("Attachments")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    VStack(spacing: 10) {
                        Text("No new files selected.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        Button(action: {
                            // File picker would go here
                            alertTitle = "Coming Soon"
                            alertMessage = "File attachment support will be added in a future update."
                            showAlert = true
                        }) {
                            Text("+ Add file")
                                .fontWeight(.semibold)
                                .foregroundColor(Color.accentCyan)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.accentCyan, lineWidth: 1)
                                )
                        }
                    }
                    .padding(14)
                    .background(Color.cardBackground)
                    .cornerRadius(16)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(Color.borderColor, lineWidth: 1)
                    )
                }
                
                // Submit Button
                Button(action: handleSubmit) {
                    HStack {
                        if isSubmitting {
                            ProgressView()
                                .tint(.black)
                        } else {
                            Text(isEdit ? "Update ticket" : "Create ticket")
                                .fontWeight(.bold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.accentCyan)
                    .foregroundColor(.black)
                    .cornerRadius(16)
                }
                .disabled(isSubmitting)
                .opacity(isSubmitting ? 0.7 : 1)
                
                // Cancel Button
                Button(action: { dismiss() }) {
                    Text("Cancel")
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
            .padding(20)
            .padding(.bottom, 40)
        }
    }
    
    // MARK: - Actions
    
    private func handleSubmit() {
        let trimmedDescription = description.trimmingCharacters(in: .whitespacesAndNewlines)
        
        guard !trimmedDescription.isEmpty else {
            alertTitle = "Description required"
            alertMessage = "Please describe the issue."
            showAlert = true
            return
        }
        
        isSubmitting = true
        
        Task {
            do {
                if isEdit, let ticketId = editingTicket?.id {
                    _ = try await ticketStore.updateTicket(
                        id: ticketId,
                        description: trimmedDescription,
                        priority: priority,
                        issueType: issueType,
                        status: nil
                    )
                } else {
                    _ = try await ticketStore.createTicket(
                        description: trimmedDescription,
                        priority: priority,
                        issueType: issueType
                    )
                }
                
                await MainActor.run {
                    dismiss()
                }
            } catch {
                await MainActor.run {
                    alertTitle = "Save failed"
                    alertMessage = "Please try again."
                    showAlert = true
                    isSubmitting = false
                }
            }
        }
    }
}

// MARK: - Option Chip

struct OptionChip: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.subheadline)
                .foregroundColor(isSelected ? .black : .secondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? Color.accentCyan : Color.clear)
                .cornerRadius(999)
                .overlay(
                    RoundedRectangle(cornerRadius: 999)
                        .stroke(isSelected ? Color.accentCyan : Color.borderColor, lineWidth: 1)
                )
        }
    }
}

// MARK: - Flexible Grid

struct FlexibleGrid<Data: RandomAccessCollection, Content: View>: View where Data.Element: Hashable {
    let data: Data
    let spacing: CGFloat
    let content: (Data.Element) -> Content
    
    init(data: Data, spacing: CGFloat = 8, @ViewBuilder content: @escaping (Data.Element) -> Content) {
        self.data = data
        self.spacing = spacing
        self.content = content
    }
    
    var body: some View {
        GeometryReader { geometry in
            generateContent(in: geometry)
        }
    }
    
    private func generateContent(in geometry: GeometryProxy) -> some View {
        var width = CGFloat.zero
        var height = CGFloat.zero
        
        return ZStack(alignment: .topLeading) {
            ForEach(Array(data.enumerated()), id: \.element) { _, item in
                content(item)
                    .padding(.horizontal, spacing / 2)
                    .padding(.vertical, spacing / 2)
                    .alignmentGuide(.leading) { dimension in
                        if abs(width - dimension.width) > geometry.size.width {
                            width = 0
                            height -= dimension.height
                        }
                        let result = width
                        if item == data.last! {
                            width = 0
                        } else {
                            width -= dimension.width
                        }
                        return result
                    }
                    .alignmentGuide(.top) { _ in
                        let result = height
                        if item == data.last! {
                            height = 0
                        }
                        return result
                    }
            }
        }
        .frame(height: 80) // Approximate height for wrapping content
    }
}

// MARK: - Preview

#Preview {
    TicketFormView()
        .environmentObject(AuthStore.shared)
}
