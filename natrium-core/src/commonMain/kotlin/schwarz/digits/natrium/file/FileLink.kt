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