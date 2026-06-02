/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
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
