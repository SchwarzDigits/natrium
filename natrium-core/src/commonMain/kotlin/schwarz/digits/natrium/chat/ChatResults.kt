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

sealed class SendMessageResult {
    data object Success : SendMessageResult()
    sealed class Failure : SendMessageResult() {
        data object NotLoggedIn : Failure()
        data object DisabledByTeam : Failure()
        data object RestrictedFileType : Failure()
        data class FileTooLarge(val fileSizeBytes: Long, val limitBytes: Long) : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class ChatHistoryResult {
    data class Success(val messages: List<ChatMessage>) : ChatHistoryResult()
    sealed class Failure : ChatHistoryResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class ToggleReactionResult {
    data object Success : ToggleReactionResult()
    sealed class Failure : ToggleReactionResult() {
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}