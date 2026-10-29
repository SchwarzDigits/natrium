# Requests & Responses

**Status:** Draft (v2 branch)

Structured request/response messages on top of regular text messages. An authority sends a
**request** (e.g. "choose one of these options", "upload a document"); the citizen answers with a
matching **response** or with a **free-text** reply. Both sides use Natrium, so Natrium owns the
wire format: it serializes outgoing values to JSON, parses incoming JSON, validates responses and
derives whether a request has been answered. Apps only see typed `MessageValue`s.

> Prerequisite: [messaging.md](./messaging.md).

## 1. Design decisions

| Topic | Decision |
|---|---|
| Transport | Plain Wire text message (`MessageContent.Text`) containing a JSON envelope. No Kalium changes. |
| Format | Compact JSON (no pretty print, defaults omitted). Binary formats (CBOR/Protobuf) would need Base64 inside a text message and save next to nothing for payloads of a few hundred bytes. |
| Clients | Clients talk "sortenrein": every participant runs Natrium. No human-readable fallback text for plain Wire clients. |
| All text is JSON | Free-text messages are wrapped in the envelope too. Non-JSON text (legacy, foreign client) is still delivered as `TextValue`. |
| Linking | A response references its request in the JSON (`request_id`, authoritative) **and** is sent as a Wire reply (`quotedMessageId`). |
| Free text | Allowed as an answer to **every** request type. It **counts as an answer**: the request is answered; the authority sends a new request if needed. |
| Attachments | Files are sent as regular file messages first; the JSON references their message ids. Natrium does the upload-then-reference sequence internally. |
| Answered state | Natrium derives it and exposes `ChatMessage.answeredByMessageId`. |
| Role checks | None in Natrium. Requests sent by the wrong side are filtered by the authority client. |
| Unknown content | Newer protocol version or unknown type → `MessageValue.UnsupportedValue` (UI: "please update the app"). |

## 2. Public API

### 2.1 Message values

```kotlin
sealed class MessageValue {
    data class TextValue(
        val value: String,
        val attachmentMessageIds: List<String> = emptyList(),   // NEW
    ) : MessageValue()
    // FileValue, LocationValue, KnockValue, SystemValue unchanged

    data class RequestValue(val request: Request) : MessageValue()                       // NEW
    data class ResponseValue(val requestMessageId: String, val response: Response) : MessageValue()  // NEW
    data class UnsupportedValue(val protocolVersion: Int?, val type: String?) : MessageValue()       // NEW
}
```

### 2.2 Requests

Every request has a `subject`, a `message` and an optional `deadline`.

```kotlin
sealed class Request {
    abstract val subject: String
    abstract val message: String
    abstract val deadline: Instant?

    data class SingleSelect(…, val options: List<Option>) : Request()
    data class MultiSelect(…, val options: List<Option>) : Request()
    data class Asset(…) : Request()                                   // "please upload documents"
    data class Appointment(…, val from: Instant, val to: Instant) : Request()
}

data class Option(val id: String, val title: String)
```

Options carry a stable `id`; responses reference ids, not titles, so a title can be reworded or
localized without breaking the answer.

### 2.3 Responses

```kotlin
sealed class Response {
    data class SingleSelect(val selectedOptionId: String) : Response()
    data class MultiSelect(val selectedOptionIds: List<String>) : Response()
    data class Asset(val attachmentMessageIds: List<String>) : Response()
    data class Appointment(val accepted: Boolean) : Response()
    data class FreeText(val text: String, val attachmentMessageIds: List<String> = emptyList()) : Response()
}
```

### 2.4 `ChatMessage`

```kotlin
data class ChatMessage(
    …,
    val answeredByMessageId: String? = null,   // NEW — only set on RequestValue messages
)
```

`answeredByMessageId` is the id of the **first** response (typed or free text) that references the
request. Computed from the messages Natrium has loaded; a response always comes after its request,
so it is part of the latest `observeMessages` window.

### 2.5 Sending

```kotlin
interface ChatOperations {
    // existing; now also accepts RequestValue and TextValue with attachments (see below)
    suspend fun sendMessage(value: MessageValue): SendMessageResult

    // NEW
    suspend fun sendResponse(
        requestMessageId: String,
        response: Response,
        attachments: List<FileLink> = emptyList(),
    ): SendMessageResult

    // NEW — free text with attachments, without a request reference
    suspend fun sendText(text: String, attachments: List<FileLink> = emptyList()): SendMessageResult
}
```

