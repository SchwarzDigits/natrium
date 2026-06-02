/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.devices

import com.wire.kalium.logic.data.client.DeleteClientParam
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.SelfClientsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import schwarz.digits.natrium.BackendConfig
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.NatriumPlatform
import schwarz.digits.natrium.session.LoginResult
import schwarz.digits.natrium.session.SessionImpl

internal class DeviceLimitResolverImpl(
    private val sessionScope: UserSessionScope,
    private val userId: UserId,
    private val password: String?,
    private val backendConfig: BackendConfig,
    private val natrium: Natrium,
    private val platform: NatriumPlatform,
) : DeviceLimitResolver {

    override suspend fun listDevices(): ListDevicesResult {
        return try {
            when (val result = sessionScope.client.fetchSelfClients()) {
                is SelfClientsResult.Success -> {
                    val currentId = result.currentClientId
                    val devices = result.clients.map { client ->
                        DeviceInfo(
                            id = client.id.value,
                            label = client.label,
                            model = client.model,
                            deviceType = client.deviceType?.name,
                            isCurrentDevice = currentId != null && client.id == currentId,
                        )
                    }
                    ListDevicesResult.Success(devices)
                }
                is SelfClientsResult.Failure.Generic ->
                    ListDevicesResult.Failure.Unknown("Failed to load devices: ${result.genericFailure}")
            }
        } catch (e: Exception) {
            ListDevicesResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun removeDevice(deviceId: String, password: String?): RemoveDeviceResult {
        return try {
            val result = sessionScope.client.deleteClient(
                DeleteClientParam(password, ClientId(deviceId))
            )
            when (result) {
                is DeleteClientResult.Success -> RemoveDeviceResult.Success
                is DeleteClientResult.Failure.InvalidCredentials ->
                    RemoveDeviceResult.Failure.InvalidCredentials("Invalid credentials")
                is DeleteClientResult.Failure.PasswordAuthRequired ->
                    RemoveDeviceResult.Failure.PasswordRequired
                is DeleteClientResult.Failure.Generic ->
                    RemoveDeviceResult.Failure.Unknown("Failed to remove device: ${result.genericFailure}")
            }
        } catch (e: Exception) {
            RemoveDeviceResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun retry(): LoginResult {
        val clientResult = withContext(Dispatchers.Default) {
            natrium.coreLogic.getSessionScope(userId).client.getOrRegister(
                RegisterClientParam(
                    password = password,
                    capabilities = null
                )
            )
        }
        return when (clientResult) {
            is RegisterClientResult.Success,
            is RegisterClientResult.E2EICertificateRequired -> {
                val session = SessionImpl(natrium, natrium.coreLogic.getSessionScope(userId), backendConfig)
                session.startLifecycleManagement(platform)
                LoginResult.Success(session)
            }
            is RegisterClientResult.Failure.TooManyClients ->
                LoginResult.Failure.TooManyDevices(this)
            is RegisterClientResult.Failure ->
                LoginResult.Failure.Error(schwarz.digits.natrium.session.LoginError.CLIENT_REGISTRATION_FAILED)
        }
    }
}
