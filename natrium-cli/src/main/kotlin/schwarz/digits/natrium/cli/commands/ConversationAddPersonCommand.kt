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
