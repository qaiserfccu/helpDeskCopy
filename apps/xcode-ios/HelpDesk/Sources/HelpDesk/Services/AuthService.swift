import Foundation

// MARK: - Auth Service

public struct AuthService {
    private let apiClient: APIClient
    
    public init(apiClient: APIClient = .shared) {
        self.apiClient = apiClient
    }
    
    public func login(email: String, password: String) async throws -> AuthSession {
        let input = LoginInput(email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), password: password)
        let response: AuthResponse = try await apiClient.post("/auth/login", body: input)
        return response.toSession()
    }
    
    public func register(name: String, email: String, password: String, role: UserRole? = nil) async throws -> AuthSession {
        let input = RegisterInput(
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            password: password,
            role: role
        )
        let response: AuthResponse = try await apiClient.post("/auth/register", body: input)
        return response.toSession()
    }
    
    public func refresh(refreshToken: String) async throws -> AuthSession {
        let response: AuthResponse = try await apiClient.post("/auth/refresh", body: ["refreshToken": refreshToken])
        return response.toSession()
    }
}
