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

package schwarz.digits.natrium.conversation

import com.wire.kalium.logic.data.id.QualifiedID

/**
 * Qualified identifier for a Conversation (maps internally to Kalium's ConversationId).
 *
 * @param value The unique ID part (UUID).
 * @param domain The backend domain (e.g. "wire.com", "staging.zinfra.io").
 */
data class ConversationId(val value: String, val domain: String) {

    override fun toString(): String = "$value@$domain"

    companion object {
        fun fromString(s: String): ConversationId {
            val parts = s.split("@", limit = 2)
            return ConversationId(parts[0], parts.getOrElse(1) { "" })
        }
    }

    internal fun toQualifiedId() = QualifiedID(value, domain)
}
