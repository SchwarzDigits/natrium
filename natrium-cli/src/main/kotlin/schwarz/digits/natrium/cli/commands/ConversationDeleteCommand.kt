/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.cli.commands

import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.conversation.ConversationId
import schwarz.digits.natrium.conversation.DeleteConversationResult
import schwarz.digits.natrium.conversation.FindConversationResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking

class ConversationDeleteCommand : CliktCommand(name = "conversation-delete") {

    private val id by option("--id", help = "Conversation-ID (value@domain)").required()

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) { echo("Not logged in"); return@runBlocking }
        when (val findResult = session.conversationManager.findConversation(ConversationId.fromString(id))) {
            is FindConversationResult.Success -> {
                when (val result = findResult.conversationOperations.delete()) {
                    is DeleteConversationResult.Success ->
                        echo("Conversation deleted")
                    is DeleteConversationResult.Failure.NotLoggedIn ->
                        echo("Not logged in")
                    is DeleteConversationResult.Failure.Unknown ->
                        echo("Error: ${result.message}")
                }
            }
            is FindConversationResult.Failure.NotLoggedIn -> echo("Not logged in")
            is FindConversationResult.Failure.NotFound -> echo("Conversation not found: $id")
            is FindConversationResult.Failure.Unknown -> echo("Error: ${findResult.message}")
        }
    }
}
