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
