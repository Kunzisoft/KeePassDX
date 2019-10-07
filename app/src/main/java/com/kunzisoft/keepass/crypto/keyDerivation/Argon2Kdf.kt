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

import android.content.res.Resources
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.Types
import java.io.IOException
import java.security.SecureRandom
import java.util.*

class Argon2Kdf internal constructor() : KdfEngine() {

    override val defaultParameters: KdfParameters
        get() {
            val p = KdfParameters(uuid)

            p.setParamUUID()
            p.setUInt32(ParamParallelism, DefaultParallelism)
            p.setUInt64(ParamMemory, DefaultMemory)
            p.setUInt64(ParamIterations, DefaultIterations)
            p.setUInt32(ParamVersion, MaxVersion)

            return p
        }

    override val defaultKeyRounds: Long
        get() = DefaultIterations

    init {
        uuid = CIPHER_UUID
    }

    override fun getName(resources: Resources): String {
        return resources.getString(R.string.kdf_Argon2)
    }

    @Throws(IOException::class)
    override fun transform(masterKey: ByteArray, p: KdfParameters): ByteArray {

        val salt = p.getByteArray(ParamSalt)
        val parallelism = p.getUInt32(ParamParallelism).toInt()
        val memory = p.getUInt64(ParamMemory)
        val iterations = p.getUInt64(ParamIterations)
        val version = p.getUInt32(ParamVersion)
        val secretKey = p.getByteArray(ParamSecretKey)
        val assocData = p.getByteArray(ParamAssocData)

        return Argon2Native.transformKey(masterKey, salt, parallelism, memory, iterations,
                secretKey, assocData, version)
    }

    override fun randomize(p: KdfParameters) {
        val random = SecureRandom()

        val salt = ByteArray(32)
        random.nextBytes(salt)

        p.setByteArray(ParamSalt, salt)
    }

    override fun getKeyRounds(p: KdfParameters): Long {
        return p.getUInt64(ParamIterations)
    }

    override fun setKeyRounds(p: KdfParameters, keyRounds: Long) {
        p.setUInt64(ParamIterations, keyRounds)
    }

    override fun getMemoryUsage(p: KdfParameters): Long {
        return p.getUInt64(ParamMemory)
    }

    override fun setMemoryUsage(p: KdfParameters, memory: Long) {
        p.setUInt64(ParamMemory, memory)
    }

    override fun getDefaultMemoryUsage(): Long {
        return DefaultMemory
    }

    override fun getParallelism(p: KdfParameters): Int {
        return p.getUInt32(ParamParallelism).toInt() // TODO Verify
    }

    override fun setParallelism(p: KdfParameters, parallelism: Int) {
        p.setUInt32(ParamParallelism, parallelism.toLong())
    }

    override fun getDefaultParallelism(): Int {
        return DefaultParallelism.toInt() // TODO Verify
    }

    companion object {

        val CIPHER_UUID: UUID = Types.bytestoUUID(
                byteArrayOf(0xEF.toByte(), 0x63.toByte(), 0x6D.toByte(), 0xDF.toByte(), 0x8C.toByte(), 0x29.toByte(), 0x44.toByte(), 0x4B.toByte(), 0x91.toByte(), 0xF7.toByte(), 0xA9.toByte(), 0xA4.toByte(), 0x03.toByte(), 0xE3.toByte(), 0x0A.toByte(), 0x0C.toByte()))

        private const val ParamSalt = "S" // byte[]
        private const  val ParamParallelism = "P" // UInt32
        private const  val ParamMemory = "M" // UInt64
        private const  val ParamIterations = "I" // UInt64
        private const  val ParamVersion = "V" // UInt32
        private const  val ParamSecretKey = "K" // byte[]
        private const  val ParamAssocData = "A" // byte[]

        private const  val MinVersion: Long = 0x10
        private const  val MaxVersion: Long = 0x13

        private const  val MinSalt = 8
        private const  val MaxSalt = Integer.MAX_VALUE

        private const  val MinIterations: Long = 1
        private const  val MaxIterations = 4294967295L

        private const  val MinMemory = (1024 * 8).toLong()
        private const  val MaxMemory = Integer.MAX_VALUE.toLong()

        private const  val MinParallelism = 1
        private const  val MaxParallelism = (1 shl 24) - 1

        private const  val DefaultIterations: Long = 2
        private const  val DefaultMemory = (1024 * 1024).toLong()
        private const  val DefaultParallelism: Long = 2
    }
}
