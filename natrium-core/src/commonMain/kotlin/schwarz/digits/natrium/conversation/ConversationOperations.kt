/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.conversation

import schwarz.digits.natrium.chat.ChatOperations
import schwarz.digits.natrium.users.UserId

interface ConversationOperations {

    fun chat() : ChatOperations
    suspend fun addMember(userId: UserId): AddMemberResult

    suspend fun removeMember(userId: UserId): RemoveMemberResult

    suspend fun getMembers(): GetMembersResult

    suspend fun getJoinLink(password: String? = null): GetJoinLinkResult

    suspend fun revokeJoinLink(): RevokeJoinLinkResult

    suspend fun getConversationInfo(): GetConversationInfoResult

    suspend fun setTitle(title: String): SetTitleResult

    suspend fun archive(): ArchiveConversationResult

    suspend fun unarchive(): UnarchiveConversationResult

    suspend fun delete(): DeleteConversationResult
}
