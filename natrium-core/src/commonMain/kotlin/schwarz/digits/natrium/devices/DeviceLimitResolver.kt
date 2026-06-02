/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.devices

import schwarz.digits.natrium.session.LoginResult

interface DeviceLimitResolver {
    suspend fun listDevices(): ListDevicesResult
    suspend fun removeDevice(deviceId: String, password: String? = null): RemoveDeviceResult
    suspend fun retry(): LoginResult
}
