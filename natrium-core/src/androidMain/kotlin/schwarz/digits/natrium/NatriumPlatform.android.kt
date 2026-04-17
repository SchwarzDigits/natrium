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

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import schwarz.digits.natrium.lifecycle.AppLifecycleState

actual class NatriumPlatform(private val context: Context) {

    internal actual val applicationId: String?
        get() = context.packageName

    internal actual val platformName: String = "Android"

    internal actual fun initialize(): CoreLogic {
        val rootPath = context.getDir("accounts", Context.MODE_PRIVATE).path
        return CoreLogic(
            userAgent = "Natrium/0.1.0 (Android)",
            appContext = context,
            rootPath = rootPath,
            kaliumConfigs = KaliumConfigs(
                shouldEncryptData = { false }
            )
        )
    }

    internal actual suspend fun observeLifecycle(): Flow<AppLifecycleState> = callbackFlow {
        val initialState = if (
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        ) AppLifecycleState.ACTIVE else AppLifecycleState.INACTIVE
        trySend(initialState)

        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                trySend(AppLifecycleState.ACTIVE)
            }
            override fun onStop(owner: LifecycleOwner) {
                trySend(AppLifecycleState.INACTIVE)
            }
        }
        withContext(Dispatchers.Main){
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        }
        awaitClose {
            GlobalScope.launch(Dispatchers.Main) {
                ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
            }
        }
    }
}
