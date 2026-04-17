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