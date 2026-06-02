/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.conversation

sealed class RequestToJoinResult {
    data class Success(val joinRequest: JoinRequest) : RequestToJoinResult()
    sealed class Failure : RequestToJoinResult() {
        data object NotLoggedIn : Failure()
        data object ConversationNotFound : Failure()
        data object AlreadyRequested : Failure()
        data object AlreadyMember : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class ListMyJoinRequestsResult {
    data class Success(val requests: List<JoinRequest>) : ListMyJoinRequestsResult()
    sealed class Failure : ListMyJoinRequestsResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class CancelJoinRequestResult {
    data object Success : CancelJoinRequestResult()
    sealed class Failure : CancelJoinRequestResult() {
        data object NotLoggedIn : Failure()
        data object NotFound : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class ListJoinRequestsResult {
    data class Success(val requests: List<JoinRequest>) : ListJoinRequestsResult()
    sealed class Failure : ListJoinRequestsResult() {
        data object NotLoggedIn : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class ApproveJoinRequestResult {
    data object Success : ApproveJoinRequestResult()
    sealed class Failure : ApproveJoinRequestResult() {
        data object NotLoggedIn : Failure()
        data object NotFound : Failure()
        data object AlreadyHandled : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}

sealed class RejectJoinRequestResult {
    data object Success : RejectJoinRequestResult()
    sealed class Failure : RejectJoinRequestResult() {
        data object NotLoggedIn : Failure()
        data object NotFound : Failure()
        data object AlreadyHandled : Failure()
        data class Unknown(val message: String, val cause: Throwable? = null) : Failure()
    }
}
