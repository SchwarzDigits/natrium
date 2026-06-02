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
import schwarz.digits.natrium.chat.MessageValue
import schwarz.digits.natrium.chat.SendMessageResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking

class ConversationChatSendCommand : CliktCommand(name = "conversation-send") {

    private val id by option("--id", help = "Conversation-ID (value@domain)").required()
    private val message by option("-m", "--message", help = "Text message to send").required()

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) { echo("Not logged in"); return@runBlocking }
        when (val findResult = session.conversationManager.findConversation(ConversationId.fromString(id))) {
            is FindConversationResult.Success -> {
                when (val result = findResult.conversationOperations.chat().sendMessage(MessageValue.TextValue(message))) {
                    is SendMessageResult.Success ->
                        echo("Message sent")
                    is SendMessageResult.Failure.NotLoggedIn ->
                        echo("Not logged in")
                    is SendMessageResult.Failure.Unknown ->
                        echo("Error: ${result.message}")
                    is SendMessageResult.Failure ->
                        echo("Send failed")
                }
            }
            is FindConversationResult.Failure.NotLoggedIn -> echo("Not logged in")
            is FindConversationResult.Failure.NotFound -> echo("Conversation not found: $id")
            is FindConversationResult.Failure.Unknown -> echo("Error: ${findResult.message}")
        }
    }
}
