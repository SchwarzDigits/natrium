/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.session.LoginResult
import schwarz.digits.natrium.session.SSOLoginError
import schwarz.digits.natrium.session.SSOLoginResult

class SSOLoginCommand : CliktCommand(name = "sso-login") {

    private val email: String? by option(
        "-e", "--email",
        help = "Email address for SSO domain lookup"
    )

    private val code: String? by option(
        "-c", "--code",
        help = "SSO code (e.g. wire-<uuid>)"
    )

    override fun run() = runBlocking {
        if (email == null && code == null) {
            echo("Either --email or --code must be provided")
            return@runBlocking
        }

        echo("Initiating SSO login ...")

        val ssoResult = if (email != null) {
            Natrium.ssoLogin(email!!)
        } else {
            Natrium.ssoLoginWithCode(code!!)
        }

        when (ssoResult) {
            is SSOLoginResult.Success -> {
                echo("Open this URL in your browser to authenticate:")
                echo(ssoResult.authorizationUrl)
                echo()
                echo("After authentication, paste the cookie value here:")
                echo("Cookie: ", trailingNewline = false)
                val cookie = readln().trim()

                if (cookie.isBlank()) {
                    echo("No cookie provided. Login aborted.")
                    return@runBlocking
                }

                echo("Completing SSO login ...")
                val loginResult = Natrium.completeSSOLogin(cookie)

                when (loginResult) {
                    is LoginResult.Success -> {
                        val info = loginResult.session.sessionInfo()
                        echo("Login successful")
                        echo("User: ${info?.user?.name ?: "unknown"}")
                        echo("Device: ${info?.device?.id ?: "unknown"}")
                    }
                    is LoginResult.Failure.TooManyDevices -> {
                        echo("Too many devices registered. Remove a device and try again.")
                    }
                    is LoginResult.Failure.Error -> {
                        echo("Login failed: ${loginResult.reason}")
                    }
                }
            }
            is SSOLoginResult.Failure.Error -> {
                when (ssoResult.reason) {
                    SSOLoginError.SSO_NOT_AVAILABLE -> echo("SSO is not available for this domain")
                    SSOLoginError.INVALID_CODE -> echo("Invalid SSO code")
                    SSOLoginError.INVALID_CODE_FORMAT -> echo("Invalid SSO code format")
                    SSOLoginError.SERVER_VERSION_NOT_SUPPORTED -> echo("Server version not supported")
                    SSOLoginError.APP_UPDATE_REQUIRED -> echo("App update required")
                    SSOLoginError.CONNECTION_ERROR -> echo("Connection error")
                }
            }
        }
    }
}
