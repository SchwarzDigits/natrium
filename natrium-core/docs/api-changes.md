# API Changes

Short history of changes to Natrium's public API, newest first. Each entry says what changed and
links to the full documentation. ⚠️ marks changes that break existing callers or the wire format.

## 2026-11-02 — Conversation metadata (`5e4341a`)

- `ConversationInfo.metadata: Map<String, String>` — app-defined key/value pairs stored with the
  conversation; empty for conversations created without.
- `ConversationManager.createConversation(title, metadata = emptyMap())`.
- `ConversationOperations.setMetadata(metadata)` — replaces the metadata, keeps the title.
  `setTitle(title)` now keeps the metadata.
- Wire format: with metadata the conversation name is `{"natrium":1,"title":…,"meta":{…}}`; without,
  it stays the plain title. Title and metadata together must fit 256 characters.

→ [Conversations § 6a Metadata](./conversations.md#6a-metadata)

## 2026-11-02 — Accompanying text on requests (`8858d20`)

- `Request.text: String` (default `""`) on every request type — a message shown above the question,
  so message and question travel as one request.

→ [Requests § 2.2](./requests.md#22-requests)

## 2026-11-02 — More request types, slots, attachments (`db638d0`)

- New request types `Request.Acknowledge(confirmation)` and `Request.FreeText(maxLength)`.
- New response `Response.Acknowledge`; a free-text request is answered only with `Response.FreeText`.
- ⚠️ `Request.Appointment` offers `slots: List<AppointmentSlot>` and `location` instead of
  `from`/`to`; `Response.Appointment(selectedSlotId)` replaces `Response.Appointment(accepted)`.
  Old appointment messages read as `UnsupportedValue`.
- `Request.Asset` gains `document` and `mimeTypes`.
- Requests carry attachments: `RequestValue.attachmentMessageIds` and
  `ChatOperations.sendRequest(request, attachments)`.
- `InvalidResponseReason.TEXT_TOO_LONG`; `UNKNOWN_OPTION` also covers unknown slot ids.

→ [Requests § 2 Public API](./requests.md#2-public-api) · [§ 3 Wire format](./requests.md#3-wire-format)

## 2026-10-29 — Structured requests & responses (`8b341d8`)

- `MessageValue.RequestValue`, `ResponseValue` and `UnsupportedValue`; `Request`
  (`SingleSelect`, `MultiSelect`, `Asset`, `Appointment`) and `Response` (matching types plus
  `FreeText`, accepted for every request).
- `TextValue.attachmentMessageIds`; `ChatMessage.answeredByMessageId`.
- `ChatOperations.sendResponse(requestMessageId, response, attachments)` and
  `sendText(text, attachments)`.
- `SendMessageResult.Failure.InvalidResponse(reason)` with `InvalidResponseReason`.
- ⚠️ Every text message Natrium sends is a JSON envelope (`{"natrium":1,"type":"TEXT",…}`), free
  text included. Plain-text messages from other clients still arrive as `TextValue`; other Wire
  clients show the raw JSON.

→ [Requests & Responses](./requests.md)
