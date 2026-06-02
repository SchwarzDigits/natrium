/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.chat

import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.users.NamedUser


interface ChatOperations {
    suspend fun sendMessage(value: MessageValue): SendMessageResult
    fun observeMessages(listener: (Collection<ChatMessage>) -> Unit): Cancellable
    suspend fun getHistory(limit: Int, offset: Int): ChatHistoryResult
    suspend fun sendTypingStarted()
    suspend fun sendTypingStopped()
    fun observeTyping(listener: (Collection<NamedUser>) -> Unit): Cancellable
    suspend fun downloadFile(messageId: String): FileDownloadResult
    suspend fun toggleReaction(messageId: String, emoji: String): ToggleReactionResult
    suspend fun sendReply(value: MessageValue, quotedMessageId: String): SendMessageResult
}