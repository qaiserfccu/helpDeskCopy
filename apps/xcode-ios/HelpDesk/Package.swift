// swift-tools-version:5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

// Note: This package is designed to be built on macOS with Xcode for iOS targets.
// The Socket.IO and Keychain dependencies require Apple platform SDKs.

let package = Package(
    name: "HelpDesk",
    platforms: [
        .iOS(.v16),
        .macOS(.v13)
    ],
    products: [
        .library(
            name: "HelpDesk",
            targets: ["HelpDesk"]
        ),
    ],
    dependencies: [
        // Socket.IO for real-time communication
        .package(url: "https://github.com/socketio/socket.io-client-swift.git", from: "16.1.0"),
        // Keychain wrapper for secure token storage
        .package(url: "https://github.com/kishikawakatsumi/KeychainAccess.git", from: "4.2.2"),
    ],
    targets: [
        .target(
            name: "HelpDesk",
            dependencies: [
                .product(name: "SocketIO", package: "socket.io-client-swift"),
                .product(name: "KeychainAccess", package: "KeychainAccess"),
            ],
            path: "Sources/HelpDesk"
        ),
    ]
)

// Build Instructions:
// 1. Open in Xcode on macOS: `open Package.swift`
// 2. Or create an Xcode project that includes this package as a dependency
// 3. Build for iOS Simulator or Device target
