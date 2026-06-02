/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
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
