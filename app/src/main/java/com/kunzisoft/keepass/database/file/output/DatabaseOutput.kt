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
package com.kunzisoft.keepass.database.file.output

import com.kunzisoft.keepass.database.file.DatabaseHeader
import com.kunzisoft.keepass.database.exception.DatabaseOutputException

import java.io.OutputStream
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

abstract class DatabaseOutput<Header : DatabaseHeader> {

    @Throws(DatabaseOutputException::class)
    protected open fun setIVs(header: Header): SecureRandom {
        val random: SecureRandom
        try {
            random = SecureRandom.getInstance("SHA1PRNG")
        } catch (e: NoSuchAlgorithmException) {
            throw DatabaseOutputException("Does not support secure random number generation.", e)
        }

        random.nextBytes(header.encryptionIV)
        random.nextBytes(header.masterSeed)

        return random
    }

    @Throws(DatabaseOutputException::class)
    abstract fun writeDatabase(outputStream: OutputStream,
                               assignMasterKey: () -> Unit)

}