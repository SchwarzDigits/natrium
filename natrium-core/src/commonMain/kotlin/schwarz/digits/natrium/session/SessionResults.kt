/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
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
