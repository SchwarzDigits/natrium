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
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSBundle
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import schwarz.digits.natrium.lifecycle.AppLifecycleState

actual class NatriumPlatform {

    internal actual val applicationId: String?
        get() = NSBundle.mainBundle.bundleIdentifier

    internal actual val platformName: String = "iOS"

    internal actual fun initialize(): CoreLogic {
        val rootPath = "${NSHomeDirectory()}/Documents/natrium"

        return CoreLogic(
            rootPath = rootPath,
            kaliumConfigs = KaliumConfigs(
                shouldEncryptData = { false },
                enableCalling = false,
                wipeOnCookieInvalid = true,
            ),
            userAgent = "Natrium/0.1.0 (iOS)"
        )
    }

    internal actual suspend fun observeLifecycle(): Flow<AppLifecycleState> = callbackFlow {
        val initialState = if (
            UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive
        ) AppLifecycleState.ACTIVE else AppLifecycleState.INACTIVE
        trySend(initialState)

        val foreground = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { trySend(AppLifecycleState.ACTIVE) }
        )
        val background = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { trySend(AppLifecycleState.INACTIVE) }
        )

        awaitClose {
            NSNotificationCenter.defaultCenter.removeObserver(foreground)
            NSNotificationCenter.defaultCenter.removeObserver(background)
        }
    }
}
