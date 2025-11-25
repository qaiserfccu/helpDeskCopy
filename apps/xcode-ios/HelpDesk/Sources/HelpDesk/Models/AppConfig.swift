import Foundation

// MARK: - App Configuration

public struct AppConfig {
    public static var shared = AppConfig()
    
    public var apiBaseUrl: String {
        if let envUrl = ProcessInfo.processInfo.environment["API_BASE_URL"] {
            return envUrl
        }
        
        #if DEBUG
        // For iOS simulator, use localhost
        // For device, use your machine's IP address
        #if targetEnvironment(simulator)
        return "http://localhost:4000"
        #else
        return "http://192.168.1.100:4000" // Change to your dev machine IP
        #endif
        #else
        return "https://helpdesk-backend.fly.dev"
        #endif
    }
    
    public var apiUrl: String {
        return "\(apiBaseUrl)/api"
    }
    
    public var environment: String {
        ProcessInfo.processInfo.environment["ENVIRONMENT"] ?? "development"
    }
    
    private init() {}
}
