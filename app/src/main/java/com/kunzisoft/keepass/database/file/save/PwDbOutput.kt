/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.file.save

import com.kunzisoft.keepass.database.file.PwDbHeader
import com.kunzisoft.keepass.database.exception.PwDbOutputException

import java.io.OutputStream
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

abstract class PwDbOutput<Header : PwDbHeader> protected constructor(protected var mOS: OutputStream) {

    @Throws(PwDbOutputException::class)
    protected open fun setIVs(header: Header): SecureRandom {
        val random: SecureRandom
        try {
            random = SecureRandom.getInstance("SHA1PRNG")
        } catch (e: NoSuchAlgorithmException) {
            throw PwDbOutputException("Does not support secure random number generation.")
        }

        random.nextBytes(header.encryptionIV)
        random.nextBytes(header.masterSeed)

        return random
    }

    @Throws(PwDbOutputException::class)
    abstract fun output()

    @Throws(PwDbOutputException::class)
    abstract fun outputHeader(outputStream: OutputStream): Header

}