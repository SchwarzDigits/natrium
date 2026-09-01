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

package schwarz.digits.natrium

import com.wire.kalium.logic.CoreLogic
import kotlinx.coroutines.flow.Flow
import schwarz.digits.natrium.lifecycle.AppLifecycleState

public expect class NatriumPlatform {

    internal fun initialize(): CoreLogic

    internal suspend fun observeLifecycle(): Flow<AppLifecycleState>

    internal val applicationId: String?

    internal val platformName: String
}