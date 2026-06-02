# Messaging

`ChatOperations` is the per-conversation messaging interface — sending text and files, observing incoming messages, reactions, typing indicators, history pagination, and file downloads.

> Prerequisite: you have a `ConversationOperations` (from [conversations.md](./conversations.md)).

## 1. Accessing chat

```kotlin
val chat: ChatOperations = conversationOperations.chat()
```

Calling `chat()` repeatedly on the same `ConversationOperations` returns the same `ChatOperations` instance. Most apps stash it once per chat screen.

## 2. Sending text

```kotlin
when (val r = chat.sendMessage(MessageValue.TextValue("Hello"))) {
    SendMessageResult.Success                      -> Unit
    SendMessageResult.Failure.NotLoggedIn          -> redirectToLogin()
    SendMessageResult.Failure.DisabledByTeam       -> showError("Messaging is disabled by your team.")
    SendMessageResult.Failure.RestrictedFileType   -> showError("That kind of attachment isn't allowed.")
    is SendMessageResult.Failure.FileTooLarge      ->
        showError("File too large (limit ${r.limitBytes / 1024 / 1024} MB).")
    is SendMessageResult.Failure.Unknown           -> showError(r.message)
}
```

`SendMessageResult` is shared across all `sendMessage` / `sendReply` calls — text messages won't actually hit `FileTooLarge` / `RestrictedFileType`, but the type's exhaustiveness still requires handling them (or use an `else -> Unit` arm).

## 3. Sending files

A file is uploaded by first describing it locally, then handing the `MessageValue.FileValue` to `sendMessage`:

```kotlin
import okio.Path

suspend fun sendFile(chat: ChatOperations, path: Path, fileName: String, mimeType: String, size: Long) {
    val fileLink = FileLink.fromLocal(
        dataPath = path,
        fileName = fileName,
        mimeType = mimeType,
        dataSize = size,
    )

    when (val r = chat.sendMessage(MessageValue.FileValue(fileLink))) {
        SendMessageResult.Success                    -> Unit
        SendMessageResult.Failure.NotLoggedIn        -> redirectToLogin()
        SendMessageResult.Failure.DisabledByTeam     -> showError("File sharing is disabled.")
        SendMessageResult.Failure.RestrictedFileType -> showError("This file type isn't allowed.")
        is SendMessageResult.Failure.FileTooLarge    ->
            showError("Max upload size is ${r.limitBytes / 1024 / 1024} MB.")
        is SendMessageResult.Failure.Unknown         -> showError(r.message)
    }
}
```

`FileLink.fromLocal(...)` is a factory that produces a `FileLink` describing a file on local disk. `Path` here is `okio.Path` — Okio is on natrium-core's classpath transitively, no separate dependency declaration needed.

When `sendMessage` returns `Success`, the SDK has accepted the request and the message is **locally** queued. The actual asset upload happens asynchronously and surfaces on `ChatMessage.fileTransferStatus` (`UPLOADING` → `UPLOADED`, or `UPLOAD_FAILED`); only after `UPLOADED` is the encrypted blob on the backend.

MIME type detection is your responsibility (e.g. extension lookup, `Files.probeContentType` on JVM, `UTType` on iOS).

## 4. Replies

```kotlin
chat.sendReply(MessageValue.TextValue("Got it!"), quotedMessageId = original.id)
```

`sendReply` has the same `SendMessageResult` shape as `sendMessage`. The receiver sees the original message as `ChatMessage.quotedMessage: QuotedMessage?` containing `messageId`, `senderName`, and `previewText` — render it as a quote bubble above the reply text.

## 5. Reactions

Reactions are **toggles** — calling with the same emoji a second time removes the reaction:

```kotlin
when (chat.toggleReaction(messageId = msg.id, emoji = "👍")) {
    ToggleReactionResult.Success           -> Unit
    is ToggleReactionResult.Failure.Unknown -> showError("Could not toggle reaction.")
}
```

