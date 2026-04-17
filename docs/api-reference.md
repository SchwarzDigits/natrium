# Natrium API Reference

All public types are in the `schwarz.digits.natrium` package namespace.

---

## Natrium (Entry Point)

`object schwarz.digits.natrium.Natrium`

| Method | Parameters | Returns |
|--------|-----------|---------|
| `fun initialize(backendConfig, platform)` | `backendConfig: BackendConfig`, `platform: NatriumPlatform` | `Unit` |
| `suspend fun login(email, password, secondFactorVerificationCode?)` | `email: String`, `password: String`, `secondFactorVerificationCode: String? = null` | `LoginResult` |
| `suspend fun ssoLogin(email)` | `email: String` | `SSOLoginResult` |
| `suspend fun ssoLoginWithCode(ssoCode)` | `ssoCode: String` | `SSOLoginResult` |
| `suspend fun completeSSOLogin(cookie)` | `cookie: String` | `LoginResult` |
| `suspend fun restoreLastSession()` | — | `Session?` |
| `fun observeAuthEvents(listener)` | `listener: (AuthEvent) -> Unit` | `Cancellable` |

---

## BackendConfig

`class schwarz.digits.natrium.BackendConfig`

| Parameter | Type | Default |
|-----------|------|---------|
| `name` | `String` | — |
| `api` | `String` | — |
| `accounts` | `String` | — |
| `webSocket` | `String` | — |
| `teams` | `String` | — |
| `blackList` | `String` | — |
| `website` | `String` | — |
| `isOnPremises` | `Boolean` | `true` |

SDK consumers construct a `BackendConfig` with their server URLs and pass it to `Natrium.initialize()`.

---

## AuthEvent

`sealed class schwarz.digits.natrium.AuthEvent`

| Subclass | Fields |
|----------|--------|
| `LoggedIn` | `val session: Session` |
| `LoggedOut` | — |

Observe via `Natrium.observeAuthEvents(listener)`. Emitted on login and logout events.

---

## Session

`interface schwarz.digits.natrium.session.Session`

| Property / Method | Type / Returns |
|-------------------|---------------|
| `val conversationManager` | `ConversationManager` |
| `val deviceManager` | `DeviceManager` |
| `suspend fun sessionInfo()` | `SessionInfo?` |
| `fun observeSessionInfo(listener)` | `listener: (SessionInfo) -> Unit` → `Cancellable` |
| `suspend fun updateDisplayName(name: String)` | `UpdateDisplayNameResult` |
| `suspend fun updateHandle(handle: String)` | `UpdateHandleResult` |
| `suspend fun updateEmail(email: String)` | `UpdateEmailResult` |
| `suspend fun logout()` | `LogoutResult` |

---

## ConversationManager

`interface schwarz.digits.natrium.conversation.ConversationManager`

| Method | Parameters | Returns |
|--------|-----------|---------|
| `fun observeConversations(listener)` | `listener: (Collection<ConversationOperations>) -> Unit` | `Cancellable` |
| `fun observeArchivedConversations(listener)` | `listener: (Collection<ConversationOperations>) -> Unit` | `Cancellable` |
| `suspend fun listConversations()` | — | `ConversationListResult` |
| `suspend fun listArchivedConversations()` | — | `ConversationListResult` |
| `suspend fun findConversation(id)` | `id: ConversationId` | `FindConversationResult` |
| `suspend fun createConversation(title)` | `title: String` | `CreateConversationResult` |
| `suspend fun joinConversation(joinLink, password?)` | `joinLink: JoinLink`, `password: String? = null` | `JoinConversationResult` |

---

## ConversationOperations

`interface schwarz.digits.natrium.conversation.ConversationOperations`

| Method | Parameters | Returns |
|--------|-----------|---------|
| `fun chat()` | — | `ChatOperations` |
| `suspend fun addMember(userId)` | `userId: UserId` | `AddMemberResult` |
| `suspend fun removeMember(userId)` | `userId: UserId` | `RemoveMemberResult` |
| `suspend fun getMembers()` | — | `GetMembersResult` |
| `suspend fun getJoinLink(password?)` | `password: String? = null` | `GetJoinLinkResult` |
| `suspend fun revokeJoinLink()` | — | `RevokeJoinLinkResult` |
| `suspend fun getConversationInfo()` | — | `GetConversationInfoResult` |
| `suspend fun setTitle(title)` | `title: String` | `SetTitleResult` |
| `suspend fun archive()` | — | `ArchiveConversationResult` |
| `suspend fun unarchive()` | — | `UnarchiveConversationResult` |
| `suspend fun delete()` | — | `DeleteConversationResult` |

