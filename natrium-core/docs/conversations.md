# Conversations

`ConversationManager` is the per-`Session` API for the conversation lifecycle: list, find, create, join. Per-conversation operations (members, title, delete, join links) live on `ConversationOperations`. Messaging itself is documented in [messaging.md](./messaging.md).

> Prerequisite: you have a `Session`. See [authentication.md](./authentication.md).

## 1. The `ConversationManager`

```kotlin
val mgr: ConversationManager = session.conversationManager
```

You never construct a `ConversationManager` yourself — it lives as a property on `Session`. There is exactly one per logged-in user.

## 2. Listing conversations

Natrium offers both a **reactive** observer (the primary pattern) and a **one-shot** snapshot.

### Reactive

```kotlin
val cancellable = session.conversationManager.observeConversations { conversations ->
    // conversations: Collection<ConversationOperations>
    // emitted on the initial load and on every subsequent change
}
```

The listener receives a fresh `Collection<ConversationOperations>` snapshot every time the underlying state changes (new conversation, member joined, title renamed). Each emission is a freshly constructed snapshot — the SDK does not promise that the `ConversationOperations` instance for a given conversation is the same across emissions. Derive any per-conversation state (title, etc.) from the entries themselves (e.g. by calling `getConversationInfo()` per entry) instead of caching state keyed by instance identity.

Cancel when the consumer is gone (typical `ViewModel.onCleared()` or Compose `DisposableEffect.onDispose`).

### One-shot snapshot

```kotlin
when (val result = session.conversationManager.listConversations()) {
    is ConversationListResult.Success ->
        useConversations(result.conversations)
    ConversationListResult.Failure.NotLoggedIn ->
        redirectToLogin()
    is ConversationListResult.Failure.SyncFailed ->
        showWarning("Couldn't sync: ${result.message}")
    is ConversationListResult.Failure.Unknown ->
        showError(result.message)
}
```

## 3. Finding a single conversation

When you already have a `ConversationId` (e.g. from a deep link or a saved-state restore):

```kotlin
val id = ConversationId.fromString("8e7c5e1c-…@example.com")

when (val result = session.conversationManager.findConversation(id)) {
    is FindConversationResult.Success ->
        openChat(result.conversationOperations)
    FindConversationResult.Failure.NotFound  ->
        showError("Conversation not found.")
    FindConversationResult.Failure.NotLoggedIn ->
        redirectToLogin()
    is FindConversationResult.Failure.Unknown ->
        showError(result.message)
}
```

`ConversationId` is a qualified id (`value@domain`). The companion function `fromString(...)` parses that canonical form; `toString()` produces it back.

## 4. Creating a conversation

```kotlin
when (val result = session.conversationManager.createConversation("Project Atlas")) {
    is CreateConversationResult.Success ->
        openChat(result.conversationOperation)         // note: singular
    CreateConversationResult.Failure.NotLoggedIn ->
        redirectToLogin()
    CreateConversationResult.Failure.Forbidden ->
        showError("You don't have permission to create conversations.")
    is CreateConversationResult.Failure.InvalidTitle ->
        showError(result.message)
    is CreateConversationResult.Failure.Unknown ->
        showError(result.message)
}
```

The returned `ConversationOperations` is ready to use immediately — the SDK constructs it from the freshly created conversation id without an additional sync step. A subsequent emission on any active `observeConversations` listener is driven by the underlying Kalium observation pipeline.

`InvalidTitle(message)` is declared as a failure branch but the current implementation does not produce it; bad titles surface as `Unknown(message, cause)` instead. Keep the branch in your `when` for forward compatibility.

## 5. Joining via link

`JoinLink` is a value class wrapping the guest-room URL or code your user pastes in:

```kotlin
val link = JoinLink("https://wire.example.com/join/abc123...")

when (val result = session.conversationManager.joinConversation(link)) {
    is JoinConversationResult.Success ->
        openChat(result.conversationOperations)
    JoinConversationResult.Failure.InvalidLink ->
        showError("This link isn't valid.")
    JoinConversationResult.Failure.IncorrectPassword ->
        showError("This conversation is password-protected.")
    JoinConversationResult.Failure.NotLoggedIn ->
        redirectToLogin()
    is JoinConversationResult.Failure.Unknown ->
        showError(result.message)
}
```

For password-protected links, pass the password as the second argument:

```kotlin
session.conversationManager.joinConversation(link, password = "swordfish")
```

`IncorrectPassword` is surfaced when the backend rejects the password. Re-prompt the user and retry.

## 6. Operating on a conversation

`ConversationOperations` is the per-conversation interface. You get it from the manager (list/find/create/join). Its methods divide into four groups:

