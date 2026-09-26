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
import com.wire.kalium.persistence.kmmSettings.ApplePersistenceConfig
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
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import okio.ByteString.Companion.encodeUtf8
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSFileManager
import platform.posix.memcpy
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitAll
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import schwarz.digits.natrium.lifecycle.AppLifecycleState

// Service name CoreCrypto uses for its keychain-stored SQLCipher salt (see healKeystoreSaltAfterContainerMove).
private const val WIRE_KEYCHAIN_SERVICE = "wire.com"

// natrium's own keychain entry that remembers the last data-container path across launches.
private const val NATRIUM_KEYCHAIN_SERVICE = "schwarz.digits.natrium.internal"
private const val HOME_DIR_ACCOUNT = "natrium_home_dir"
// Kalium's former keychain service: the data root "<home>/Documents/natrium".
private const val LEGACY_SERVICE_SUFFIX = "/Documents/natrium"

actual class NatriumPlatform {

    internal actual val applicationId: String?
        get() = NSBundle.mainBundle.bundleIdentifier

    internal actual val platformName: String = "iOS"

    internal actual fun initialize(): CoreLogic {
        val stableService = NSBundle.mainBundle.bundleIdentifier ?: "schwarz.digits.natrium"
        val legacyHomes = moveKaliumKeychainItemsToStableService(stableService)
        healKeystoreSaltAfterContainerMove(legacyHomes)
        val rootPath = "${NSHomeDirectory()}/Documents/natrium"

        return CoreLogic(
            rootPath = rootPath,
            keychainConfig = ApplePersistenceConfig(serviceName = stableService),
            kaliumConfigs = KaliumConfigs(
                // Kalium's global and user databases are encrypted with SQLCipher.
                shouldEncryptData = { true },
                enableCalling = false,
                wipeOnCookieInvalid = true,
            ),
            userAgent = "Natrium/0.1.0 (iOS)"
        )
    }

    // --- INTERIM WORKAROUND for a CoreCrypto bug — see natrium-core/docs/bugfix-corecrypto-ios-salt.md
    //
    // CoreCrypto (keystore/src/connection/ios_wal_compat.rs) stores the SQLCipher `cipher_salt` in the
    // iOS keychain under the account `keystore_salt_<sha256(ABSOLUTE keystore path)>` (service
    // "wire.com"). It has to, because it sets cipher_plaintext_header_size = 32 for iOS
    // background-kill compatibility, which moves the salt out of the DB file. But iOS does NOT
    // guarantee a stable absolute data-container path — on app updates / redeploys / restores the
    // container UUID can change (verified on a real device). When it does, the salt's keychain key
    // changes, CoreCrypto can't find the salt, and SQLCipher fails to derive the key:
    // `MlsException.Other: Error code 1: SQL error or missing database`.
    //
    // This self-heals it: on each launch we remember the previous home directory and, if it changed,
    // copy every keystore's salt from the OLD path-derived key to the NEW one BEFORE Kalium opens
    // CoreCrypto. Non-destructive — it only ADDS the salt under the new key; existing keychain items
    // and keystore files are untouched. Remove once CoreCrypto ships a path-independent salt key.
    //
    // [legacyHomes] are home directories that older versions of the app ran in, taken from Kalium's path-based keychain
    // services (see moveKaliumKeychainItemsToStableService). Those versions didn't remember their home directory here.
    private fun healKeystoreSaltAfterContainerMove(legacyHomes: List<String>) {
        try {
            val rememberedHome = keychainReadBytes(NATRIUM_KEYCHAIN_SERVICE, HOME_DIR_ACCOUNT)?.decodeToString()
            val currentHome = NSHomeDirectory()
            val previousHomes = (listOfNotNull(rememberedHome) + legacyHomes).distinct().filter { it != currentHome }

            if (previousHomes.isNotEmpty()) {
                val root = "$currentHome/Documents/natrium"
                val fm = NSFileManager.defaultManager
                val enumerator = fm.enumeratorAtPath(root)
                while (enumerator != null) {
                    val rel = enumerator.nextObject() as? String ?: break
                    if (rel.substringAfterLast('/') != "keystore") continue
                    val newPath = "$root/$rel"
                    if (!isRegularFile(fm, newPath)) continue
                    val salt = previousHomes.firstNotNullOfOrNull { previousHome ->
                        keychainReadBytes(WIRE_KEYCHAIN_SERVICE, saltKey("$previousHome/Documents/natrium/$rel"))
                    } ?: continue
                    keychainWriteBytes(WIRE_KEYCHAIN_SERVICE, saltKey(newPath), salt)
                }
            }
            keychainWriteBytes(NATRIUM_KEYCHAIN_SERVICE, HOME_DIR_ACCOUNT, currentHome.encodeToByteArray())
        } catch (_: Throwable) {
            // Never let the workaround break initialization.
        }
    }

    // Kalium used to keep its keychain items (auth tokens, the passphrases of the databases and keystores, user settings)
    // under the service "<home>/Documents/natrium", derived from the data container's path, which iOS may change on an
    // update. Since Kalium takes the service from the app, these items would no longer be found: the session is lost and,
    // with wipeOnCookieInvalid, the local data too. They are copied once to [stableService]; an item that already exists
    // there is never overwritten, and the old one is deleted only after the copy exists. Returns the home directories
    // named by the old services.
    private fun moveKaliumKeychainItemsToStableService(stableService: String): List<String> {
        val legacyHomes = mutableSetOf<String>()
        try {
            keychainServiceAccounts()
                .filter { (service, _) -> service.endsWith(LEGACY_SERVICE_SUFFIX) && service != stableService }
                .forEach { (service, account) ->
                    legacyHomes += service.removeSuffix(LEGACY_SERVICE_SUFFIX)
                    if (keychainReadBytes(stableService, account) == null) {
                        val value = keychainReadBytes(service, account) ?: return@forEach
                        keychainWriteBytes(stableService, account, value)
                    }
                    if (keychainReadBytes(stableService, account) != null) keychainDelete(service, account)
                }
        } catch (_: Throwable) {
            // Never let the migration break initialization.
        }
        return legacyHomes.toList()
    }

    // Service and account of every generic password this app can read.
    @OptIn(ExperimentalForeignApi::class)
    private fun keychainServiceAccounts(): List<Pair<String, String>> = memScoped {
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecReturnAttributes to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitAll,
        )
        val result = alloc<CFTypeRefVar>()
        if (SecItemCopyMatching(query, result.ptr) != 0) return@memScoped emptyList()
        val items = CFBridgingRelease(result.value) as? List<*> ?: return@memScoped emptyList()
        val serviceKey = CFBridgingRelease(CFRetain(kSecAttrService)) as String
        val accountKey = CFBridgingRelease(CFRetain(kSecAttrAccount)) as String
        items.mapNotNull { item ->
            val attributes = item as? Map<*, *> ?: return@mapNotNull null
            val service = attributes[serviceKey] as? String ?: return@mapNotNull null
            val account = attributes[accountKey] as? String ?: return@mapNotNull null
            service to account
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun keychainDelete(service: String, account: String) {
        memScoped {
            SecItemDelete(
                cfDictionaryOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to CFBridgingRetain(service),
                    kSecAttrAccount to CFBridgingRetain(account),
                ),
            )
        }
    }

    // Mirrors CoreCrypto: account = "keystore_salt_" + hex(sha256(utf8(absolute_keystore_path))).
    private fun saltKey(keystorePath: String): String =
        "keystore_salt_" + keystorePath.encodeUtf8().sha256().hex()

    @OptIn(ExperimentalForeignApi::class)
    private fun isRegularFile(fm: NSFileManager, path: String): Boolean = memScoped {
        val isDir = alloc<BooleanVar>()
        fm.fileExistsAtPath(path, isDirectory = isDir.ptr) && !isDir.value
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun keychainReadBytes(service: String, account: String): ByteArray? = memScoped {
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to CFBridgingRetain(service),
            kSecAttrAccount to CFBridgingRetain(account),
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        val result = alloc<CFTypeRefVar>()
        if (SecItemCopyMatching(query, result.ptr) != 0) return@memScoped null
        val dataRef = result.value ?: return@memScoped null
        val len = CFDataGetLength(dataRef.reinterpret()).toInt()
        val src = CFDataGetBytePtr(dataRef.reinterpret())
        val out = ByteArray(len)
        if (len > 0 && src != null) out.usePinned { memcpy(it.addressOf(0), src, len.convert()) }
        CFRelease(dataRef)
        out
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun keychainWriteBytes(service: String, account: String, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        memScoped {
            SecItemDelete(
                cfDictionaryOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to CFBridgingRetain(service),
                    kSecAttrAccount to CFBridgingRetain(account),
                ),
            )
        }
        memScoped {
            val cfData = bytes.usePinned { pinned ->
                CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.convert())
            }
            SecItemAdd(
                cfDictionaryOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to CFBridgingRetain(service),
                    kSecAttrAccount to CFBridgingRetain(account),
                    kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock,
                    kSecValueData to cfData,
                ),
                null,
            )
            CFRelease(cfData)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun MemScope.cfDictionaryOf(vararg pairs: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
        val map = mapOf(*pairs)
        val keys = allocArrayOf(*map.keys.toTypedArray())
        val values = allocArrayOf(*map.values.toTypedArray())
        return CFDictionaryCreate(
            null, keys.reinterpret(), values.reinterpret(), map.size.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr,
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
