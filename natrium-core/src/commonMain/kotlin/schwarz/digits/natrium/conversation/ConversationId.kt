/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
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
