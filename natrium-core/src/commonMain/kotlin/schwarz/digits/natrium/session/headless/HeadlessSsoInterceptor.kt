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

/**
 * Lets a consumer inject parameters into the requests Natrium makes while driving the SSO/SAML
 * flow without a browser (see [schwarz.digits.natrium.Natrium.ssoLoginHeadless]).
 *
 * It is called **once per outgoing request to a non-Wire host** — i.e. to the external IdP /
 * backend component that simulates the SAML responses. Requests to Wire's own hosts are handled
 * internally and are never surfaced here nor made editable.
 *
 * The type is deliberately transport-agnostic: no Ktor (or any other HTTP-library) types leak
 * through the [HeadlessSsoRequest] / [HeadlessSsoInjection] boundary. As a single-method
 * (`fun interface`) it maps to a plain closure in Kotlin and to a one-method protocol in Swift.
 */
public fun interface HeadlessSsoInterceptor {
    /**
     * @param request read-only description of the request Natrium is about to send to an
     *   external host.
     * @return the parameters to add to that request. Return [HeadlessSsoInjection.None] to send
     *   the request unchanged.
     */
    public fun intercept(request: HeadlessSsoRequest): HeadlessSsoInjection
}

/** HTTP method of a headless SSO request. */
public enum class HeadlessSsoMethod { GET, POST }

/**
 * Read-only view of an outgoing external request in the headless SSO flow.
 *
 * @property url full request URL (including its existing query).
 * @property host the request host (always a non-Wire host when passed to an interceptor).
 * @property method [HeadlessSsoMethod.GET] or [HeadlessSsoMethod.POST].
 * @property queryParameters the query parameters already present on [url].
 * @property formFields the form fields already present (non-empty only for a POST form submit,
 *   e.g. the `SAMLResponse` / `RelayState` of a SAML POST binding).
 */
public class HeadlessSsoRequest internal constructor(
    public val url: String,
    public val host: String,
    public val method: HeadlessSsoMethod,
    public val queryParameters: Map<String, String>,
    public val formFields: Map<String, String>,
)

/**
 * The parameters a [HeadlessSsoInterceptor] wants to add to a request. Built via [Builder];
 * use [None] for a no-op. Values are *added* to the request (existing values are kept).
 */
public class HeadlessSsoInjection private constructor(
    public val headers: Map<String, String>,
    public val queryParameters: Map<String, String>,
    public val formFields: Map<String, String>,
) {
    public class Builder {
        private val headers = mutableMapOf<String, String>()
        private val queryParameters = mutableMapOf<String, String>()
        private val formFields = mutableMapOf<String, String>()

        /** Add (or overwrite) an HTTP header. */
        public fun header(name: String, value: String): Builder = apply { headers[name] = value }

        /** Add (or overwrite) a URL query parameter. */
        public fun queryParameter(name: String, value: String): Builder =
            apply { queryParameters[name] = value }

        /** Add (or overwrite) a form field (applies to POST form submits). */
        public fun formField(name: String, value: String): Builder =
            apply { formFields[name] = value }

        public fun build(): HeadlessSsoInjection =
            HeadlessSsoInjection(headers.toMap(), queryParameters.toMap(), formFields.toMap())
    }

    public companion object {
        /** Inject nothing — send the request unchanged. */
        public val None: HeadlessSsoInjection =
            HeadlessSsoInjection(emptyMap(), emptyMap(), emptyMap())
    }
}
