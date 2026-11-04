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

    data class RequestValue(
        val request: Request,
        val attachmentMessageIds: List<String> = emptyList(),
    ) : MessageValue()                                                                    // NEW
    data class ResponseValue(val requestMessageId: String, val response: Response) : MessageValue()  // NEW
    data class UnsupportedValue(val protocolVersion: Int?, val type: String?) : MessageValue()       // NEW
}
```

### 2.2 Requests

Every request has a `message` (the question to the other party), an optional `subject` (short title),
an optional `text` (an accompanying message shown above the question, e.g. the letter the question
belongs to) and an optional `deadline`. Files sent along with a request live on `RequestValue.attachmentMessageIds`.
Several questions are sent as several requests, one after the other.

```kotlin
sealed class Request {
    abstract val subject: String
    abstract val message: String
    abstract val deadline: Instant?
    abstract val text: String

    data class SingleSelect(…, val options: List<Option>) : Request()
    data class MultiSelect(…, val options: List<Option>, val minSelections: Int? = null, val maxSelections: Int? = null) : Request()
    data class Asset(…, val document: String = "", val mimeTypes: List<String> = emptyList()) : Request()
    data class Appointment(…, val slots: List<AppointmentSlot>, val location: String = "") : Request()
    data class Acknowledge(…, val confirmation: String) : Request()     // confirm a fixed sentence
    data class FreeText(…, val maxLength: Int? = null) : Request()      // answer is free text only
}

data class Option(val id: String, val title: String)
data class AppointmentSlot(val id: String, val from: Instant, val to: Instant)
```

Options and slots carry a stable `id`; responses reference ids, not titles, so a title can be reworded
or localized without breaking the answer.

- `Asset.document` names what is expected ("signed site plan"); `mimeTypes` restricts the file types
  (e.g. `application/pdf`), empty means any. Natrium does not enforce `mimeTypes`; the answering UI does.
- `MultiSelect.minSelections` / `maxSelections` optionally bound how many options the other party picks;
  without them it is at least one and at most all.
- `Appointment` offers one or more slots; the other party picks exactly one.
- `Acknowledge.confirmation` is the sentence the other party confirms ("I have taken note of the notice.").

### 2.3 Responses

```kotlin
sealed class Response {
    data class SingleSelect(val selectedOptionId: String) : Response()
    data class MultiSelect(val selectedOptionIds: List<String>) : Response()
    data class Asset(val attachmentMessageIds: List<String>) : Response()
    data class Appointment(val selectedSlotId: String) : Response()
    data object Acknowledge : Response()
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
request. Responses whose status is `FAILED`/`FAILED_REMOTELY` do not count, so a failed answer can be
retried. Computed from the messages Natrium has loaded; a response always comes after its request,
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

    // NEW — request with attachments
    suspend fun sendRequest(request: Request, attachments: List<FileLink> = emptyList()): SendMessageResult

    // NEW — free text with attachments, without a request reference
    suspend fun sendText(text: String, attachments: List<FileLink> = emptyList()): SendMessageResult
}
```

- `sendMessage(RequestValue(...))` → JSON request (ids in `attachmentMessageIds` are sent as they are).
- `sendRequest(request, attachments)` → uploads `attachments` first, then sends the request referencing them.
- `sendMessage(TextValue(text))` → JSON free text (no attachments; use `sendText` for those).
- `sendMessage(ResponseValue(...))` → same as `sendResponse` without attachments.
- `sendResponse(...)` / `sendText(...)` / `sendRequest(...)` with `attachments`: each `FileLink` is uploaded as a file
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
    UNKNOWN_OPTION,           // selected option/slot id not in the request
    EMPTY_SELECTION,          // MultiSelect with no ids
    SELECTION_OUT_OF_RANGE,   // MultiSelect with fewer than minSelections or more than maxSelections ids
    MISSING_ATTACHMENTS,      // Asset response without attachments
    TEXT_TOO_LONG,            // FreeText longer than FreeText request's maxLength
}
```

Validation rules:

| Request | Accepted responses |
|---|---|
| `SingleSelect` | `SingleSelect` (id ∈ options), `FreeText` |
| `MultiSelect` | `MultiSelect` (≥ 1 id, all ∈ options, no duplicates, count within `minSelections`–`maxSelections` if set), `FreeText` |
| `Asset` | `Asset` (≥ 1 attachment), `FreeText` |
| `Appointment` | `Appointment` (slot id ∈ slots), `FreeText` |
| `Acknowledge` | `Acknowledge`, `FreeText` |
| `FreeText` | `FreeText` (≤ `maxLength` if set) |

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
| `MULTI_SELECT` | `options`, `min_selections` / `max_selections` (optional) |
| `ASSET` | `document` (optional), `mime_types` (optional list) |
| `APPOINTMENT` | `slots` (`[{id, from, to}]`, ISO-8601 instants), `location` (optional) |
| `ACKNOWLEDGE` | `confirmation` |
| `FREE_TEXT` | `max_length` (optional) |

Common fields: `subject` (default `""`), `message`, `deadline` (optional ISO-8601 instant),
`text` (default `""`; encoded last, after the type-specific fields and `deadline`).
Files sent with the request are referenced in the envelope's top-level `attachments` (file message ids),
as for free text:

```json
{"natrium":1,"type":"REQUEST","request":{"type":"ACKNOWLEDGE","message":"Bitte bestätigen Sie den Erhalt.",
  "confirmation":"Ich habe den Bescheid zur Kenntnis genommen."},"attachments":["4f1c…"]}
```

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
| `APPOINTMENT` | `selected` (slot id) |
| `ACKNOWLEDGE` | — |
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
of the raw JSON: request → `subject` (or `message` if empty), free text and free-text response → `text`,
other responses and unsupported messages → `null`.

## 4. Open points

- **Max text length:** the maximum Wire text message size still has to be verified in Kalium. Long
  requests (many options, long `message`) may need a limit check → `Failure.Unknown`/new branch.
- **Editing / deleting** a request or response is not covered. A deleted request makes its
  responses reference an unknown id (receiver: response is still shown, request lookup fails).
- **Payment** is intentionally not part of Natrium (needs a payment provider outside messaging).
- **Answered check on send:** `sendResponse` looks for existing responses in the 500 most recent
  messages of the conversation. A response older than that window is not detected.
- **History windows:** `getHistory` pages may contain a request without its response (response is
  newer and therefore in the live window). Apps merging pages should take `answeredByMessageId`
  from the most recent snapshot.
