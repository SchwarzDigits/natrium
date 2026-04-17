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