Reactions surface on `ChatMessage.reactions: Map<String, ReactionInfo>` keyed by emoji. `ReactionInfo` carries `count` and an `isSelf` flag (`true` when the current user has reacted with that emoji).

## 6. Observing messages

```kotlin
val cancellable = chat.observeMessages { messages ->
    // messages: Collection<ChatMessage>
    // emitted on initial load and on every change (new, edited, status update, reaction)
}
```

The collection is pre-sorted by `timestamp` (ascending) before being delivered to the callback, so you can render it directly without re-sorting.

Cancel as usual when leaving the screen.

`ChatMessage` carries everything the UI needs to render one bubble:

| Field | Type | Meaning |
|---|---|---|
| `id`                  | `String`                     | Stable message id (use as list key, reply target, reaction target). |
| `sender`              | `NamedUser`                  | `userId` + display name (`name` is nullable). |
| `value`               | `MessageValue`               | Content; see §10 for variants. |
| `timestamp`           | `kotlinx.datetime.Instant`   | Server-side send time. |
| `status`              | `MessageStatus`              | Send / delivery state — see §7. |
| `isSelf`              | `Boolean`                    | Did the current user send this message? |
| `fileTransferStatus`  | `FileTransferStatus?`        | Non-null only when `value` is `FileValue` — see §7. |
| `isEdited`            | `Boolean`                    | `true` if the sender edited after sending. |
| `systemText`          | `String?`                    | Pre-rendered text for `SystemValue` messages. |
| `reactions`           | `Map<String, ReactionInfo>`  | Aggregated reactions per emoji. |
| `quotedMessage`       | `QuotedMessage?`             | Reference to the quoted message, if any. |

## 7. Message and file-transfer states

### `MessageStatus`

| Value | Meaning |
|---|---|
| `PENDING`          | Locally queued, not yet acknowledged by the backend. Show a spinner / pending indicator. |
| `SENT`             | The backend accepted the message. |
| `DELIVERED`        | At least one recipient device has received it. |
| `READ`             | At least one recipient has read it. |
| `FAILED`           | Send permanently failed locally. Offer "retry". |
| `FAILED_REMOTELY`  | A remote recipient rejected the message. |

### `FileTransferStatus`

Set on `ChatMessage.fileTransferStatus` only when `value` is `FileValue`; on text and other variants it is `null`.

| Value | Direction | Meaning |
|---|---|---|
| `UPLOADING`        | Outgoing | Bytes are being uploaded. |
| `UPLOAD_FAILED`    | Outgoing | The upload did not complete. |
| `UPLOADED`         | Outgoing | Upload completed; the backend has the encrypted blob. |
| `NOT_DOWNLOADED`   | Incoming | The message metadata is here, but the blob hasn't been downloaded. |
| `DOWNLOADING`      | Incoming | Download in progress. |
| `DOWNLOAD_FAILED`  | Incoming | Download attempt failed; offer "retry". |
| `DOWNLOADED`       | Incoming | Local file is available on disk. |

## 8. Downloading files

For incoming `FileValue` messages, the blob is **not** fetched automatically. Trigger download explicitly:

```kotlin
when (val r = chat.downloadFile(messageId = msg.id)) {
    is FileDownloadResult.Success ->
        openFile(r.filePath)                          // okio.Path on local disk
    FileDownloadResult.Failure.NotLoggedIn ->
        redirectToLogin()
    is FileDownloadResult.Failure.Unknown ->
        showError(r.message)
}
```

Internally the SDK combines two Kalium streams (the message stream and an asset-status stream) before fanning out to your `observeMessages` callback, so `ChatMessage.fileTransferStatus` updates arrive on the existing message-observer subscription — no separate observer is needed for progress UI. The exact transition order is owned by the underlying Kalium asset pipeline.

