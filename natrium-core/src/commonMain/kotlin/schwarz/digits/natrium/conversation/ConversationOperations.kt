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
