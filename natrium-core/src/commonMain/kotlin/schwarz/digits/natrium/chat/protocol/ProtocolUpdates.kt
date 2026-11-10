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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import schwarz.digits.natrium.session.UpdateRecommended

/**
 * Remembers that newer content arrived (see [UpdateRecommended]). Process-wide, like the protocol
 * itself: a newer peer is newer for every session of this app.
 */
internal object ProtocolUpdates {
    private val state = MutableStateFlow<UpdateRecommended?>(null)
    val updates: StateFlow<UpdateRecommended?> = state.asStateFlow()

    fun report(version: Int) {
        state.update { current ->
            val highest = maxOf(version, current?.highestSeenVersion ?: MessageProtocol.VERSION)
            if (current != null && highest == current.highestSeenVersion) current
            else UpdateRecommended(MessageProtocol.VERSION, highest)
        }
    }

    /** Back to "nothing newer seen" — for tests, which share this object. */
    fun reset() {
        state.value = null
    }
}
