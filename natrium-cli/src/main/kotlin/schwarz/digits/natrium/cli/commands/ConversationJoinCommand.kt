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
