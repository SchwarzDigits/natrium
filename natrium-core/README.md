# natrium-core

Kotlin Multiplatform library that wraps Wire's [Kalium](https://github.com/wireapp/kalium) SDK with a thin, consumer-friendly API for secure messaging.

## Install

`natrium-core` is published to **Maven Central** — no extra repository declaration required.

```kotlin
repositories { mavenCentral() }

dependencies {
    implementation("schwarz.opensource.natrium:natrium-core:0.0.1")
}
```

Requirements: Kotlin 2.3.0, JDK 17, Android `minSdk` 26.

## Targets

| Target               | Source set    |
|----------------------|---------------|
| JVM                  | `jvmMain`     |
| Android (minSdk 26)  | `androidMain` |
| iOS arm64            | `iosMain`     |
| iOS simulator (arm64)| `iosMain`     |

All public API lives in `commonMain`.

## What's inside

After `Natrium.login(...)` you hold a `Session` that exposes:

- **`conversationManager`** — list, find, create, and join conversations; per-conversation operations (members, title, join links, delete).
- **`chat()`** on each conversation — send/receive text and files, replies, reactions, typing indicators, message history.
- **`deviceManager`** — list and remove the user's registered devices.

## Design principles

- **Callback + `Cancellable`** for reactive APIs instead of `Flow`. Function-type callbacks bridge directly to Swift closures, so the same API is callable from Kotlin and Swift without an additional wrapper layer.
- **Sealed result types** (`Success` / `Failure.*`) for every fallible operation. No exceptions for expected failures.
- **Manager pattern**: `Natrium` → `Session` → managers → per-conversation `ConversationOperations` → `ChatOperations`.
- **Lifecycle-gated sync**: the SDK signals active/inactive based on platform state (Android `ProcessLifecycleOwner`, iOS UIKit notifications, JVM always-on) and starts/cancels a Kalium sync request accordingly.

## Documentation

| Document | What's in it |
|---|---|
| [Getting Started](./docs/getting-started.md) | Install the dependency, initialize Natrium, perform a first login. |
| [Concepts](./docs/concepts.md) | Object hierarchy, the callback+`Cancellable` pattern, sealed result types, lifecycle, threading, storage. |
| [Authentication](./docs/authentication.md) | Email/password login, 2FA, SSO (email-initiated and direct-code), session restoration, auth events, logout, profile updates, device-limit recovery. |
| [Conversations](./docs/conversations.md) | List, find, create, join, delete conversations; members; join links. |
| [Messaging](./docs/messaging.md) | Text, files, replies, reactions, observing messages and typing, file downloads, history, system events. |
| [Platforms](./docs/platforms.md) | Android, iOS, and JVM specifics — storage locations, lifecycle wiring, user-agent. |
| [API Reference](./docs/api-reference.md) | Full method/parameter/result-type listing — the source of truth for signatures. |

If you've never used Natrium before, read in this order: **Getting Started → Concepts → Authentication → Conversations → Messaging**. Pull up the API Reference as a side-channel when you need exact signatures or the complete list of failure branches.

## Related module

[`natrium-cli`](../natrium-cli/README.md) is a JVM reference client built on `natrium-core` — useful for smoke-testing a backend or learning the API surface from a known-working consumer. It is not intended for production deployment.

## License

Natrium wrapper code is licensed under the **EUPL v. 1.2 only**. The Kalium SDK
dependency is **not** licensed under the EUPL; it may only be used under the
GPL-3.0 or a commercial licence from Wire Swiss GmbH. Linking Natrium with
Kalium does not cause EUPL copyleft to extend to Kalium.

See the [root README](../README.md#license), [LICENSE](../LICENSE), and
[NOTICE](../NOTICE).
