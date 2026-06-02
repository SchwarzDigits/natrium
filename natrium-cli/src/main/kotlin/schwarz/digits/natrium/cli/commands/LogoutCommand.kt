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
import schwarz.digits.natrium.session.LogoutResult
import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.runBlocking

class LogoutCommand : CliktCommand(name = "logout") {

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) {
            echo("Not logged in")
            return@runBlocking
        }
        when (val result = session.logout()) {
            is LogoutResult.Success ->
                echo("Logged out")
            is LogoutResult.Failure.Unknown ->
                echo("Logout failed: ${result.message}")
        }
    }
}
