# Case Links

A **case link** lets another party start a conversation with this user — for example, attached to
an application, so that the receiving side opens a case (a conversation) for it and adds the user.
Natrium generates the link; the app shows it, shares it or attaches it wherever it belongs.

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
when (val r = session.generateCaseLink()) {
    is GenerateCaseLinkResult.Success -> {
        r.caseLink.url   // https://authority.example/import#eyJ2IjoxLCJ1c2Vy…
        r.caseLink.ref   // 8f0c2c1e-…, unique per call
    }
    GenerateCaseLinkResult.Failure.NotConfigured -> …   // caseLinkBase is not set
    GenerateCaseLinkResult.Failure.NotLoggedIn -> …
    is GenerateCaseLinkResult.Failure.Unknown -> …
}
```

Every call generates a new `ref`. Keep it if the app needs to know later which link belongs to
what (e.g. which application it was attached to).

## 3. Format

```
<caseLinkBase>#<token>
token = base64url, no padding, of {"v":1,"user":"<value@domain>","ref":"<uuid>"}
```

- The token is in the URL **fragment**, so the server serving the page never receives it (the user
  id does not end up in access logs).
- Base64 only keeps the payload from being read at a glance — it is **not** a protection. Anyone
  with the link can decode it.
- `v` versions the payload; readers ignore unknown fields and reject newer versions.
- The receiving side uses `ref` as the technical id of the case: opening the same link again finds
  the existing conversation instead of creating a second one.

The web SDK reads the same format with `parseCaseLink(linkOrToken)`.
