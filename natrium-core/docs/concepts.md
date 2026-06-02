# Concepts

This page describes the patterns the SDK consistently uses. Once you understand these four — the object hierarchy, the callback + `Cancellable` pattern, sealed result types, and lifecycle awareness — every other API call follows the same shape.

## 1. Object hierarchy

Natrium's public API is structured as a chain. Each level is responsible for a smaller scope of behavior.

```
Natrium                          (object, app-wide entry point)
  ├─ initialize()                — once per process
  ├─ login() / ssoLogin() / ...  — produces a Session on success
  ├─ restoreLastSession()        — recovers persisted Session
  └─ observeAuthEvents()         — login/logout notifications

Session                          (interface, one per logged-in user)
  ├─ conversationManager: ConversationManager
  ├─ deviceManager: DeviceManager
  ├─ sessionInfo() / observeSessionInfo()
  ├─ updateDisplayName() / updateHandle() / updateEmail()
  └─ logout()

ConversationManager              (one per Session)
  ├─ observeConversations() / observeArchivedConversations()
  ├─ listConversations() / listArchivedConversations()
  ├─ findConversation(id)
  ├─ createConversation(title)
  └─ joinConversation(joinLink, password?)

ConversationOperations           (one per conversation)
  ├─ chat(): ChatOperations
  ├─ getConversationInfo() / setTitle()
  ├─ addMember() / removeMember() / getMembers()
  ├─ getJoinLink() / revokeJoinLink()
  └─ archive() / unarchive() / delete()

ChatOperations                   (one per conversation, for messaging)
  ├─ sendMessage() / sendReply()
  ├─ observeMessages() / getHistory()
  ├─ toggleReaction() / downloadFile()
  └─ sendTypingStarted() / sendTypingStopped() / observeTyping()
```

The rule of thumb: **everything starts from a `Session`**. The `Session` is what `login` returns, what `restoreLastSession` returns, and what `AuthEvent.LoggedIn` carries. There is no other entry point for user-scoped operations.

`ConversationOperations` instances are never constructed by you — you receive them from `ConversationManager.observeConversations(...)`, `listConversations()`, `findConversation(id)`, or as the result of `createConversation`/`joinConversation`. Calling `ConversationOperations.chat()` on one of those instances repeatedly returns the same cached `ChatOperations`; you don't construct that either.

## 2. Reactive APIs — Callback + `Cancellable`

Natrium does **not** expose Kotlin `Flow`s in its public interface. Reactive APIs use the callback + `Cancellable` pattern instead.

**Why no `Flow`?** Kotlin's `Flow` has no native bridging to Swift/Objective-C; consuming it from iOS typically requires an extra wrapper layer (e.g. SKIE or KMP-NativeCoroutines). Function-type callbacks compile directly to Swift closures, so the same Natrium API can be called from Kotlin and Swift without additional bindings.

The pattern is always the same:

```kotlin
val cancellable: Cancellable = someObject.observeSomething { value ->
    // called on every update; runs on a background dispatcher
}

// when you no longer want updates:
cancellable.cancel()
```

`Cancellable` has exactly one method — `cancel()` — which stops the underlying coroutine job. After cancellation no further callbacks are delivered.

### Mental model

If you are familiar with `Flow`, the rough equivalence is:

| `Flow` idiom | Natrium equivalent |
|---|---|
| `flow.collect { ... }` inside a `launch` | `observeXxx { ... }` |
| Cancelling the `Job` | `cancellable.cancel()` |
| `flow.first()` | one-shot `suspend fun` (e.g. `listConversations()`) |

### Cleanup discipline

Always cancel observers when the consuming scope ends. In Android/Compose this typically means:

```kotlin
DisposableEffect(Unit) {
    val cancellable = Natrium.observeAuthEvents { /* ... */ }
    onDispose { cancellable.cancel() }
}
```

In a `ViewModel`:

```kotlin
class ConversationsViewModel(session: Session) : ViewModel() {
    private val cancellable = session.conversationManager.observeConversations { /* ... */ }

    override fun onCleared() {
        cancellable.cancel()
        super.onCleared()
    }
}
```

Forgetting to cancel leaks a coroutine and prevents GC of the listener closure.

## 3. Sealed result types

Every fallible operation returns a sealed `Result` class with a `Success` branch and one or more `Failure` branches. **Natrium never throws for expected failures** — exceptions are reserved for programmer errors and infrastructure faults.

The pattern is:

```kotlin
sealed class XxxResult {
    data class Success(...) : XxxResult()         // or data object Success
    sealed class Failure : XxxResult() {
        data object SomeKnownReason : Failure()
        data class AnotherReason(val detail: String) : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}
```

