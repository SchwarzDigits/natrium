/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.conversation

import schwarz.digits.natrium.Cancellable

interface ConversationManager {
    fun observeConversations(listener: (Collection<ConversationOperations>) -> Unit): Cancellable
    fun observeArchivedConversations(listener: (Collection<ConversationOperations>) -> Unit): Cancellable

    suspend fun listConversations() : ConversationListResult

    suspend fun listArchivedConversations(): ConversationListResult
    suspend fun findConversation(id: ConversationId): FindConversationResult

    suspend fun createConversation(title: String): CreateConversationResult

    suspend fun joinConversation(joinLink: JoinLink, password: String? = null): JoinConversationResult
}