---

## ChatOperations

`interface schwarz.digits.natrium.chat.ChatOperations`

| Method | Parameters | Returns |
|--------|-----------|---------|
| `suspend fun sendMessage(value)` | `value: MessageValue` | `SendMessageResult` |
| `suspend fun sendReply(value, quotedMessageId)` | `value: MessageValue`, `quotedMessageId: String` | `SendMessageResult` |
| `suspend fun downloadFile(messageId)` | `messageId: String` | `FileDownloadResult` |
| `suspend fun toggleReaction(messageId, emoji)` | `messageId: String`, `emoji: String` | `ToggleReactionResult` |
| `fun observeMessages(listener)` | `listener: (Collection<ChatMessage>) -> Unit` | `Cancellable` |
| `suspend fun getHistory(limit, offset)` | `limit: Int`, `offset: Int` | `ChatHistoryResult` |
| `suspend fun sendTypingStarted()` | — | `Unit` |
| `suspend fun sendTypingStopped()` | — | `Unit` |
| `fun observeTyping(listener)` | `listener: (Collection<NamedUser>) -> Unit` | `Cancellable` |

---

## DeviceManager

`interface schwarz.digits.natrium.devices.DeviceManager`

| Method | Parameters | Returns |
|--------|-----------|---------|
| `suspend fun list()` | — | `ListDevicesResult` |
| `suspend fun remove(deviceId, password?)` | `deviceId: String`, `password: String? = null` | `RemoveDeviceResult` |

---

## Data Types

### BackendConfig

