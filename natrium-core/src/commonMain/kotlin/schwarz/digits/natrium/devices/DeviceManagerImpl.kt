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
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.SelfClientsResult
import kotlinx.coroutines.CoroutineScope

internal class DeviceManagerImpl(
    private val scope: UserSessionScope,
    private val coroutineScope: CoroutineScope
): DeviceManager {
    override suspend fun list(): ListDevicesResult {
        return try {
            when (val result = scope.client.fetchSelfClients()) {
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

    override suspend fun remove(deviceId: String, password: String?): RemoveDeviceResult {
        return try {
            val result = scope.client.deleteClient(DeleteClientParam(password, ClientId(deviceId)))
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

}