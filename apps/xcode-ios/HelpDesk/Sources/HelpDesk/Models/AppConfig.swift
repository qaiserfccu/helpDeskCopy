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
        // For physical device testing, replace with your development machine's IP address
        // You can find your IP with: ifconfig | grep "inet " | grep -v 127.0.0.1
        #if targetEnvironment(simulator)
        return "http://localhost:4000"
        #else
        // TODO: Replace with your development machine's local IP address for device testing
        // Example: return "http://192.168.x.x:4000"
        return "http://localhost:4000"
        #endif
        #else
        // Production backend URL
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
