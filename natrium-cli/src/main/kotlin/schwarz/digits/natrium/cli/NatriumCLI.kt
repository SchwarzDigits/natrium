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

package schwarz.digits.natrium.cli

import schwarz.digits.natrium.BackendConfig
import schwarz.digits.natrium.LogLevel
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.NatriumPlatform
import schwarz.digits.natrium.cli.commands.AuthEventsCommand
import schwarz.digits.natrium.cli.commands.ArchiveConversationCommand
import schwarz.digits.natrium.cli.commands.ConversationAddPersonCommand
import schwarz.digits.natrium.cli.commands.ConversationDeleteCommand
import schwarz.digits.natrium.cli.commands.ConversationJoinLinkCommand
import schwarz.digits.natrium.cli.commands.ConversationJoinCommand
import schwarz.digits.natrium.cli.commands.ConversationMembersCommand
import schwarz.digits.natrium.cli.commands.ConversationRemovePersonCommand
import schwarz.digits.natrium.cli.commands.ConversationChatSendCommand
import schwarz.digits.natrium.cli.commands.ConversationsCommand
import schwarz.digits.natrium.cli.commands.ConsoleCommand
import schwarz.digits.natrium.cli.commands.CreateConversationCommand
import schwarz.digits.natrium.cli.commands.LoginCommand
import schwarz.digits.natrium.cli.commands.LogoutCommand
import schwarz.digits.natrium.cli.commands.SSOLoginCommand
import schwarz.digits.natrium.cli.commands.StatusCommand
import schwarz.digits.natrium.cli.commands.UpdateDisplayNameCommand
import schwarz.digits.natrium.cli.commands.UpdateEmailCommand
import schwarz.digits.natrium.cli.commands.UpdateHandleCommand
import schwarz.digits.natrium.initLogging
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum

/**
 * Root CLI command for Natrium.
 *
 * Initializes the Natrium singleton and registers all subcommands.
 */
class NatriumCLI : CliktCommand(name = "natrium") {

    private val logLevel by option(
        "--log-level",
        help = "Log-Level: VERBOSE, DEBUG, INFO, WARN, ERROR, DISABLED"
    ).enum<LogLevel>().default(LogLevel.DISABLED)

    init {
        subcommands(
            LoginCommand(),
            SSOLoginCommand(),
            StatusCommand(),
            LogoutCommand(),
            UpdateDisplayNameCommand(),
            UpdateHandleCommand(),
            UpdateEmailCommand(),
            ConversationsCommand(),
            CreateConversationCommand(),
            ArchiveConversationCommand(),
            ConversationAddPersonCommand(),
            ConversationRemovePersonCommand(),
            ConversationMembersCommand(),
            ConversationJoinLinkCommand(),
            ConversationJoinCommand(),
            ConversationChatSendCommand(),
            ConversationDeleteCommand(),
            AuthEventsCommand(),
            ConsoleCommand(),
        )
    }

    override fun run() {
        initLogging(logLevel)
        Natrium.initialize(
            BackendConfig(
                name = BackendProperties.NAME,
                api = BackendProperties.API,
                accounts = BackendProperties.ACCOUNTS,
                webSocket = BackendProperties.WEB_SOCKET,
                teams = BackendProperties.TEAMS,
                blackList = BackendProperties.BLACK_LIST,
                website = BackendProperties.WEBSITE,
            ),
            NatriumPlatform(),
        )
    }
}
