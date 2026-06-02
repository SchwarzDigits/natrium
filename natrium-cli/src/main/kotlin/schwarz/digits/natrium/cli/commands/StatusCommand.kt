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
