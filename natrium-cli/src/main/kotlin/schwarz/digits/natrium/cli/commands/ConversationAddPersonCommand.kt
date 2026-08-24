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
