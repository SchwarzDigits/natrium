# Authentication

Everything around obtaining and managing a `Session` — login, SSO, restoration, observation, logout, profile updates, and device-limit recovery.

> Prerequisite: you have already called `Natrium.initialize(backendConfig, platform)` once. See [getting-started.md](./getting-started.md).

## 1. Email / password login

```kotlin
suspend fun Natrium.login(
    email: String,
    password: String,
    secondFactorVerificationCode: String? = null,
): LoginResult
```

`LoginResult` has three top-level branches:

```kotlin
sealed class LoginResult {
    data class Success(val session: Session) : LoginResult()
    sealed class Failure : LoginResult() {
        data class Error(val reason: LoginError) : Failure()
        data class TooManyDevices(val resolver: DeviceLimitResolver) : Failure()
    }
}
```

A complete `when`:

```kotlin
when (val result = Natrium.login(email, password)) {
    is LoginResult.Success -> proceedTo(result.session)

    is LoginResult.Failure.Error -> when (result.reason) {
        LoginError.EMAIL_OR_PASSWORD_WRONG       -> showError("Wrong email or password.")
        LoginError.SECOND_FA_CODE_REQUIRED       -> show2FAPrompt()
        LoginError.INVALID_2FA_CODE              -> showError("The 2FA code is invalid.")
        LoginError.ACCOUNT_LOCKED                -> showError("Your account is suspended.")
        LoginError.ACCOUNT_NOT_ACTIVATED         -> showError("Please activate your account first.")
        LoginError.LOGIN_FAILED                  -> showError("Login failed. Please try again.")
        LoginError.SERVER_VERSION_NOT_SUPPORTED  -> showError("Server is incompatible with this app.")
        LoginError.APP_UPDATE_REQUIRED           -> showError("Please update the app.")
        LoginError.CONNECTION_ERROR              -> showError("Network unavailable.")
        LoginError.SESSION_COULD_NOT_BE_SAVED    -> showError("Local storage error.")
        LoginError.CLIENT_REGISTRATION_FAILED    -> showError("Could not register this device.")
    }

    is LoginResult.Failure.TooManyDevices ->
        showDeviceLimitFlow(result.resolver)   // see §7
}
```

Wording suggestions are illustrative — adapt to your product voice. The important property is that the `when` is exhaustive over `LoginError`.

## 2. Two-factor authentication

The 2FA flow is **two `login(...)` calls**, not a separate API:

```kotlin
// 1) Attempt without 2FA
val first = Natrium.login(email, password)

if (first is LoginResult.Failure.Error &&
    first.reason == LoginError.SECOND_FA_CODE_REQUIRED) {

    // 2) Prompt the user for the code (delivered out-of-band by the backend)
    val code = await2FACodeFromUser()

    val second = Natrium.login(email, password, secondFactorVerificationCode = code)
    // handle `second` exactly like the email/password example above
}
```

`LoginError.INVALID_2FA_CODE` means the code was rejected; reprompt the user.

## 3. SSO login

SSO is **the primary login path for enterprise tenants**. It is a 3-step flow:

1. Initiate SSO — get a redirect URL.
2. Send the user through their IdP via that URL (external browser / Custom Tab / web view).
3. Complete the login with the cookie the IdP redirects back with.

### 3a. Email-initiated SSO (Enterprise Discovery)

Use this when your app does not know the SSO code up front but does know the user's work email:

```kotlin
suspend fun Natrium.ssoLogin(email: String): SSOLoginResult
```

Under the hood, Natrium looks up the email's domain on the backend to find the tenant's configured SSO provider, then initiates the redirect.

### 3b. Direct-code SSO

Use this when the SSO code is already known (admin-provisioned, QR-code onboarding, custom flow):

```kotlin
suspend fun Natrium.ssoLoginWithCode(ssoCode: String): SSOLoginResult
```

Both functions return the same `SSOLoginResult`:

```kotlin
sealed class SSOLoginResult {
    data class Success(val authorizationUrl: String) : SSOLoginResult()
    sealed class Failure : SSOLoginResult() {
        data class Error(val reason: SSOLoginError) : Failure()
    }
}
```

### 3c. Send the user to the IdP

