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
                                val status = if (info.isArchived) "archived" else "active"
                                echo("  ${index + 1}. ${info.title.padEnd(40)} [$status]   ID: ${info.id}")
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
