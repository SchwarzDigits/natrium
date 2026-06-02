/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.devices

sealed class ListDevicesResult {
    data class Success(val devices: List<DeviceInfo>) : ListDevicesResult()
    sealed class Failure : ListDevicesResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class RemoveDeviceResult {
    data object Success : RemoveDeviceResult()
    sealed class Failure : RemoveDeviceResult() {
        data object NotLoggedIn : Failure()
        data object PasswordRequired : Failure()
        data class InvalidCredentials(val message: String) : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}
