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