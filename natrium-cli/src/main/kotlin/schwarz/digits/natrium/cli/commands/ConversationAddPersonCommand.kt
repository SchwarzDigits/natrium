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
import schwarz.digits.natrium.conversation.AddMemberResult
import schwarz.digits.natrium.conversation.ConversationId
import schwarz.digits.natrium.conversation.FindConversationResult
import schwarz.digits.natrium.users.UserId
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking

class ConversationAddPersonCommand : CliktCommand(name = "conversation-add-person") {

    private val conversationIdStr by option("--conversation-id", help = "Conversation-ID (value@domain)").required()
    private val userIdStr by option("--user-id", help = "User-ID (value@domain)").required()

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) { echo("Not logged in"); return@runBlocking }
        val conversationId = ConversationId.fromString(conversationIdStr)
        val userId = UserId.fromString(userIdStr)
        when (val findResult = session.conversationManager.findConversation(conversationId)) {
            is FindConversationResult.Success -> {
                when (val result = findResult.conversationOperations.addMember(userId)) {
                    is AddMemberResult.Success ->
                        echo("Member added")
                    is AddMemberResult.Failure.NotLoggedIn ->
                        echo("Not logged in")
                    is AddMemberResult.Failure.Unknown ->
                        echo("Error: ${result.message}")
                }
            }
            is FindConversationResult.Failure.NotLoggedIn -> echo("Not logged in")
            is FindConversationResult.Failure.NotFound -> echo("Conversation not found: $conversationIdStr")
            is FindConversationResult.Failure.Unknown -> echo("Error: ${findResult.message}")
        }
    }
}