Open `authorizationUrl` in an external browsing context. Choose what fits your platform:

| Platform | Recommended | Notes |
|---|---|---|
| Android | Custom Tabs (`CustomTabsIntent`) | Falls back to default browser; preserves cookies and saves credentials. |
| iOS     | `SFSafariViewController` or `ASWebAuthenticationSession` | `ASWebAuthenticationSession` is best when your backend redirects to a custom URL scheme. |
| Desktop / JVM | `java.awt.Desktop.browse(URI)` | Opens the user's default browser. |

After the user authenticates, the IdP redirects back to a callback URL configured per tenant. The cookie value the SDK needs is delivered as part of that response; the **exact transport (URL parameter, response header, intercepted cookie) is backend-/IdP-specific** and is out of scope for this SDK — coordinate with your backend operator for the concrete extraction rule.

A typical convention used by Wire-compatible backends is to redirect back to a custom URL scheme registered to your app, with the cookie attached as a query parameter — for example:

```
wire://sso-login/success?cookie=<url-encoded-cookie>
wire://sso-login/failure?errorCode=<code>
```

Your app then registers a deep-link handler for this scheme, parses the `cookie` parameter, URL-decodes it, and passes it to step 3d. Confirm the actual scheme and parameter names with your backend operator before relying on these.

### 3d. Complete the login

```kotlin
suspend fun Natrium.completeSSOLogin(cookie: String): LoginResult
```

The result is a regular `LoginResult` — handle it the same way as email/password login (including the `TooManyDevices` branch).

### 3e. End-to-end SSO snippet

A typical ViewModel-shaped implementation:

```kotlin
class SsoLoginViewModel : ViewModel() {

    fun start(email: String) = viewModelScope.launch {
        when (val initiate = Natrium.ssoLogin(email)) {
            is SSOLoginResult.Success ->
                emitOpenUrl(initiate.authorizationUrl)
            is SSOLoginResult.Failure.Error ->
                showSsoError(initiate.reason)
        }
    }

    fun onIdpReturned(cookie: String) = viewModelScope.launch {
        when (val finish = Natrium.completeSSOLogin(cookie)) {
            is LoginResult.Success                    -> onLoggedIn(finish.session)
            is LoginResult.Failure.Error              -> showError(finish.reason)
            is LoginResult.Failure.TooManyDevices     -> showDeviceLimitFlow(finish.resolver)
        }
    }
}
```

### 3f. `SSOLoginError` reference

| Value | Meaning | Suggested UI |
|---|---|---|
| `SSO_NOT_AVAILABLE`            | Backend reports no SSO is configured for this email's domain (or the call to discover it failed). | "SSO is not available for your account. Use email and password instead." |
| `INVALID_CODE`                 | The SSO code (passed to `ssoLoginWithCode` or derived in `ssoLogin`) is not recognized. | "This SSO code is not valid." |
| `INVALID_CODE_FORMAT`          | Code is syntactically wrong. | "Please check the SSO code and try again." |
| `SERVER_VERSION_NOT_SUPPORTED` | Backend protocol is too old for this SDK. | "Server is incompatible. Contact your administrator." |
| `APP_UPDATE_REQUIRED`          | This app is too old for the backend. | "Please update the app." |
| `CONNECTION_ERROR`             | Network or transient backend failure. | "Cannot reach the server. Check your connection." |

### 3g. Headless (browserless) SSO

When your backend runs a component that acts as the SAML IdP and *simulates* the expected SAML
responses, you don't need a browser at all. Natrium can imitate the browser and drive the whole
SAML redirect/POST chain itself — you only supply the parameters that component needs.

```kotlin
suspend fun Natrium.ssoLoginHeadless(
    ssoCode: String,
    interceptor: HeadlessSsoInterceptor,
): LoginResult
```

One call does initiate → drive → complete and returns a regular `LoginResult` (handle it exactly
like §1, including the `TooManyDevices` branch). There is no `authorizationUrl` to open and no deep
link to register.

**The interceptor** is invoked once per outgoing request **to a non-Wire host** (i.e. to the
external IdP component). Requests to Wire's own hosts — derived from your `BackendConfig` — are
driven internally and never surfaced. No HTTP-library types leak through the boundary.

