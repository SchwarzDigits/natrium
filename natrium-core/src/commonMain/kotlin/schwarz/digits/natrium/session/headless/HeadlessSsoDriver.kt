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

package schwarz.digits.natrium.session.headless

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom

/** Why the headless SSO flow could not be completed. */
internal enum class HeadlessSsoFailure {
    /** A request could not be sent / the host was unreachable. */
    NETWORK,

    /** The HTTP/SAML dance did not end in the expected `wire://sso-login/success` redirect. */
    FLOW,
}

/** Result of driving the SSO/SAML flow to its terminal redirect. */
internal sealed interface HeadlessSsoOutcome {
    /** [cookie] is the value of the `cookie` query parameter of `wire://sso-login/success`. */
    data class Success(val cookie: String) : HeadlessSsoOutcome
    data class Failure(val reason: HeadlessSsoFailure, val detail: String? = null) : HeadlessSsoOutcome
}

/**
 * Imitates a browser through the SAML redirect/POST chain without ever opening one.
 *
 * Starting from the IdP authorization URL, it follows HTTP redirects and auto-submits SAML
 * POST-binding forms, persisting cookies along the way, until the Wire SP issues the terminal
 * `wire://sso-login/success/?cookie=…` (or `…/failure`) redirect. For every request to a
 * **non-Wire** host it invokes the consumer's [HeadlessSsoInterceptor] so they can inject the
 * parameters the external IdP component needs; Wire-internal requests are handled silently.
 *
 * @param wireHosts hosts considered Wire-internal (from [schwarz.digits.natrium.BackendConfig]).
 * @param engine test seam — inject a `MockEngine` to run hermetically; `null` uses the platform
 *   engine via [headlessHttpClient].
 * @param maxHops safety bound on the redirect/form chain length.
 */
internal class HeadlessSsoDriver(
    private val wireHosts: Set<String>,
    private val engine: HttpClientEngine? = null,
    private val maxHops: Int = 20,
) {

    suspend fun run(authorizationUrl: String, interceptor: HeadlessSsoInterceptor): HeadlessSsoOutcome {
        val client = newClient()
        try {
            var nextUrl = authorizationUrl
            var method = HeadlessSsoMethod.GET
            var formFields: Map<String, String> = emptyMap()

            repeat(maxHops) {
                val parsed = Url(nextUrl)
                val external = parsed.host !in wireHosts && !parsed.protocol.name.equals("wire", true)

                val injection = if (external) {
                    interceptor.intercept(
                        HeadlessSsoRequest(
                            url = nextUrl,
                            host = parsed.host,
                            method = method,
                            queryParameters = parsed.parameters.toStringMap(),
                            formFields = formFields,
                        ),
                    )
                } else {
                    HeadlessSsoInjection.None
                }

                val response: HttpResponse = try {
                    client.request(nextUrl) {
                        this.method = if (method == HeadlessSsoMethod.POST) HttpMethod.Post else HttpMethod.Get
                        injection.headers.forEach { (name, value) -> header(name, value) }
                        injection.queryParameters.forEach { (name, value) -> parameter(name, value) }
                        if (method == HeadlessSsoMethod.POST) {
                            val body = Parameters.build {
                                formFields.forEach { (name, value) -> append(name, value) }
                                injection.formFields.forEach { (name, value) -> append(name, value) }
                            }
                            setBody(FormDataContent(body))
                        }
                    }
                } catch (t: Throwable) {
                    return HeadlessSsoOutcome.Failure(HeadlessSsoFailure.NETWORK, t.message)
                }

                val status = response.status.value
                when {
                    status in 300..399 -> {
                        val location = response.headers[HttpHeaders.Location]
                            ?: return HeadlessSsoOutcome.Failure(HeadlessSsoFailure.FLOW, "redirect without Location")
                        val resolved = resolveUrl(nextUrl, location)
                        terminalOutcome(resolved)?.let { return it }
                        nextUrl = resolved
                        method = if (status == 307 || status == 308) method else HeadlessSsoMethod.GET
                        formFields = emptyMap()
                    }

                    status == 200 -> {
                        val form = SamlFormParser.parse(response.bodyAsText())
                            ?: return HeadlessSsoOutcome.Failure(
                                HeadlessSsoFailure.FLOW,
                                "200 response with neither redirect nor auto-submit form",
                            )
                        val action = resolveUrl(nextUrl, form.action.ifBlank { nextUrl })
                        terminalOutcome(action)?.let { return it }
                        nextUrl = action
                        method = form.method
                        formFields = form.fields
                    }

                    else -> return HeadlessSsoOutcome.Failure(HeadlessSsoFailure.FLOW, "unexpected HTTP status $status")
                }
            }
            return HeadlessSsoOutcome.Failure(HeadlessSsoFailure.FLOW, "exceeded $maxHops hops without terminal redirect")
        } finally {
            client.close()
        }
    }

    private fun newClient(): HttpClient {
        val config: HttpClientConfig<*>.() -> Unit = {
            followRedirects = false
            expectSuccess = false
            install(HttpCookies)
        }
        return engine?.let { HttpClient(it, config) } ?: headlessHttpClient(config)
    }

    /** Recognizes and decodes the terminal `wire://sso-login/{success,failure}` redirect. */
    private fun terminalOutcome(url: String): HeadlessSsoOutcome? {
        if (!url.startsWith("wire://", ignoreCase = true)) return null
        val parsed = Url(url)
        return when (parsed.segments.firstOrNull { it.isNotBlank() }?.lowercase()) {
            "success" -> {
                val cookie = parsed.parameters["cookie"]
                if (cookie.isNullOrBlank()) {
                    HeadlessSsoOutcome.Failure(HeadlessSsoFailure.FLOW, "SSO success redirect had no cookie")
                } else {
                    HeadlessSsoOutcome.Success(cookie)
                }
            }

            "failure" -> HeadlessSsoOutcome.Failure(
                HeadlessSsoFailure.FLOW,
                parsed.parameters["errorCode"] ?: "SSO failure",
            )

            else -> HeadlessSsoOutcome.Failure(HeadlessSsoFailure.FLOW, "unexpected wire redirect: $url")
        }
    }

    private fun resolveUrl(base: String, location: String): String =
        if (HAS_SCHEME.containsMatchIn(location)) location
        else URLBuilder(base).takeFrom(location).buildString()

    private fun Parameters.toStringMap(): Map<String, String> =
        entries().associate { (key, values) -> key to (values.firstOrNull() ?: "") }

    private companion object {
        private val HAS_SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
    }
}
