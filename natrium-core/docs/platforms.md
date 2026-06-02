# Platforms

Natrium is one published artifact (`schwarz.opensource.natrium:natrium-core`) with three platform-specific runtimes selected automatically by Gradle/CocoaPods. This page documents the differences you should be aware of when integrating: storage location, lifecycle wiring, the `NatriumPlatform` constructor, and the reported user-agent.

## Android

### Module setup

In your Android (or KMP-with-Android) module's `build.gradle.kts`:

```kotlin
android {
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation("schwarz.opensource.natrium:natrium-core:0.0.1")
}
```

For a KMP module, add the dependency under `commonMain` (or the `androidMain` source set) — see [getting-started.md](./getting-started.md).

### Initialization

Initialize in `Application.onCreate` with the **application** context:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Natrium.initialize(backendConfig, NatriumPlatform(this))
    }
}
```

Then register the `Application` subclass in `AndroidManifest.xml`:

```xml
<application android:name=".MyApp" ...>
```

Don't use an `Activity` context — Natrium outlives any single Activity.

### Lifecycle

Foreground/background detection is wired via [`ProcessLifecycleOwner`](https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner). When the process enters `STARTED`, the SDK starts a Kalium sync request; on `STOPPED` it cancels that request. What Kalium does with the WebSocket / pending work on cancel (disconnect, pause, drain) is owned by Kalium's sync executor.

You do not need to interact with `ProcessLifecycleOwner` yourself. The dependency on `androidx.lifecycle:lifecycle-process` is brought in transitively by `natrium-core`.

### Storage

Persisted state lives under:

```
/data/data/<your-package>/app_accounts
```

…which is reached via `context.getDir("accounts", Context.MODE_PRIVATE)`. This is in your app's private data directory — not visible to other apps, automatically wiped on uninstall.

### User agent

Reported to the backend as `Natrium/<version> (Android)`.

## iOS

### Module setup

Consume natrium-core through either:

- **CocoaPods** — your Kotlin Multiplatform module exposes a Framework that your iOS app links against (`use_frameworks!` in the `Podfile`).
- **SwiftPM** — same idea via the [SwiftPM-friendly Kotlin/Native packaging](https://kotlinlang.org/docs/multiplatform-spm-export.html).

Pick whichever your shared module is set up for. The dependency declaration in the KMP module's `build.gradle.kts` is the standard one from [getting-started.md](./getting-started.md).

### Initialization

Initialize in your shared entry point — typically `MainViewController()` for Compose Multiplatform or the SwiftUI/UIKit bridge:

```kotlin
fun MainViewController(): UIViewController {
    Natrium.initialize(backendConfig, NatriumPlatform())
    return ComposeUIViewController { App() }
}
```

Call this **once** when the app process starts. If you embed Natrium in a UIKit app via a wrapper function, call `Natrium.initialize` from your `AppDelegate.application(_:didFinishLaunchingWithOptions:)` bridge.

### Lifecycle

Foreground/background state is sourced from UIKit notifications:

- `UIApplicationDidBecomeActiveNotification` → SDK starts a Kalium sync request.
- `UIApplicationDidEnterBackgroundNotification` → SDK cancels the active sync request.

Observers are attached to the default notification centre when `Session` lifecycle management starts. The initial state is sampled from `UIApplication.sharedApplication.applicationState` before any notification fires, so an app launched while in the background starts with the correct (inactive) signal rather than briefly assuming active. Background WebSocket behaviour beyond the cancellation is owned by Kalium.

### Storage

Persisted state lives under:

```
<NSHomeDirectory>/Documents/natrium
```

…which is in your app sandbox's `Documents` directory.

A security note: on iOS the SDK runs with `wipeOnCookieInvalid = true`. If the backend rejects the auth cookie (typically after a forced remote logout), Natrium wipes the local account state. Treat any `AuthEvent.LoggedOut` you didn't initiate as a signal to redirect the user back to login.

### User agent

Reported to the backend as `Natrium/<version> (iOS)`.

## JVM (desktop / server / CLI)

### Module setup

```kotlin
repositories { mavenCentral() }

dependencies {
    implementation("schwarz.opensource.natrium:natrium-core:0.0.1")
}
```

`natrium-core` is plain Kotlin/JVM here — no AGP / no Cocoa, just a regular library. Requires JDK 17+.

### Initialization

```kotlin
fun main() {
    Natrium.initialize(backendConfig, NatriumPlatform())
    // ... drive your CLI / desktop UI here
}
```

`NatriumPlatform()` takes no arguments on JVM.

### Lifecycle

JVM has no foreground/background concept — the SDK emits `ACTIVE` once at start-up and stays active for the lifetime of the process. The Kalium sync request stays open continuously while the process runs.

If you need to clean up before exit (e.g. flush state, close the SDK), call `Session.logout()` on the active session — there is no separate "shutdown" method.

### Storage

Persisted state lives under:

```
$HOME/.natrium
```

…resolved via `System.getProperty("user.home")`. If you need multiple parallel JVM instances against different accounts, run them under different OS users or override `user.home`; the SDK does not currently sandbox concurrent processes that share this directory.

### User agent

Reported to the backend as `Natrium/<version> (JVM)`.

## Reference CLI

The repository ships `natrium-cli`, a JVM-only reference client built on `natrium-core`. It is useful for:

- smoke-testing a backend before integrating the SDK into a real app,
- scripting two-user scenarios end-to-end,
- learning the API surface from a known-working consumer.

See [`natrium-cli/README.md`](../../natrium-cli/README.md) for the list of commands. The CLI is **not** intended as a production deployment target.

## Summary

| Aspect | Android | iOS | JVM |
|---|---|---|---|
| `NatriumPlatform()` arg | `Context` (application) | (none) | (none) |
| Storage root | `<package-dir>/app_accounts` | `<sandbox>/Documents/natrium` | `~/.natrium` |
| Lifecycle source | `ProcessLifecycleOwner` | UIKit notifications | always-active |
| Background sync request | Cancelled | Cancelled | Always running |
| Auto-wipe on auth fail | No | **Yes** (`wipeOnCookieInvalid`) | No |
| User-agent suffix | `(Android)` | `(iOS)` | `(JVM)` |
