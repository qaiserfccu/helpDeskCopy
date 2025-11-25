import SwiftUI

// MARK: - Root View

public struct RootView: View {
    @StateObject private var authStore = AuthStore.shared
    
    public init() {}
    
    public var body: some View {
        Group {
            if !authStore.isInitialized {
                LoadingView()
            } else if authStore.isAuthenticated {
                DashboardView()
            } else {
                LoginView()
            }
        }
        .environmentObject(authStore)
        .task {
            await authStore.bootstrap()
        }
        .preferredColorScheme(.dark)
    }
}

// MARK: - Loading View

struct LoadingView: View {
    var body: some View {
        ZStack {
            Color.appBackground.ignoresSafeArea()
            
            VStack(spacing: 20) {
                ProgressView()
                    .tint(Color.accentCyan)
                    .scaleEffect(1.5)
                
                Text("Loading...")
                    .font(.headline)
                    .foregroundColor(.white)
            }
        }
    }
}

// MARK: - Preview

#Preview {
    RootView()
}
