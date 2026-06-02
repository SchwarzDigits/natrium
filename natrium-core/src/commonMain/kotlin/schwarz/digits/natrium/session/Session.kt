/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.session

import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.conversation.ConversationManager
import schwarz.digits.natrium.devices.DeviceManager

interface Session {
    val conversationManager: ConversationManager
    val deviceManager: DeviceManager

    suspend fun sessionInfo(): SessionInfo?
    fun observeSessionInfo(listener: (SessionInfo) -> Unit): Cancellable
    suspend fun updateDisplayName(name: String): UpdateDisplayNameResult
    suspend fun updateHandle(handle: String): UpdateHandleResult
    suspend fun updateEmail(email: String): UpdateEmailResult
    suspend fun logout(): LogoutResult
}