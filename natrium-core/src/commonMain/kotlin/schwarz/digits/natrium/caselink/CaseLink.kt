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

package schwarz.digits.natrium.caselink

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import schwarz.digits.natrium.users.UserId

/**
 * A link that lets another party start a conversation with this user — e.g. attached to an
 * application, so the receiving side can open a case for it (see docs/case-links.md).
 *
 * [ref] is the link's unique id, generated anew for every link; the receiving side stores it to
 * find the conversation again when the same link is opened twice.
 */
data class CaseLink(
    val url: String,
    val ref: String,
)

sealed class GenerateCaseLinkResult {
    data class Success(val caseLink: CaseLink) : GenerateCaseLinkResult()
    sealed class Failure : GenerateCaseLinkResult() {
        /** [schwarz.digits.natrium.BackendConfig.caseLinkBase] is not set. */
        data object NotConfigured : Failure()
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

/**
 * Link format: `<caseLinkBase>#<token>`, the token being base64url (no padding) of
 * `{"v":1,"user":"<value@domain>","ref":"<uuid>"}`. The token sits in the fragment, so it is not
 * sent to the server that serves the page.
 */
@OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
internal object CaseLinkCodec {

    const val VERSION = 1

    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    data class Payload(val user: UserId, val ref: String)

    fun newRef(): String = Uuid.random().toString()

    fun encode(base: String, payload: Payload): String {
        val json = buildJsonObject {
            put("v", VERSION)
            put("user", payload.user.toString())
            put("ref", payload.ref)
        }.toString()
        return "$base#${base64.encode(json.encodeToByteArray())}"
    }

    /** Reads a link or a bare token; null if it is not a case link of a supported version. */
    fun decode(linkOrToken: String): Payload? = try {
        val token = linkOrToken.substringAfter('#').trim()
        val obj = Json.parseToJsonElement(base64.decode(token).decodeToString()) as? JsonObject
        val version = (obj?.get("v") as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
        val user = (obj?.get("user") as? JsonPrimitive)?.takeIf { it.isString }?.content
        val ref = (obj?.get("ref") as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (version == null || version > VERSION || user == null || ref.isNullOrBlank()) null
        else Payload(UserId.fromString(user), ref)
    } catch (_: Exception) {
        null
    }
}
