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

package schwarz.digits.natrium.session

import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.client.SelfClientsResult
import com.wire.kalium.logic.feature.user.DisplayNameUpdateResult
import com.wire.kalium.logic.feature.user.SetUserHandleResult
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.conflate
import schwarz.digits.natrium.AuthEvent
import schwarz.digits.natrium.BackendConfig
import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.NatriumPlatform
import schwarz.digits.natrium.conversation.ConversationManager
import schwarz.digits.natrium.conversation.ConversationManagerImpl
import schwarz.digits.natrium.devices.DeviceInfo
import schwarz.digits.natrium.devices.DeviceManager
import schwarz.digits.natrium.devices.DeviceManagerImpl
import schwarz.digits.natrium.lifecycle.AppLifecycleState
import schwarz.digits.natrium.lifecycle.LifecycleAware
import schwarz.digits.natrium.users.NamedUser
import schwarz.digits.natrium.users.UserId

internal class SessionImpl(
    private val natrium: Natrium,
    private val sessionScope: UserSessionScope,
    private val backendConfig: BackendConfig
) : Session, LifecycleAware {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lifecycleJob: Job? = null
    private var syncJob: Job? = null

    override val conversationManager: ConversationManager = ConversationManagerImpl(sessionScope, scope)
    override val deviceManager: DeviceManager = DeviceManagerImpl(sessionScope, scope)

    override suspend fun sessionInfo(): SessionInfo? {
        val selfUser = sessionScope.users.getSelfUser() ?: return null
        val currentDevice = try {
            when (val result = sessionScope.client.fetchSelfClients()) {
                is SelfClientsResult.Success -> result.clients
                    .firstOrNull { result.currentClientId != null && it.id == result.currentClientId }
                    ?.let { DeviceInfo(it.id.value, it.label, it.model, it.deviceType?.name, true) }
                else -> null
            }
        } catch (_: Exception) { null }

        return SessionInfo(
            user = NamedUser(UserId.fromWire(selfUser.id), selfUser.name),
            handle = selfUser.handle,
            email = selfUser.email,
            backend = backendConfig.name,
            device = currentDevice
        )
    }

    override fun observeSessionInfo(listener: (SessionInfo) -> Unit): Cancellable {
        val job = scope.launch {
            sessionScope.users.observeSelfUser()
                .conflate()
                .collect { selfUser ->
                    listener(
                        SessionInfo(
                            user = NamedUser(UserId.fromWire(selfUser.id), selfUser.name),
                            handle = selfUser.handle,
                            email = selfUser.email,
                            backend = backendConfig.name,
                            device = null
                        )
                    )
                }
        }
        return Cancellable(job)
    }

    override suspend fun updateDisplayName(name: String): UpdateDisplayNameResult {
        return try {
            when (sessionScope.users.updateDisplayName(name)) {
                is DisplayNameUpdateResult.Success -> UpdateDisplayNameResult.Success
                is DisplayNameUpdateResult.Failure -> UpdateDisplayNameResult.Failure.Unknown(
                    "Failed to update display name"
                )
            }
        } catch (e: Exception) {
            UpdateDisplayNameResult.Failure.Unknown(e.message ?: "Failed to update display name", e)
        }
    }

    override suspend fun updateHandle(handle: String): UpdateHandleResult {
        return try {
            when (sessionScope.users.setUserHandle(handle)) {
                is SetUserHandleResult.Success -> UpdateHandleResult.Success
                is SetUserHandleResult.Failure.InvalidHandle -> UpdateHandleResult.Failure.InvalidHandle
                is SetUserHandleResult.Failure.HandleExists -> UpdateHandleResult.Failure.HandleExists
                is SetUserHandleResult.Failure.Generic -> UpdateHandleResult.Failure.Unknown(
                    "Failed to update handle"
                )
            }
        } catch (e: Exception) {
            UpdateHandleResult.Failure.Unknown(e.message ?: "Failed to update handle", e)
        }
    }

    override suspend fun updateEmail(email: String): UpdateEmailResult {
        return try {
            when (sessionScope.users.updateEmail(email)) {
                is UpdateEmailUseCase.Result.Success -> UpdateEmailResult.Success
                is UpdateEmailUseCase.Result.Failure.InvalidEmail -> UpdateEmailResult.Failure.InvalidEmail
                is UpdateEmailUseCase.Result.Failure.EmailAlreadyInUse -> UpdateEmailResult.Failure.EmailAlreadyInUse
                is UpdateEmailUseCase.Result.Failure.GenericFailure -> UpdateEmailResult.Failure.Unknown(
                    "Failed to update email"
                )
            }
        } catch (e: Exception) {
            UpdateEmailResult.Failure.Unknown(e.message ?: "Failed to update email", e)
        }
    }

    override suspend fun logout(): LogoutResult {
        return try {
            scope.cancel()
            sessionScope.logout(LogoutReason.SELF_SOFT_LOGOUT)
            natrium.emitAuthEvent(AuthEvent.LoggedOut)
            LogoutResult.Success
        } catch (e: Exception) {
            LogoutResult.Failure.Unknown(e.message ?: "Logout failed", e)
        }
    }

    internal fun startLifecycleManagement(platform: NatriumPlatform) {
        onActive()
        lifecycleJob = scope.launch {
            platform.observeLifecycle().collect { state ->
                when (state) {
                    AppLifecycleState.ACTIVE -> onActive()
                    AppLifecycleState.INACTIVE -> onInactive()
                }
            }
        }

    }

    override fun onActive() {
        syncJob = scope.launch(Dispatchers.Default) {
            sessionScope.syncExecutor.request {
                awaitCancellation()
            }
        }
    }

    override fun onInactive() {
        syncJob?.cancel()
        syncJob = null
    }

}
