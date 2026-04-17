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
