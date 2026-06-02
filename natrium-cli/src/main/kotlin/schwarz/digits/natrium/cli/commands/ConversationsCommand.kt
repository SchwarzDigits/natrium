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
