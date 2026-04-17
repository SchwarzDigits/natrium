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
