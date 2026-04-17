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

import schwarz.digits.natrium.devices.DeviceLimitResolver

sealed class LoginResult {
    data class Success(val session: Session) : LoginResult()
    sealed class Failure : LoginResult() {
        data class Error(val reason: LoginError) : Failure()
        data class TooManyDevices(val resolver: DeviceLimitResolver) : Failure()
    }
}

sealed class LogoutResult {
    data object Success : LogoutResult()
    sealed class Failure : LogoutResult() {
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class UpdateDisplayNameResult {
    data object Success : UpdateDisplayNameResult()
    sealed class Failure : UpdateDisplayNameResult() {
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class SSOLoginResult {
    data class Success(val authorizationUrl: String) : SSOLoginResult()
    sealed class Failure : SSOLoginResult() {
        data class Error(val reason: SSOLoginError) : Failure()
    }
}

sealed class UpdateHandleResult {
    data object Success : UpdateHandleResult()
    sealed class Failure : UpdateHandleResult() {
        data object InvalidHandle : Failure()
        data object HandleExists : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class UpdateEmailResult {
    data object Success : UpdateEmailResult()
    sealed class Failure : UpdateEmailResult() {
        data object InvalidEmail : Failure()
        data object EmailAlreadyInUse : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}
enum class SSOLoginError {
    SSO_NOT_AVAILABLE,
    INVALID_CODE,
    INVALID_CODE_FORMAT,
    SERVER_VERSION_NOT_SUPPORTED,
    APP_UPDATE_REQUIRED,
    CONNECTION_ERROR,
}
