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
