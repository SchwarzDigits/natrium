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
import schwarz.digits.natrium.conversation.ConversationListResult
import schwarz.digits.natrium.conversation.GetConversationInfoResult
import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.runBlocking

class ConversationsCommand : CliktCommand(name = "conversations") {

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) { echo("Not logged in"); return@runBlocking }
        echo("Syncing with server...")
        when (val result = session.conversationManager.listConversations()) {
            is ConversationListResult.Success -> {
                val conversationOpsList = result.conversations
                if (conversationOpsList.isEmpty()) {
                    echo("(No conversations found)")
                } else {
                    echo("${conversationOpsList.size} ${if (conversationOpsList.size == 1) "conversation" else "conversations"} found:")
                    echo("")
                    conversationOpsList.forEachIndexed { index, conversationOps ->
                        when (val infoResult = conversationOps.getConversationInfo()) {
                            is GetConversationInfoResult.Success -> {
                                val info = infoResult.conversationInfo
                                echo("  ${index + 1}. ${info.title.padEnd(40)}   ID: ${info.id}")
                            }
                            is GetConversationInfoResult.Failure -> {
                                echo("  ${index + 1}. (could not load conversation info)")
                            }
                        }
                    }
                }
            }
            is ConversationListResult.Failure.NotLoggedIn -> echo("Not logged in")
            is ConversationListResult.Failure.SyncFailed -> echo("Sync failed: ${result.message}")
            is ConversationListResult.Failure.Unknown -> echo("Error: ${result.message}")
        }
    }
}
