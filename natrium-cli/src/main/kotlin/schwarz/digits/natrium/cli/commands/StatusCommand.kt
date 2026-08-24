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
