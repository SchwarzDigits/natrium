# Case Links

A **case link** lets another party start a conversation with this user — for example, attached to
an application, so that the receiving side opens a case (a conversation) for it and adds the user.
The app chooses the link's id (`ref`), Natrium builds the link; the app shows it, shares it or
attaches it wherever it belongs.

> Prerequisite: [conversations.md](./conversations.md) (the receiving side stores the link's `ref`
> as conversation metadata).

## 1. Configuration

The link points at the receiving side's web client. Its base URL is part of the backend
configuration:

```kotlin
Natrium.initialize(
    BackendConfig(
        …,
        caseLinkBase = "https://authority.example/import",   // null: the app does not use case links
    ),
    platform,
)
```

## 2. Generating a link

```kotlin
when (val r = session.generateCaseLink(ref = application.id)) {
    is GenerateCaseLinkResult.Success -> {
        r.caseLink.url   // https://authority.example/import#eyJ2IjoxLCJ1c2Vy…
        r.caseLink.ref   // the ref passed in
    }
    GenerateCaseLinkResult.Failure.NotConfigured -> …   // caseLinkBase is not set
    GenerateCaseLinkResult.Failure.InvalidRef -> …      // ref is blank
    GenerateCaseLinkResult.Failure.NotLoggedIn -> …
    is GenerateCaseLinkResult.Failure.Unknown -> …
}
```

The `ref` is the app's own id for what the link belongs to — e.g. the application it is attached
to. Natrium does not generate or store it; it only puts it into the link.

- **Unique per case.** The receiving side treats two links with the same `ref` as the same case
  (§ 3). Use one `ref` per case, never the same one for two different things. A random UUID is a
  good choice when the app has no id of its own.
- **Deterministic.** The same user and `ref` always give the same link, so the app can generate it
  again whenever it needs it instead of storing the URL.
- Not blank; otherwise any string. Keep it short, it ends up in the link.

## 3. Format

```
<caseLinkBase>#<token>
token = base64url, no padding, of {"v":1,"user":"<value@domain>","ref":"<ref>"}
```

- The token is in the URL **fragment**, so the server serving the page never receives it (the user
  id does not end up in access logs).
- Base64 only keeps the payload from being read at a glance — it is **not** a protection. Anyone
  with the link can decode it.
- `v` versions the payload; readers ignore unknown fields and reject newer versions.
- The receiving side uses `ref` as the technical id of the case: opening the same link again finds
  the existing conversation instead of creating a second one.

The web SDK reads the same format with `parseCaseLink(linkOrToken)`.
