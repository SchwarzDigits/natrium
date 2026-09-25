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

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeadlessSsoDriverTest {

    private val wireHost = "nginz-https.example-wire.cloud"
    private val idpHost = "idp.example.com"

    @Test
    fun drivesSamlPostBindingAndReturnsDecodedCookie() = runTest {
        val seenExternalHosts = mutableListOf<String>()

        val engine = MockEngine { request ->
            when (request.url.host) {
                idpHost -> respond(
                    content = """
                        <html><body onload="document.forms[0].submit()">
                          <form method="post" action="https://$wireHost/sso/finalize-login">
                            <input type="hidden" name="SAMLResponse" value="BASE64==" />
                            <input type="hidden" name="RelayState" value="rs-123" />
                          </form>
                        </body></html>
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/html"),
                )

                wireHost -> respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(
                        HttpHeaders.Location,
                        "wire://sso-login/success/?cookie=zuid%3Dabc123&userId=u1&location=cfg1",
                    ),
                )

                else -> respond("unexpected ${request.url}", HttpStatusCode.InternalServerError)
            }
        }

        val interceptor = HeadlessSsoInterceptor { request ->
            seenExternalHosts += request.host
            HeadlessSsoInjection.Builder().queryParameter("tenant", "acme").build()
        }

        val outcome = HeadlessSsoDriver(wireHosts = setOf(wireHost), engine = engine)
            .run("https://$idpHost/authorize?SAMLRequest=abc", interceptor)

        assertTrue(outcome is HeadlessSsoOutcome.Success, "expected Success but was $outcome")
        assertEquals("zuid=abc123", outcome.cookie)
        // Core invariant: the interceptor fires ONLY for the external IdP host, never Wire hosts.
        assertEquals(listOf(idpHost), seenExternalHosts)
    }

    @Test
    fun mapsWireFailureRedirectToFlowFailure() = runTest {
        val engine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.Found,
                headers = headersOf(HttpHeaders.Location, "wire://sso-login/failure/?errorCode=nope"),
            )
        }

        val outcome = HeadlessSsoDriver(wireHosts = setOf(wireHost), engine = engine)
            .run("https://$idpHost/authorize", HeadlessSsoInterceptor { HeadlessSsoInjection.None })

        assertTrue(outcome is HeadlessSsoOutcome.Failure, "expected Failure but was $outcome")
        assertEquals(HeadlessSsoFailure.FLOW, outcome.reason)
    }
}
