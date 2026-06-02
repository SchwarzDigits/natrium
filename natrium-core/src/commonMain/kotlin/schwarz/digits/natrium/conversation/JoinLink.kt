/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.conversation

import kotlin.jvm.JvmInline

/**
 * A guest room link URL for a conversation. Pass to [ConversationManager.joinConversation] to join.
 */
@JvmInline
value class JoinLink(val value: String)
