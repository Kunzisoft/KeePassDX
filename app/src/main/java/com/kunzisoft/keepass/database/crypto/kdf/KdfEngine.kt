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
package com.kunzisoft.keepass.database.crypto.kdf

import com.kunzisoft.keepass.utils.UnsignedInt
import java.io.IOException
import java.io.Serializable
import java.util.*

// TODO Parcelable
abstract class KdfEngine : Serializable {

    var uuid: UUID? = null

    abstract val defaultParameters: KdfParameters

    @Throws(IOException::class)
    abstract fun transform(masterKey: ByteArray, kdfParameters: KdfParameters): ByteArray

    abstract fun randomize(kdfParameters: KdfParameters)

    /*
     * ITERATIONS
     */

    abstract fun getKeyRounds(kdfParameters: KdfParameters): Long

    abstract fun setKeyRounds(kdfParameters: KdfParameters, keyRounds: Long)

    abstract val defaultKeyRounds: Long

    open val minKeyRounds: Long
        get() = 1

    open val maxKeyRounds: Long
        get() = UnsignedInt.MAX_VALUE.toKotlinLong()

    /*
     * MEMORY
     */

    open fun getMemoryUsage(kdfParameters: KdfParameters): Long {
        return UNKNOWN_VALUE
    }

    open fun setMemoryUsage(kdfParameters: KdfParameters, memory: Long) {
        // Do nothing by default
    }

    open val defaultMemoryUsage: Long
        get() = UNKNOWN_VALUE

    open val minMemoryUsage: Long
        get() = 1

    open val maxMemoryUsage: Long
        get() = UnsignedInt.MAX_VALUE.toKotlinLong()

    /*
     * PARALLELISM
     */

    open fun getParallelism(kdfParameters: KdfParameters): Long {
        return UNKNOWN_VALUE
    }

    open fun setParallelism(kdfParameters: KdfParameters, parallelism: Long) {
        // Do nothing by default
    }

    open val defaultParallelism: Long
        get() = UNKNOWN_VALUE

    open val minParallelism: Long
        get() = 1L

    open val maxParallelism: Long
        get() = UnsignedInt.MAX_VALUE.toKotlinLong()

    companion object {
        const val UNKNOWN_VALUE: Long = -1L
    }
}
