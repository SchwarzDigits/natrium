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
import schwarz.digits.natrium.Natrium
import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.awaitCancellation

class AuthEventsCommand : CliktCommand(name = "auth-events", help = "Subscribe to auth events and print them") {

    override fun run() = runBlocking {
        echo("Listening for auth events (Ctrl+C to stop)...")
        val cancellable = Natrium.observeAuthEvents { event ->
            when (event) {
                is AuthEvent.LoggedIn -> echo("AUTH EVENT: LoggedIn (user session established)")
                is AuthEvent.LoggedOut -> echo("AUTH EVENT: LoggedOut")
            }
        }
        try {
            awaitCancellation()
        } finally {
            cancellable.cancel()
        }
    }
}
