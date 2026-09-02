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