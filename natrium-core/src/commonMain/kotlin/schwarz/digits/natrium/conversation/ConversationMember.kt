/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.conversation

import schwarz.digits.natrium.users.UserId

/**
 * A participant in a Conversation.
 */
data class ConversationMember(
    val userId: UserId,
    val name: String,
)
