/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.action

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.hardware.HardwareKey

class UpdateCompressionBinariesDatabaseRunnable (
    context: Context,
    database: ContextualDatabase,
    private val oldCompressionAlgorithm: CompressionAlgorithm,
    private val newCompressionAlgorithm: CompressionAlgorithm,
    saveDatabase: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : SaveDatabaseRunnable(
    context,
    database,
    saveDatabase,
    null,
    challengeResponseRetriever
) {

    override fun onStartRun() {
        // Set new compression
        if (database.allowDataCompression) {
            try {
                database.apply {
                    updateDataBinaryCompression(oldCompressionAlgorithm, newCompressionAlgorithm)
                    compressionAlgorithm = newCompressionAlgorithm
                }
            } catch (e: Exception) {
                setError(e)
            }
        }

        super.onStartRun()
    }

    override fun onFinishRun() {
        super.onFinishRun()

        if (database.allowDataCompression) {
            if (!result.isSuccess) {
                try {
                    database.apply {
                        compressionAlgorithm = oldCompressionAlgorithm
                        updateDataBinaryCompression(newCompressionAlgorithm, oldCompressionAlgorithm)
                    }
                } catch (e: Exception) {
                    setError(e)
                }
            }
        }
    }
}
