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
import schwarz.digits.natrium.conversation.GetConversationInfoResult
import schwarz.digits.natrium.conversation.JoinConversationResult
import schwarz.digits.natrium.conversation.JoinLink
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking

class ConversationJoinCommand : CliktCommand(name = "conversation-join") {

    private val joinLink by option("--join-link", help = "Join link").required()
    private val password by option("--password", "-p", help = "Password (if protected)")

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) { echo("Not logged in"); return@runBlocking }
        when (val result = session.conversationManager.joinConversation(JoinLink(joinLink), password)) {
            is JoinConversationResult.Success -> {
                when (val infoResult = result.conversationOperations.getConversationInfo()) {
                    is GetConversationInfoResult.Success ->
                        echo("Joined! Conversation ID: ${infoResult.conversationInfo.id}")
                    is GetConversationInfoResult.Failure ->
                        echo("Joined! (could not load conversation details)")
                }
            }
            is JoinConversationResult.Failure.NotLoggedIn ->
                echo("Not logged in")
            is JoinConversationResult.Failure.IncorrectPassword ->
                echo("Wrong password")
            is JoinConversationResult.Failure.InvalidLink ->
                echo("Invalid join link")
            is JoinConversationResult.Failure.Unknown ->
                echo("Error: ${result.message}")
        }
    }
}
