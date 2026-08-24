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
