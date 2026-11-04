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
| Unknown content | Newer protocol version or unknown type → `MessageValue.UnsupportedValue`, and `Session.observeUpdateRecommended` fires once so the app can ask for an update. |
| Evolution | Extend, don't change: see [4. Versioning strategy](#4-versioning-strategy). |

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

Cases 2 and 3 — content only a newer Natrium writes — also report to
`Session.observeUpdateRecommended` (`UpdateRecommended(supportedVersion, highestSeenVersion)`).
Case 4 does not: a broken message is not a reason to update.

### 3.5 Quote previews

`QuotedMessage.previewText` for a quoted Natrium message is derived from the parsed value instead
of the raw JSON: request → `subject` (or `message` if empty), free text and free-text response → `text`,
other responses and unsupported messages → `null`.

## 4. Versioning strategy

Rules 1–3 and the update signal apply today; rule 4 describes how a future reorganisation of the
format will be rolled out.

### 4.1 Situation

Natrium owns the SDKs and the wire format, not the apps built on them. The authority's web client
(natrium-web) updates with every deploy; a citizen's app updates when its user lets it. For a long
time two parties of one conversation will speak different protocol revisions, in both directions.
Every change has to keep three things working: an **older reader** must still show what it can, an
**older answerer** must still produce answers the newer side accepts, and nobody may lose a
message.

What is deliberately **not** done: no embedded fallback copies in one message (they would roughly
double a message carrying a long letter in `text`), no capability handshake, no sub-version field.

### 4.2 Rule 1 — extend, don't change

New optional fields are simply added; older readers ignore unknown fields (3.4 rule 5). No version
bump. A field never changes its meaning or its shape.

```json
{"natrium":1,"type":"REQUEST","request":{
  "type":"MULTI_SELECT",
  "message":"Welche Materialien verwenden Sie?",
  "options":[{"id":"o1","title":"Holz"},{"id":"o2","title":"Stahl"},{"id":"o3","title":"Glas"}],
  "min_selections":2
}}
```

An older reader shows the question without the bound.

**Constraints are advisory for older peers.** A new field that restricts the answer (like
`min_selections`) may be broken by an older answerer, who never saw it:

```json
{"natrium":1,"type":"RESPONSE","request_id":"req-42","response":{"type":"MULTI_SELECT","selected":["o1"]}}
```

The receiving side accepts such an answer and may flag it ("outside the bounds") — it never rejects
or drops it. `sendResponse` enforces constraints only for the sender's own answers.

### 4.3 Rule 2 — a changed meaning gets a new name

If a field has to be read differently, the new form travels under a new name **next to** the old
one for a transition period. Newer readers prefer the new field; older ones keep reading the old.

```json
{"natrium":1,"type":"REQUEST","request":{
  "type":"APPOINTMENT",
  "message":"Wann passt Ihnen der Ortstermin?",
  "slots":  [{"id":"s1","from":"2026-11-11T09:00:00Z","to":"2026-11-11T09:30:00Z"}],
  "slots_v2":[{"id":"s1","start":"2026-11-11T09:00:00Z","minutes":30,"location":"Lindenweg 4"}]
}}
```

- **Ids stay stable** across both forms (`s1` in both), so an answer `{"selected":"s1"}` written
  against either form means the same slot.
- Only the renamed field is duplicated, and only during the transition — `text` (the letter) never
  should be.
- Once no supported client reads the old field any more, it is dropped and only `slots_v2`
  remains. When that is, is decided by time and the apps' minimum store version; the protocol
  does not tell.

### 4.4 Rule 3 — new types

A new request or response type is added within the current version. Older readers get
`UnsupportedValue(1, "REQUEST")` for it and `Session.observeUpdateRecommended` fires (3.4).
Free text still answers every request, so a sender that knows the peer cannot read the new type
can choose an existing one instead.

### 4.5 Rule 4 — a full reorganisation sends two messages

Only when rules 1–3 cannot express a change. The sender sends the content twice: first in the old
version, then in the new one, which names the first:

```json
{"natrium":1,"type":"REQUEST","request":{"type":"SINGLE_SELECT","message":"…","options":[…]}}
```
```json
{"natrium":2,"type":"REQUEST","supersedes":"<message id of the first>","request":{…new shape…}}
```

| Reader | Sees |
|---|---|
| version 1 | the first message; the second is `UnsupportedValue` and fires `observeUpdateRecommended` |
| version 2 | the second; hides the superseded first and maps responses to it onto the second |

Costs, accepted for the rare case: both messages count as unread and may notify twice, and one of
the two can fail to send (the sender resends the missing one).

### 4.6 What apps do

- Do **not** render every `UnsupportedValue` as a "please update" bubble: after a reorganisation
  every message would appear twice. Hide it or show a quiet placeholder.
- Ask for the update **once**, from `Session.observeUpdateRecommended`
  (`UpdateRecommended(supportedVersion, highestSeenVersion)`); it replays its state to late
  listeners.

### 4.7 Checklist for a protocol change

1. Can it be an optional field? → rule 1, done. Does it constrain answers? → receivers tolerate.
2. Does an existing field change its meaning? → rule 2: new name next to the old, same ids.
3. New request/response type? → rule 3; add it to the known types of both SDKs.
4. None of that? → rule 4, raise `MessageProtocol.VERSION` / `PROTOCOL_VERSION` in both SDKs.
5. Kotlin and natrium-web change in the same step, with the same JSON in both test suites.