```kotlin
val result = Natrium.ssoLoginHeadless(ssoCode) { request: HeadlessSsoRequest ->
    // request.url / request.host / request.method / request.queryParameters / request.formFields
    HeadlessSsoInjection.Builder()
        .queryParameter("tenant", "acme")
        .header("X-Tenant-Token", token)
        .formField("subject", userIdentifier)   // applied to SAML POST-binding submits
        .build()
}

when (result) {
    is LoginResult.Success                -> onLoggedIn(result.session)
    is LoginResult.Failure.Error          -> showError(result.reason)  // LOGIN_FAILED / CONNECTION_ERROR
    is LoginResult.Failure.TooManyDevices -> showDeviceLimitFlow(result.resolver)
}
```

Natrium follows HTTP redirects **and** auto-submits SAML POST-binding forms (`SAMLResponse` /
`RelayState`), preserving cookies across the chain, until the backend issues the terminal
`wire://sso-login/success?cookie=…` redirect — whose cookie it then exchanges for a session just
like `completeSSOLogin`. This assumes the IdP component runs on a host different from your Wire
`api` host (so it can be told apart from Wire-internal traffic).

## 4. Session restoration

After a process restart Natrium has a persisted account on disk but no live `Session` object. Recover it on app start:

```kotlin
suspend fun Natrium.restoreLastSession(): Session?
```

Returns `null` when:
- nothing was ever persisted (fresh install), **or**
- the persisted account is invalid / has been remotely logged out.

On success, an `AuthEvent.LoggedIn(session)` is emitted on the auth-event stream after the `Session` is constructed (so any observer registered before the `restoreLastSession()` call returns will receive it). Treat the call as a one-shot during app startup — calling it concurrently from multiple places is not guarded and would produce multiple session instances and events.

```kotlin
@Composable
fun AppRoot() {
    var session by remember { mutableStateOf<Session?>(null) }
    var restoring by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val cancellable = Natrium.observeAuthEvents { event ->
            when (event) {
                is AuthEvent.LoggedIn  -> session = event.session
                AuthEvent.LoggedOut    -> session = null
            }
        }
        onDispose { cancellable.cancel() }
    }

    LaunchedEffect(Unit) {
        Natrium.restoreLastSession()  // result fans out via observeAuthEvents
        restoring = false
    }

    when {
        restoring        -> SplashScreen()
        session != null  -> MainNav(session!!)
        else             -> LoginScreen()
    }
}
```

## 5. Observing auth events

```kotlin
fun Natrium.observeAuthEvents(listener: (AuthEvent) -> Unit): Cancellable

sealed class AuthEvent {
    data class LoggedIn(val session: Session) : AuthEvent()
    data object LoggedOut : AuthEvent()
}
```

Fires on:
- `Natrium.login` success → `LoggedIn`
- `Natrium.completeSSOLogin` success → `LoggedIn`. `ssoLogin` and `ssoLoginWithCode` only initiate the IdP redirect and never emit on the auth-event stream themselves — neither on success nor on failure.
- `Natrium.restoreLastSession` recovery success → `LoggedIn`
- `Session.logout()` → `LoggedOut`
- Remote logout (server invalidates the session, e.g. password changed elsewhere) → `LoggedOut`

The `Cancellable` must be cancelled when the consumer scope ends. Two observers on the same `Natrium` object are both fine — events fan out to every active listener.

## 6. Logout

```kotlin
suspend fun Session.logout(): LogoutResult

sealed class LogoutResult {
    data object Success : LogoutResult()
    sealed class Failure : LogoutResult() {
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}
```

After `logout()` returns:
- The session's internal coroutine scope is cancelled — any active observers tied to that scope stop firing. Discard your `Session` reference.
- An `AuthEvent.LoggedOut` is emitted on the auth-event stream.
- The underlying Kalium logout is performed as a soft logout (`LogoutReason.SELF_SOFT_LOGOUT`); on-disk artifacts are managed by Kalium's logout semantics. After a logout, `Natrium.restoreLastSession()` should be treated as the authoritative signal for what is recoverable.

