import SwiftUI

// MARK: - App Colors

public extension Color {
    // Primary background colors (matching React Native dark theme)
    static let appBackground = Color(hex: "020617")
    static let cardBackground = Color(hex: "0F172A")
    static let inputBackground = Color(hex: "1E293B")
    static let selectedBackground = Color(hex: "082F49")
    
    // Border colors
    static let borderColor = Color(hex: "1E293B")
    
    // Accent colors
    static let accentCyan = Color(hex: "22D3EE")
    
    // Text colors
    static let textPrimary = Color(hex: "F8FAFC")
    static let textSecondary = Color(hex: "94A3B8")
    static let textMuted = Color(hex: "475569")
    
    // Status colors
    static let statusOpen = Color(hex: "0F172A")
    static let statusInProgress = Color(hex: "1E3A8A")
    static let statusResolved = Color(hex: "0F766E")
    
    // Alert colors
    static let dangerRed = Color(hex: "DC2626")
    static let warningYellow = Color(hex: "FBBF24")
    
    // Initialize from hex string
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

// MARK: - App Styling Utilities

public extension View {
    /// Apply the app's primary button style
    func primaryButtonStyle() -> some View {
        self
            .fontWeight(.bold)
            .foregroundColor(.black)
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.accentCyan)
            .cornerRadius(16)
    }
    
    /// Apply the app's secondary button style
    func secondaryButtonStyle() -> some View {
        self
            .fontWeight(.semibold)
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding()
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color.borderColor, lineWidth: 1)
            )
    }
    
    /// Apply the app's card style
    func cardStyle() -> some View {
        self
            .padding(16)
            .background(Color.cardBackground)
            .cornerRadius(18)
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(Color.borderColor, lineWidth: 1)
            )
    }
}
