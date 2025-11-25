import Foundation
import SocketIO

// MARK: - Ticket Socket Events

public enum TicketSocketEvent: String {
    case ticketsCreated = "tickets:created"
    case ticketsUpdated = "tickets:updated"
    case ticketsActivity = "tickets:activity"
}

public struct TicketCreatedPayload: Codable {
    public let ticket: Ticket
}

public struct TicketUpdatedPayload: Codable {
    public let ticket: Ticket
}

public struct TicketActivityPayload: Codable {
    public let ticketId: String
    public let activity: TicketActivityEntry
}

// MARK: - Ticket Socket Manager

public final class TicketSocketManager: ObservableObject {
    public static let shared = TicketSocketManager()
    
    @Published public var isConnected = false
    @Published public var isRealtimeAvailable = true
    
    private var manager: SocketManager?
    private var socket: SocketIOClient?
    private var currentToken: String?
    private var pollingTimer: Timer?
    private let baseURL: String
    
    // Event handlers
    public var onTicketCreated: ((Ticket) -> Void)?
    public var onTicketUpdated: ((Ticket) -> Void)?
    public var onTicketActivity: ((String, TicketActivityEntry) -> Void)?
    
    private init(baseURL: String = AppConfig.shared.apiBaseUrl) {
        self.baseURL = baseURL
    }
    
    // MARK: - Connection Management
    
    public func connect(accessToken: String) {
        // Skip if already connected with same token
        if socket?.status == .connected && currentToken == accessToken {
            return
        }
        
        disconnect()
        currentToken = accessToken
        
        guard let url = URL(string: baseURL) else {
            print("TicketSocket: Invalid base URL")
            return
        }
        
        manager = SocketManager(
            socketURL: url,
            config: [
                .log(false),
                .compress,
                .forceWebsockets(false), // Allow polling fallback
                .reconnects(true),
                .reconnectAttempts(-1),
                .reconnectWait(2),
                .extraHeaders(["Authorization": "Bearer \(accessToken)"])
            ]
        )
        
        socket = manager?.defaultSocket
        
        setupEventHandlers()
        
        socket?.connect(withPayload: ["token": "Bearer \(accessToken)"])
    }
    
    public func disconnect() {
        stopPollingFallback()
        socket?.removeAllHandlers()
        socket?.disconnect()
        socket = nil
        manager = nil
        currentToken = nil
        
        DispatchQueue.main.async {
            self.isConnected = false
        }
    }
    
    public func syncSession(accessToken: String?) {
        guard let token = accessToken else {
            disconnect()
            return
        }
        
        if socket?.status != .connected || currentToken != token {
            connect(accessToken: token)
        }
    }
    
    // MARK: - Event Handlers
    
    private func setupEventHandlers() {
        guard let socket = socket else { return }
        
        socket.on(clientEvent: .connect) { [weak self] _, _ in
            print("TicketSocket: Connected")
            DispatchQueue.main.async {
                self?.isConnected = true
                self?.isRealtimeAvailable = true
            }
            self?.stopPollingFallback()
        }
        
        socket.on(clientEvent: .disconnect) { [weak self] data, _ in
            print("TicketSocket: Disconnected - \(data)")
            DispatchQueue.main.async {
                self?.isConnected = false
            }
            self?.startPollingFallback()
        }
        
        socket.on(clientEvent: .error) { [weak self] data, _ in
            print("TicketSocket: Error - \(data)")
            
            // Check if it's an auth error
            if let errorData = data.first as? [String: Any],
               let message = errorData["message"] as? String,
               self?.isAuthRelatedError(message) == true {
                // Token refresh would be handled by NotificationCenter
                NotificationCenter.default.post(name: .sessionExpired, object: nil)
            } else {
                self?.startPollingFallback()
            }
        }
        
        // Ticket events
        socket.on(TicketSocketEvent.ticketsCreated.rawValue) { [weak self] data, _ in
            guard let payloadData = data.first,
                  let jsonData = try? JSONSerialization.data(withJSONObject: payloadData),
                  let payload = try? JSONDecoder().decode(TicketCreatedPayload.self, from: jsonData) else {
                print("TicketSocket: Failed to decode tickets:created payload")
                return
            }
            
            DispatchQueue.main.async {
                self?.onTicketCreated?(payload.ticket)
            }
        }
        
        socket.on(TicketSocketEvent.ticketsUpdated.rawValue) { [weak self] data, _ in
            guard let payloadData = data.first,
                  let jsonData = try? JSONSerialization.data(withJSONObject: payloadData),
                  let payload = try? JSONDecoder().decode(TicketUpdatedPayload.self, from: jsonData) else {
                print("TicketSocket: Failed to decode tickets:updated payload")
                return
            }
            
            DispatchQueue.main.async {
                self?.onTicketUpdated?(payload.ticket)
            }
        }
        
        socket.on(TicketSocketEvent.ticketsActivity.rawValue) { [weak self] data, _ in
            guard let payloadData = data.first,
                  let jsonData = try? JSONSerialization.data(withJSONObject: payloadData),
                  let payload = try? JSONDecoder().decode(TicketActivityPayload.self, from: jsonData) else {
                print("TicketSocket: Failed to decode tickets:activity payload")
                return
            }
            
            DispatchQueue.main.async {
                self?.onTicketActivity?(payload.ticketId, payload.activity)
            }
        }
    }
    
    // MARK: - Polling Fallback
    
    private func startPollingFallback() {
        stopPollingFallback()
        
        DispatchQueue.main.async { [weak self] in
            self?.isRealtimeAvailable = false
            
            self?.pollingTimer = Timer.scheduledTimer(withTimeInterval: 15.0, repeats: true) { _ in
                NotificationCenter.default.post(name: .ticketRefreshNeeded, object: nil)
            }
        }
    }
    
    private func stopPollingFallback() {
        pollingTimer?.invalidate()
        pollingTimer = nil
    }
    
    // MARK: - Helpers
    
    private func isAuthRelatedError(_ message: String) -> Bool {
        let normalized = message.lowercased()
        return normalized.contains("invalid socket token") ||
               normalized.contains("socket token missing") ||
               normalized.contains("jwt expired") ||
               normalized.contains("invalid token")
    }
}

// MARK: - Notification Names

public extension Notification.Name {
    static let ticketRefreshNeeded = Notification.Name("ticketRefreshNeeded")
}
