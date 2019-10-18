/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.crypto.keyDerivation

import com.kunzisoft.keepass.database.ObjectNameResource

import java.io.IOException
import java.io.Serializable
import java.util.UUID

// TODO Parcelable
abstract class KdfEngine : ObjectNameResource, Serializable {

    var uuid: UUID? = null

    abstract val defaultParameters: KdfParameters

    @Throws(IOException::class)
    abstract fun transform(masterKey: ByteArray, p: KdfParameters): ByteArray

    abstract fun randomize(p: KdfParameters)

    /*
     * ITERATIONS
     */

    abstract fun getKeyRounds(p: KdfParameters): Long

    abstract fun setKeyRounds(p: KdfParameters, keyRounds: Long)

    abstract val defaultKeyRounds: Long

    open val minKeyRounds: Long
        get() = 1

    open val maxKeyRounds: Long
        get() = Int.MAX_VALUE.toLong()

    /*
     * MEMORY
     */

    open fun getMemoryUsage(p: KdfParameters): Long {
        return UNKNOWN_VALUE.toLong()
    }

    open fun setMemoryUsage(p: KdfParameters, memory: Long) {
        // Do nothing by default
    }

    open val defaultMemoryUsage: Long
        get() = UNKNOWN_VALUE.toLong()

    open val minMemoryUsage: Long
        get() = 1

    open val maxMemoryUsage: Long
        get() = Int.MAX_VALUE.toLong()

    /*
     * PARALLELISM
     */

    open fun getParallelism(p: KdfParameters): Int {
        return UNKNOWN_VALUE
    }

    open fun setParallelism(p: KdfParameters, parallelism: Int) {
        // Do nothing by default
    }

    open val defaultParallelism: Int
        get() = UNKNOWN_VALUE

    open val minParallelism: Int
        get() = 1

    open val maxParallelism: Int
        get() = Int.MAX_VALUE

    companion object {
        const val UNKNOWN_VALUE = -1
    }
}
