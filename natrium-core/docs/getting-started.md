# Getting Started

This guide walks you from an empty project to a first successful login against your backend.

## Requirements

- **Kotlin** 2.3.0 (Natrium is currently built against this version; consumers should match)
- **JDK 17** or newer
- **Android**: AGP 9.0+, `minSdk = 26`
- **iOS**: arm64 device or simulator (Kotlin/Native)
- A reachable Wire-compatible backend (you will need the `api`, `accounts`, `webSocket`, `teams`, `blackList`, `website` URLs)

## Add the dependency

`natrium-core` is published to **Maven Central**, so no extra repository declaration is required.

In your KMP module's `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("schwarz.opensource.natrium:natrium-core:0.0.1")
        }
    }
}
```

For a JVM-only or Android-only consumer the same artifact coordinate works; Gradle will resolve the matching variant via Kotlin metadata.

> **Note on the Kalium dependency**: `natrium-core` brings Wire's `kalium-logic` (published as `schwarz.opensource.natrium:logic`) in transitively. You do not need to depend on it directly.
>
> Kalium is **not** licensed under the EUPL. It may only be used under the GPL-3.0 or a commercial licence from Wire Swiss GmbH. Linking Natrium with Kalium does not cause EUPL copyleft to extend to Kalium. See the [root README](../../README.md#license) and [NOTICE](../../NOTICE).

## Initialize Natrium

Initialization is a single call to `Natrium.initialize(backendConfig, platform)`. Do it **once** per process — typically from `Application.onCreate()` (Android), `MainViewController()` / `AppDelegate.application(_:didFinishLaunchingWithOptions:)` (iOS), or `fun main()` (JVM). Calling it a second time on the same process is not currently guarded and is not part of the supported API contract — treat it as a one-shot.

### Configure the backend

`Natrium.initialize` takes a `BackendConfig` with seven URLs (`name`, `api`, `accounts`, `webSocket`, `teams`, `blackList`, `website`) plus an `isOnPremises` flag that defaults to `true`. All values come from your backend operator.

Don't hard-code them in source. The typical pattern is to keep them in `local.properties` (or another file outside version control) and generate a Kotlin constants object at build time, then construct `BackendConfig` from those constants and pass it to `Natrium.initialize`. The Showcase app demonstrates this setup end-to-end.

### Optional: enable logging early

```kotlin
initLogging(LogLevel.DEBUG)   // VERBOSE, DEBUG, INFO, WARN, ERROR, DISABLED
```

If you skip this, logging defaults to `WARN` on first `Natrium.initialize(...)` call.

### Platform-specific initialization

Pick the snippet for your target. Each platform has a tiny constructor difference for `NatriumPlatform`.

**Android** — call from `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initLogging(LogLevel.DEBUG)
        Natrium.initialize(backendConfig, NatriumPlatform(this))
    }
}
```

`NatriumPlatform(context)` requires the **application** context (not an Activity). Lifecycle awareness is wired automatically via `ProcessLifecycleOwner`.

**iOS** — call from your KMP entry point (typically `MainViewController`):

```kotlin
fun MainViewController() = ComposeUIViewController {
    Natrium.initialize(backendConfig, NatriumPlatform())
    App()
}
```

UIKit lifecycle notifications are attached automatically.

**JVM** (desktop / server / CLI):

```kotlin
fun main() {
    initLogging(LogLevel.DEBUG)
    Natrium.initialize(backendConfig, NatriumPlatform())
    // ... rest of your app
}
```

On JVM, the process is always considered "active" — there is no foreground/background distinction.

## First login

Natrium supports two login methods. Pick the one your tenant is configured for.

### Email + password

```kotlin
val result = Natrium.login(
    email = "user@example.com",
    password = "correct horse battery staple",
)

when (result) {
    is LoginResult.Success -> {
        val session: Session = result.session
        // proceed to your app
    }
    is LoginResult.Failure.Error -> {
        // result.reason is a LoginError enum (e.g. EMAIL_OR_PASSWORD_WRONG)
    }
    is LoginResult.Failure.TooManyDevices -> {
        // user must remove an old device before continuing
        // see authentication.md → "Handling too many devices"
    }
}
```

2FA: pass the verification code as the optional third parameter `secondFactorVerificationCode`. If the backend signals 2FA is required, the first call returns `LoginResult.Failure.Error(LoginError.SECOND_FA_CODE_REQUIRED)`; the user enters the code and you retry.

### SSO

SSO is a two-step flow that involves the user's browser/IdP:

```kotlin
// Step 1 — derive the SSO code from the email's domain (Enterprise Discovery)
val initiate = Natrium.ssoLogin("user@example.com")

when (initiate) {
    is SSOLoginResult.Success -> {
        val url = initiate.authorizationUrl
        // open url externally — Custom Tabs / SFSafariViewController / Desktop.browse
    }
    is SSOLoginResult.Failure.Error -> { /* see authentication.md */ }
}

// Step 2 — after the IdP redirects back to your app, extract the cookie and call:
val finish = Natrium.completeSSOLogin(cookie = "<cookie from redirect>")

when (finish) {
    is LoginResult.Success -> { /* session ready */ }
    is LoginResult.Failure -> { /* same branches as email/password login */ }
}
```

If the SSO code is already known out-of-band (admin-provisioned, QR code, …), skip the email-domain discovery and call `Natrium.ssoLoginWithCode(ssoCode)` instead — the result type and the second `completeSSOLogin` step are identical.

See [authentication.md](./authentication.md) for cookie extraction details, all `SSOLoginError` branches, and a complete ViewModel example.

## Observe auth state

Auth events fire on every login/logout, including ones triggered by the server (e.g. the user is logged out remotely). Register a listener once at the top of your UI tree:

```kotlin
val cancellable = Natrium.observeAuthEvents { event ->
    when (event) {
        is AuthEvent.LoggedIn -> {
            currentSession = event.session
        }
        AuthEvent.LoggedOut -> {
            currentSession = null
        }
    }
}

// later, when you tear down the screen / scope:
cancellable.cancel()
```

On app start, also try to pick up an existing session:

```kotlin
val restored: Session? = Natrium.restoreLastSession()
```

Returns `null` if nothing is persisted (fresh install / after logout). On success, an `AuthEvent.LoggedIn` is also emitted, so the same observer above receives it.

> **A note on code samples in this documentation**: snippets across these docs use placeholder helpers like `showError(...)`, `redirectToLogin()`, `emitOpenUrl(...)`, etc. These are **your** UI/navigation functions — they are not part of the SDK. Adapt them to your app's architecture.

## Next steps

- [Concepts](./concepts.md) — understand the object hierarchy and the callback+`Cancellable` pattern before going deeper.
- [Authentication](./authentication.md) — full coverage of every login branch, profile updates, device limits.
- [Conversations](./conversations.md) and [Messaging](./messaging.md) — once you have a `Session`, this is what you do with it.
- [Platforms](./platforms.md) — platform-specific storage and lifecycle details.

A complete Compose Multiplatform demo application exists as a separate repository — ask the Natrium team for the link. It demonstrates ViewModel patterns, navigation, and end-to-end SDK usage on Android, iOS, and Desktop.
