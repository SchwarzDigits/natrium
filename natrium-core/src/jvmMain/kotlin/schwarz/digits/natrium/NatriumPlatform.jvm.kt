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
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import schwarz.digits.natrium.lifecycle.AppLifecycleState

actual class NatriumPlatform {

    internal actual val applicationId: String? = null

    internal actual val platformName: String = "JVM"

    internal actual fun initialize(): CoreLogic {
        val rootPath = "${System.getProperty("user.home")}/.natrium"
        return CoreLogic(
            rootPath = rootPath,
            kaliumConfigs = KaliumConfigs(
                shouldEncryptData = { false },
                enableCalling = false
            ),
            userAgent = "Natrium/0.1.0 (JVM)"
        )
    }

    internal actual suspend fun observeLifecycle(): Flow<AppLifecycleState> = flow {
        // JVM/Desktop has no lifecycle concept — app is always active
        emit(AppLifecycleState.ACTIVE)
        awaitCancellation()
    }
}
