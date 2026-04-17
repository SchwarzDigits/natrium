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
import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.runBlocking

class StatusCommand : CliktCommand(name = "status") {

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) {
            echo("Not logged in")
            return@runBlocking
        }
        val info = session.sessionInfo()
        if (info == null) {
            echo("Session info unavailable")
            return@runBlocking
        }
        echo("Logged in as: ${info.user.name}")
        echo("  Handle:  ${info.handle ?: "-"}")
        echo("  Email:   ${info.email ?: "-"}")
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
}
