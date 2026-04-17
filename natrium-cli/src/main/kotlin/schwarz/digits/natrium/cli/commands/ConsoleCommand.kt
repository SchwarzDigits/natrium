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

import schwarz.digits.natrium.AuthEvent
import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.devices.DeviceLimitResolver
import schwarz.digits.natrium.devices.ListDevicesResult
import schwarz.digits.natrium.devices.RemoveDeviceResult
import schwarz.digits.natrium.conversation.AddMemberResult
import schwarz.digits.natrium.conversation.ArchiveConversationResult
import schwarz.digits.natrium.conversation.ConversationId
import schwarz.digits.natrium.conversation.ConversationListResult
import schwarz.digits.natrium.conversation.ConversationOperations
import schwarz.digits.natrium.conversation.CreateConversationResult
import schwarz.digits.natrium.conversation.FindConversationResult
import schwarz.digits.natrium.conversation.GetConversationInfoResult
import schwarz.digits.natrium.conversation.GetJoinLinkResult
import schwarz.digits.natrium.conversation.GetMembersResult
import schwarz.digits.natrium.conversation.JoinConversationResult
import schwarz.digits.natrium.conversation.JoinLink
import schwarz.digits.natrium.conversation.RemoveMemberResult
import schwarz.digits.natrium.conversation.RevokeJoinLinkResult
import schwarz.digits.natrium.chat.ChatHistoryResult
import schwarz.digits.natrium.chat.ChatMessage
import schwarz.digits.natrium.chat.MessageStatus
import schwarz.digits.natrium.chat.MessageValue
import schwarz.digits.natrium.chat.SendMessageResult
import schwarz.digits.natrium.session.LoginError
import schwarz.digits.natrium.session.LoginResult
import schwarz.digits.natrium.session.LogoutResult
import schwarz.digits.natrium.session.Session
import schwarz.digits.natrium.session.UpdateDisplayNameResult
import schwarz.digits.natrium.users.UserId
import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.runBlocking

class ConsoleCommand : CliktCommand(name = "console") {

    private var session: Session? = null
    private var authEventsCancellable: Cancellable? = null

    override fun run() = runBlocking {
        session = Natrium.restoreLastSession()
        echo("Natrium Console (type help for commands, quit to exit)")
        echo()

        while (true) {
            print("natrium> ")
            System.out.flush()
            val line = readlnOrNull()?.trim() ?: break
            if (line.isBlank()) continue

            val parts = line.split(" ", limit = 2)
            val command = parts[0].lowercase()
            val args = parts.getOrElse(1) { "" }.trim()

            try {
                when (command) {
                    "help", "?" -> showHelp()
                    "quit", "exit" -> break
                    "login" -> handleLogin()
                    "status" -> handleStatus()
                    "logout" -> handleLogout()
                    "update-display-name" -> handleUpdateDisplayName(args)
                    "conversations" -> handleConversations()
                    "conversation-create" -> handleCreateConversation(args)
                    "conversation-archive" -> handleArchiveConversation(args)
                    "conversation-members" -> handleConversationMembers(args)
                    "conversation-add-person" -> handleAddPerson(args)
                    "conversation-remove-person" -> handleRemovePerson(args)
                    "conversation-joinlink" -> handleJoinLink(args)
                    "conversation-revoke-joinlink" -> handleRevokeJoinLink(args)
                    "conversation-join" -> handleJoin(args)
                    "conversation-send" -> handleConversationSend(args)
                    "conversation-history" -> handleConversationHistory(args)
                    "conversation-chat" -> handleConversationChat(args)
                    "auth-events" -> handleAuthEvents()
                    else -> echo("Unknown command: $command (help for help)")
                }
            } catch (e: Exception) {
                echo("Error: ${e.message}")
            }
        }
        echo("Goodbye!")
    }

    // --- Help ---

    private fun showHelp() {
        echo("""
            |Available commands:
            |  login                Log in to backend
            |  status               Show session status
            |  logout               Log out
            |  update-display-name <name>  Update your display name
            |  conversations                List conversations
            |  conversation-create <title>  Create a new conversation
            |  conversation-archive <id>    Archive conversation
            |  conversation-members <id>    Show participants
            |  conversation-add-person <conversation-id> <user-id>     Add member
            |  conversation-remove-person <conversation-id> <user-id>  Remove member
            |  conversation-joinlink <id> [password]             Get join link
            |  conversation-revoke-joinlink <id>                Revoke join link
            |  conversation-join <join-link> [password]         Join via join link
            |  conversation-send <conversation-id> <message>    Send text message
            |  conversation-history <conversation-id> [limit]   Show message history
            |  conversation-chat <conversation-id>              Interactive chat mode
            |  auth-events          Toggle auth event subscription
            |  help                 Show this help
            |  quit                 Exit
        """.trimMargin())
    }

