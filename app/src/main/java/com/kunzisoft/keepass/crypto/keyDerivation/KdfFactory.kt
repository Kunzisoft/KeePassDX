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

import com.kunzisoft.keepass.database.exception.UnknownKDF

import java.util.ArrayList

object KdfFactory {

    var aesKdf = AesKdf()
    var argon2Kdf = Argon2Kdf()

    var kdfListV3: MutableList<KdfEngine> = ArrayList()
    var kdfListV4: MutableList<KdfEngine> = ArrayList()

    init {
        kdfListV3.add(aesKdf)

        kdfListV4.add(aesKdf)
        kdfListV4.add(argon2Kdf)
    }

    @Throws(UnknownKDF::class)
    fun getEngineV4(kdfParameters: KdfParameters?): KdfEngine {
        val unknownKDFException = UnknownKDF()
        if (kdfParameters == null) {
            throw unknownKDFException
        }
        for (engine in kdfListV4) {
            if (engine.uuid == kdfParameters.uuid) {
                return engine
            }
        }
        throw unknownKDFException
    }

}