See [BackendConfig](#backendconfig) section above.

### SessionInfo
`data class schwarz.digits.natrium.session.SessionInfo`

| Field | Type |
|-------|------|
| `user` | `NamedUser` |
| `handle` | `String?` |
| `email` | `String?` |
| `backend` | `String` |
| `device` | `DeviceInfo?` |

### ConversationInfo
`data class schwarz.digits.natrium.conversation.ConversationInfo`

| Field | Type |
|-------|------|
| `id` | `ConversationId` |
| `title` | `String` |
| `isArchived` | `Boolean` |

### ConversationId
`data class schwarz.digits.natrium.conversation.ConversationId`

| Field | Type |
|-------|------|
| `value` | `String` |
| `domain` | `String` |

| Method | Returns |
|--------|---------|
| `fun toString()` | `String` (`value@domain`) |
| `companion fun fromString(s)` | `ConversationId` |

### ConversationMember
`data class schwarz.digits.natrium.conversation.ConversationMember`

| Field | Type |
|-------|------|
| `userId` | `UserId` |
| `name` | `String` |

### JoinLink
`@JvmInline value class schwarz.digits.natrium.conversation.JoinLink`

| Field | Type |
|-------|------|
| `value` | `String` |

### JoinRequest
`data class schwarz.digits.natrium.conversation.JoinRequest`

| Field | Type |
|-------|------|
| `id` | `JoinRequestId` |
| `conversationId` | `ConversationId` |
| `requester` | `NamedUser` |
| `status` | `JoinRequestStatus` |
| `message` | `String?` |
| `createdAt` | `Instant` |

### JoinRequestId
`@JvmInline value class schwarz.digits.natrium.conversation.JoinRequestId`

| Field | Type |
|-------|------|
| `value` | `String` |

### JoinRequestStatus
`enum schwarz.digits.natrium.conversation.JoinRequestStatus`

Values: `PENDING`, `APPROVED`, `REJECTED`

### ChatMessage
`data class schwarz.digits.natrium.chat.ChatMessage`

| Field | Type |
|-------|------|
| `id` | `String` |
| `sender` | `NamedUser` |
| `value` | `MessageValue` |
| `timestamp` | `Instant` |
| `status` | `MessageStatus` |
| `isSelf` | `Boolean` |
| `fileTransferStatus` | `FileTransferStatus?` |
| `isEdited` | `Boolean` |
| `systemText` | `String?` |
| `reactions` | `Map<String, ReactionInfo>` |
| `quotedMessage` | `QuotedMessage?` |

### MessageValue
`sealed class schwarz.digits.natrium.chat.MessageValue`

| Subclass | Fields |
|----------|--------|
| `TextValue` | `value: String` |
| `FileValue` | `fileLink: FileLink` |
| `LocationValue` | `latitude: Double`, `longitude: Double`, `name: String?` |
| `KnockValue` | `hotKnock: Boolean` |
| `SystemValue` | `event: SystemEvent` |

### SystemEvent
`enum schwarz.digits.natrium.chat.SystemEvent`

Values: `MEMBER_ADDED`, `MEMBER_REMOVED`, `CONVERSATION_RENAMED`, `MISSED_CALL`, `OTHER`

### MessageStatus
`enum schwarz.digits.natrium.chat.MessageStatus`

Values: `PENDING`, `SENT`, `DELIVERED`, `READ`, `FAILED`, `FAILED_REMOTELY`

### FileTransferStatus
`enum schwarz.digits.natrium.chat.FileTransferStatus`

Values: `UPLOADING`, `UPLOAD_FAILED`, `UPLOADED`, `NOT_DOWNLOADED`, `DOWNLOADING`, `DOWNLOAD_FAILED`, `DOWNLOADED`

### ReactionInfo
`data class schwarz.digits.natrium.chat.ReactionInfo`

| Field | Type |
|-------|------|
| `count` | `Int` |
| `isSelf` | `Boolean` |

### QuotedMessage
`data class schwarz.digits.natrium.chat.QuotedMessage`

| Field | Type |
|-------|------|
| `messageId` | `String` |
| `senderName` | `String?` |
| `previewText` | `String?` |

### FileLink
`interface schwarz.digits.natrium.file.FileLink`

| Property | Type |
|----------|------|
| `id` | `String` |
| `mimeType` | `String` |
| `dataPath` | `Path` |
| `dataSize` | `Long` |
| `fileName` | `String` |
| `uploadedAt` | `Instant` |

| Method | Parameters | Returns |
|--------|-----------|---------|
| `companion fun fromLocal(dataPath, fileName, mimeType, dataSize)` | `dataPath: Path`, `fileName: String`, `mimeType: String`, `dataSize: Long` | `FileLink` |

### DeviceLimitResolver
`interface schwarz.digits.natrium.devices.DeviceLimitResolver`

| Method | Parameters | Returns |
|--------|-----------|---------|
| `suspend fun listDevices()` | — | `ListDevicesResult` |
| `suspend fun removeDevice(deviceId, password?)` | `deviceId: String`, `password: String? = null` | `RemoveDeviceResult` |
| `suspend fun retry()` | — | `LoginResult` |

### DeviceInfo
`data class schwarz.digits.natrium.devices.DeviceInfo`

| Field | Type |
|-------|------|
| `id` | `String` |
| `label` | `String?` |
| `model` | `String?` |
| `deviceType` | `String?` |
| `isCurrentDevice` | `Boolean` |

### UserId
`data class schwarz.digits.natrium.users.UserId`

| Field | Type |
|-------|------|
| `value` | `String` |
| `domain` | `String` |

| Method | Returns |
|--------|---------|
| `fun toString()` | `String` (`value@domain`) |
| `companion fun fromString(s)` | `UserId` |

### NamedUser
`data class schwarz.digits.natrium.users.NamedUser`

| Field | Type |
|-------|------|
| `userId` | `UserId` |
| `name` | `String?` |

### Cancellable
`class schwarz.digits.natrium.Cancellable`

| Method | Returns |
|--------|---------|
| `fun cancel()` | `Unit` |

### LogLevel
`enum schwarz.digits.natrium.LogLevel`

Values: `VERBOSE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `DISABLED`

### initLogging
`fun schwarz.digits.natrium.initLogging(level: LogLevel)`

Initializes logging for Natrium and Kalium. Call at app startup before `Natrium.initialize()`. Default level: `WARN`.

---

## Result Types

All result types are sealed classes following the pattern `Success` / `Failure.*`.

| Result Type | Success | Failure Variants |
|-------------|---------|-----------------|
| `LoginResult` | `Success(session: Session)` | `Failure.Error(reason: LoginError)`, `Failure.TooManyDevices(resolver: DeviceLimitResolver)` |
| `SSOLoginResult` | `Success(authorizationUrl: String)` | `Failure.Error(reason: SSOLoginError)` |
| `LogoutResult` | `Success` | `Failure.Unknown(message, cause?)` |
| `UpdateDisplayNameResult` | `Success` | `Failure.Unknown(message, cause?)` |
| `UpdateHandleResult` | `Success` | `Failure.InvalidHandle`, `Failure.HandleExists`, `Failure.Unknown(message, cause?)` |
| `UpdateEmailResult` | `Success` | `Failure.InvalidEmail`, `Failure.EmailAlreadyInUse`, `Failure.Unknown(message, cause?)` |
| `ConversationListResult` | `Success(conversations: List<ConversationOperations>)` | `Failure.NotLoggedIn`, `Failure.SyncFailed(message)`, `Failure.Unknown(message, cause?)` |
| `CreateConversationResult` | `Success(conversationOperation: ConversationOperations)` | `Failure.NotLoggedIn`, `Failure.Forbidden`, `Failure.InvalidTitle(message)`, `Failure.Unknown(message, cause?)` |
| `FindConversationResult` | `Success(conversationOperations: ConversationOperations)` | `Failure.NotLoggedIn`, `Failure.NotFound`, `Failure.Unknown(message, cause?)` |
| `JoinConversationResult` | `Success(conversationOperations: ConversationOperations)` | `Failure.NotLoggedIn`, `Failure.IncorrectPassword`, `Failure.InvalidLink`, `Failure.Unknown(message, cause?)` |
| `CloseConversationResult` | `Success` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `ConversationObserveResult` | `Success` | `Failure.NotLoggedIn`, `Failure.ConversationNotFound` |
| `GetConversationInfoResult` | `Success(conversationInfo: ConversationInfo)` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `SetTitleResult` | `Success` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `ArchiveConversationResult` | `Success` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `UnarchiveConversationResult` | `Success` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `DeleteConversationResult` | `Success` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `AddMemberResult` | `Success` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `RemoveMemberResult` | `Success` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `GetMembersResult` | `Success(members: List<ConversationMember>)` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `GetJoinLinkResult` | `Success(joinLink: JoinLink)` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `RevokeJoinLinkResult` | `Success` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `SendMessageResult` | `Success` | `Failure.NotLoggedIn`, `Failure.DisabledByTeam`, `Failure.RestrictedFileType`, `Failure.FileTooLarge(bytes, limit)`, `Failure.Unknown(message, cause?)` |
| `ChatHistoryResult` | `Success(messages: List<ChatMessage>)` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `FileDownloadResult` | `Success(filePath: Path)` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `ToggleReactionResult` | `Success` | `Failure.Unknown` |
| `RequestToJoinResult` | `Success(joinRequest: JoinRequest)` | `Failure.NotLoggedIn`, `Failure.ConversationNotFound`, `Failure.AlreadyRequested`, `Failure.AlreadyMember`, `Failure.Unknown(message, cause?)` |
| `ListMyJoinRequestsResult` | `Success(requests: List<JoinRequest>)` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `CancelJoinRequestResult` | `Success` | `Failure.NotLoggedIn`, `Failure.NotFound`, `Failure.Unknown(message, cause?)` |
| `ListJoinRequestsResult` | `Success(requests: List<JoinRequest>)` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `ApproveJoinRequestResult` | `Success` | `Failure.NotLoggedIn`, `Failure.NotFound`, `Failure.AlreadyHandled`, `Failure.Unknown(message, cause?)` |
| `RejectJoinRequestResult` | `Success` | `Failure.NotLoggedIn`, `Failure.NotFound`, `Failure.AlreadyHandled`, `Failure.Unknown(message, cause?)` |
| `ListDevicesResult` | `Success(devices: List<DeviceInfo>)` | `Failure.NotLoggedIn`, `Failure.Unknown(message, cause?)` |
| `RemoveDeviceResult` | `Success` | `Failure.NotLoggedIn`, `Failure.PasswordRequired`, `Failure.InvalidCredentials(message)`, `Failure.Unknown(message, cause?)` |

### LoginError
`enum schwarz.digits.natrium.session.LoginError`

Values: `SERVER_VERSION_NOT_SUPPORTED`, `APP_UPDATE_REQUIRED`, `CONNECTION_ERROR`, `EMAIL_OR_PASSWORD_WRONG`, `SECOND_FA_CODE_REQUIRED`, `INVALID_2FA_CODE`, `ACCOUNT_LOCKED`, `ACCOUNT_NOT_ACTIVATED`, `LOGIN_FAILED`, `SESSION_COULD_NOT_BE_SAVED`, `CLIENT_REGISTRATION_FAILED`

### SSOLoginError
`enum schwarz.digits.natrium.session.SSOLoginError`

Values: `SSO_NOT_AVAILABLE`, `INVALID_CODE`, `INVALID_CODE_FORMAT`, `SERVER_VERSION_NOT_SUPPORTED`, `APP_UPDATE_REQUIRED`, `CONNECTION_ERROR`

---

## Platform

`expect class schwarz.digits.natrium.NatriumPlatform`

Platform-specific initialization. Construct before calling `Natrium.initialize()`.

| Platform | Constructor |
|----------|------------|
| Android | `NatriumPlatform(context: Context)` |
| iOS | `NatriumPlatform()` |
| JVM | `NatriumPlatform()` |
