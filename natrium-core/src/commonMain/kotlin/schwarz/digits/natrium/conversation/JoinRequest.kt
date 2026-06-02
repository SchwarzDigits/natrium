/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.conversation

import kotlinx.datetime.Instant
import schwarz.digits.natrium.users.NamedUser

data class JoinRequest(
    val id: JoinRequestId,
    val conversationId: ConversationId,
    val requester: NamedUser,
    val status: JoinRequestStatus,
    val message: String?,
    val createdAt: Instant,
)