    // --- Auth ---

    private suspend fun handleLogin() {
        print("Email: ")
        System.out.flush()
        val email = readlnOrNull()?.trim()
        if (email.isNullOrBlank()) { echo("Cancelled."); return }

        print("Password: ")
        System.out.flush()
        val password = readlnOrNull()?.trim()
        if (password.isNullOrBlank()) { echo("Cancelled."); return }

        echo("Connecting ...")
        var result = Natrium.login(email, password)

        if (result is LoginResult.Failure.Error && result.reason == LoginError.SECOND_FA_CODE_REQUIRED) {
            echo("2FA required")
            print("2FA code: ")
            System.out.flush()
            val code = readlnOrNull()?.trim()
            if (code.isNullOrBlank()) { echo("Cancelled."); return }
            result = Natrium.login(email, password, secondFactorVerificationCode = code)
        }

        result = handleLoginResult(result, password)

        if (result is LoginResult.Success) {
            session = result.session
            val info = result.session.sessionInfo()
            echo("Login successful")
            echo("User: ${info?.user?.name ?: "unknown"}")
            echo("Device: ${info?.device?.id ?: "unknown"}")
        }
    }

    private suspend fun handleLoginResult(result: LoginResult, password: String): LoginResult {
        when (result) {
            is LoginResult.Success -> return result
            is LoginResult.Failure.TooManyDevices -> return handleTooManyDevices(result.resolver, password)
            is LoginResult.Failure.Error -> {
                when (result.reason) {
                    LoginError.EMAIL_OR_PASSWORD_WRONG -> echo("Login failed: invalid credentials")
                    LoginError.SECOND_FA_CODE_REQUIRED -> echo("2FA failed")
                    LoginError.INVALID_2FA_CODE -> echo("Invalid 2FA code")
                    LoginError.CONNECTION_ERROR -> echo("Network error")
                    LoginError.LOGIN_FAILED -> echo("Login failed")
                    LoginError.ACCOUNT_LOCKED -> echo("Account locked")
                    LoginError.ACCOUNT_NOT_ACTIVATED -> echo("Account not activated")
                    LoginError.CLIENT_REGISTRATION_FAILED -> echo("Client registration failed")
                    LoginError.SESSION_COULD_NOT_BE_SAVED -> echo("Session could not be saved")
                    LoginError.SERVER_VERSION_NOT_SUPPORTED -> echo("Server version not supported")
                    LoginError.APP_UPDATE_REQUIRED -> echo("App update required")
                }
                return result
            }
        }
    }

    private suspend fun handleTooManyDevices(resolver: DeviceLimitResolver, password: String): LoginResult {
        echo("Too many devices registered. You need to remove one to continue.")

        when (val listResult = resolver.listDevices()) {
            is ListDevicesResult.Success -> {
                echo("\nRegistered devices:")
                listResult.devices.forEachIndexed { index, device ->
                    val label = device.label ?: device.model ?: "Unknown device"
                    echo("  ${index + 1}. $label (ID: ${device.id})")
                }

                print("\nSelect a device to remove (number): ")
                System.out.flush()
                val selection = readlnOrNull()?.trim()?.toIntOrNull()
                if (selection == null || selection < 1 || selection > listResult.devices.size) {
                    echo("Invalid selection. Login aborted.")
                    return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
                }

                val deviceToRemove = listResult.devices[selection - 1]
                echo("Removing device: ${deviceToRemove.label ?: deviceToRemove.id}")

                when (resolver.removeDevice(deviceToRemove.id, password)) {
                    is RemoveDeviceResult.Success -> {
                        echo("Device removed. Retrying client registration...")
                        return handleLoginResult(resolver.retry(), password)
                    }
                    is RemoveDeviceResult.Failure.PasswordRequired -> {
                        echo("Password required to remove device.")
                        return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
                    }
                    is RemoveDeviceResult.Failure.InvalidCredentials -> {
                        echo("Invalid credentials for device removal.")
                        return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
                    }
                    is RemoveDeviceResult.Failure -> {
                        echo("Failed to remove device.")
                        return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
                    }
                }
            }
            is ListDevicesResult.Failure -> {
                echo("Failed to load device list.")
                return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
            }
        }
    }

