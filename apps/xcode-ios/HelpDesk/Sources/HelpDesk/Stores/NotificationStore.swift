import Foundation
import SwiftUI

// MARK: - Notification Types

public enum NotificationType: String, Codable {
    case activity
    case ticket
}

public struct NotificationEntry: Identifiable, Codable, Equatable {
    public let id: String
    public let ticketId: String
    public let actor: String
    public let summary: String
    public let createdAt: String
    public let type: NotificationType
    public var isRead: Bool
    
    public init(id: String, ticketId: String, actor: String, summary: String, createdAt: String, type: NotificationType, isRead: Bool = false) {
        self.id = id
        self.ticketId = ticketId
        self.actor = actor
        self.summary = summary
        self.createdAt = createdAt
        self.type = type
        self.isRead = isRead
    }
}

// MARK: - Notification Store

@MainActor
public final class NotificationStore: ObservableObject {
    public static let shared = NotificationStore()
    
    private static let maxNotifications = 50
    
    @Published public private(set) var notifications: [NotificationEntry] = []
    @Published public private(set) var toastQueue: [NotificationEntry] = []
    
    public var unreadCount: Int {
        notifications.filter { !$0.isRead }.count
    }
    
    private init() {}
    
    // MARK: - Actions
    
    public func addNotification(id: String, ticketId: String, actor: String, summary: String, createdAt: String, type: NotificationType) {
        // Check if already exists
        guard !notifications.contains(where: { $0.id == id }) else { return }
        
        let entry = NotificationEntry(
            id: id,
            ticketId: ticketId,
            actor: actor,
            summary: summary,
            createdAt: createdAt,
            type: type,
            isRead: false
        )
        
        notifications.insert(entry, at: 0)
        
        // Limit notifications
        if notifications.count > Self.maxNotifications {
            notifications = Array(notifications.prefix(Self.maxNotifications))
        }
        
        // Add to toast queue
        toastQueue.append(entry)
    }
    
    public func seedFromHistory(_ entries: [NotificationEntry]) {
        guard notifications.isEmpty, !entries.isEmpty else { return }
        
        notifications = entries.prefix(Self.maxNotifications).map { entry in
            var mutableEntry = entry
            mutableEntry.isRead = true
            return mutableEntry
        }
    }
    
    public func markRead(_ id: String) {
        guard let index = notifications.firstIndex(where: { $0.id == id }) else { return }
        notifications[index].isRead = true
    }
    
    public func markTicketRead(_ ticketId: String) {
        for index in notifications.indices {
            if notifications[index].ticketId == ticketId {
                notifications[index].isRead = true
            }
        }
    }
    
    public func markAllRead() {
        for index in notifications.indices {
            notifications[index].isRead = true
        }
    }
    
    public func dequeueToast() {
        guard !toastQueue.isEmpty else { return }
        toastQueue.removeFirst()
    }
    
    public func clearAll() {
        notifications = []
        toastQueue = []
    }
}
