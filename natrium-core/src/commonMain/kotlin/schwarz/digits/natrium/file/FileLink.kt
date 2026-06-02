/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.file

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okio.FileSystem
import okio.Path


interface FileLink {
    val id: String
    val mimeType: String
    val dataPath: Path
    val dataSize: Long
    val fileName: String
    val uploadedAt: Instant

    companion object {
        fun fromLocal(dataPath: Path, fileName: String, mimeType: String, dataSize: Long): FileLink =
            LocalFileLink(dataPath, fileName, mimeType, dataSize)
    }
}

private class LocalFileLink(
    override val dataPath: Path,
    override val fileName: String,
    override val mimeType: String,
    override val dataSize: Long,
) : FileLink {
    override val id: String = dataPath.name
    override val uploadedAt: Instant = Clock.System.now()
}