# natrium-core

The Natrium SDK -- a Kotlin Multiplatform library that wraps Wire's Kalium SDK.

## Targets

| Target             | Source set       |
|--------------------|------------------|
| JVM                | `jvmMain`        |
| Android (minSdk 26)| `androidMain`    |
| iOS arm64          | `iosMain`        |
| iOS simulator      | `iosMain`        |

All public API code lives in `commonMain`.

## Public API Surface

### Entry Point

```kotlin
// Initialize with backend config and platform
Natrium.initialize(
    BackendConfig(
        name = "staging",
        api = "https://...",
        accounts = "https://...",
        webSocket = "https://...",
        teams = "https://...",
        blackList = "https://...",
        website = "https://...",
    ),
    NatriumPlatform(),  // or NatriumPlatform(context) on Android
)

// Login
val result = Natrium.login(email, password)
when (result) {
    is LoginResult.Success -> { /* result.session */ }
    is LoginResult.Failure.Error -> { /* result.reason */ }
    is LoginResult.Failure.TooManyDevices -> { /* result.resolver */ }
}

// Or restore a previous session
val session: Session? = Natrium.restoreLastSession()

// Observe auth state changes (login/logout events)
val cancellable = Natrium.observeAuthEvents { event ->
    when (event) {
        is AuthEvent.LoggedIn -> { /* event.session */ }
        is AuthEvent.LoggedOut -> { /* handle logout */ }
    }
}
```

### Session

After login, `Session` provides access to managers and profile updates:

```kotlin
session.conversationManager    // ConversationManager -- create, list, observe conversations
session.deviceManager          // DeviceManager -- list and remove devices

session.updateDisplayName("New Name")  // Returns UpdateDisplayNameResult
session.updateHandle("new-handle")     // Returns UpdateHandleResult
session.updateEmail("new@example.com") // Returns UpdateEmailResult

session.logout()               // Returns LogoutResult, cancels scope
```

### Conversation Management

```kotlin
// Observe conversations reactively (returns Cancellable, not Flow)
val cancellable = session.conversationManager.observeConversations { conversations ->
    conversations.forEach { convOps -> println(convOps.getConversationInfo()) }
}

// Create a new conversation
val createResult = session.conversationManager.createConversation(conversationInfo)
```

### Chat

```kotlin
val chat = convOps.chat()

// Send a message
chat.sendTextMessage("Hello")

// Observe incoming messages
val cancellable = chat.observeMessages { messages ->
    messages.forEach { println("${it.sender.name}: ${it.text}") }
}
```

### Device Management

```kotlin
val devices = session.deviceManager.list()
session.deviceManager.remove(deviceId, password)
```

## Design Principles

- **Callback + Cancellable pattern** for all reactive APIs. No Kotlin Flow in the public interface, ensuring Swift/iOS compatibility without wrapper libraries.
- **Sealed result types** for all operations (`Success` / `Failure.*`). No exceptions for expected errors.
- **Manager pattern** for API surface: `Natrium` -> `Session` -> managers -> per-conversation `ConversationOperations` -> `ChatOperations`.
- **Lifecycle-aware sync** via the `LifecycleAware` interface (Android foreground/background, iOS notifications, JVM always-on).

## Package Structure

All code lives under `schwarz.digits.natrium`:

```
schwarz.digits.natrium           Natrium, BackendConfig, AuthEvent, Cancellable, LogLevel
schwarz.digits.natrium.conversation  ConversationManager, ConversationOperations, ConversationInfo, ConversationId
schwarz.digits.natrium.chat      ChatOperations, ChatMessage, MessageStatus
schwarz.digits.natrium.devices   DeviceManager, DeviceInfo
schwarz.digits.natrium.lifecycle LifecycleAware, AppLifecycleState
schwarz.digits.natrium.session   Session, SessionInfo, LoginError
schwarz.digits.natrium.users     UserId, NamedUser
```

## Full API Reference

See [docs/api-reference.md](../docs/api-reference.md) for a complete listing of all public methods, parameters, and result types.
