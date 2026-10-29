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

package schwarz.digits.natrium.chat

import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.file.FileLink
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

    /**
     * Answers the request with id [requestMessageId]. [attachments] are uploaded as file messages first
     * and their message ids appended to the response; if an upload fails, the response is not sent.
     */
    suspend fun sendResponse(
        requestMessageId: String,
        response: Response,
        attachments: List<FileLink> = emptyList(),
    ): SendMessageResult

    /** Sends free text with [attachments], uploaded as file messages before the text is sent. */
    suspend fun sendText(text: String, attachments: List<FileLink> = emptyList()): SendMessageResult
}