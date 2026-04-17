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
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.kalium.logic.data.conversation.CreateConversationParam
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.ConversationCreationResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import schwarz.digits.natrium.Cancellable

internal class ConversationManagerImpl(private val sessionScope: UserSessionScope, private val coroutineScope: CoroutineScope) : ConversationManager {

    override fun observeConversations(listener: (Collection<ConversationOperations>) -> Unit): Cancellable {
      return internalObserve(listener, false)
    }

    private fun internalObserve(listener: (Collection<ConversationOperations>) -> Unit, fromArchive: Boolean) : Cancellable{
        val job = coroutineScope.launch {
            sessionScope.conversations
                .observeConversationListDetailsWithEvents(
                    fromArchive = fromArchive,
                    conversationFilter = ConversationFilter.All,
                    strictMlsFilter = false,
                )
                .conflate()
                .collect { list ->
                    listener(list.map {
                        ConversationOperationsImpl(it.conversationDetails.conversation.id, sessionScope, coroutineScope)
                    })
                }
        }
        return Cancellable(job)
    }

    override fun observeArchivedConversations(listener: (Collection<ConversationOperations>) -> Unit): Cancellable {
        return internalObserve(listener, true)
    }

    override suspend fun findConversation(id: ConversationId): FindConversationResult {
        return try {
            when (sessionScope.conversations.observeConversationDetails(id.toQualifiedId()).first()) {
                is ObserveConversationDetailsUseCase.Result.Success ->
                    FindConversationResult.Success(ConversationOperationsImpl(id, sessionScope, coroutineScope))
                is ObserveConversationDetailsUseCase.Result.Failure ->
                    FindConversationResult.Failure.NotFound
            }
        } catch (e: Exception) {
            FindConversationResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun listConversations(): ConversationListResult {
       return internalListConversations(false)
    }

    override suspend fun listArchivedConversations(): ConversationListResult {
        return internalListConversations(true)
    }

    private suspend fun internalListConversations(fromArchive: Boolean): ConversationListResult {
        return try {
            val allDetails = sessionScope.conversations.observeConversationListDetails(fromArchive = fromArchive).first()
            val conversations = allDetails.map { ConversationOperationsImpl(it.conversation.id, sessionScope, coroutineScope) }
            ConversationListResult.Success(conversations = conversations)
        } catch (e: Exception) {
            ConversationListResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun createConversation(title: String): CreateConversationResult {
        return try {
            val defaultProtocol = sessionScope.getDefaultProtocol()
            val convProtocol = CreateConversationParam.Protocol
                .fromSupportedProtocolToConversationOptionsProtocol(defaultProtocol)

            val result = sessionScope.conversations.createRegularGroup(
                name = title,
                userIdList = emptyList(),
                options = CreateConversationParam(
                    protocol = convProtocol,
                    access = setOf(Conversation.Access.INVITE, Conversation.Access.CODE),
                    accessRole = setOf(
                        Conversation.AccessRole.TEAM_MEMBER,
                        Conversation.AccessRole.NON_TEAM_MEMBER,
                        Conversation.AccessRole.GUEST,
                    ),
                ),
            )
            when (result) {
                is ConversationCreationResult.Success -> {
                    CreateConversationResult.Success(ConversationOperationsImpl(result.conversation.id, sessionScope, coroutineScope))
                }
                is ConversationCreationResult.Forbidden ->
                    CreateConversationResult.Failure.Forbidden
                is ConversationCreationResult.SyncFailure ->
                    CreateConversationResult.Failure.Unknown("Sync failed")
                is ConversationCreationResult.UnknownFailure ->
                    CreateConversationResult.Failure.Unknown("Conversation creation failed: ${result.cause}")
                is ConversationCreationResult.BackendConflictFailure ->
                    CreateConversationResult.Failure.Unknown("Backend conflict: ${result.domains}")
            }
        } catch (e: Exception) {
            CreateConversationResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun joinConversation(joinLink: JoinLink, password: String?): JoinConversationResult {
        val (code, key) = parseGuestLink(joinLink.value)
            ?: return JoinConversationResult.Failure.InvalidLink
        return try {
            val result = sessionScope.conversations.joinConversationViaCode(
                code = code,
                key = key,
                domain = null,
                password = password
            )
            when (result) {
                is JoinConversationViaCodeUseCase.Result.Success.Changed ->
                    JoinConversationResult.Success(ConversationOperationsImpl(result.conversationId, sessionScope, coroutineScope))
                is JoinConversationViaCodeUseCase.Result.Success.Unchanged ->
                    JoinConversationResult.Success(ConversationOperationsImpl(result.conversationId!!, sessionScope, coroutineScope))
                is JoinConversationViaCodeUseCase.Result.Failure.IncorrectPassword ->
                    JoinConversationResult.Failure.IncorrectPassword
                is JoinConversationViaCodeUseCase.Result.Failure.Generic ->
                    JoinConversationResult.Failure.Unknown("Join failed: ${result.failure}")
            }
        } catch (e: Exception) {
            JoinConversationResult.Failure.Unknown(e.message ?: "Unknown error", e)
        }
    }
}