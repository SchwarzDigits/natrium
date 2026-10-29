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

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import schwarz.digits.natrium.chat.Option
import schwarz.digits.natrium.chat.Request
import schwarz.digits.natrium.chat.Response

// Wire DTOs — kept internal so the JSON format can evolve independently of the public API.
// `natrium` and `type` have no defaults on purpose: with encodeDefaults = false they would be dropped.

@Serializable
internal data class TextEnvelope(
    val natrium: Int,
    val type: String,
    val text: String,
    val attachments: List<String> = emptyList(),
)

@Serializable
internal data class RequestEnvelope(
    val natrium: Int,
    val type: String,
    val request: RequestDto,
)

@Serializable
internal data class ResponseEnvelope(
    val natrium: Int,
    val type: String,
    @SerialName("request_id") val requestId: String,
    val response: ResponseDto,
)

@Serializable
internal data class OptionDto(val id: String, val title: String)

@Serializable
internal sealed interface RequestDto {
    @Serializable
    @SerialName("SINGLE_SELECT")
    data class SingleSelect(
        val subject: String = "",
        val message: String,
        val options: List<OptionDto>,
        val deadline: Instant? = null,
    ) : RequestDto

    @Serializable
    @SerialName("MULTI_SELECT")
    data class MultiSelect(
        val subject: String = "",
        val message: String,
        val options: List<OptionDto>,
        val deadline: Instant? = null,
    ) : RequestDto

    @Serializable
    @SerialName("ASSET")
    data class Asset(
        val subject: String = "",
        val message: String,
        val deadline: Instant? = null,
    ) : RequestDto

    @Serializable
    @SerialName("APPOINTMENT")
    data class Appointment(
        val subject: String = "",
        val message: String,
        val from: Instant,
        val to: Instant,
        val deadline: Instant? = null,
    ) : RequestDto
}

@Serializable
internal sealed interface ResponseDto {
    @Serializable
    @SerialName("SINGLE_SELECT")
    data class SingleSelect(val selected: String) : ResponseDto

    @Serializable
    @SerialName("MULTI_SELECT")
    data class MultiSelect(val selected: List<String>) : ResponseDto

    @Serializable
    @SerialName("ASSET")
    data class Asset(val attachments: List<String> = emptyList()) : ResponseDto

    @Serializable
    @SerialName("APPOINTMENT")
    data class Appointment(val accepted: Boolean) : ResponseDto

    @Serializable
    @SerialName("FREE_TEXT")
    data class FreeText(val text: String, val attachments: List<String> = emptyList()) : ResponseDto
}

internal fun Request.toDto(): RequestDto = when (this) {
    is Request.SingleSelect -> RequestDto.SingleSelect(subject, message, options.map { it.toDto() }, deadline)
    is Request.MultiSelect -> RequestDto.MultiSelect(subject, message, options.map { it.toDto() }, deadline)
    is Request.Asset -> RequestDto.Asset(subject, message, deadline)
    is Request.Appointment -> RequestDto.Appointment(subject, message, from, to, deadline)
}

internal fun RequestDto.toDomain(): Request = when (this) {
    is RequestDto.SingleSelect -> Request.SingleSelect(subject, message, options.map { it.toDomain() }, deadline)
    is RequestDto.MultiSelect -> Request.MultiSelect(subject, message, options.map { it.toDomain() }, deadline)
    is RequestDto.Asset -> Request.Asset(subject, message, deadline)
    is RequestDto.Appointment -> Request.Appointment(subject, message, from, to, deadline)
}

internal fun Response.toDto(): ResponseDto = when (this) {
    is Response.SingleSelect -> ResponseDto.SingleSelect(selectedOptionId)
    is Response.MultiSelect -> ResponseDto.MultiSelect(selectedOptionIds)
    is Response.Asset -> ResponseDto.Asset(attachmentMessageIds)
    is Response.Appointment -> ResponseDto.Appointment(accepted)
    is Response.FreeText -> ResponseDto.FreeText(text, attachmentMessageIds)
}

internal fun ResponseDto.toDomain(): Response = when (this) {
    is ResponseDto.SingleSelect -> Response.SingleSelect(selected)
    is ResponseDto.MultiSelect -> Response.MultiSelect(selected)
    is ResponseDto.Asset -> Response.Asset(attachments)
    is ResponseDto.Appointment -> Response.Appointment(accepted)
    is ResponseDto.FreeText -> Response.FreeText(text, attachments)
}

private fun Option.toDto() = OptionDto(id, title)
private fun OptionDto.toDomain() = Option(id, title)
