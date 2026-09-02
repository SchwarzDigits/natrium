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
