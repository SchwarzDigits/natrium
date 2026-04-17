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

package schwarz.digits.natrium.conversation

import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.ArchiveStatusUpdateResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.RenamingResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import schwarz.digits.natrium.chat.ChatOperations
import schwarz.digits.natrium.chat.ChatOperationsImpl
import schwarz.digits.natrium.users.UserId

internal class ConversationOperationsImpl(
    private val conversationId: ConversationId,
    private val sessionScope: UserSessionScope,
    private val coroutineScope: CoroutineScope
) : ConversationOperations {

    private val chatOperations: ChatOperations by lazy {
        ChatOperationsImpl(
            conversationId,
            sessionScope,
            coroutineScope
        )
    }

    constructor(
        qualifiedId: QualifiedID,
        sessionScope: UserSessionScope,
        coroutineScope: CoroutineScope
    ) : this(ConversationId(qualifiedId.value, qualifiedId.domain), sessionScope, coroutineScope)

    override fun chat(): ChatOperations {
        return chatOperations
    }

    override suspend fun addMember(userId: UserId): AddMemberResult {
        return try {
            val result = sessionScope.conversations.addMemberToConversationUseCase(
                conversationId = QualifiedID(conversationId.value, conversationId.domain),
                userIdList = listOf(userId.toWire()),
            )
            when (result) {
                is AddMemberToConversationUseCase.Result.Success ->
                    AddMemberResult.Success

                is AddMemberToConversationUseCase.Result.Failure ->
                    AddMemberResult.Failure.Unknown("Failed to add person: ${result.cause}")
            }
        } catch (e: Exception) {
            AddMemberResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun removeMember(userId: UserId): RemoveMemberResult {
        return try {
            val result = sessionScope.conversations.removeMemberFromConversation(
                conversationId = QualifiedID(conversationId.value, conversationId.domain),
                userIdToRemove = userId.toWire(),
            )
            when (result) {
                is RemoveMemberFromConversationUseCase.Result.Success ->
                    RemoveMemberResult.Success

                is RemoveMemberFromConversationUseCase.Result.Failure ->
                    RemoveMemberResult.Failure.Unknown("Failed to remove person: ${result.cause}")
            }
        } catch (e: Exception) {
            RemoveMemberResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun getMembers(): GetMembersResult {
        return try {
            val members = sessionScope.conversations.observeConversationMembers(
                QualifiedID(
                    conversationId.value,
                    conversationId.domain
                )
            ).first()
            val conversationMembers = members.map { detail ->
                ConversationMember(
                    userId = UserId(detail.user.id.value, detail.user.id.domain),
                    name = detail.user.name ?: detail.user.id.value,
                )
            }
            GetMembersResult.Success(conversationMembers)
        } catch (e: Exception) {
            GetMembersResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun getJoinLink(password: String?): GetJoinLinkResult {
        return try {
            val convId = QualifiedID(conversationId.value, conversationId.domain)

            val genResult = sessionScope.conversations.generateGuestRoomLink(convId, password)
            when (genResult) {
                is GenerateGuestRoomLinkResult.Success -> { /* link created, fetch below */
                }

                is GenerateGuestRoomLinkResult.Failure -> {
                    // Kalium has a deserialization bug: when a code already exists,
                    // the backend returns 200 OK with a flat response that Kalium
                    // cannot parse (expects event-wrapped format from 201 Created).
                    // Fall back to observing the locally cached link.
                    val existing = sessionScope.conversations.observeGuestRoomLink(convId).first()
                    if (existing is ObserveGuestRoomLinkResult.Success && existing.link != null) {
                        return GetJoinLinkResult.Success(JoinLink(existing.link!!.link))
                    }
                    return GetJoinLinkResult.Failure.Unknown(
                        "Failed to generate join link: ${genResult.cause}"
                    )
                }
            }

            // Fetch the newly generated link
            val linkResult = sessionScope.conversations.observeGuestRoomLink(convId).first()
            when (linkResult) {
                is ObserveGuestRoomLinkResult.Failure ->
                    GetJoinLinkResult.Failure.Unknown("Failed to fetch join link")

                is ObserveGuestRoomLinkResult.Success -> {
                    val link = linkResult.link
                        ?: return GetJoinLinkResult.Failure.Unknown("No join link available")
                    GetJoinLinkResult.Success(JoinLink(link.link))
                }
            }
        } catch (e: Exception) {
            GetJoinLinkResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun revokeJoinLink(): RevokeJoinLinkResult {
        return try {
            val result = sessionScope.conversations.revokeGuestRoomLink(
                QualifiedID(
                    conversationId.value,
                    conversationId.domain
                )
            )
            when (result) {
                is RevokeGuestRoomLinkResult.Success ->
                    RevokeJoinLinkResult.Success

                is RevokeGuestRoomLinkResult.Failure ->
                    RevokeJoinLinkResult.Failure.Unknown("Failed to revoke join link: ${result.cause}")
            }
        } catch (e: Exception) {
            RevokeJoinLinkResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    private fun convId() = QualifiedID(conversationId.value, conversationId.domain)

    private suspend fun readCurrentDetails(): ObserveConversationDetailsUseCase.Result =
        sessionScope.conversations.observeConversationDetails(convId()).first()

    override suspend fun getConversationInfo(): GetConversationInfoResult = try {
        when (val details = readCurrentDetails()) {
            is ObserveConversationDetailsUseCase.Result.Success -> {
                val conv = details.conversationDetails.conversation
                GetConversationInfoResult.Success(
                    ConversationInfo(id = conversationId, title = conv.name ?: "", isArchived = conv.archived)
                )
            }
            is ObserveConversationDetailsUseCase.Result.Failure ->
                GetConversationInfoResult.Failure.Unknown("Failed to load conversation details")
        }
    } catch (e: Exception) {
        GetConversationInfoResult.Failure.Unknown(e.message ?: "Unknown error", e)
    }

    override suspend fun setTitle(title: String): SetTitleResult = try {
        val result = sessionScope.conversations.renameConversation(convId(), title)
        if (result is RenamingResult.Success) SetTitleResult.Success
        else SetTitleResult.Failure.Unknown("Rename failed")
    } catch (e: Exception) {
        SetTitleResult.Failure.Unknown(e.message ?: "Unknown error", e)
    }

    override suspend fun archive(): ArchiveConversationResult = try {
        val result = sessionScope.conversations.updateConversationArchivedStatus(convId(), true, onlyLocally = false)
        if (result is ArchiveStatusUpdateResult.Success) ArchiveConversationResult.Success
        else ArchiveConversationResult.Failure.Unknown("Archive failed")
    } catch (e: Exception) {
        ArchiveConversationResult.Failure.Unknown(e.message ?: "Unknown error", e)
    }

    override suspend fun unarchive(): UnarchiveConversationResult = try {
        val result = sessionScope.conversations.updateConversationArchivedStatus(convId(), false, onlyLocally = false)
        if (result is ArchiveStatusUpdateResult.Success) UnarchiveConversationResult.Success
        else UnarchiveConversationResult.Failure.Unknown("Unarchive failed")
    } catch (e: Exception) {
        UnarchiveConversationResult.Failure.Unknown(e.message ?: "Unknown error", e)
    }

    override suspend fun delete(): DeleteConversationResult {
        return try {
            sessionScope.conversations.deleteTeamConversation(
                QualifiedID(
                    conversationId.value,
                    conversationId.domain
                )
            )
            DeleteConversationResult.Success
        } catch (e: Exception) {
            DeleteConversationResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }
}