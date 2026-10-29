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

package schwarz.digits.natrium.chat

import kotlinx.datetime.Instant

/**
 * A structured request sent to the other party of a conversation.
 * Every request can be answered with the matching [Response] type or with [Response.FreeText].
 */
sealed class Request {
    abstract val subject: String
    abstract val message: String
    abstract val deadline: Instant?

    data class SingleSelect(
        override val subject: String = "",
        override val message: String,
        val options: List<Option>,
        override val deadline: Instant? = null,
    ) : Request()

    data class MultiSelect(
        override val subject: String = "",
        override val message: String,
        val options: List<Option>,
        override val deadline: Instant? = null,
    ) : Request()

    /** Asks the other party to upload one or more files. */
    data class Asset(
        override val subject: String = "",
        override val message: String,
        override val deadline: Instant? = null,
    ) : Request()

    /** Proposes an appointment that the other party accepts or declines. */
    data class Appointment(
        override val subject: String = "",
        override val message: String,
        val from: Instant,
        val to: Instant,
        override val deadline: Instant? = null,
    ) : Request()
}

/** A selectable option. Responses reference the stable [id], never the [title]. */
data class Option(
    val id: String,
    val title: String,
)

/** An answer to a [Request]. [FreeText] is accepted for every request type. */
sealed class Response {
    data class SingleSelect(val selectedOptionId: String) : Response()
    data class MultiSelect(val selectedOptionIds: List<String>) : Response()
    data class Asset(val attachmentMessageIds: List<String> = emptyList()) : Response()
    data class Appointment(val accepted: Boolean) : Response()
    data class FreeText(
        val text: String,
        val attachmentMessageIds: List<String> = emptyList(),
    ) : Response()
}

enum class InvalidResponseReason {
    /** The referenced message is not known in this conversation. */
    REQUEST_NOT_FOUND,
    /** The referenced message is not a [MessageValue.RequestValue]. */
    NOT_A_REQUEST,
    /** The request already has a response. */
    ALREADY_ANSWERED,
    /** The response type does not match the request type (and is not [Response.FreeText]). */
    TYPE_MISMATCH,
    /** A selected option id is not part of the request's options. */
    UNKNOWN_OPTION,
    /** A [Response.MultiSelect] without any selected option. */
    EMPTY_SELECTION,
    /** A [Response.Asset] without any attachment. */
    MISSING_ATTACHMENTS,
}
