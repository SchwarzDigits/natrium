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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.session.LoginResult
import schwarz.digits.natrium.session.headless.HeadlessSsoInjection
import schwarz.digits.natrium.session.headless.HeadlessSsoInterceptor

/**
 * Browserless SSO login. Natrium imitates the browser and drives the SAML flow itself; the
 * `--query` / `--header` / `--form` options are injected into every request to the external IdP
 * component (never into Wire-internal requests).
 */
class SSOLoginHeadlessCommand : CliktCommand(name = "sso-login-headless") {

    private val code: String by option(
        "-c", "--code",
        help = "SSO code (e.g. wire-<uuid>)",
    ).required()

    private val userId: String? by option(
        "--user-id",
        help = "Stub IdP: injected as the ${StubIdpHeaders.USER_ID} header",
    )

    private val stubEmail: String? by option(
        "--email",
        help = "Stub IdP: injected as the ${StubIdpHeaders.EMAIL} header",
    )

    private val displayName: String? by option(
        "--display-name",
        help = "Stub IdP: injected as the ${StubIdpHeaders.DISPLAY_NAME} header",
    )

    private val queryParams: List<String> by option(
        "-q", "--query",
        help = "Inject a query parameter into external (IdP) requests: key=value (repeatable)",
    ).multiple()

    private val headerParams: List<String> by option(
        "-H", "--header",
        help = "Inject a header into external (IdP) requests: key=value (repeatable)",
    ).multiple()

    private val formParams: List<String> by option(
        "-f", "--form",
        help = "Inject a form field into external (IdP) POST submits: key=value (repeatable)",
    ).multiple()

    override fun run() = runBlocking {
        val queries = queryParams.parsePairs()
        val headers = headerParams.parsePairs()
        val forms = formParams.parsePairs()

        val interceptor = HeadlessSsoInterceptor { request ->
            echo("→ injecting into external host: ${request.host}")
            HeadlessSsoInjection.Builder()
                .apply {
                    userId?.let { header(StubIdpHeaders.USER_ID, it) }
                    stubEmail?.let { header(StubIdpHeaders.EMAIL, it) }
                    displayName?.let { header(StubIdpHeaders.DISPLAY_NAME, it) }
                    queries.forEach { (key, value) -> queryParameter(key, value) }
                    headers.forEach { (key, value) -> header(key, value) }
                    forms.forEach { (key, value) -> formField(key, value) }
                }
                .build()
        }

        echo("Starting headless SSO login ...")
        when (val result = Natrium.ssoLoginHeadless(code, interceptor)) {
            is LoginResult.Success -> {
                val info = result.session.sessionInfo()
                echo("Login successful")
                echo("User: ${info?.user?.name ?: "unknown"}")
                echo("Device: ${info?.device?.id ?: "unknown"}")
            }
            is LoginResult.Failure.TooManyDevices -> {
                echo("Too many devices registered. Remove a device and try again.")
            }
            is LoginResult.Failure.Error -> {
                echo("Login failed: ${result.reason}")
            }
        }
    }

    private fun List<String>.parsePairs(): List<Pair<String, String>> = mapNotNull { entry ->
        val index = entry.indexOf('=')
        if (index <= 0) {
            echo("Ignoring malformed key=value entry: '$entry'")
            null
        } else {
            entry.substring(0, index) to entry.substring(index + 1)
        }
    }
}

/** Headers the backend stub IdP expects for headless SSO (contract provided by the backend team). */
private object StubIdpHeaders {
    const val USER_ID = "X-Stub-User-Id"
    const val EMAIL = "X-Stub-Email"
    const val DISPLAY_NAME = "X-Stub-Display-Name"
}
