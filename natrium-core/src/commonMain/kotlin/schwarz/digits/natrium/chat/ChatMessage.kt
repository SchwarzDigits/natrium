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
