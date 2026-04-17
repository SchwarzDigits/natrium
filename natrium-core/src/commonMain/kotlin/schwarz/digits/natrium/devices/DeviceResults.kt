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
