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

    suspend fun delete(): DeleteConversationResult
}
