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
import schwarz.digits.natrium.session.UpdateHandleResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking

class UpdateHandleCommand : CliktCommand(name = "update-handle") {

    private val handle by option("--handle", help = "New user handle").required()

    override fun run() = runBlocking {
        val session = Natrium.restoreLastSession()
        if (session == null) {
            echo("Not logged in")
            return@runBlocking
        }
        when (val result = session.updateHandle(handle)) {
            is UpdateHandleResult.Success ->
                echo("Handle updated")
            is UpdateHandleResult.Failure.InvalidHandle ->
                echo("Invalid handle format")
            is UpdateHandleResult.Failure.HandleExists ->
                echo("Handle already taken")
            is UpdateHandleResult.Failure.Unknown ->
                echo("Failed to update handle: ${result.message}")
        }
    }
}
