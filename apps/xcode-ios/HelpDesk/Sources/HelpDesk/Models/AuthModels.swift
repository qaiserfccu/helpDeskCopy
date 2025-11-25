import Foundation

// MARK: - User and Auth Types

public enum UserRole: String, Codable, CaseIterable {
    case user
    case agent
    case admin
}

public struct AuthUser: Codable, Identifiable, Equatable {
    public let id: String
    public let name: String
    public let email: String
    public let role: UserRole
    
    public init(id: String, name: String, email: String, role: UserRole) {
        self.id = id
        self.name = name
        self.email = email
        self.role = role
    }
}

public struct AuthSession: Codable, Equatable {
    public let user: AuthUser
    public let accessToken: String
    public let refreshToken: String
    
    public init(user: AuthUser, accessToken: String, refreshToken: String) {
        self.user = user
        self.accessToken = accessToken
        self.refreshToken = refreshToken
    }
}

public struct LoginInput: Codable {
    public let email: String
    public let password: String
    
    public init(email: String, password: String) {
        self.email = email
        self.password = password
    }
}

public struct RegisterInput: Codable {
    public let name: String
    public let email: String
    public let password: String
    public let role: UserRole?
    
    public init(name: String, email: String, password: String, role: UserRole? = nil) {
        self.name = name
        self.email = email
        self.password = password
        self.role = role
    }
}

// API Response Types
public struct AuthTokens: Codable {
    public let accessToken: String
    public let refreshToken: String
}

public struct AuthResponse: Codable {
    public let user: AuthUser
    public let tokens: AuthTokens
    
    public func toSession() -> AuthSession {
        AuthSession(
            user: user,
            accessToken: tokens.accessToken,
            refreshToken: tokens.refreshToken
        )
    }
}

// User Summary for listing
public struct UserSummary: Codable, Identifiable, Equatable {
    public let id: String
    public let name: String
    public let email: String
    public let role: UserRole
    
    public init(id: String, name: String, email: String, role: UserRole) {
        self.id = id
        self.name = name
        self.email = email
        self.role = role
    }
}
