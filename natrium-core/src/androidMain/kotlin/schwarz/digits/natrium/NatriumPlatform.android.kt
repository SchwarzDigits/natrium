/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
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