To log back in, call `Natrium.login(...)` / `Natrium.ssoLogin(...)`. Do **not** keep a reference to the old `Session` and try to reuse it.

```kotlin
suspend fun signOut() {
    when (session.logout()) {
        LogoutResult.Success           -> navigateToLogin()
        is LogoutResult.Failure.Unknown -> showError("Logout failed; please try again.")
    }
}
```

## 7. Handling "too many devices"

The backend enforces a limit on the number of registered devices per account. When the limit is reached, login returns:

```kotlin
LoginResult.Failure.TooManyDevices(resolver: DeviceLimitResolver)
```

The `resolver` is a one-shot helper that lets the user pick an existing device to remove, then retries the login automatically:

```kotlin
interface DeviceLimitResolver {
    suspend fun listDevices(): ListDevicesResult
    suspend fun removeDevice(deviceId: String, password: String? = null): RemoveDeviceResult
    suspend fun retry(): LoginResult
}
```

A complete flow:

```kotlin
suspend fun resolveDeviceLimit(resolver: DeviceLimitResolver, password: String) {
    val devices = when (val list = resolver.listDevices()) {
        is ListDevicesResult.Success     -> list.devices
        is ListDevicesResult.Failure     -> { showError(); return }
    }

    val toRemove = askUserWhichDevice(devices) ?: return

    when (val removed = resolver.removeDevice(toRemove.id, password)) {
        RemoveDeviceResult.Success                       -> Unit
        RemoveDeviceResult.Failure.PasswordRequired      -> { askForPassword(); return }
        is RemoveDeviceResult.Failure.InvalidCredentials -> { showError(removed.message); return }
        RemoveDeviceResult.Failure.NotLoggedIn           -> { showError(); return }
        is RemoveDeviceResult.Failure.Unknown            -> { showError(removed.message); return }
    }

    // retry the login that originally hit the limit
    when (val retry = resolver.retry()) {
        is LoginResult.Success                -> onLoggedIn(retry.session)
        is LoginResult.Failure.Error          -> showError(retry.reason)
        is LoginResult.Failure.TooManyDevices -> showError("Still too many devices.")
    }
}
```

`password` may be required by the backend when removing a device — the SDK surfaces this as `PasswordRequired` if the backend rejects the call for missing password authentication, and `InvalidCredentials` if the password was wrong. Re-prompt the user accordingly.

> **Note**: `resolver.retry()` on success returns `LoginResult.Success(session)` directly but does **not** emit `AuthEvent.LoggedIn` on the auth-event stream — pick up the new `Session` from the result yourself.

`DeviceManager` (`session.deviceManager.list()` / `.remove(...)`) is a similarly shaped API used **after** a successful login to manage devices from settings — see [api-reference.md](./api-reference.md#devicemanager).

## 8. Profile updates

```kotlin
suspend fun Session.updateDisplayName(name: String): UpdateDisplayNameResult
suspend fun Session.updateHandle(handle: String):     UpdateHandleResult
suspend fun Session.updateEmail(email: String):       UpdateEmailResult
```

`UpdateDisplayNameResult` is symmetric to `LogoutResult` — just `Success` or `Failure.Unknown`. Handle and email have additional validation branches:

```kotlin
when (val r = session.updateHandle("new-handle")) {
    UpdateHandleResult.Success                  -> Unit
    UpdateHandleResult.Failure.InvalidHandle    -> showError("That handle isn't valid.")
    UpdateHandleResult.Failure.HandleExists     -> showError("That handle is already taken.")
    is UpdateHandleResult.Failure.Unknown       -> showError(r.message)
}

when (val r = session.updateEmail("new@example.com")) {
    UpdateEmailResult.Success                       -> Unit
    UpdateEmailResult.Failure.InvalidEmail          -> showError("Not a valid email.")
    UpdateEmailResult.Failure.EmailAlreadyInUse     -> showError("That email is already taken.")
    is UpdateEmailResult.Failure.Unknown            -> showError(r.message)
}
```

To read the current profile, use `session.sessionInfo()` (one-shot) or `session.observeSessionInfo { info -> ... }` (reactive). `SessionInfo` carries `user: NamedUser`, `handle`, `email`, `backend` (the URL), and `device: DeviceInfo?` (the current device).