- `sendMessage(RequestValue(...))` → JSON request.
- `sendMessage(TextValue(text))` → JSON free text (no attachments; use `sendText` for those).
- `sendMessage(ResponseValue(...))` → same as `sendResponse` without attachments.
- `sendResponse(...)` / `sendText(...)` with `attachments`: each `FileLink` is uploaded as a file
  message, the returned message ids are written into `attachmentMessageIds`, then the JSON message
  is sent. Ids already present in the passed `Response` are kept and the uploaded ones appended.
  If an upload fails, the JSON message is **not** sent and the upload failure is returned.

New failure branch:

```kotlin
sealed class SendMessageResult {
    sealed class Failure : SendMessageResult() {
        …
        data class InvalidResponse(val reason: InvalidResponseReason) : Failure()   // NEW
    }
}

enum class InvalidResponseReason {
    REQUEST_NOT_FOUND,        // requestMessageId is not a known message in this conversation
    NOT_A_REQUEST,            // referenced message is not a RequestValue
    ALREADY_ANSWERED,         // request already has answeredByMessageId
    TYPE_MISMATCH,            // e.g. MultiSelect response to a SingleSelect request
    UNKNOWN_OPTION,           // selected id not in the request's options
    EMPTY_SELECTION,          // MultiSelect with no ids
    MISSING_ATTACHMENTS,      // Asset response without attachments
}
```

Validation rules:

| Request | Accepted responses |
|---|---|
| `SingleSelect` | `SingleSelect` (id ∈ options), `FreeText` |
| `MultiSelect` | `MultiSelect` (≥ 1 id, all ∈ options, no duplicates), `FreeText` |
| `Asset` | `Asset` (≥ 1 attachment), `FreeText` |
| `Appointment` | `Appointment`, `FreeText` |

## 3. Wire format

Every Natrium text message is a single JSON object. Field `natrium` is the protocol version and the
marker that the text is a Natrium envelope. Keys are `snake_case`; fields with default values are
omitted.

### 3.1 Free text

```json
{"natrium":1,"type":"TEXT","text":"Guten Tag, ich habe eine Frage …"}
```

With attachments:

```json
{"natrium":1,"type":"TEXT","text":"Anbei die Unterlagen","attachments":["4f1c…","9a2e…"]}
```

### 3.2 Request

```json
{"natrium":1,"type":"REQUEST","request":{
  "type":"SINGLE_SELECT",
  "subject":"Wohnsitz",
  "message":"Ist das Ihr Hauptwohnsitz?",
  "options":[{"id":"yes","title":"Ja"},{"id":"no","title":"Nein"}],
  "deadline":"2026-11-15T23:59:59Z"
}}
```

| `request.type` | Additional fields |
|---|---|
| `SINGLE_SELECT` | `options` |
| `MULTI_SELECT` | `options` |
| `ASSET` | — |
| `APPOINTMENT` | `from`, `to` (ISO-8601 instants) |

Common fields: `subject` (default `""`), `message`, `deadline` (optional ISO-8601 instant).

### 3.3 Response

```json
{"natrium":1,"type":"RESPONSE","request_id":"<request message id>","response":{
  "type":"SINGLE_SELECT","selected":"yes"
}}
```

| `response.type` | Fields |
|---|---|
| `SINGLE_SELECT` | `selected` (option id) |
| `MULTI_SELECT` | `selected` (list of option ids) |
| `ASSET` | `attachments` (file message ids) |
| `APPOINTMENT` | `accepted` (boolean) |
| `FREE_TEXT` | `text`, `attachments` (optional) |

The response message is additionally sent as a Wire reply quoting the request
(`quotedMessageId = request_id`).

### 3.4 Parsing rules (receiver)

1. Text is not a JSON object, or has no numeric `natrium` field → `TextValue(text)`.
2. `natrium` > supported version → `UnsupportedValue(version, type)`.
3. Unknown top-level `type` or unknown `request.type` / `response.type` → `UnsupportedValue`.
4. Malformed known type (missing required field, wrong type) → `UnsupportedValue`.
5. Unknown **additional** fields are ignored (forward compatible within a version).

### 3.5 Quote previews

`QuotedMessage.previewText` for a quoted Natrium message is derived from the parsed value instead
of the raw JSON: request → `subject` (or `message` if empty), free text → `text`, response → a short
summary (e.g. selected option title), unsupported → `null`.

## 4. Open points

- **Max text length:** the maximum Wire text message size still has to be verified in Kalium. Long
  requests (many options, long `message`) may need a limit check → `Failure.Unknown`/new branch.
- **Editing / deleting** a request or response is not covered. A deleted request makes its
  responses reference an unknown id (receiver: response is still shown, request lookup fails).
- **Payment** is intentionally not part of Natrium (needs a payment provider outside messaging).
- **History windows:** `getHistory` pages may contain a request without its response (response is
  newer and therefore in the live window). Apps merging pages should take `answeredByMessageId`
  from the most recent snapshot.
