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