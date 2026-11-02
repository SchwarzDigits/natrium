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
 * [message] is the question to the other party; [subject] is an optional short title.
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

    /**
     * Asks the other party to upload files. [document] names what is expected (e.g. "signed site plan"),
     * [mimeTypes] optionally restricts the accepted file types (e.g. `application/pdf`); empty means any.
     */
    data class Asset(
        override val subject: String = "",
        override val message: String,
        val document: String = "",
        val mimeTypes: List<String> = emptyList(),
        override val deadline: Instant? = null,
    ) : Request()

    /** Offers appointment [slots]; the other party picks exactly one. */
    data class Appointment(
        override val subject: String = "",
        override val message: String,
        val slots: List<AppointmentSlot>,
        val location: String = "",
        override val deadline: Instant? = null,
    ) : Request()

    /** Asks the other party to confirm the fixed sentence [confirmation]. */
    data class Acknowledge(
        override val subject: String = "",
        override val message: String,
        val confirmation: String,
        override val deadline: Instant? = null,
    ) : Request()

    /** Asks a question that is answered with [Response.FreeText]; [maxLength] limits the answer if set. */
    data class FreeText(
        override val subject: String = "",
        override val message: String,
        val maxLength: Int? = null,
        override val deadline: Instant? = null,
    ) : Request()
}

/** A selectable option. Responses reference the stable [id], never the [title]. */
data class Option(
    val id: String,
    val title: String,
)

/** A proposed appointment. Responses reference the stable [id]. */
data class AppointmentSlot(
    val id: String,
    val from: Instant,
    val to: Instant,
)

/** An answer to a [Request]. [FreeText] is accepted for every request type. */
sealed class Response {
    data class SingleSelect(val selectedOptionId: String) : Response()
    data class MultiSelect(val selectedOptionIds: List<String>) : Response()
    data class Asset(val attachmentMessageIds: List<String> = emptyList()) : Response()
    data class Appointment(val selectedSlotId: String) : Response()
    data object Acknowledge : Response()
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
    /** A selected option or slot id is not part of the request. */
    UNKNOWN_OPTION,
    /** A [Response.MultiSelect] without any selected option. */
    EMPTY_SELECTION,
    /** A [Response.Asset] without any attachment. */
    MISSING_ATTACHMENTS,
    /** A [Response.FreeText] longer than the [Request.FreeText.maxLength] of the request. */
    TEXT_TOO_LONG,
}
