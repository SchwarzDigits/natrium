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

import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageAssetStatus
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.upload.AssetUploadParams
import com.wire.kalium.logic.feature.asset.upload.ScheduleNewAssetMessageResult
import com.wire.kalium.logic.feature.message.MessageOperationResult
import com.wire.kalium.logic.feature.message.ToggleReactionResult as KaliumToggleReactionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.conversation.ConversationId
import schwarz.digits.natrium.file.FileLink
import schwarz.digits.natrium.users.NamedUser
import schwarz.digits.natrium.users.UserId

internal class ChatOperationsImpl(
    private val conversationId: ConversationId,
    private val scope: UserSessionScope,
    private val coroutineScope: CoroutineScope
) : ChatOperations {

    private suspend fun sendTextMessage(text: String, quotedMessageId: String? = null): SendMessageResult {
        return try {
            when (val result = scope.messages.sendTextMessage(
                conversationId.toQualifiedId(), text, quotedMessageId = quotedMessageId
            )) {
                is MessageOperationResult.Success -> SendMessageResult.Success
                is MessageOperationResult.Failure ->
                    SendMessageResult.Failure.Unknown("Failed to send message: ${result.error}")
            }
        } catch (e: Exception) {
            SendMessageResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    private suspend fun sendFileMessage(value: MessageValue.FileValue): SendMessageResult {
        return try {
            val fileLink = value.fileLink

            val isImage = fileLink.mimeType.startsWith("image/")
            val sizeLimit = scope.users.getAssetSizeLimit(isImage)
            if (fileLink.dataSize > sizeLimit) {
                return SendMessageResult.Failure.FileTooLarge(fileLink.dataSize, sizeLimit)
            }

            when (val result = scope.messages.sendAssetMessage(
                AssetUploadParams(
                    conversationId = conversationId.toQualifiedId(),
                    assetDataPath = fileLink.dataPath,
                    assetDataSize = fileLink.dataSize,
                    assetName = fileLink.fileName,
                    assetMimeType = fileLink.mimeType,
                    assetWidth = null,
                    assetHeight = null,
                    audioLengthInMs = 0L,
                    audioNormalizedLoudness = null,
                )
            )) {
                is ScheduleNewAssetMessageResult.Success -> SendMessageResult.Success
                is ScheduleNewAssetMessageResult.Failure.DisabledByTeam ->
                    SendMessageResult.Failure.DisabledByTeam

                is ScheduleNewAssetMessageResult.Failure.RestrictedFileType ->
                    SendMessageResult.Failure.RestrictedFileType

                is ScheduleNewAssetMessageResult.Failure.Generic ->
                    SendMessageResult.Failure.Unknown("Failed to send file: ${result.coreFailure}")
            }

        } catch (e: Exception) {
            SendMessageResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun sendMessage(value: MessageValue): SendMessageResult {
        return when (value) {
            is MessageValue.TextValue -> sendTextMessage(value.value)
            is MessageValue.FileValue -> sendFileMessage(value)
            is MessageValue.LocationValue,
            is MessageValue.KnockValue,
            is MessageValue.SystemValue ->
                SendMessageResult.Failure.Unknown("Sending this message type is not supported")
        }
    }

    override fun observeMessages(listener: (Collection<ChatMessage>) -> Unit): Cancellable {
        val job = coroutineScope.launch {
            val messagesFlow = scope.messages.getRecentMessages(
                conversationId = conversationId.toQualifiedId(),
                visibility = listOf(Message.Visibility.VISIBLE),
            )
            val assetStatusesFlow = scope.messages.observeAssetStatuses(conversationId.toQualifiedId())

            combine(messagesFlow, assetStatusesFlow) { messages, statuses ->
                filterAndMapChatMessages(messages, statuses)
            }.conflate().collect { listener(it) }
        }
        return Cancellable(job)
    }

    override suspend fun getHistory(
        limit: Int,
        offset: Int
    ): ChatHistoryResult {
        return try {
            val messageList = scope.messages.getRecentMessages(
                conversationId = conversationId.toQualifiedId(),
                limit = limit,
                offset = offset,
                visibility = listOf(Message.Visibility.VISIBLE),
            ).first()
            val assetStatuses = scope.messages
                .observeAssetStatuses(conversationId.toQualifiedId())
                .first()
            ChatHistoryResult.Success(filterAndMapChatMessages(messageList, assetStatuses))
        } catch (e: Exception) {
            ChatHistoryResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun sendTypingStarted() {
        try {
            scope.conversations.sendTypingEvent(
                conversationId.toQualifiedId(),
                Conversation.TypingIndicatorMode.STARTED,
            )
        } catch (e: Exception) {
            //TODO
        }
    }

    override suspend fun sendTypingStopped() {
        try {
            scope.conversations.sendTypingEvent(
                conversationId.toQualifiedId(),
                Conversation.TypingIndicatorMode.STOPPED,
            )
        } catch (e: Exception) {
            // TODO
        }
    }

    override fun observeTyping(listener: (Collection<NamedUser>) -> Unit): Cancellable {
        val job = coroutineScope.launch {
            scope.conversations
                .observeUsersTyping(conversationId = conversationId.toQualifiedId())
                .conflate()
                .collect {
                    listener(it.map { user ->
                        NamedUser(
                            UserId.fromWire(user.userId),
                            user.userName
                        )
                    })
                }
        }
        return Cancellable(job)
    }

    override suspend fun downloadFile(messageId: String): FileDownloadResult {
        return try {
            val result = scope.messages.getAssetMessage(
                conversationId = conversationId.toQualifiedId(),
                messageId = messageId,
            ).await()

            when (result) {
                is MessageAssetResult.Success -> FileDownloadResult.Success(result.decodedAssetPath)
                is MessageAssetResult.Failure -> FileDownloadResult.Failure.Unknown(
                    message = result.coreFailure.toString()
                )
            }
        } catch (e: Exception) {
            FileDownloadResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun toggleReaction(messageId: String, emoji: String): ToggleReactionResult {
        return try {
            when (scope.messages.toggleReaction(conversationId.toQualifiedId(), messageId, emoji)) {
                is KaliumToggleReactionResult.Success ->
                    ToggleReactionResult.Success

                is KaliumToggleReactionResult.Failure ->
                    ToggleReactionResult.Failure.Unknown("Failed to toggle reaction")
            }
        } catch (e: Exception) {
            ToggleReactionResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun sendReply(value: MessageValue, quotedMessageId: String): SendMessageResult {
        return when (value) {
            is MessageValue.TextValue -> sendTextMessage(value.value, quotedMessageId)
            is MessageValue.FileValue -> sendFileMessage(value) // quotedMessageId TODO when Kalium supports it
            is MessageValue.LocationValue,
            is MessageValue.KnockValue,
            is MessageValue.SystemValue ->
                SendMessageResult.Failure.Unknown("Sending this message type is not supported")
        }
    }

    //---

    private fun filterAndMapChatMessages(
        messages: List<Message>,
        assetStatuses: Map<String, MessageAssetStatus>,
    ): List<ChatMessage> {
        val regular = messages
            .filterIsInstance<Message.Regular>()
            .filter {
                it.content is MessageContent.Text
                    || it.content is MessageContent.Asset
                    || it.content is MessageContent.Location
                    || it.content is MessageContent.Knock
                    || it.content is MessageContent.Multipart
            }
            .mapNotNull { message ->
                try {
                    mapRegularMessage(message, assetStatuses)
                } catch (e: Exception) {
                    println("NATRIUM: Failed to map regular message ${message.id}: $e")
                    e.printStackTrace()
                    null
                }
            }

        val system = messages
            .filterIsInstance<Message.System>()
            .mapNotNull { message ->
                try {
                    mapSystemMessage(message)
                } catch (e: Exception) {
                    println("NATRIUM: Failed to map system message ${message.id}: $e")
                    e.printStackTrace()
                    null
                }
            }

        return (regular + system).sortedBy { it.timestamp }
    }

    private fun mapRegularMessage(
        message: Message.Regular,
        assetStatuses: Map<String, MessageAssetStatus>,
    ): ChatMessage? {
        val isEdited = message.editStatus is Message.EditStatus.Edited
        val sender = NamedUser(UserId.fromWire(message.senderUserId), message.senderUserName)
        val reactions = mapReactions(message.reactions)
        return when (message.content) {
            is MessageContent.Asset ->
                ChatMessage(
                    id = message.id,
                    sender = sender,
                    value = MessageValue.FileValue(
                        mapToFileLink(message.id, message.content as MessageContent.Asset, message.date)
                    ),
                    timestamp = message.date,
                    status = mapStatus(message.status),
                    isSelf = message.isSelfMessage,
                    fileTransferStatus = assetStatuses[message.id]?.let {
                        mapTransferStatus(it.transferStatus)
                    },
                    isEdited = isEdited,
                    reactions = reactions,
                )
            is MessageContent.Text -> {
                val textContent = message.content as MessageContent.Text
                ChatMessage(
                    id = message.id,
                    sender = sender,
                    value = MessageValue.TextValue(textContent.value),
                    timestamp = message.date,
                    status = mapStatus(message.status),
                    isSelf = message.isSelfMessage,
                    isEdited = isEdited,
                    reactions = reactions,
                    quotedMessage = mapQuotedMessage(textContent.quotedMessageDetails),
                )
            }
            is MessageContent.Location -> {
                val loc = message.content as MessageContent.Location
                ChatMessage(
                    id = message.id,
                    sender = sender,
                    value = MessageValue.LocationValue(loc.latitude, loc.longitude, loc.name),
                    timestamp = message.date,
                    status = mapStatus(message.status),
                    isSelf = message.isSelfMessage,
                    isEdited = isEdited,
                    reactions = reactions,
                )
            }
            is MessageContent.Knock -> {
                ChatMessage(
                    id = message.id,
                    sender = sender,
                    value = MessageValue.KnockValue((message.content as MessageContent.Knock).hotKnock),
                    timestamp = message.date,
                    status = mapStatus(message.status),
                    isSelf = message.isSelfMessage,
                    reactions = reactions,
                )
            }
            is MessageContent.Multipart -> {
                val multipart = message.content as MessageContent.Multipart
                ChatMessage(
                    id = message.id,
                    sender = sender,
                    value = MessageValue.TextValue(multipart.value ?: ""),
                    timestamp = message.date,
                    status = mapStatus(message.status),
                    isSelf = message.isSelfMessage,
                    isEdited = isEdited,
                    reactions = reactions,
                    quotedMessage = mapQuotedMessage(multipart.quotedMessageDetails),
                )
            }
            else -> null
        }
    }

    private fun mapSystemMessage(message: Message.System): ChatMessage? {
        val (event, text) = when (val content = message.content) {
            is MessageContent.MemberChange.Added ->
                SystemEvent.MEMBER_ADDED to "joined"
            is MessageContent.MemberChange.Removed ->
                SystemEvent.MEMBER_REMOVED to "left"
            is MessageContent.ConversationRenamed ->
                SystemEvent.CONVERSATION_RENAMED to "renamed to \"${content.conversationName}\""
            is MessageContent.MissedCall ->
                SystemEvent.MISSED_CALL to "Missed call"
            else -> return null
        }
        return ChatMessage(
            id = message.id,
            sender = NamedUser(UserId.fromWire(message.senderUserId), message.senderUserName),
            value = MessageValue.SystemValue(event),
            timestamp = message.date,
            status = MessageStatus.DELIVERED,
            isSelf = false,
            systemText = text,
        )
    }

    private fun mapReactions(reactions: Message.Reactions): Map<String, ReactionInfo> {
        if (reactions == Message.Reactions.EMPTY) return emptyMap()
        return reactions.reactions.mapValues { (_, data) ->
            ReactionInfo(count = data.count, isSelf = data.isSelf)
        }
    }

    private fun mapQuotedMessage(details: MessageContent.QuotedMessageDetails?): QuotedMessage? {
        if (details == null) return null
        val previewText = when (val content = details.quotedContent) {
            is MessageContent.QuotedMessageDetails.Text -> content.value
            is MessageContent.QuotedMessageDetails.Asset -> content.assetName ?: content.assetMimeType
            is MessageContent.QuotedMessageDetails.Location -> content.locationName
            is MessageContent.QuotedMessageDetails.Multipart -> content.text
            MessageContent.QuotedMessageDetails.Deleted -> null
            MessageContent.QuotedMessageDetails.Invalid -> null
        }
        return QuotedMessage(
            messageId = details.messageId,
            senderName = details.senderName,
            previewText = previewText,
        )
    }

    private fun mapToFileLink(messageId: String, asset: MessageContent.Asset, messageDate: Instant): FileLink {
        val content = asset.value
        return object : FileLink {
            override val id: String = content.remoteData.assetId
            override val mimeType: String = content.mimeType
            override val dataPath: Path = content.localData?.assetDataPath?.toPath()
                ?: (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "natrium-asset-${content.remoteData.assetId}")
            override val dataSize: Long = content.sizeInBytes
            override val fileName: String = content.name ?: "unnamed"
            override val uploadedAt: Instant = messageDate
        }
    }

    private fun mapStatus(status: Message.Status): MessageStatus = when (status) {
        is Message.Status.Pending -> MessageStatus.PENDING
        is Message.Status.Sent -> MessageStatus.SENT
        is Message.Status.Delivered -> MessageStatus.DELIVERED
        is Message.Status.Read -> MessageStatus.READ
        is Message.Status.Failed -> MessageStatus.FAILED
        is Message.Status.FailedRemotely -> MessageStatus.FAILED_REMOTELY
    }

    private fun mapTransferStatus(status: AssetTransferStatus): FileTransferStatus = when (status) {
        AssetTransferStatus.UPLOAD_IN_PROGRESS -> FileTransferStatus.UPLOADING
        AssetTransferStatus.FAILED_UPLOAD -> FileTransferStatus.UPLOAD_FAILED
        AssetTransferStatus.UPLOADED -> FileTransferStatus.UPLOADED
        AssetTransferStatus.NOT_DOWNLOADED -> FileTransferStatus.NOT_DOWNLOADED
        AssetTransferStatus.DOWNLOAD_IN_PROGRESS -> FileTransferStatus.DOWNLOADING
        AssetTransferStatus.FAILED_DOWNLOAD, AssetTransferStatus.NOT_FOUND -> FileTransferStatus.DOWNLOAD_FAILED
        AssetTransferStatus.SAVED_INTERNALLY, AssetTransferStatus.SAVED_EXTERNALLY -> FileTransferStatus.DOWNLOADED
    }
}