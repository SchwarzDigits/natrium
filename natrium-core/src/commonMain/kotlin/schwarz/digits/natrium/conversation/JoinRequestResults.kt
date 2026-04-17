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
