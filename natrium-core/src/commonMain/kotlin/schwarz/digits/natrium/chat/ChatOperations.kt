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