`Success.filePath` is an [`okio.Path`](https://square.github.io/okio/) on local disk. Treat it as read-only; if the user wants to keep the file persistently, copy it to a location your app controls.

## 9. History pagination

`observeMessages` delivers an in-memory window. To load older messages on scroll-up:

```kotlin
when (val r = chat.getHistory(limit = 50, offset = currentMessageCount)) {
    is ChatHistoryResult.Success ->
        prependMessages(r.messages)
    ChatHistoryResult.Failure.NotLoggedIn ->
        redirectToLogin()
    is ChatHistoryResult.Failure.Unknown ->
        showError(r.message)
}
```

Pagination semantics:
- `limit` is the maximum number of messages to return.
- `offset` is how many messages, from the most-recent end, to skip.
- The returned `List<ChatMessage>` is sorted by `timestamp` (ascending), the same way `observeMessages` snapshots are.

Loaded history messages also appear in the `observeMessages` callback (both read from the same Kalium-backed message store). If your screen merges a paginated history fetch with the reactive stream, de-duplicate by `ChatMessage.id` before rendering.

## 10. Typing indicators

Send (fire-and-forget — no result type):

```kotlin
chat.sendTypingStarted()
// ... user types ...
chat.sendTypingStopped()
```

A common pattern is to call `sendTypingStarted()` on input changes, debounce, then call `sendTypingStopped()` after a short idle / when the message is sent. The SDK delegates both calls to Kalium and swallows transport errors, so redundant or out-of-order calls won't crash; you should still avoid spamming them.

Observe (other users typing):

```kotlin
val cancellable = chat.observeTyping { users ->
    // users: Collection<NamedUser> currently typing in this conversation
    showTypingIndicator(users)
}
```

Updates are conflated by the SDK (rapid successive states collapse into the latest one), so your listener won't be called more often than it can render. Cancel when the chat screen tears down.

## 11. Message variants (`MessageValue`)

```kotlin
sealed class MessageValue {
    data class TextValue(val value: String) : MessageValue()
    data class FileValue(val fileLink: FileLink) : MessageValue()
    data class LocationValue(
        val latitude: Float,
        val longitude: Float,
        val name: String? = null,
    ) : MessageValue()
    data class KnockValue(val hotKnock: Boolean) : MessageValue()
    data class SystemValue(val event: SystemEvent) : MessageValue()
}
```

A render-side `when`:

```kotlin
@Composable
fun MessageBubble(msg: ChatMessage) {
    when (val v = msg.value) {
        is MessageValue.TextValue     -> Text(v.value)
        is MessageValue.FileValue     -> FileAttachment(msg, v.fileLink)
        is MessageValue.LocationValue -> LocationCard(v.latitude, v.longitude, v.name)
        is MessageValue.KnockValue    -> KnockBubble(hot = v.hotKnock)
        is MessageValue.SystemValue   -> SystemEventRow(msg.systemText ?: "")
    }
}
```

### System events

`SystemValue` messages have a `SystemEvent` discriminator. The SDK currently emits four of the five values; `OTHER` is reserved for future system-message types and is not produced today.

| Value | When it appears | `systemText` content |
|---|---|---|
| `MEMBER_ADDED`         | A user joined the conversation. | `"joined"` |
| `MEMBER_REMOVED`       | A user left or was removed. | `"left"` |
| `CONVERSATION_RENAMED` | The conversation title was changed. | `"renamed to \"<new title>\""` |
| `MISSED_CALL`          | A call ended without being answered. | `"Missed call"` |
| `OTHER`                | (reserved; not currently produced) | — |

`ChatMessage.systemText` carries the **short English** fragment shown above. It is intentionally minimal so consumers can compose their own UI string using the `sender.name` and the conversation context (e.g. "Alice joined the conversation"). It is **not** localized — apps that target multiple languages should map from `SystemEvent` themselves.

### Location and knock messages

`LocationValue` carries WGS-84 coordinates as `Float`s and an optional human-readable name. Render as a map preview or as a simple "📍 *name* — *lat, lon*" row.

`KnockValue` is a "ping" — a short attention-getter. `hotKnock = true` indicates a more emphatic ping (UI typically distinguishes with sound / animation).

You **cannot** currently send `LocationValue`, `KnockValue`, or `SystemValue` via `sendMessage` from the public API; they only appear on incoming messages.
