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

import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageAssetStatus
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.upload.AssetUploadParams
import com.wire.kalium.logic.feature.asset.upload.ScheduleNewAssetMessageResult
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
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
import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.chat.protocol.MessageProtocol
import schwarz.digits.natrium.chat.protocol.ResponseRules
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

    private suspend fun sendFileMessage(value: MessageValue.FileValue): SendMessageResult =
        when (val result = uploadFile(value.fileLink)) {
            is UploadResult.Success -> SendMessageResult.Success
            is UploadResult.Failure -> result.failure
        }

    private suspend fun uploadFile(fileLink: FileLink): UploadResult {
        return try {
            val isImage = fileLink.mimeType.startsWith("image/")
            val sizeLimit = scope.users.getAssetSizeLimit(isImage)
            if (fileLink.dataSize > sizeLimit) {
                return UploadResult.Failure(SendMessageResult.Failure.FileTooLarge(fileLink.dataSize, sizeLimit))
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
                is ScheduleNewAssetMessageResult.Success -> UploadResult.Success(result.messageId)
                is ScheduleNewAssetMessageResult.Failure.DisabledByTeam ->
                    UploadResult.Failure(SendMessageResult.Failure.DisabledByTeam)

                is ScheduleNewAssetMessageResult.Failure.RestrictedFileType ->
                    UploadResult.Failure(SendMessageResult.Failure.RestrictedFileType)

                is ScheduleNewAssetMessageResult.Failure.Generic ->
                    UploadResult.Failure(
                        SendMessageResult.Failure.Unknown("Failed to send file: ${result.coreFailure}")
                    )
            }

        } catch (e: Exception) {
            UploadResult.Failure(SendMessageResult.Failure.Unknown(e.message ?: "Unknown error", e))
        }
    }

    /** Uploads all [attachments] in order; stops at the first failure. */
    private suspend fun uploadAll(attachments: List<FileLink>): UploadAllResult {
        val ids = attachments.map { fileLink ->
            when (val result = uploadFile(fileLink)) {
                is UploadResult.Success -> result.messageId
                is UploadResult.Failure -> return UploadAllResult.Failure(result.failure)
            }
        }
        return UploadAllResult.Success(ids)
    }

    private suspend fun sendProtocolMessage(value: MessageValue, quotedMessageId: String? = null): SendMessageResult {
        val text = MessageProtocol.encode(value)
            ?: return SendMessageResult.Failure.Unknown("Sending this message type is not supported")
        return sendTextMessage(text, quotedMessageId)
    }

    override suspend fun sendMessage(value: MessageValue): SendMessageResult {
        return when (value) {
            is MessageValue.FileValue -> sendFileMessage(value)
            is MessageValue.ResponseValue -> sendResponse(value.requestMessageId, value.response)
            else -> sendProtocolMessage(value)
        }
    }

    override suspend fun sendRequest(request: Request, attachments: List<FileLink>): SendMessageResult {
        val ids = when (val upload = uploadAll(attachments)) {
            is UploadAllResult.Success -> upload.messageIds
            is UploadAllResult.Failure -> return upload.failure
        }
        return sendProtocolMessage(MessageValue.RequestValue(request, ids))
    }

    override suspend fun sendText(text: String, attachments: List<FileLink>): SendMessageResult {
        val ids = when (val upload = uploadAll(attachments)) {
            is UploadAllResult.Success -> upload.messageIds
            is UploadAllResult.Failure -> return upload.failure
        }
        return sendProtocolMessage(MessageValue.TextValue(text, ids))
    }

    override suspend fun sendResponse(
        requestMessageId: String,
        response: Response,
        attachments: List<FileLink>,
    ): SendMessageResult {
        val lookup = try {
            lookupRequest(requestMessageId)
        } catch (e: Exception) {
            return SendMessageResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
        val request = when (lookup) {
            is RequestLookup.Found -> lookup.request
            is RequestLookup.Invalid -> return SendMessageResult.Failure.InvalidResponse(lookup.reason)
        }
        ResponseRules.validate(request, response, pendingAttachments = attachments.size)?.let {
            return SendMessageResult.Failure.InvalidResponse(it)
        }

        val uploadedIds = when (val upload = uploadAll(attachments)) {
            is UploadAllResult.Success -> upload.messageIds
            is UploadAllResult.Failure -> return upload.failure
        }
        val completeResponse = when (response) {
            is Response.Asset -> response.copy(attachmentMessageIds = response.attachmentMessageIds + uploadedIds)
            is Response.FreeText -> response.copy(attachmentMessageIds = response.attachmentMessageIds + uploadedIds)
            else -> response
        }
        return sendProtocolMessage(
            MessageValue.ResponseValue(requestMessageId, completeResponse),
            quotedMessageId = requestMessageId,
        )
    }

    /**
     * Finds the request in the recent messages (falling back to a direct lookup) and checks that it has no
     * response yet. Responses are always newer than their request, so the recent window contains them.
     */
    private suspend fun lookupRequest(requestMessageId: String): RequestLookup {
        val recent = filterAndMapChatMessages(
            scope.messages.getRecentMessages(
                conversationId = conversationId.toQualifiedId(),
                limit = ANSWER_LOOKUP_LIMIT,
                offset = 0,
                visibility = listOf(Message.Visibility.VISIBLE),
            ).first(),
            emptyMap(),
        )
        val requestMessage = recent.firstOrNull { it.id == requestMessageId }
            ?: when (val result = scope.messages.getMessageById(conversationId.toQualifiedId(), requestMessageId)) {
                is GetMessageByIdUseCase.Result.Success ->
                    (result.message as? Message.Regular)?.let { mapRegularMessage(it, emptyMap()) }
                is GetMessageByIdUseCase.Result.Failure -> null
            }
            ?: return RequestLookup.Invalid(InvalidResponseReason.REQUEST_NOT_FOUND)

        val request = (requestMessage.value as? MessageValue.RequestValue)?.request
            ?: return RequestLookup.Invalid(InvalidResponseReason.NOT_A_REQUEST)
        if (ResponseRules.firstResponseId(requestMessageId, recent) != null) {
            return RequestLookup.Invalid(InvalidResponseReason.ALREADY_ANSWERED)
        }
        return RequestLookup.Found(request)
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
            is MessageValue.FileValue -> sendFileMessage(value) // quotedMessageId TODO when Kalium supports it
            // a response always quotes its request
            is MessageValue.ResponseValue -> sendResponse(value.requestMessageId, value.response)
            else -> sendProtocolMessage(value, quotedMessageId)
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

        return ResponseRules.withAnsweredState((regular + system).sortedBy { it.timestamp })
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
                    value = MessageProtocol.decode(textContent.value),
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
                    value = MessageProtocol.decode(multipart.value ?: ""),
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
            is MessageContent.QuotedMessageDetails.Text -> MessageProtocol.previewText(content.value)
            is MessageContent.QuotedMessageDetails.Asset -> content.assetName ?: content.assetMimeType
            is MessageContent.QuotedMessageDetails.Location -> content.locationName
            is MessageContent.QuotedMessageDetails.Multipart -> content.text?.let { MessageProtocol.previewText(it) }
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
            override val dataPath: Path =
                FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "natrium-asset-${content.remoteData.assetId}"
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

    private sealed interface UploadResult {
        data class Success(val messageId: String) : UploadResult
        data class Failure(val failure: SendMessageResult.Failure) : UploadResult
    }

    private sealed interface UploadAllResult {
        data class Success(val messageIds: List<String>) : UploadAllResult
        data class Failure(val failure: SendMessageResult.Failure) : UploadAllResult
    }

    private sealed interface RequestLookup {
        data class Found(val request: Request) : RequestLookup
        data class Invalid(val reason: InvalidResponseReason) : RequestLookup
    }

    private companion object {
        const val ANSWER_LOOKUP_LIMIT = 500
    }
}