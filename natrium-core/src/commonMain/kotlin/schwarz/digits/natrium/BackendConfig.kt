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

package schwarz.digits.natrium

import com.wire.kalium.logic.configuration.server.ServerConfig.Links
import io.ktor.http.Url

class BackendConfig(
    val name: String,
    val api: String,
    val accounts: String,
    val webSocket: String,
    val teams: String,
    val blackList: String,
    val website: String,
    val isOnPremises: Boolean = true,
) {
    internal val kaliumLinks: Links = Links(
        api = api,
        accounts = accounts,
        webSocket = webSocket,
        teams = teams,
        blackList = blackList,
        website = website,
        title = name,
        isOnPremises = isOnPremises,
        apiProxy = null,
    )

    /**
     * Hosts that belong to Wire (the backend itself). Used by the headless SSO flow to tell
     * Wire-internal requests apart from requests to the external IdP: only the latter are
     * surfaced to the consumer's [schwarz.digits.natrium.session.headless.HeadlessSsoInterceptor].
     */
    internal val wireHosts: Set<String> =
        listOf(api, accounts, webSocket, teams, blackList, website)
            .mapNotNull { runCatching { Url(it).host }.getOrNull() }
            .filter { it.isNotBlank() }
            .toSet()
}
