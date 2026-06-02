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
import schwarz.digits.natrium.conversation.CreateConversationResult
import schwarz.digits.natrium.conversation.GetConversationInfoResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking

class CreateConversationCommand : CliktCommand(name = "conversation-create") {

    private val title by option("--title", "-t", help = "Title of the new conversation").required()

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) { echo("Not logged in"); return@runBlocking }
        when (val result = session.conversationManager.createConversation(title)) {
            is CreateConversationResult.Success -> {
                when (val infoResult = result.conversationOperation.getConversationInfo()) {
                    is GetConversationInfoResult.Success -> {
                        echo("Conversation created: ${infoResult.conversationInfo.title}")
                        echo("  ID: ${infoResult.conversationInfo.id}")
                    }
                    is GetConversationInfoResult.Failure ->
                        echo("Conversation created (could not load details)")
                }
            }
            is CreateConversationResult.Failure.NotLoggedIn ->
                echo("Not logged in")
            is CreateConversationResult.Failure.Forbidden ->
                echo("Permission denied")
            is CreateConversationResult.Failure.InvalidTitle ->
                echo("Invalid title: ${result.message}")
            is CreateConversationResult.Failure.Unknown ->
                echo("Error: ${result.message}")
        }
    }
}
