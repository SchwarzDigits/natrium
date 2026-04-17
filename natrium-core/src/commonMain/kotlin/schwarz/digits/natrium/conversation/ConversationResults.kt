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

package schwarz.digits.natrium.conversation

sealed class ConversationListResult {
    data class Success(val conversations: List<ConversationOperations>) : ConversationListResult()
    sealed class Failure : ConversationListResult() {
        data object NotLoggedIn : Failure()
        data class SyncFailed(val message: String) : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class FindConversationResult {
    data class Success(val conversationOperations: ConversationOperations) : FindConversationResult()
    sealed class Failure : FindConversationResult() {
        data object NotLoggedIn : Failure()
        data object NotFound : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class CreateConversationResult {
    data class Success(val conversationOperation: ConversationOperations) : CreateConversationResult()
    sealed class Failure : CreateConversationResult() {
        data object NotLoggedIn : Failure()
        data object Forbidden : Failure()
        data class InvalidTitle(val message: String) : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class JoinConversationResult {
    data class Success(val conversationOperations: ConversationOperations) : JoinConversationResult()
    sealed class Failure : JoinConversationResult() {
        data object NotLoggedIn : Failure()
        data object IncorrectPassword : Failure()
        data object InvalidLink : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class CloseConversationResult {
    data object Success : CloseConversationResult()
    sealed class Failure : CloseConversationResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class ConversationObserveResult {
    data object Success : ConversationObserveResult()
    sealed class Failure : ConversationObserveResult() {
        data object NotLoggedIn : Failure()
        data object ConversationNotFound : Failure()
    }
}

sealed class GetConversationInfoResult {
    data class Success(val conversationInfo: ConversationInfo) : GetConversationInfoResult()
    sealed class Failure : GetConversationInfoResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class SetTitleResult {
    data object Success : SetTitleResult()
    sealed class Failure : SetTitleResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class ArchiveConversationResult {
    data object Success : ArchiveConversationResult()
    sealed class Failure : ArchiveConversationResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class UnarchiveConversationResult {
    data object Success : UnarchiveConversationResult()
    sealed class Failure : UnarchiveConversationResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class DeleteConversationResult {
    data object Success : DeleteConversationResult()
    sealed class Failure : DeleteConversationResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class AddMemberResult {
    data object Success : AddMemberResult()
    sealed class Failure : AddMemberResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class RemoveMemberResult {
    data object Success : RemoveMemberResult()
    sealed class Failure : RemoveMemberResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class GetMembersResult {
    data class Success(val members: List<ConversationMember>) : GetMembersResult()
    sealed class Failure : GetMembersResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class GetJoinLinkResult {
    data class Success(val joinLink: JoinLink) : GetJoinLinkResult()
    sealed class Failure : GetJoinLinkResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class RevokeJoinLinkResult {
    data object Success : RevokeJoinLinkResult()
    sealed class Failure : RevokeJoinLinkResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}
