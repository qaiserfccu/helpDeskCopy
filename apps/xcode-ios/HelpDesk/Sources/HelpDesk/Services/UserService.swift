import Foundation

// MARK: - User Service

public struct UserService {
    private let apiClient: APIClient
    
    public init(apiClient: APIClient = .shared) {
        self.apiClient = apiClient
    }
    
    public func fetchUsers(role: UserRole? = nil) async throws -> [UserSummary] {
        var queryItems: [URLQueryItem] = []
        
        if let role = role {
            queryItems.append(URLQueryItem(name: "role", value: role.rawValue))
        }
        
        let response: UsersResponse = try await apiClient.get(
            "/users",
            queryItems: queryItems.isEmpty ? nil : queryItems
        )
        return response.users
    }
    
    public func createUser(name: String, email: String, password: String, role: UserRole) async throws -> UserSummary {
        struct CreateUserPayload: Codable {
            let name: String
            let email: String
            let password: String
            let role: UserRole
        }
        
        let payload = CreateUserPayload(name: name, email: email, password: password, role: role)
        let response: UserResponse = try await apiClient.post("/users", body: payload)
        return response.user
    }
    
    public func updateUser(id: String, name: String? = nil, email: String? = nil, password: String? = nil, role: UserRole? = nil) async throws -> UserSummary {
        struct UpdateUserPayload: Codable {
            let name: String?
            let email: String?
            let password: String?
            let role: UserRole?
        }
        
        let payload = UpdateUserPayload(name: name, email: email, password: password, role: role)
        let response: UserResponse = try await apiClient.patch("/users/\(id)", body: payload)
        return response.user
    }
    
    public func deleteUser(id: String) async throws -> UserSummary {
        let response: UserResponse = try await apiClient.delete("/users/\(id)")
        return response.user
    }
}

// Helper response type
private struct UserResponse: Codable {
    let user: UserSummary
}
