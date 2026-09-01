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
