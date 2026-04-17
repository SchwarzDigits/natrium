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

package schwarz.digits.natrium.conversation

/** Extracts code and key from a Kalium guest link URL (e.g. https://…?key=X&code=Y). */
internal fun parseGuestLink(url: String): Pair<String, String>? {
    val query = url.substringAfter("?", "")
    if (query.isEmpty()) return null
    val params = query.split("&").mapNotNull {
        val parts = it.split("=", limit = 2)
        if (parts.size == 2) parts[0] to parts[1] else null
    }.toMap()
    val code = params["code"] ?: return null
    val key = params["key"] ?: return null
    return code to key
}
