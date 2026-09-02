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
        val job = coroutineScope.launch {
            sessionScope.conversations
                .observeConversationListDetailsWithEvents(
                    fromArchive = false,
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
        return try {
            val allDetails = sessionScope.conversations.observeConversationListDetails(fromArchive = false).first()
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