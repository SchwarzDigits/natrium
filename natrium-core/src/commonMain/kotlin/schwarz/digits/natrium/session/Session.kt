/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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