    private suspend fun handleStatus() {
        val s = requireSession() ?: return
        val info = s.sessionInfo()
        if (info == null) { echo("Session info unavailable"); return }
        echo("Logged in as: ${info.user.name}")
        echo("  Backend: ${info.backend}")
        echo("  Domain:  ${info.user.userId.domain}")
        val device = info.device
        if (device != null) {
            val clientDisplay = listOfNotNull(device.label, device.model)
                .joinToString(" / ").ifEmpty { null }
            if (clientDisplay != null) {
                echo("  Client:  ${device.id} ($clientDisplay)")
            } else {
                echo("  Client:  ${device.id}")
            }
        }
        echo("  User-ID: ${info.user.userId}")
    }

    private suspend fun handleLogout() {
        val s = session ?: Natrium.restoreLastSession()
        if (s == null) { echo("Not logged in"); return }
        when (val result = s.logout()) {
            is LogoutResult.Success -> {
                session = null
                echo("Logged out")
            }
            is LogoutResult.Failure.Unknown -> echo("Logout failed: ${result.message}")
        }
    }

    private suspend fun handleUpdateDisplayName(args: String) {
        if (args.isBlank()) { echo("Usage: update-display-name <name>"); return }
        val s = requireSession() ?: return
        when (val result = s.updateDisplayName(args)) {
            is UpdateDisplayNameResult.Success -> echo("Display name updated")
            is UpdateDisplayNameResult.Failure.Unknown -> echo("Failed to update display name: ${result.message}")
        }
    }

    private fun handleAuthEvents() {
        if (authEventsCancellable != null) {
            authEventsCancellable?.cancel()
            authEventsCancellable = null
            echo("Auth events: unsubscribed")
        } else {
            authEventsCancellable = Natrium.observeAuthEvents { event ->
                when (event) {
                    is AuthEvent.LoggedIn -> echo("\n[AUTH EVENT] LoggedIn")
                    is AuthEvent.LoggedOut -> echo("\n[AUTH EVENT] LoggedOut")
                }
            }
            echo("Auth events: subscribed (use auth-events again to stop)")
        }
    }

    private suspend fun requireSession(): Session? {
        val s = session ?: Natrium.restoreLastSession().also { session = it }
        if (s == null) echo("Not logged in")
        return s
    }

    // --- Conversation helper ---

    private suspend fun findConversationOps(s: Session, conversationIdStr: String): ConversationOperations? {
        return when (val findResult = s.conversationManager.findConversation(ConversationId.fromString(conversationIdStr))) {
            is FindConversationResult.Success -> findResult.conversationOperations
            is FindConversationResult.Failure.NotLoggedIn -> { echo("Not logged in"); null }
            is FindConversationResult.Failure.NotFound -> { echo("Conversation not found: $conversationIdStr"); null }
            is FindConversationResult.Failure.Unknown -> { echo("Error: ${findResult.message}"); null }
        }
    }

    // --- Conversations ---

