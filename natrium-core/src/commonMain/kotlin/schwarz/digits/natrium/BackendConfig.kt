/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium

import com.wire.kalium.logic.configuration.server.ServerConfig.Links

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
}
