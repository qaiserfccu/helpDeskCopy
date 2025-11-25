import SwiftUI

// MARK: - Register View

public struct RegisterView: View {
    @EnvironmentObject var authStore: AuthStore
    @Environment(\.dismiss) var dismiss
    
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var selectedRole: UserRole = .user
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    
    public init() {}
    
    public var body: some View {
        ZStack {
            Color.appBackground.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(spacing: 8) {
                        Text("Create your Help Desk account")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                        
                        Text("Access the workspace instantly.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 20)
                    
                    // Demo Accounts Quick Fill
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Quick fill demo accounts")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        HStack(spacing: 12) {
                            ForEach(DemoAccount.allAccounts) { account in
                                DemoAccountButton(account: account) {
                                    email = account.email
                                    password = account.password
                                    selectedRole = account.role
                                }
                            }
                        }
                    }
                    
                    // Form Fields
                    VStack(spacing: 18) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Full name")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            TextField("Ada Lovelace", text: $name)
                                .textFieldStyle(AppTextFieldStyle())
                                .textInputAutocapitalization(.words)
                        }
                        
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Email")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            TextField("you@example.com", text: $email)
                                .textFieldStyle(AppTextFieldStyle())
                                .textInputAutocapitalization(.never)
                                .keyboardType(.emailAddress)
                                .autocorrectionDisabled()
                        }
                        
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Password")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            SecureField("At least 6 characters", text: $password)
                                .textFieldStyle(AppTextFieldStyle())
                        }
                        
                        // Role Selection
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Role")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            VStack(spacing: 12) {
                                ForEach(RoleOption.allOptions) { option in
                                    RoleOptionCard(
                                        option: option,
                                        isSelected: selectedRole == option.role
                                    ) {
                                        selectedRole = option.role
                                    }
                                }
                            }
                        }
                    }
                    
                    // Error Message
                    if let error = errorMessage {
                        Text(error)
                            .font(.subheadline)
                            .foregroundColor(.red)
                    }
                    
                    // Register Button
                    Button(action: handleRegister) {
                        HStack {
                            if isSubmitting {
                                ProgressView()
                                    .tint(.black)
                            } else {
                                Text("Create account")
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
                    
                    // Back to Login
                    Button(action: { dismiss() }) {
                        Text("Already have an account? Sign in")
                            .fontWeight(.semibold)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(24)
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .foregroundColor(.white)
                }
            }
        }
    }
    
    private func handleRegister() {
        guard !name.isEmpty else {
            errorMessage = "Please enter your name"
            return
        }
        guard !email.isEmpty else {
            errorMessage = "Please enter your email"
            return
        }
        guard password.count >= 6 else {
            errorMessage = "Password must be at least 6 characters"
            return
        }
        
        isSubmitting = true
        errorMessage = nil
        
        Task {
            do {
                try await authStore.register(name: name, email: email, password: password, role: selectedRole)
            } catch let apiError as APIError {
                await MainActor.run {
                    if case .httpError(let code, _) = apiError, code == 409 {
                        errorMessage = "An account with that email already exists."
                    } else {
                        errorMessage = apiError.localizedDescription
                    }
                    isSubmitting = false
                }
            } catch {
                await MainActor.run {
                    errorMessage = "We couldn't create your account"
                    isSubmitting = false
                }
            }
        }
    }
}

// MARK: - Role Option

struct RoleOption: Identifiable {
    let id = UUID()
    let label: String
    let description: String
    let role: UserRole
    
    static let allOptions = [
        RoleOption(label: "User", description: "Submit and track your own tickets", role: .user),
        RoleOption(label: "Agent", description: "Work assigned tickets", role: .agent),
        RoleOption(label: "Admin", description: "Configure and manage the workspace", role: .admin)
    ]
}

struct RoleOptionCard: View {
    let option: RoleOption
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 6) {
                Text(option.label)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(isSelected ? Color.accentCyan : .white)
                
                Text(option.description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(isSelected ? Color.selectedBackground : Color.cardBackground)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? Color.accentCyan : Color.borderColor, lineWidth: 1)
            )
        }
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        RegisterView()
            .environmentObject(AuthStore.shared)
    }
}
