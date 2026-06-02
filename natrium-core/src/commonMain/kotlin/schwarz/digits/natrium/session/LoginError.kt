/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.session

enum class LoginError {
    SERVER_VERSION_NOT_SUPPORTED,
    APP_UPDATE_REQUIRED,
    CONNECTION_ERROR,
    EMAIL_OR_PASSWORD_WRONG,
    SECOND_FA_CODE_REQUIRED,
    INVALID_2FA_CODE,
    ACCOUNT_LOCKED,
    ACCOUNT_NOT_ACTIVATED,
    LOGIN_FAILED,
    SESSION_COULD_NOT_BE_SAVED,
    CLIENT_REGISTRATION_FAILED
}