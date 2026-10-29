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

package schwarz.digits.natrium.chat.protocol

import schwarz.digits.natrium.chat.ChatMessage
import schwarz.digits.natrium.chat.InvalidResponseReason
import schwarz.digits.natrium.chat.MessageStatus
import schwarz.digits.natrium.chat.MessageValue
import schwarz.digits.natrium.chat.Request
import schwarz.digits.natrium.chat.Response

internal object ResponseRules {

    /**
     * Checks that [response] is a valid answer to [request]. [pendingAttachments] counts files that are
     * uploaded before sending and therefore not yet part of the response's attachment ids.
     */
    fun validate(request: Request, response: Response, pendingAttachments: Int = 0): InvalidResponseReason? {
        if (response is Response.FreeText) return null
        return when (request) {
            is Request.SingleSelect -> when {
                response !is Response.SingleSelect -> InvalidResponseReason.TYPE_MISMATCH
                request.options.none { it.id == response.selectedOptionId } -> InvalidResponseReason.UNKNOWN_OPTION
                else -> null
            }
            is Request.MultiSelect -> when {
                response !is Response.MultiSelect -> InvalidResponseReason.TYPE_MISMATCH
                response.selectedOptionIds.isEmpty() -> InvalidResponseReason.EMPTY_SELECTION
                response.selectedOptionIds.toSet().size != response.selectedOptionIds.size ||
                    response.selectedOptionIds.any { id -> request.options.none { it.id == id } } ->
                    InvalidResponseReason.UNKNOWN_OPTION
                else -> null
            }
            is Request.Asset -> when {
                response !is Response.Asset -> InvalidResponseReason.TYPE_MISMATCH
                response.attachmentMessageIds.size + pendingAttachments == 0 -> InvalidResponseReason.MISSING_ATTACHMENTS
                else -> null
            }
            is Request.Appointment ->
                if (response !is Response.Appointment) InvalidResponseReason.TYPE_MISMATCH else null
        }
    }

    /** Id of the first response to [requestMessageId] in [messages]; failed sends do not count. */
    fun firstResponseId(requestMessageId: String, messages: List<ChatMessage>): String? =
        messages.firstOrNull { it.isResponseTo(requestMessageId) }?.id

    /** Sets [ChatMessage.answeredByMessageId] on every request in [messages] (sorted by timestamp). */
    fun withAnsweredState(messages: List<ChatMessage>): List<ChatMessage> {
        val firstResponseByRequest = mutableMapOf<String, String>()
        messages.forEach { message ->
            val value = message.value
            if (value is MessageValue.ResponseValue && message.countsAsSent()) {
                firstResponseByRequest.getOrPut(value.requestMessageId) { message.id }
            }
        }
        if (firstResponseByRequest.isEmpty()) return messages
        return messages.map { message ->
            if (message.value is MessageValue.RequestValue) {
                message.copy(answeredByMessageId = firstResponseByRequest[message.id])
            } else {
                message
            }
        }
    }

    private fun ChatMessage.isResponseTo(requestMessageId: String): Boolean {
        val value = value
        return value is MessageValue.ResponseValue && value.requestMessageId == requestMessageId && countsAsSent()
    }

    private fun ChatMessage.countsAsSent() =
        status != MessageStatus.FAILED && status != MessageStatus.FAILED_REMOTELY
}
