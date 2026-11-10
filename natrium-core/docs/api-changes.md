# API Changes

Changes to Natrium's public API per version, newest first. Each entry says what changed and links
to the full documentation. ⚠️ marks changes that break existing callers or the wire format.

## v2 (unreleased)

Developed on branch `v2`; this section describes the difference to `main` and is updated in place
until the release.

### Breaking changes at a glance

No public API was removed or renamed; new parameters have defaults. What breaks:

- ⚠️ **Wire format:** every text message is a JSON envelope (details below). Clients on `main` and
  plain Wire clients show raw JSON, so all participants have to move to v2 together.
- ⚠️ **Exhaustive `when`:** `MessageValue` has new subclasses (`RequestValue`, `ResponseValue`,
  `UnsupportedValue`) and `SendMessageResult.Failure` has `InvalidResponse` — a `when` without
  `else` over them no longer compiles.
- ⚠️ **Own implementations of the interfaces** (fakes, mocks): `ChatOperations` gained
  `sendRequest`, `sendResponse`, `sendText`; `ConversationOperations` gained `setMetadata`;
  `Session` gained `generateCaseLink` and `observeUpdateRecommended`;
  `ConversationManager.createConversation` gained a `metadata` parameter. Callers are not affected.
- Binary only: `TextValue`, `ConversationInfo` and `BackendConfig` gained fields, which changes their constructor,
  `copy` and `componentN`; code compiled against `main` has to be recompiled.

### Structured requests & responses

- New message values: `MessageValue.RequestValue(request, attachmentMessageIds)`,
  `ResponseValue(requestMessageId, response)` and `UnsupportedValue(protocolVersion, type)`
  (newer or unknown content → "please update the app").
- `Request` types: `SingleSelect`, `MultiSelect` (`options`, optional `minSelections`/`maxSelections`), `Appointment` (`slots`, `location`),
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
- `Session.observeUpdateRecommended(listener)` → `UpdateRecommended(supportedVersion,
  highestSeenVersion)`: fires once newer protocol content (higher version, unknown type) arrived,
  so the app can ask for an update once instead of at every `UnsupportedValue`. Compatibility rules
  for future protocol changes: [requests.md 4](./requests.md#4-versioning-strategy).
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

### Case links

- `BackendConfig.caseLinkBase: String? = null` — base URL of case links.
- `Session.generateCaseLink(ref: String): GenerateCaseLinkResult` → `CaseLink(url, ref)`; the app
  passes the link's `ref` (its own id for the case), Natrium does not generate one.
  `Failure.InvalidRef` for a blank `ref`.
- Format: `<caseLinkBase>#<token>`, token = base64url of `{"v":1,"user":…,"ref":…}`.

→ [Case Links](./case-links.md)
