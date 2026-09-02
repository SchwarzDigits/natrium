/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the EUPL v. 1.2 only.
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the Licence for the specific language governing
 * permissions and limitations under the Licence.
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