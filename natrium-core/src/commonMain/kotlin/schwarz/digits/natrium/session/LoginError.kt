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