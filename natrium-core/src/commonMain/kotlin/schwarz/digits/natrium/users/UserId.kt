/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.users

import com.wire.kalium.logic.data.id.QualifiedID

data class UserId(val value: String, val domain: String) {

    override fun toString(): String = "$value@$domain"

    companion object {
        fun fromString(s: String): UserId {
            val parts = s.split("@", limit = 2)
            return UserId(parts[0], parts.getOrElse(1) { "" })
        }
        internal fun fromWire(id: com.wire.kalium.logic.data.user.UserId) = UserId(id.value, id.domain)
    }

    internal fun toWire() = QualifiedID(value, domain)
}