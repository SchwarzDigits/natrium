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
