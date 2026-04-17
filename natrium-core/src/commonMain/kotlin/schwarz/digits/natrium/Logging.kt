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