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
import schwarz.digits.natrium.session.UpdateEmailResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking

class UpdateEmailCommand : CliktCommand(name = "update-email") {

    private val email by option("--email", help = "New email address").required()

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) {
            echo("Not logged in")
            return@runBlocking
        }
        when (val result = session.updateEmail(email)) {
            is UpdateEmailResult.Success ->
                echo("Verification email sent — check your inbox to confirm")
            is UpdateEmailResult.Failure.InvalidEmail ->
                echo("Invalid email address")
            is UpdateEmailResult.Failure.EmailAlreadyInUse ->
                echo("Email already in use")
            is UpdateEmailResult.Failure.Unknown ->
                echo("Failed to update email: ${result.message}")
        }
    }
}
