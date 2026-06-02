/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium

import schwarz.digits.natrium.session.Session

sealed class AuthEvent {
    data class LoggedIn(val session: Session) : AuthEvent()
    data object LoggedOut : AuthEvent()
}
