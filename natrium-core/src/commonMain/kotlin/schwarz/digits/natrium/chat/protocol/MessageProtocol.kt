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

package schwarz.digits.natrium.chat.protocol

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import schwarz.digits.natrium.chat.MessageValue
import schwarz.digits.natrium.chat.Response

/**
 * Wire format of Natrium text messages (see docs/requests.md): every text is a JSON envelope
 * carrying the protocol version in field `natrium`.
 */
internal object MessageProtocol {

    const val VERSION = 1

    private const val TYPE_TEXT = "TEXT"
    private const val TYPE_REQUEST = "REQUEST"
    private const val TYPE_RESPONSE = "RESPONSE"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    /** Encodes a sendable value; returns null for values that are not transported as text. */
    fun encode(value: MessageValue): String? = when (value) {
        is MessageValue.TextValue ->
            json.encodeToString(TextEnvelope(VERSION, TYPE_TEXT, value.value, value.attachmentMessageIds))
        is MessageValue.RequestValue ->
            json.encodeToString(
                RequestEnvelope(VERSION, TYPE_REQUEST, value.request.toDto(), value.attachmentMessageIds)
            )
        is MessageValue.ResponseValue ->
            json.encodeToString(
                ResponseEnvelope(VERSION, TYPE_RESPONSE, value.requestMessageId, value.response.toDto())
            )
        is MessageValue.FileValue,
        is MessageValue.LocationValue,
        is MessageValue.KnockValue,
        is MessageValue.SystemValue,
        is MessageValue.UnsupportedValue -> null
    }

    /** Decodes a received text. Text that is not a Natrium envelope is returned as plain [MessageValue.TextValue]. */
    fun decode(text: String): MessageValue {
        if (!text.trimStart().startsWith("{")) return MessageValue.TextValue(text)
        val obj = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: return MessageValue.TextValue(text)

        val version = obj["natrium"]?.primitiveOrNull()?.intOrNull ?: return MessageValue.TextValue(text)
        val type = obj["type"]?.primitiveOrNull()?.contentOrNull
        if (version > VERSION) return MessageValue.UnsupportedValue(version, type)

        return try {
            when (type) {
                TYPE_TEXT -> json.decodeFromJsonElement(TextEnvelope.serializer(), obj).let {
                    MessageValue.TextValue(it.text, it.attachments)
                }
                TYPE_REQUEST -> json.decodeFromJsonElement(RequestEnvelope.serializer(), obj).let {
                    MessageValue.RequestValue(it.request.toDomain(), it.attachments)
                }
                TYPE_RESPONSE -> json.decodeFromJsonElement(ResponseEnvelope.serializer(), obj).let {
                    MessageValue.ResponseValue(it.requestId, it.response.toDomain())
                }
                else -> MessageValue.UnsupportedValue(version, type)
            }
        } catch (_: SerializationException) {
            MessageValue.UnsupportedValue(version, type)
        } catch (_: IllegalArgumentException) {
            MessageValue.UnsupportedValue(version, type)
        }
    }

    /** Human-readable preview of a text message, e.g. for quotes. */
    fun previewText(text: String): String? = when (val value = decode(text)) {
        is MessageValue.TextValue -> value.value
        is MessageValue.RequestValue -> value.request.subject.ifEmpty { value.request.message }
        is MessageValue.ResponseValue -> (value.response as? Response.FreeText)?.text
        else -> null
    }

    private fun kotlinx.serialization.json.JsonElement.primitiveOrNull() =
        try {
            jsonPrimitive
        } catch (_: IllegalArgumentException) {
            null
        }
}
