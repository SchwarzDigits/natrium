# API Changes

Changes to Natrium's public API per version, newest first. Each entry says what changed and links
to the full documentation. ⚠️ marks changes that break existing callers or the wire format.

## v2 (unreleased)

Developed on branch `v2`; this section describes the difference to `main` and is updated in place
until the release.

### Structured requests & responses

- New message values: `MessageValue.RequestValue(request, attachmentMessageIds)`,
  `ResponseValue(requestMessageId, response)` and `UnsupportedValue(protocolVersion, type)`
  (newer or unknown content → "please update the app").
- `Request` types: `SingleSelect`, `MultiSelect` (`options`), `Appointment` (`slots`, `location`),
  `Asset` (`document`, `mimeTypes`), `Acknowledge` (`confirmation`), `FreeText` (`maxLength`).
  Every request has `message` (the question), `text` (accompanying message), `subject` and
  `deadline`.
- `Response` types matching the requests, plus `Response.FreeText`, which answers every request
  and counts as an answer. A free-text request takes only `FreeText`.
- `ChatMessage.answeredByMessageId` — the request's first valid response.
- Sending: `ChatOperations.sendRequest(request, attachments)`,
  `sendResponse(requestMessageId, response, attachments)` and `sendText(text, attachments)`;
  attachments are uploaded as file messages first and referenced by id.
- `SendMessageResult.Failure.InvalidResponse(reason)` with `InvalidResponseReason`.
- `TextValue.attachmentMessageIds`.
- ⚠️ Every text message Natrium sends is a JSON envelope (`{"natrium":1,"type":"TEXT",…}`), free
  text included. Plain text from other clients still arrives as `TextValue`; other Wire clients
  show the raw JSON.

→ [Requests & Responses](./requests.md) · [Public API](./requests.md#2-public-api) ·
[Wire format](./requests.md#3-wire-format)

### Conversation metadata

- `ConversationInfo.metadata: Map<String, String>` — app-defined key/value pairs stored with the
  conversation; empty for conversations created without.
- `ConversationManager.createConversation(title, metadata = emptyMap())`.
- `ConversationOperations.setMetadata(metadata)` keeps the title; `setTitle(title)` keeps the
  metadata.
- Wire format: with metadata the conversation name is `{"natrium":1,"title":…,"meta":{…}}`;
  without, it stays the plain title. Title and metadata together must fit 256 characters. Other
  Wire clients show the raw envelope as the name.

→ [Conversations § 6a Metadata](./conversations.md#6a-metadata)
