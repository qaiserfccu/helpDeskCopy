import Foundation

// MARK: - API Client

public enum APIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(statusCode: Int, message: String?)
    case decodingError(Error)
    case encodingError(Error)
    case networkError(Error)
    case unauthorized
    case unknown
    
    public var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .httpError(let statusCode, let message):
            return message ?? "HTTP Error: \(statusCode)"
        case .decodingError(let error):
            return "Failed to decode response: \(error.localizedDescription)"
        case .encodingError(let error):
            return "Failed to encode request: \(error.localizedDescription)"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .unauthorized:
            return "Unauthorized - please sign in again"
        case .unknown:
            return "An unknown error occurred"
        }
    }
}

public actor APIClient {
    public static let shared = APIClient()
    
    private let baseURL: String
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder
    
    private var accessToken: String?
    private var refreshToken: String?
    private var isRefreshing = false
    private var pendingRequests: [(CheckedContinuation<Void, Error>)] = []
    
    public init(baseURL: String = AppConfig.shared.apiUrl) {
        self.baseURL = baseURL
        
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: config)
        
        self.decoder = JSONDecoder()
        self.decoder.keyDecodingStrategy = .useDefaultKeys
        
        self.encoder = JSONEncoder()
        self.encoder.keyEncodingStrategy = .useDefaultKeys
    }
    
    // MARK: - Token Management
    
    public func setTokens(accessToken: String?, refreshToken: String?) {
        self.accessToken = accessToken
        self.refreshToken = refreshToken
    }
    
    public func clearTokens() {
        self.accessToken = nil
        self.refreshToken = nil
    }
    
    // MARK: - Error Message Extraction
    
    /// Extracts error message from API response data
    private func extractErrorMessage(from data: Data) -> String? {
        // Try to decode as a simple message object
        if let response = try? decoder.decode([String: String].self, from: data),
           let message = response["message"] {
            return message
        }
        
        // Try to decode as a more complex error response
        struct ErrorResponse: Decodable {
            let message: String?
            let error: String?
        }
        
        if let response = try? decoder.decode(ErrorResponse.self, from: data) {
            return response.message ?? response.error
        }
        
        return nil
    }
    
    // MARK: - Request Methods
    
    public func get<T: Decodable>(_ path: String, queryItems: [URLQueryItem]? = nil) async throws -> T {
        try await request(method: "GET", path: path, queryItems: queryItems)
    }
    
    public func post<T: Decodable, B: Encodable>(_ path: String, body: B) async throws -> T {
        try await request(method: "POST", path: path, body: body)
    }
    
    public func post<T: Decodable>(_ path: String) async throws -> T {
        try await request(method: "POST", path: path)
    }
    
    public func patch<T: Decodable, B: Encodable>(_ path: String, body: B) async throws -> T {
        try await request(method: "PATCH", path: path, body: body)
    }
    
    public func delete<T: Decodable>(_ path: String) async throws -> T {
        try await request(method: "DELETE", path: path)
    }
    
    // MARK: - Private Request Implementation
    
    private func request<T: Decodable>(
        method: String,
        path: String,
        queryItems: [URLQueryItem]? = nil,
        body: (any Encodable)? = nil,
        retryOnUnauthorized: Bool = true
    ) async throws -> T {
        guard var urlComponents = URLComponents(string: "\(baseURL)\(path)") else {
            throw APIError.invalidURL
        }
        
        if let queryItems = queryItems, !queryItems.isEmpty {
            urlComponents.queryItems = queryItems
        }
        
        guard let url = urlComponents.url else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let body = body {
            do {
                request.httpBody = try encoder.encode(body)
            } catch {
                throw APIError.encodingError(error)
            }
        }
        
        do {
            let (data, response) = try await session.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.invalidResponse
            }
            
            // Handle 401 Unauthorized
            if httpResponse.statusCode == 401 && retryOnUnauthorized {
                if let refreshToken = self.refreshToken {
                    let refreshed = try await refreshTokens(refreshToken: refreshToken)
                    if refreshed {
                        // Retry the request with new token
                        return try await self.request(
                            method: method,
                            path: path,
                            queryItems: queryItems,
                            body: body,
                            retryOnUnauthorized: false
                        )
                    }
                }
                throw APIError.unauthorized
            }
            
            // Handle other error status codes
            guard 200..<300 ~= httpResponse.statusCode else {
                let message = extractErrorMessage(from: data)
                throw APIError.httpError(statusCode: httpResponse.statusCode, message: message)
            }
            
            do {
                return try decoder.decode(T.self, from: data)
            } catch {
                throw APIError.decodingError(error)
            }
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.networkError(error)
        }
    }
    
    private func refreshTokens(refreshToken: String) async throws -> Bool {
        if isRefreshing {
            // Wait for ongoing refresh
            try await withCheckedThrowingContinuation { continuation in
                pendingRequests.append(continuation)
            }
            return accessToken != nil
        }
        
        isRefreshing = true
        defer {
            isRefreshing = false
            // Resume all pending requests
            let pending = pendingRequests
            pendingRequests = []
            for continuation in pending {
                continuation.resume()
            }
        }
        
        do {
            let response: AuthResponse = try await request(
                method: "POST",
                path: "/auth/refresh",
                body: ["refreshToken": refreshToken],
                retryOnUnauthorized: false
            )
            
            self.accessToken = response.tokens.accessToken
            self.refreshToken = response.tokens.refreshToken
            
            // Notify the auth store about the new tokens
            await MainActor.run {
                NotificationCenter.default.post(
                    name: .tokenRefreshed,
                    object: nil,
                    userInfo: [
                        "accessToken": response.tokens.accessToken,
                        "refreshToken": response.tokens.refreshToken,
                        "user": response.user
                    ]
                )
            }
            
            return true
        } catch {
            self.accessToken = nil
            self.refreshToken = nil
            return false
        }
    }
}

// MARK: - Notification Names

public extension Notification.Name {
    static let tokenRefreshed = Notification.Name("tokenRefreshed")
    static let sessionExpired = Notification.Name("sessionExpired")
}
