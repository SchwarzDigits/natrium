/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
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