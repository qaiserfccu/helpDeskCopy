import SwiftUI

// MARK: - Login View

public struct LoginView: View {
    @EnvironmentObject var authStore: AuthStore
    
    // Pre-filled with demo credentials for development convenience
    // In production builds, these should be empty strings
    #if DEBUG
    @State private var email = "admin@helpdesk.local"
    @State private var password = "ChangeMe123!"
    #else
    @State private var email = ""
    @State private var password = ""
    #endif
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            ZStack {
                Color.appBackground.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Header
                        VStack(spacing: 8) {
                            Text("Sign in to Help Desk")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                            
                            Text("Use your workspace credentials to continue.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .padding(.top, 40)
                        
                        // Demo Accounts
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Quick fill demo accounts")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
                            HStack(spacing: 12) {
                                ForEach(DemoAccount.allAccounts) { account in
                                    DemoAccountButton(account: account) {
                                        email = account.email
                                        password = account.password
                                    }
                                }
                            }
                        }
                        
                        // Form Fields
                        VStack(spacing: 18) {
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
                                
                                SecureField("••••••••", text: $password)
                                    .textFieldStyle(AppTextFieldStyle())
                            }
                        }
                        
                        // Error Message
                        if let error = errorMessage {
                            Text(error)
                                .font(.subheadline)
                                .foregroundColor(.red)
                        }
                        
                        // Sign In Button
                        Button(action: handleSignIn) {
                            HStack {
                                if isSubmitting {
                                    ProgressView()
                                        .tint(.black)
                                } else {
                                    Text("Sign in")
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
                        
                        // Register Link
                        NavigationLink(destination: RegisterView()) {
                            Text("Need an account? Create one")
                                .fontWeight(.semibold)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(24)
                }
            }
            .navigationBarHidden(true)
        }
    }
    
    private func handleSignIn() {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = "Please enter email and password"
            return
        }
        
        isSubmitting = true
        errorMessage = nil
        
        Task {
            do {
                try await authStore.signIn(email: email, password: password)
            } catch {
                await MainActor.run {
                    errorMessage = "Invalid email or password"
                    isSubmitting = false
                }
            }
        }
    }
}

// MARK: - Demo Account Model

struct DemoAccount: Identifiable {
    let id = UUID()
    let label: String
    let email: String
    let password: String
    let role: UserRole
    
    static let allAccounts = [
        DemoAccount(label: "User", email: "user@helpdesk.local", password: "12345@", role: .user),
        DemoAccount(label: "Agent", email: "agent@helpdesk.local", password: "12345@", role: .agent),
        DemoAccount(label: "Admin", email: "admin@helpdesk.local", password: "12345@", role: .admin)
    ]
}

struct DemoAccountButton: View {
    let account: DemoAccount
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 4) {
                Text(account.label)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                
                Text(account.email)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(Color.cardBackground)
            .cornerRadius(14)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(Color.borderColor, lineWidth: 1)
            )
        }
    }
}

// MARK: - Custom Text Field Style

struct AppTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(14)
            .background(Color.inputBackground)
            .foregroundColor(.white)
            .cornerRadius(12)
    }
}

// MARK: - Preview

#Preview {
    LoginView()
        .environmentObject(AuthStore.shared)
}