| Group | Methods |
|---|---|
| Messaging | `chat(): ChatOperations` — see [messaging.md](./messaging.md) |
| Metadata  | `getConversationInfo()`, `setTitle(title)` |
| Members   | `getMembers()`, `addMember(userId)`, `removeMember(userId)` |
| Sharing   | `getJoinLink(password?)`, `revokeJoinLink()` |
| Lifecycle | `delete()` |

### 6a. Metadata

```kotlin
val info = (ops.getConversationInfo() as? GetConversationInfoResult.Success)?.conversationInfo
// ConversationInfo(id, title)

when (val r = ops.setTitle("New Title")) {
    SetTitleResult.Success              -> Unit
    SetTitleResult.Failure.NotLoggedIn  -> redirectToLogin()
    is SetTitleResult.Failure.Unknown   -> showError(r.message)
}
```

`getConversationInfo` failure branches are `NotLoggedIn` and `Unknown(message, cause)`.

### 6b. Members

```kotlin
val userId = UserId.fromString("bob@example.com")

when (val r = ops.addMember(userId)) {
    AddMemberResult.Success             -> Unit
    AddMemberResult.Failure.NotLoggedIn -> redirectToLogin()
    is AddMemberResult.Failure.Unknown  -> showError(r.message)
}

when (val r = ops.removeMember(userId)) {
    RemoveMemberResult.Success             -> Unit
    RemoveMemberResult.Failure.NotLoggedIn -> redirectToLogin()
    is RemoveMemberResult.Failure.Unknown  -> showError(r.message)
}

when (val r = ops.getMembers()) {
    is GetMembersResult.Success ->
        showMembers(r.members)            // List<ConversationMember(userId, name)>
    GetMembersResult.Failure.NotLoggedIn ->
        redirectToLogin()
    is GetMembersResult.Failure.Unknown ->
        showError(r.message)
}
```

`UserId` is a qualified id like `ConversationId`. Parse user-supplied strings with `UserId.fromString("value@domain")`.

### 6c. Join links

To produce or rotate a guest-room link:

```kotlin
// No password
when (val r = ops.getJoinLink()) {
    is GetJoinLinkResult.Success           -> shareLink(r.joinLink.value)
    GetJoinLinkResult.Failure.NotLoggedIn  -> redirectToLogin()
    is GetJoinLinkResult.Failure.Unknown   -> showError(r.message)
}

// Password-protected — call with the chosen password
ops.getJoinLink(password = "swordfish")

// Invalidate any existing link
when (val r = ops.revokeJoinLink()) {
    RevokeJoinLinkResult.Success             -> Unit
    RevokeJoinLinkResult.Failure.NotLoggedIn -> redirectToLogin()
    is RevokeJoinLinkResult.Failure.Unknown  -> showError(r.message)
}
```

If a guest-room link already exists on the backend, the SDK returns the cached value; to invalidate it use `revokeJoinLink()` and call `getJoinLink(...)` again.

### 6d. Delete

```kotlin
when (val r = ops.delete()) {
    DeleteConversationResult.Success             -> Unit
    DeleteConversationResult.Failure.NotLoggedIn -> redirectToLogin()
    is DeleteConversationResult.Failure.Unknown  -> showError(r.message)
}
```

`delete()` calls Kalium's `deleteTeamConversation` use case; the visibility and persistence consequences for the current user, the conversation, and other members are determined by Kalium and the backend.

## 7. Putting it together

A minimal end-to-end pattern for an inbox screen:

```kotlin
class InboxViewModel(private val session: Session) : ViewModel() {
    private val _items = MutableStateFlow<List<ConversationItem>>(emptyList())
    val items: StateFlow<List<ConversationItem>> = _items

    private val cancellable = session.conversationManager.observeConversations { ops ->
        viewModelScope.launch {
            _items.value = ops.map { c ->
                val info = (c.getConversationInfo() as? GetConversationInfoResult.Success)
                    ?.conversationInfo
                ConversationItem(
                    operations = c,
                    title      = info?.title ?: "(unknown)",
                )
            }
        }
    }

    fun create(title: String) = viewModelScope.launch {
        when (session.conversationManager.createConversation(title)) {
            is CreateConversationResult.Success -> Unit  // observer picks it up
            is CreateConversationResult.Failure -> emitError("Could not create conversation.")
        }
    }

    override fun onCleared() {
        cancellable.cancel()
        super.onCleared()
    }
}

data class ConversationItem(
    val operations: ConversationOperations,
    val title: String,
)
```

Pass `ConversationItem.operations` down into the chat screen to start sending messages — see [messaging.md](./messaging.md).