So a typical caller looks like:

```kotlin
when (val result = session.conversationManager.createConversation("Team Chat")) {
    is CreateConversationResult.Success -> {
        val ops = result.conversationOperation
        // use ops
    }
    is CreateConversationResult.Failure.Forbidden     -> showError("You're not allowed to create conversations.")
    is CreateConversationResult.Failure.InvalidTitle  -> showError(result.message)
    is CreateConversationResult.Failure.NotLoggedIn   -> redirectToLogin()
    is CreateConversationResult.Failure.Unknown       -> showError(result.message)
}
```

A few common branches that recur across many result types:

- **`Unknown(message, cause)`** — generic catch-all wrapping a Kalium error or caught exception. Log `cause` if you have an error reporter; show a user-friendly message based on `message`. In the current SDK release this is the branch most failure paths converge on.
- **`NotLoggedIn`** — declared on most session-scoped result types (conversation, chat, device listings). In the current release the implementation does **not** emit this branch — session-invalid scenarios surface as `Unknown` or via `AuthEvent.LoggedOut` on the auth-event stream. The branch exists for forward compatibility; still handle it defensively (e.g. redirect to login) so your `when` blocks stay exhaustive against future changes.

See [api-reference.md](./api-reference.md) for the complete list of branches per result type.

## 4. Lifecycle awareness

The SDK gates a Kalium sync request on the platform's foreground/background state. You do not interact with this directly — the `NatriumPlatform` you pass to `Natrium.initialize` wires it for you:

| Platform | Active state signal | Inactive state signal |
|---|---|---|
| Android | `ProcessLifecycleOwner.STARTED` | `ProcessLifecycleOwner.STOPPED` |
| iOS     | `UIApplicationDidBecomeActiveNotification` | `UIApplicationDidEnterBackgroundNotification` |
| JVM     | Always active                  | (never inactive)                       |

What this means in practice:

- On Android/iOS, the SDK starts a Kalium sync request on `ACTIVE` and cancels it on `INACTIVE`. Your observers stay registered across these transitions; what the underlying transport (WebSocket, delta APIs) does on cancellation/restart is owned by Kalium.
- On JVM, sync runs continuously.
- You **don't** need to call any "start sync" / "stop sync" method — there isn't one in the public API.

Push-notification-driven background message delivery is **not** in scope of the current public-API surface.

## 5. Threading & coroutines

`Natrium.login`, `Natrium.ssoLogin`, `Natrium.ssoLoginWithCode`, `Natrium.completeSSOLogin`, and `DeviceLimitResolver.retry()` explicitly wrap their heavy work in `withContext(Dispatchers.Default)`. Other `suspend` functions (on `Session`, `ConversationManager`, `ConversationOperations`, `ChatOperations`, `DeviceManager`) delegate directly to Kalium and inherit whatever dispatching Kalium does — you can still call them from any coroutine scope safely.

```kotlin
viewModelScope.launch {
    val result = session.conversationManager.createConversation("Team Chat")
    // continues on viewModelScope's dispatcher; update UI here
}
```

Callback-based observers (`observeConversations`, `observeMessages`, `observeAuthEvents`, `observeTyping`, `observeSessionInfo`) fire on a background dispatcher (`Dispatchers.Default` from the SDK's internal scope). If you update UI state from inside an observer, hop back yourself:

```kotlin
val cancellable = session.conversationManager.observeConversations { conversations ->
    viewModelScope.launch {
        _state.update { it.copy(conversations = conversations.toList()) }
    }
}
```

`StateFlow`/`MutableStateFlow` updates are themselves thread-safe, so the snippet above works without explicit dispatcher hops if you only mutate state.

## 6. Storage & state persistence

Persistence is owned by the underlying Kalium SDK. After a successful login Kalium maintains, under the per-platform root directory (see [platforms.md](./platforms.md)):

- the authenticated account record,
- the cryptographic device identity (Proteus / MLS keys),
- the local conversation/message database.

There is no public natrium-core API to inspect or manipulate these files directly. The only public entry points that touch persisted state are:

- `Natrium.restoreLastSession()` — reads the most-recent persisted account.
- `Session.logout()` — performs a soft logout via Kalium and tears down the in-memory session scope. Whether the persisted account record is also wiped is determined by Kalium's logout semantics (in the current release it uses `LogoutReason.SELF_SOFT_LOGOUT`, which keeps device-local artifacts in place); treat the result of `restoreLastSession()` after a logout as the authoritative signal of what is still recoverable.

`Natrium.initialize(...)` is **not** destructive — it sets up the SDK without touching any pre-existing on-disk state.
