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
