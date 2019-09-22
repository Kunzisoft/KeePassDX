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
import java.util.UUID

abstract class KdfEngine : ObjectNameResource {

    var uuid: UUID? = null

    abstract val defaultParameters: KdfParameters

    abstract val defaultKeyRounds: Long

    @Throws(IOException::class)
    abstract fun transform(masterKey: ByteArray, p: KdfParameters): ByteArray

    abstract fun randomize(p: KdfParameters)

    abstract fun getKeyRounds(p: KdfParameters): Long

    abstract fun setKeyRounds(p: KdfParameters, keyRounds: Long)

    open fun getMemoryUsage(p: KdfParameters): Long {
        return UNKNOWN_VALUE.toLong()
    }

    open fun setMemoryUsage(p: KdfParameters, memory: Long) {
        // Do nothing by default
    }

    open fun getDefaultMemoryUsage(): Long {
        return UNKNOWN_VALUE.toLong()
    }

    open fun getParallelism(p: KdfParameters): Int {
        return UNKNOWN_VALUE
    }

    open fun setParallelism(p: KdfParameters, parallelism: Int) {
        // Do nothing by default
    }

    open fun getDefaultParallelism(): Int {
        return UNKNOWN_VALUE
    }

    companion object {
        const val UNKNOWN_VALUE = -1
    }
}
