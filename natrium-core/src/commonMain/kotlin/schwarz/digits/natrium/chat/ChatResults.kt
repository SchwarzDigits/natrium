/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
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