/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package schwarz.digits.natrium

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.user.SsoId
import com.wire.kalium.logic.data.user.SsoManagedBy
import com.wire.kalium.logic.data.user.UserId as KaliumUserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.EnterpriseLoginResult
import com.wire.kalium.logic.feature.auth.LoginRedirectPath
import com.wire.kalium.logic.feature.auth.LogoutCallback
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.data.auth.login.ProxyCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import schwarz.digits.natrium.devices.DeviceLimitResolverImpl
import schwarz.digits.natrium.session.LoginError
import schwarz.digits.natrium.session.LoginResult
import schwarz.digits.natrium.session.SSOLoginError
import schwarz.digits.natrium.session.SSOLoginResult
import schwarz.digits.natrium.session.Session
import schwarz.digits.natrium.session.SessionImpl

object Natrium {

    internal lateinit var coreLogic: CoreLogic
        private set

    private lateinit var platform: NatriumPlatform
    private lateinit var backendConfig: BackendConfig

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val authEventFlow = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)

    fun initialize(backendConfig: BackendConfig, platform: NatriumPlatform) {
        ensureLoggingInitialized()
        this.backendConfig = backendConfig
        this.platform = platform
        coreLogic = platform.initialize()

        coreLogic.getGlobalScope().logoutCallbackManager.register(
            object : LogoutCallback {
                override suspend fun invoke(userId: KaliumUserId, reason: LogoutReason) {
                    authEventFlow.tryEmit(AuthEvent.LoggedOut)
                }
            }
        )
    }

    fun observeAuthEvents(listener: (AuthEvent) -> Unit): Cancellable {
        val job = scope.launch {
            authEventFlow
                .conflate()
                .collect { listener(it) }
        }
        return Cancellable(job)
    }

    internal fun emitAuthEvent(event: AuthEvent) {
        authEventFlow.tryEmit(event)
    }

    private fun startLifecycleManagement(session: SessionImpl) {
        session.startLifecycleManagement(platform)
    }


    suspend fun restoreLastSession() : Session? {
        val result = coreLogic.globalScope { session.currentSession() }
        val accountInfo = (result as? CurrentSessionResult.Success)?.accountInfo
        if (accountInfo == null || !accountInfo.isValid()) return null
        val userId = accountInfo.userId
        return SessionImpl(this, coreLogic.getSessionScope(userId), backendConfig).also {
            startLifecycleManagement(it)
            emitAuthEvent(AuthEvent.LoggedIn(it))
        }
    }

    suspend fun login(
        email: String,
        password: String,
        secondFactorVerificationCode: String? = null,
    ): LoginResult {

        // Step 1: Obtain AuthenticationScope (negotiates API version with server)
        val authScope = withContext(Dispatchers.Default) {
            coreLogic.versionedAuthenticationScope(backendConfig.kaliumLinks).invoke(proxyCredentials = null)
        }.let {
            when (it) {
                is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope
                is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                    return LoginResult.Failure.Error(LoginError.SERVER_VERSION_NOT_SUPPORTED)
                }

                is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                    return LoginResult.Failure.Error(LoginError.APP_UPDATE_REQUIRED)
                }

                is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                    return LoginResult.Failure.Error(LoginError.CONNECTION_ERROR)
                }
            }
        }

        // Step 2: Perform login
        val loginResult = withContext(Dispatchers.Default) {
            authScope.login(
                userIdentifier = email,
                password = password,
                shouldPersistClient = true,
                secondFactorVerificationCode = secondFactorVerificationCode,
            )
        }

        if (loginResult !is AuthenticationResult.Success) {

            return when (loginResult) {
                is AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination ->
                    LoginResult.Failure.Error(LoginError.EMAIL_OR_PASSWORD_WRONG)

                is AuthenticationResult.Failure.InvalidCredentials.Missing2FA ->
                    LoginResult.Failure.Error(LoginError.SECOND_FA_CODE_REQUIRED)

                is AuthenticationResult.Failure.InvalidCredentials.Invalid2FA ->
                    LoginResult.Failure.Error(LoginError.INVALID_2FA_CODE)

                is AuthenticationResult.Failure.AccountSuspended ->
                    LoginResult.Failure.Error(LoginError.ACCOUNT_LOCKED)

                is AuthenticationResult.Failure.AccountPendingActivation ->
                    LoginResult.Failure.Error(LoginError.ACCOUNT_NOT_ACTIVATED)

                else -> LoginResult.Failure.Error(LoginError.LOGIN_FAILED)
            }
        }

        return storeAccountAndRegisterClient(
            serverConfigId = loginResult.serverConfigId,
            ssoId = loginResult.ssoID,
            accountTokens = loginResult.authData,
            proxyCredentials = loginResult.proxyCredentials,
            managedBy = loginResult.managedBy,
            password = password,
        )
    }

    suspend fun ssoLogin(email: String): SSOLoginResult {
        val authScope = resolveAuthScope() ?: return SSOLoginResult.Failure.Error(SSOLoginError.CONNECTION_ERROR)

        val domainResult = withContext(Dispatchers.Default) {
            authScope.getLoginFlowForDomainUseCase(email)
        }

        val ssoCode = when (domainResult) {
            is EnterpriseLoginResult.Success -> {
                when (val path = domainResult.loginRedirectPath) {
                    is LoginRedirectPath.SSO -> path.ssoCode
                    else -> return SSOLoginResult.Failure.Error(SSOLoginError.SSO_NOT_AVAILABLE)
                }
            }
            is EnterpriseLoginResult.Failure ->
                return SSOLoginResult.Failure.Error(SSOLoginError.SSO_NOT_AVAILABLE)
        }

        return initiateSSOLogin(authScope, ssoCode)
    }

    suspend fun ssoLoginWithCode(ssoCode: String): SSOLoginResult {
        val authScope = resolveAuthScope() ?: return SSOLoginResult.Failure.Error(SSOLoginError.CONNECTION_ERROR)
        return initiateSSOLogin(authScope, ssoCode)
    }

    suspend fun completeSSOLogin(cookie: String): LoginResult {
        val authScope = resolveAuthScope() ?: return LoginResult.Failure.Error(LoginError.CONNECTION_ERROR)

        val sessionResult = withContext(Dispatchers.Default) {
            authScope.ssoLoginScope.getLoginSession(cookie)
        }

        return when (sessionResult) {
            is SSOLoginSessionResult.Success -> {
                storeAccountAndRegisterClient(
                    serverConfigId = authScope.currentServerConfig().id,
                    ssoId = sessionResult.ssoId,
                    accountTokens = sessionResult.accountTokens,
                    proxyCredentials = sessionResult.proxyCredentials,
                    managedBy = sessionResult.managedBy,
                    password = null,
                )
            }
            is SSOLoginSessionResult.Failure ->
                LoginResult.Failure.Error(LoginError.LOGIN_FAILED)
        }
    }

    private suspend fun resolveAuthScope(): AuthenticationScope? {
        return withContext(Dispatchers.Default) {
            coreLogic.versionedAuthenticationScope(backendConfig.kaliumLinks).invoke(proxyCredentials = null)
        }.let {
            when (it) {
                is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope
                else -> null
            }
        }
    }

    private suspend fun initiateSSOLogin(authScope: AuthenticationScope, ssoCode: String): SSOLoginResult {
        val result = withContext(Dispatchers.Default) {
            authScope.ssoLoginScope.initiate(SSOInitiateLoginUseCase.Param.WithRedirect(ssoCode))
        }

        return when (result) {
            is SSOInitiateLoginResult.Success ->
                SSOLoginResult.Success(result.requestUrl)
            is SSOInitiateLoginResult.Failure.InvalidCode ->
                SSOLoginResult.Failure.Error(SSOLoginError.INVALID_CODE)
            is SSOInitiateLoginResult.Failure.InvalidCodeFormat ->
                SSOLoginResult.Failure.Error(SSOLoginError.INVALID_CODE_FORMAT)
            is SSOInitiateLoginResult.Failure.InvalidRedirect ->
                SSOLoginResult.Failure.Error(SSOLoginError.INVALID_CODE)
            is SSOInitiateLoginResult.Failure.Generic ->
                SSOLoginResult.Failure.Error(SSOLoginError.CONNECTION_ERROR)
        }
    }

    private suspend fun storeAccountAndRegisterClient(
        serverConfigId: String,
        ssoId: SsoId?,
        accountTokens: AccountTokens,
        proxyCredentials: ProxyCredentials?,
        managedBy: SsoManagedBy?,
        password: String?,
    ): LoginResult {
        // Store account in the global session
        val userId = withContext(Dispatchers.Default) {
            coreLogic.getGlobalScope().addAuthenticatedAccount(
                session = StoreSessionParam(
                    serverConfigId = serverConfigId,
                    ssoId = ssoId,
                    accountTokens = accountTokens,
                    proxyCredentials = proxyCredentials,
                    isPersistentWebSocketEnabled = false,
                    managedBy = managedBy,
                ),
                replace = false
            )
        }.let {
            when (it) {
                is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                is AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists ->
                    accountTokens.userId

                is AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation -> {
                    return LoginResult.Failure.Error(LoginError.SESSION_COULD_NOT_BE_SAVED)
                }

                is AddAuthenticatedUserUseCase.Result.Failure.Generic -> {
                    return LoginResult.Failure.Error(LoginError.SESSION_COULD_NOT_BE_SAVED)
                }
            }
        }

        // Register cryptographic client
        val clientResult = withContext(Dispatchers.Default) {
            coreLogic.getSessionScope(userId).client.getOrRegister(
                RegisterClientParam(
                    password = password,
                    capabilities = null
                )
            )
        }
        when (clientResult) {
            is RegisterClientResult.Success,
            is RegisterClientResult.E2EICertificateRequired -> {
                return LoginResult.Success(SessionImpl(this, coreLogic.getSessionScope(userId), backendConfig).also {
                    startLifecycleManagement(it)
                    emitAuthEvent(AuthEvent.LoggedIn(it))
                })
            }

            is RegisterClientResult.Failure.TooManyClients -> {
                val resolver = DeviceLimitResolverImpl(
                    sessionScope = coreLogic.getSessionScope(userId),
                    userId = userId,
                    password = password,
                    backendConfig = backendConfig,
                    natrium = this,
                    platform = platform,
                )
                return LoginResult.Failure.TooManyDevices(resolver)
            }

            is RegisterClientResult.Failure ->
                return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
        }
    }
}