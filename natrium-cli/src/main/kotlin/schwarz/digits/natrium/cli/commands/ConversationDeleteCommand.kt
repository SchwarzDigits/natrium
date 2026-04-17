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
