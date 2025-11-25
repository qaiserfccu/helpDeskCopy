import Foundation
import SwiftUI
import KeychainAccess

// MARK: - Auth Store

@MainActor
public final class AuthStore: ObservableObject {
    public static let shared = AuthStore()
    
    @Published public private(set) var isInitialized = false
    @Published public private(set) var session: AuthSession?
    @Published public private(set) var isOfflineSession = false
    @Published public private(set) var isStaleSession = false
    
    private let keychain = Keychain(service: "com.helpdesk.ios")
    private let sessionKey = "helpdesk_session"
    private let authService = AuthService()
    
    public var isAuthenticated: Bool {
        session != nil
    }
    
    public var currentUser: AuthUser? {
        session?.user
    }
    
    private init() {
        setupNotificationObservers()
    }
    
    // MARK: - Bootstrap
    
    public func bootstrap() async {
        // Load persisted session
        if let sessionData = try? keychain.getData(sessionKey),
           let session = try? JSONDecoder().decode(AuthSession.self, from: sessionData) {
            self.session = session
            
            // Configure API client with tokens
            await APIClient.shared.setTokens(
                accessToken: session.accessToken,
                refreshToken: session.refreshToken
            )
            
            // Connect socket
            TicketSocketManager.shared.connect(accessToken: session.accessToken)
        }
        
        isInitialized = true
    }
    
    // MARK: - Authentication Actions
    
    public func signIn(email: String, password: String) async throws {
        let session = try await authService.login(email: email, password: password)
        await applySession(session)
    }
    
    public func register(name: String, email: String, password: String, role: UserRole?) async throws {
        let session = try await authService.register(name: name, email: email, password: password, role: role)
        await applySession(session)
    }
    
    public func signOut() async {
        // Clear tokens
        await APIClient.shared.clearTokens()
        
        // Disconnect socket
        TicketSocketManager.shared.disconnect()
        
        // Clear persisted session
        try? keychain.remove(sessionKey)
        
        // Clear state
        session = nil
        isOfflineSession = false
        isStaleSession = false
    }
    
    public func refreshSession() async {
        guard let refreshToken = session?.refreshToken else { return }
        
        do {
            let newSession = try await authService.refresh(refreshToken: refreshToken)
            await applySession(newSession)
        } catch {
            print("Session refresh failed: \(error.localizedDescription)")
            isStaleSession = true
        }
    }
    
    // MARK: - Session Management
    
    public func applySession(_ session: AuthSession, offline: Bool = false) async {
        self.session = session
        self.isOfflineSession = offline
        self.isStaleSession = false
        
        // Persist session
        if let sessionData = try? JSONEncoder().encode(session) {
            try? keychain.set(sessionData, key: sessionKey)
        }
        
        // Configure API client
        await APIClient.shared.setTokens(
            accessToken: session.accessToken,
            refreshToken: session.refreshToken
        )
        
        // Connect socket
        TicketSocketManager.shared.connect(accessToken: session.accessToken)
    }
    
    // MARK: - Notification Observers
    
    private func setupNotificationObservers() {
        NotificationCenter.default.addObserver(
            forName: .tokenRefreshed,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            guard let self = self,
                  let userInfo = notification.userInfo,
                  let accessToken = userInfo["accessToken"] as? String,
                  let refreshToken = userInfo["refreshToken"] as? String,
                  let user = userInfo["user"] as? AuthUser else { return }
            
            Task { @MainActor in
                let newSession = AuthSession(user: user, accessToken: accessToken, refreshToken: refreshToken)
                await self.applySession(newSession)
            }
        }
        
        NotificationCenter.default.addObserver(
            forName: .sessionExpired,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                await self?.signOut()
            }
        }
    }
}