    private suspend fun handleConversations() {
        val s = requireSession() ?: return
        echo("Syncing with server...")
        when (val result = s.conversationManager.listConversations()) {
            is ConversationListResult.Success -> {
                val conversationOpsList = result.conversations
                if (conversationOpsList.isEmpty()) {
                    echo("(No conversations found)")
                } else {
                    echo("${conversationOpsList.size} ${if (conversationOpsList.size == 1) "conversation" else "conversations"} found:")
                    echo()
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

    private suspend fun handleCreateConversation(args: String) {
        if (args.isBlank()) { echo("Usage: conversation-create <title>"); return }
        val s = requireSession() ?: return
        when (val result = s.conversationManager.createConversation(args)) {
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
            is CreateConversationResult.Failure.NotLoggedIn -> echo("Not logged in")
            is CreateConversationResult.Failure.Forbidden -> echo("Permission denied")
            is CreateConversationResult.Failure.InvalidTitle -> echo("Invalid title: ${result.message}")
            is CreateConversationResult.Failure.Unknown -> echo("Error: ${result.message}")
        }
    }

    private suspend fun handleArchiveConversation(args: String) {
        if (args.isBlank()) { echo("Usage: conversation-archive <id>"); return }
        val s = requireSession() ?: return
        val conversationOps = findConversationOps(s, args) ?: return
        when (val result = conversationOps.archive()) {
            is ArchiveConversationResult.Success -> echo("Conversation archived")
            is ArchiveConversationResult.Failure.NotLoggedIn -> echo("Not logged in")
            is ArchiveConversationResult.Failure.Unknown -> echo("Error: ${result.message}")
        }
    }

    private suspend fun handleConversationMembers(args: String) {
        if (args.isBlank()) { echo("Usage: conversation-members <id>"); return }
        val s = requireSession() ?: return
        val conversationOps = findConversationOps(s, args) ?: return
        when (val result = conversationOps.getMembers()) {
            is GetMembersResult.Success -> {
                val members = result.members
                if (members.isEmpty()) {
                    echo("(No participants)")
                } else {
                    echo("${members.size} ${if (members.size == 1) "participant" else "participants"}:")
                    echo()
                    members.forEachIndexed { index, member ->
                        echo("  ${index + 1}. ${member.name.padEnd(30)} ID: ${member.userId}")
                    }
                }
            }
            is GetMembersResult.Failure.NotLoggedIn -> echo("Not logged in")
            is GetMembersResult.Failure.Unknown -> echo("Error: ${result.message}")
        }
    }

    private suspend fun handleAddPerson(args: String) {
        val parts = args.split(" ", limit = 2)
        if (parts.size < 2) { echo("Usage: conversation-add-person <conversation-id> <user-id>"); return }
        val s = requireSession() ?: return
        val conversationOps = findConversationOps(s, parts[0]) ?: return
        val userId = UserId.fromString(parts[1])
        when (val result = conversationOps.addMember(userId)) {
            is AddMemberResult.Success -> echo("Member added")
            is AddMemberResult.Failure.NotLoggedIn -> echo("Not logged in")
            is AddMemberResult.Failure.Unknown -> echo("Error: ${result.message}")
        }
    }

    private suspend fun handleRemovePerson(args: String) {
        val parts = args.split(" ", limit = 2)
        if (parts.size < 2) { echo("Usage: conversation-remove-person <conversation-id> <user-id>"); return }
        val s = requireSession() ?: return
        val conversationOps = findConversationOps(s, parts[0]) ?: return
        val userId = UserId.fromString(parts[1])
        when (val result = conversationOps.removeMember(userId)) {
            is RemoveMemberResult.Success -> echo("Member removed")
            is RemoveMemberResult.Failure.NotLoggedIn -> echo("Not logged in")
            is RemoveMemberResult.Failure.Unknown -> echo("Error: ${result.message}")
        }
    }

    private suspend fun handleJoinLink(args: String) {
        val parts = args.split(" ", limit = 2)
        if (parts.isEmpty() || parts[0].isBlank()) { echo("Usage: conversation-joinlink <id> [password]"); return }
        val s = requireSession() ?: return
        val conversationOps = findConversationOps(s, parts[0]) ?: return
        val password = parts.getOrNull(1)?.trim()?.ifEmpty { null }
        when (val result = conversationOps.getJoinLink(password)) {
            is GetJoinLinkResult.Success -> {
                echo("Join link: ${result.joinLink.value}")
            }
            is GetJoinLinkResult.Failure.NotLoggedIn -> echo("Not logged in")
            is GetJoinLinkResult.Failure.Unknown -> echo("Error: ${result.message}")
        }
    }

    private suspend fun handleRevokeJoinLink(args: String) {
        if (args.isBlank()) { echo("Usage: conversation-revoke-joinlink <id>"); return }
        val s = requireSession() ?: return
        val conversationOps = findConversationOps(s, args.trim()) ?: return
        when (val result = conversationOps.revokeJoinLink()) {
            is RevokeJoinLinkResult.Success -> echo("Join link revoked")
            is RevokeJoinLinkResult.Failure.NotLoggedIn -> echo("Not logged in")
            is RevokeJoinLinkResult.Failure.Unknown -> echo("Error: ${result.message}")
        }
    }

    private suspend fun handleJoin(args: String) {
        val parts = args.split(" ", limit = 2)
        if (parts.isEmpty() || parts[0].isBlank()) { echo("Usage: conversation-join <join-link> [password]"); return }
        val joinLink = parts[0]
        val password = parts.getOrNull(1)?.trim()?.ifEmpty { null }
        val s = requireSession() ?: return
        when (val result = s.conversationManager.joinConversation(JoinLink(joinLink), password)) {
            is JoinConversationResult.Success -> {
                when (val infoResult = result.conversationOperations.getConversationInfo()) {
                    is GetConversationInfoResult.Success ->
                        echo("Joined! Conversation ID: ${infoResult.conversationInfo.id}")
                    is GetConversationInfoResult.Failure ->
                        echo("Joined! (could not load conversation details)")
                }
            }
            is JoinConversationResult.Failure.NotLoggedIn -> echo("Not logged in")
            is JoinConversationResult.Failure.IncorrectPassword -> echo("Wrong password")
            is JoinConversationResult.Failure.InvalidLink -> echo("Invalid join link")
            is JoinConversationResult.Failure.Unknown -> echo("Error: ${result.message}")
        }
    }

    // --- Chat ---

    private suspend fun handleConversationSend(args: String) {
        val parts = args.split(" ", limit = 2)
        if (parts.size < 2 || parts[1].isBlank()) {
            echo("Usage: conversation-send <conversation-id> <message>")
            return
        }
        val s = requireSession() ?: return
        val conversationOps = findConversationOps(s, parts[0]) ?: return
        val text = parts[1].removeSurrounding("\"")
        echo("Sending message to conversation ${parts[0]}...")
        when (val result = conversationOps.chat().sendMessage(MessageValue.TextValue(text))) {
            is SendMessageResult.Success -> echo("Message sent")
            is SendMessageResult.Failure.NotLoggedIn -> echo("Not logged in")
            is SendMessageResult.Failure.Unknown -> echo("Error: ${result.message}")
            is SendMessageResult.Failure -> echo("Send failed")
        }
    }

    private suspend fun handleConversationHistory(args: String) {
        val parts = args.split(" ", limit = 2)
        if (parts.isEmpty() || parts[0].isBlank()) {
            echo("Usage: conversation-history <conversation-id> [limit]")
            return
        }
        val limit = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 20
        val s = requireSession() ?: return
        val conversationOps = findConversationOps(s, parts[0]) ?: return
        when (val result = conversationOps.chat().getHistory(limit = limit, offset = 0)) {
            is ChatHistoryResult.Success -> {
                val messages = result.messages
                if (messages.isEmpty()) {
                    echo("(No messages)")
                } else {
                    messages.forEach { msg ->
                        val sender = msg.sender.name ?: msg.sender.userId.toString()
                        val status = when (msg.status) {
                            MessageStatus.PENDING -> " [pending]"
                            MessageStatus.FAILED -> " [failed]"
                            else -> ""
                        }
                        echo("  [${msg.timestamp}] $sender: ${msg.displayText()}$status")
                    }
                }
            }
            is ChatHistoryResult.Failure.NotLoggedIn -> echo("Not logged in")
            is ChatHistoryResult.Failure.Unknown -> echo("Error: ${result.message}")
        }
    }

    private suspend fun handleConversationChat(args: String) {
        if (args.isBlank()) { echo("Usage: conversation-chat <conversation-id>"); return }
        val s = requireSession() ?: return
        val conversationOps = findConversationOps(s, args) ?: return
        val chat = conversationOps.chat()

        echo("Chat mode for conversation $args (/help for commands, /quit to exit)")
        echo("Loading recent messages...")

        when (val history = chat.getHistory(limit = 10, offset = 0)) {
            is ChatHistoryResult.Success -> {
                history.messages.forEach { msg ->
                    val sender = msg.sender.name ?: msg.sender.userId.toString()
                    echo("  [${msg.timestamp}] $sender: ${msg.displayText()}")
                }
                if (history.messages.isNotEmpty()) echo("---")
            }
            is ChatHistoryResult.Failure -> echo("(Could not load history)")
        }

        while (true) {
            print("chat> ")
            System.out.flush()
            val input = readlnOrNull()?.trim() ?: break
            if (input == "/quit" || input == "/back") break
            if (input == "/help") {
                echo("""
                    |Chat commands:
                    |  /help   Show this help
                    |  /quit   Leave chat mode (back to natrium>)
                    |  /back   Same as /quit
                    |
                    |  Any other input is sent as a message.
                """.trimMargin())
                continue
            }
            if (input.isBlank()) continue

            when (val result = chat.sendMessage(MessageValue.TextValue(input))) {
                is SendMessageResult.Success -> echo("  > sent")
                is SendMessageResult.Failure.NotLoggedIn -> { echo("Not logged in"); break }
                is SendMessageResult.Failure.Unknown -> echo("  > failed: ${result.message}")
                is SendMessageResult.Failure -> echo("  > send failed")
            }
        }
        echo("Left chat mode")
    }
}

private fun ChatMessage.displayText(): String = when (val v = value) {
    is MessageValue.TextValue -> v.value
    is MessageValue.FileValue -> "[File: ${v.fileLink.fileName}]"
    is MessageValue.LocationValue -> "[Location: ${v.name ?: "${v.latitude}, ${v.longitude}"}]"
    is MessageValue.KnockValue -> "[Knock]"
    is MessageValue.SystemValue -> systemText ?: "[System]"
}
