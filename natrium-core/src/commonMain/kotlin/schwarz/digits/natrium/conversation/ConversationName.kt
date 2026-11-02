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
package schwarz.digits.natrium.conversation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/**
 * Conversation title plus metadata in Wire's single conversation name field
 * (see docs/conversations.md). Without metadata the name is the plain title, so
 * other clients and older conversations read as before; with metadata it is a
 * JSON envelope:
 *
 * ```json
 * {"natrium":1,"title":"Building authority · BA-2026-0412","meta":{"case_number":"BA-2026-0412"}}
 * ```
 */
internal const val NAME_TOO_LONG = "Title and metadata exceed the conversation name limit"

internal object ConversationName {

    const val VERSION = 1

    /** Wire's limit for a conversation name. */
    const val MAX_LENGTH = 256

    private val json = Json

    data class Decoded(val title: String, val metadata: Map<String, String>)

    fun decode(name: String): Decoded {
        val plain = Decoded(name, emptyMap())
        if (!name.trimStart().startsWith("{")) return plain
        val obj = try {
            json.parseToJsonElement(name) as? JsonObject
        } catch (_: Exception) {
            null
        } ?: return plain
        val version = (obj["natrium"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull ?: return plain
        if (version > VERSION) return plain
        val title = (obj["title"] as? JsonPrimitive)?.contentOrNull ?: ""
        val meta = (obj["meta"] as? JsonObject)
            ?.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.takeIf { it.isString }?.let { k to it.content } }
            ?.toMap()
            .orEmpty()
        return Decoded(title, meta)
    }

    /** The name for [title] and [metadata]; the plain title when there is no metadata. */
    fun encode(title: String, metadata: Map<String, String>): String {
        if (metadata.isEmpty()) return title
        return buildJsonObject {
            put("natrium", VERSION)
            if (title.isNotEmpty()) put("title", title)
            put("meta", buildJsonObject { metadata.forEach { (k, v) -> put(k, v) } })
        }.toString()
    }
}
