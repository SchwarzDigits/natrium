/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.chat


import kotlinx.datetime.Instant
import schwarz.digits.natrium.file.FileLink
import schwarz.digits.natrium.users.NamedUser

data class ReactionInfo(
    val count: Int,
    val isSelf: Boolean,
)

data class QuotedMessage(
    val messageId: String,
    val senderName: String?,
    val previewText: String?,
)

data class ChatMessage(
    val id: String,
    val sender: NamedUser,
    val value: MessageValue,
    val timestamp: Instant,
    val status: MessageStatus,
    val isSelf: Boolean,
    val fileTransferStatus: FileTransferStatus? = null,
    val isEdited: Boolean = false,
    val systemText: String? = null,
    val reactions: Map<String, ReactionInfo> = emptyMap(),
    val quotedMessage: QuotedMessage? = null,
)

sealed class MessageValue {
    data class TextValue(val value: String) : MessageValue()
    data class FileValue(val fileLink: FileLink) : MessageValue()
    data class LocationValue(
        val latitude: Float,
        val longitude: Float,
        val name: String? = null,
    ) : MessageValue()
    data class KnockValue(val hotKnock: Boolean) : MessageValue()
    data class SystemValue(val event: SystemEvent) : MessageValue()
}

enum class SystemEvent {
    MEMBER_ADDED,
    MEMBER_REMOVED,
    CONVERSATION_RENAMED,
    MISSED_CALL,
    OTHER,
}

enum class MessageStatus { PENDING, SENT, DELIVERED, READ, FAILED, FAILED_REMOTELY }

enum class FileTransferStatus {
    UPLOADING, UPLOAD_FAILED, UPLOADED,
    NOT_DOWNLOADED, DOWNLOADING, DOWNLOAD_FAILED, DOWNLOADED,
}
