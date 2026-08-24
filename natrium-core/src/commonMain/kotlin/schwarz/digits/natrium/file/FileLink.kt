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