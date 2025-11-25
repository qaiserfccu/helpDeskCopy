# HelpDesk iOS App (Swift/SwiftUI)

Native iOS implementation of the HelpDesk mobile application, replicating the React Native app functionality using Swift and SwiftUI.

## Overview

This is a complete native iOS port of the HelpDesk React Native application. It maintains feature parity including:

- **Authentication**: Login, Registration, Token refresh, Secure session storage
- **Tickets Management**: Create, View, Update, Assign, Resolve tickets
- **Real-time Updates**: Socket.IO integration for live ticket events
- **Role-based Access**: User, Agent, Admin roles with appropriate permissions
- **Offline Support**: Keychain-based session persistence
- **Dark Theme**: Consistent with the React Native app design

## Architecture

### Project Structure

```
HelpDesk/
├── Package.swift                 # Swift Package Manager manifest
├── Sources/
│   └── HelpDesk/
│       ├── HelpDeskApp.swift     # App entry point
│       ├── Models/               # Data models
│       │   ├── AuthModels.swift
│       │   ├── TicketModels.swift
│       │   └── AppConfig.swift
│       ├── Services/             # API services
│       │   ├── APIClient.swift
│       │   ├── AuthService.swift
│       │   ├── TicketService.swift
│       │   └── UserService.swift
│       ├── Stores/               # State management (ObservableObject)
│       │   ├── AuthStore.swift
│       │   ├── TicketStore.swift
│       │   └── NotificationStore.swift
│       ├── Realtime/             # Socket.IO integration
│       │   └── TicketSocketManager.swift
│       ├── Views/                # SwiftUI views
│       │   ├── LoginView.swift
│       │   ├── RegisterView.swift
│       │   ├── DashboardView.swift
│       │   ├── TicketDetailView.swift
│       │   └── TicketFormView.swift
│       ├── Navigation/           # Navigation structure
│       │   └── RootView.swift
│       └── Utilities/            # Helpers and extensions
│           └── Colors.swift
└── Resources/                    # Assets (images, etc.)
```

### Key Components

#### State Management
Using SwiftUI's `@StateObject` and `@EnvironmentObject` for state management, similar to Zustand in React Native:
- `AuthStore`: Handles authentication state and token management
- `TicketStore`: Manages ticket data and operations
- `NotificationStore`: Handles in-app notifications

#### Networking
- `APIClient`: Actor-based API client with automatic token refresh
- Services layer for domain-specific API calls

#### Real-time
- `TicketSocketManager`: Socket.IO client for real-time ticket updates
- Polling fallback when WebSocket connections fail

## Dependencies

- [Socket.IO Client Swift](https://github.com/socketio/socket.io-client-swift) - Real-time communication
- [KeychainAccess](https://github.com/kishikawakatsumi/KeychainAccess) - Secure token storage

## Requirements

- iOS 16.0+
- Xcode 15.0+
- Swift 5.9+

## Setup

### 1. Open in Xcode

```bash
cd apps/xcode-ios/HelpDesk
open Package.swift
```

Or create a new Xcode project and add this package as a local dependency.

### 2. Configure API URL

Edit `Sources/HelpDesk/Models/AppConfig.swift` to set your backend URL:

```swift
#if targetEnvironment(simulator)
return "http://localhost:4000"
#else
return "http://YOUR_DEV_MACHINE_IP:4000" // For physical devices
#endif
```

### 3. Run the Backend

Make sure the backend server is running:

```bash
cd apps/backend
npm run dev
```

### 4. Build and Run

Build and run on simulator or device using Xcode.

## Features Parity with React Native

| Feature | React Native | iOS Swift |
|---------|-------------|-----------|
| Login/Register | ✅ | ✅ |
| Demo Account Presets | ✅ | ✅ |
| Dashboard with Stats | ✅ | ✅ |
| Ticket List with Filters | ✅ | ✅ |
| Ticket Create/Edit | ✅ | ✅ |
| Ticket Detail View | ✅ | ✅ |
| Assignment Management | ✅ | ✅ |
| Real-time Updates (Socket.IO) | ✅ | ✅ |
| Notifications | ✅ | ✅ |
| Offline Session | ✅ | ✅ |
| Role-based UI | ✅ | ✅ |
| Dark Theme | ✅ | ✅ |
| File Attachments | ✅ | 🚧 (UI placeholder) |

## Real-time Events

The app listens for the following Socket.IO events:
- `tickets:created` - New ticket created
- `tickets:updated` - Ticket updated
- `tickets:activity` - Ticket activity (status changes, assignments, etc.)

## Demo Accounts

Pre-configured demo accounts for **development and testing only**:

| Role | Email | Password |
|------|-------|----------|
| User | user@helpdesk.local | 12345@ |
| Agent | agent@helpdesk.local | 12345@ |
| Admin | admin@helpdesk.local | 12345@ |

> ⚠️ **Security Note**: These credentials are for local development environments only. Never use default or demo credentials in production. Always configure secure, unique credentials for production deployments.

## Contributing

1. Follow Swift naming conventions and SwiftUI best practices
2. Keep the design consistent with the React Native app
3. Ensure all new features maintain role-based access control
4. Test on both simulator and physical devices

## License

Same license as the parent project.
