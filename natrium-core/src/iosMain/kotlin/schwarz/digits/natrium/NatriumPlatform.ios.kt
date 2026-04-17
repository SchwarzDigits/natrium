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
