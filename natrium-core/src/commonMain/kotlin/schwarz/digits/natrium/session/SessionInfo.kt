/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.session

import schwarz.digits.natrium.devices.DeviceInfo
import schwarz.digits.natrium.users.NamedUser

data class SessionInfo (
    val user: NamedUser,
    val handle: String?,
    val email: String?,
    val backend: String,
    val device: DeviceInfo?
)