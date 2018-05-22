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
package com.kunzisoft.keepass.crypto.keyDerivation;

import com.kunzisoft.keepass.database.exception.UnknownKDF;

import java.util.ArrayList;
import java.util.List;

public class KdfFactory {

    public static AesKdf aesKdf = new AesKdf();
    public static Argon2Kdf argon2Kdf = new Argon2Kdf();

    public static List<KdfEngine> kdfListV3 = new ArrayList<>();
    public static List<KdfEngine> kdfListV4 = new ArrayList<>();

    static {
        kdfListV3.add(aesKdf);

        kdfListV4.add(aesKdf);
        kdfListV4.add(argon2Kdf);
    }

    public static KdfEngine getEngineV4(KdfParameters kdfParameters) throws UnknownKDF {
        UnknownKDF unknownKDFException = new UnknownKDF();
        if (kdfParameters == null) {
            throw unknownKDFException;
        }
        for (KdfEngine engine: kdfListV4) {
            if (engine.getUUID().equals(kdfParameters.getUUID())) {
                return engine;
            }
        }
        throw unknownKDFException;
    }

}
