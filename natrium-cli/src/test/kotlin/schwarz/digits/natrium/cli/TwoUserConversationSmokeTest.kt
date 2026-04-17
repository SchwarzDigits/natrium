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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.Properties

class TwoUserConversationSmokeTest : FunSpec({

    test("two users: create, join via link, exchange messages, delete") {
        val props = loadLocalProperties()

        val email1 = System.getenv("NATRIUM_TEST_EMAIL") ?: props.getProperty("test.email")
        val password1 = System.getenv("NATRIUM_TEST_PASSWORD") ?: props.getProperty("test.password")
        val email2 = System.getenv("NATRIUM_TEST_EMAIL_2") ?: props.getProperty("test.email2")
        val password2 = System.getenv("NATRIUM_TEST_PASSWORD_2") ?: props.getProperty("test.password2")

        if (email1.isNullOrBlank() || password1.isNullOrBlank() ||
            email2.isNullOrBlank() || password2.isNullOrBlank()
        ) {
            println("SKIPPED: test.email / test.password / test.email2 / test.password2 not set")
            return@test
        }

        var conversationId: String? = null
        try {
            // User 1: login and create conversation
            val login1Out = runCli("login", "-e", email1, "-p", password1)
            login1Out shouldContain "Login successful"

            val createOut = runCli("conversation-create", "-t", "Two-User Smoke Test")
            createOut shouldContain "Conversation created"
            conversationId = parseConversationId(createOut)
            conversationId.shouldNotBeNull()

            // User 1: generate join link
            val joinLinkOut = runCli("conversation-joinlink", "--id", conversationId)
            joinLinkOut shouldContain "Join link:"
            val joinLink = parseJoinLink(joinLinkOut)
            joinLink.shouldNotBeNull()

            // User 1: send first message
            val send1Out = runCli("conversation-send", "--id", conversationId, "-m", "Hello from User 1")
            send1Out shouldContain "Message sent"

            // User 1: logout
            val logout1Out = runCli("logout")
            logout1Out shouldContain "Logged out"

            // User 2: login and join conversation
            val login2Out = runCli("login", "-e", email2, "-p", password2)
            login2Out shouldContain "Login successful"

            val joinOut = runCli("conversation-join", "--join-link", joinLink)
            joinOut shouldContain "Joined!"

            // User 2: send a reply
            val send2Out = runCli("conversation-send", "--id", conversationId, "-m", "Hello from User 2")
            send2Out shouldContain "Message sent"

            // User 2: logout
            val logout2Out = runCli("logout")
            logout2Out shouldContain "Logged out"

            // User 1: login again, send another message, then clean up
            val login1Again = runCli("login", "-e", email1, "-p", password1)
            login1Again shouldContain "Login successful"

            val send3Out = runCli("conversation-send", "--id", conversationId, "-m", "Reply from User 1")
            send3Out shouldContain "Message sent"

            val deleteOut = runCli("conversation-delete", "--id", conversationId)
            deleteOut shouldContain "Conversation deleted"
            conversationId = null

            val finalLogout = runCli("logout")
            finalLogout shouldContain "Logged out"
        } finally {
            // Best-effort cleanup: try to delete conversation and logout
            if (conversationId != null) {
                try { runCli("logout") } catch (_: Exception) {}
                try {
                    runCli("login", "-e", email1, "-p", password1)
                    runCli("conversation-delete", "--id", conversationId)
                } catch (_: Exception) {}
            }
            try { runCli("logout") } catch (_: Exception) {}
        }
    }
})

private fun runCli(vararg args: String): String {
    val baos = ByteArrayOutputStream()
    val printStream = PrintStream(baos, true, "UTF-8")
    val originalOut = System.out
    val originalErr = System.err
    try {
        System.setOut(printStream)
        System.setErr(printStream)
        NatriumCLI().parse(args.toList())
    } catch (e: Exception) {
        printStream.println("CLI exception: ${e.message}")
    } finally {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }
    return baos.toString("UTF-8")
}

private fun parseConversationId(output: String): String? {
    val regex = Regex("""ID:\s*(.+)""")
    return regex.find(output)?.groupValues?.get(1)?.trim()
}

private fun parseJoinLink(output: String): String? {
    val regex = Regex("""Join link:\s*(.+)""")
    return regex.find(output)?.groupValues?.get(1)?.trim()
}

private fun loadLocalProperties(): Properties {
    val props = Properties()
    var dir = File(System.getProperty("user.dir"))
    while (dir.parentFile != null) {
        val candidate = File(dir, "local.properties")
        if (candidate.exists()) {
            candidate.inputStream().use { props.load(it) }
            return props
        }
        dir = dir.parentFile
    }
    return props
}
