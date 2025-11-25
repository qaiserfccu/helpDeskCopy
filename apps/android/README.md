# HelpDesk Android App

Native Android implementation of the HelpDesk mobile app using Kotlin and Jetpack Compose, maintaining feature parity with the React Native version including real-time Socket.IO integration.

## Features

- **Authentication**
  - Login with email/password
  - Registration with role selection (User, Agent, Admin)
  - Secure token storage using Android EncryptedSharedPreferences
  - Offline session caching and biometric unlock support

- **Dashboard**
  - Real-time ticket list with status counts
  - Filter by status (Open, In Progress, Resolved)
  - Filter by assigned tickets (for agents/admins)
  - Pull-to-refresh functionality
  - Real-time updates via Socket.IO

- **Ticket Management**
  - View ticket details with full activity history
  - Create new tickets with priority and issue type selection
  - Edit existing tickets (for owners and admins)
  - Assign tickets to agents (admin only)
  - Request ticket assignment (agent only)
  - Resolve tickets (assigned agent only)
  - Reopen resolved tickets (admin only)

- **Admin Features**
  - Organization-wide status summary
  - User management (create, edit, delete users)
  - Agent workload visualization
  - High priority and stale ticket alerts
  - Recent activity feed

- **Real-time Features**
  - Socket.IO integration for live updates
  - Automatic ticket refresh on create/update events
  - Session-aware socket connection management

## Tech Stack

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture layers
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp
- **Real-time**: Socket.IO Client 2.1.0
- **State Management**: Kotlin Flow + StateFlow
- **Navigation**: Compose Navigation
- **Security**: Android Security Crypto (EncryptedSharedPreferences)

## Project Structure

```
app/src/main/java/com/helpdesk/app/
├── data/
│   ├── api/           # API interfaces, Socket.IO manager
│   ├── local/         # SecureStorage for encrypted preferences
│   ├── model/         # API DTOs
│   └── repository/    # Repository implementations
├── domain/
│   └── model/         # Domain models (User, Ticket, etc.)
├── di/                # Hilt modules
├── ui/
│   ├── theme/         # Compose theme (colors, typography)
│   ├── navigation/    # Navigation host and routes
│   ├── screens/       # Screen composables and ViewModels
│   │   ├── auth/      # Login, Register
│   │   ├── dashboard/ # Dashboard
│   │   ├── ticket/    # Detail, Form
│   │   ├── settings/  # Settings
│   │   └── admin/     # User Management, Status Summary
│   └── components/    # Shared UI components
└── util/              # Utilities (Result, NetworkMonitor)
```

## Setup

1. **Prerequisites**
   - Android Studio Hedgehog or later
   - JDK 17
   - Android SDK 34

2. **Configuration**
   - Update `API_BASE_URL` in `app/build.gradle.kts` if needed
   - Default: `http://10.0.2.2:4000` (Android emulator localhost)

3. **Build**
   - Open the project in Android Studio (recommended)
   - Android Studio will automatically generate the Gradle wrapper
   - Or manually generate the wrapper: `gradle wrapper --gradle-version=8.5`
   - Then build: `./gradlew assembleDebug`

4. **Run**
   - Open in Android Studio
   - Select a device/emulator
   - Run the app

## API Configuration

The app connects to the HelpDesk backend API. Configure the base URL:

- **Development (emulator)**: `http://10.0.2.2:4000`
- **Development (device)**: `http://<your-local-ip>:4000`
- **Production**: Set via `EXPO_PUBLIC_API_URL` environment variable or update `buildConfigField`

## Feature Parity with React Native

This native Android app maintains feature parity with the React Native version:

| Feature | React Native | Android Kotlin |
|---------|-------------|----------------|
| Login/Register | ✅ | ✅ |
| Dashboard with stats | ✅ | ✅ |
| Ticket list with filters | ✅ | ✅ |
| Ticket detail view | ✅ | ✅ |
| Create/Edit tickets | ✅ | ✅ |
| Assign tickets | ✅ | ✅ |
| Resolve/Reopen | ✅ | ✅ |
| Real-time Socket.IO | ✅ | ✅ |
| Offline session | ✅ | ✅ |
| User management | ✅ | ✅ |
| Status summary | ✅ | ✅ |
| Pull-to-refresh | ✅ | ✅ |
| Dark theme | ✅ | ✅ |

## License

Part of the HelpDesk project.
