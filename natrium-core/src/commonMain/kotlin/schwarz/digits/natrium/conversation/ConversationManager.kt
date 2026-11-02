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

import schwarz.digits.natrium.Cancellable

interface ConversationManager {
    fun observeConversations(listener: (Collection<ConversationOperations>) -> Unit): Cancellable

    suspend fun listConversations() : ConversationListResult

    suspend fun findConversation(id: ConversationId): FindConversationResult

    /**
     * Creates a group conversation. [metadata] is stored with it and returned in [ConversationInfo];
     * title and metadata together must fit Wire's conversation name (256 characters, encoded).
     */
    suspend fun createConversation(
        title: String,
        metadata: Map<String, String> = emptyMap(),
    ): CreateConversationResult

    suspend fun joinConversation(joinLink: JoinLink, password: String? = null): JoinConversationResult
}
