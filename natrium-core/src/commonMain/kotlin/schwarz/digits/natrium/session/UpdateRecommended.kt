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

package schwarz.digits.natrium.session

/**
 * Natrium received content that this version cannot read because it is newer: a higher protocol
 * version, or a message, request or response type it does not know yet. The content itself arrives
 * as [schwarz.digits.natrium.chat.MessageValue.UnsupportedValue]; this is the signal to ask the user
 * once to update the app, instead of at every such message.
 *
 * [highestSeenVersion] is the highest protocol version seen so far; it equals [supportedVersion] when
 * only an unknown type of the same version arrived.
 */
data class UpdateRecommended(
    val supportedVersion: Int,
    val highestSeenVersion: Int,
)
