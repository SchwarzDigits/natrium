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
import schwarz.digits.natrium.conversation.FindConversationResult
import schwarz.digits.natrium.conversation.GetMembersResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking

class ConversationMembersCommand : CliktCommand(name = "conversation-members") {

    private val id by option("--id", help = "Conversation-ID (value@domain)").required()

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) { echo("Not logged in"); return@runBlocking }
        when (val findResult = session.conversationManager.findConversation(ConversationId.fromString(id))) {
            is FindConversationResult.Success -> {
                when (val result = findResult.conversationOperations.getMembers()) {
                    is GetMembersResult.Success -> {
                        val members = result.members
                        if (members.isEmpty()) {
                            echo("(No participants)")
                        } else {
                            echo("${members.size} ${if (members.size == 1) "participant" else "participants"}:")
                            echo("")
                            members.forEachIndexed { index, member ->
                                echo("  ${index + 1}. ${member.name.padEnd(30)} ID: ${member.userId}")
                            }
                        }
                    }
                    is GetMembersResult.Failure.NotLoggedIn ->
                        echo("Not logged in")
                    is GetMembersResult.Failure.Unknown ->
                        echo("Error: ${result.message}")
                }
            }
            is FindConversationResult.Failure.NotLoggedIn -> echo("Not logged in")
            is FindConversationResult.Failure.NotFound -> echo("Conversation not found: $id")
            is FindConversationResult.Failure.Unknown -> echo("Error: ${findResult.message}")
        }
    }
}
