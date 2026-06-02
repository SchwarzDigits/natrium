/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium

import com.wire.kalium.common.logger.CoreLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger

/**
* Log level for Natrium and Kalium.
*
* Wraps [KaliumLogLevel] so that consumers don't need Kalium imports.
*/
enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR, DISABLED;

    internal fun toKalium(): KaliumLogLevel = when (this) {
        VERBOSE -> KaliumLogLevel.VERBOSE
        DEBUG -> KaliumLogLevel.DEBUG
        INFO -> KaliumLogLevel.INFO
        WARN -> KaliumLogLevel.WARN
        ERROR -> KaliumLogLevel.ERROR
        DISABLED -> KaliumLogLevel.DISABLED
    }
}

private var loggingInitialized = false

/**
 * Initializes logging for Natrium and the underlying Kalium SDK.
 *
 * Should be called once at app startup before [schwarz.digits.natrium.Natrium.initialize].
 * If not called, logging defaults to [LogLevel.WARN] when Natrium is initialized.
 */
fun initLogging(level: LogLevel = LogLevel.WARN) {
    CoreLogger.init(KaliumLogger.Config(level.toKalium()))
    loggingInitialized = true
}

internal fun ensureLoggingInitialized() {
    if (!loggingInitialized) initLogging()
}