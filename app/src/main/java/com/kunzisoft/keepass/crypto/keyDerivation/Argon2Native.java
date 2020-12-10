/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.crypto.keyDerivation;

import com.kunzisoft.keepass.crypto.NativeLib;
import com.kunzisoft.keepass.utils.UnsignedInt;

import java.io.IOException;

public class Argon2Native {

    enum CType {
        ARGON2_D(0),
        ARGON2_I(1),
        ARGON2_ID(2);

        int cValue = 0;

        CType(int i) {
            cValue = i;
        }
    }

    public static byte[] transformKey(Argon2Kdf.Type type, byte[] password, byte[] salt, UnsignedInt parallelism,
                                      UnsignedInt memory, UnsignedInt iterations, byte[] secretKey,
                                      byte[] associatedData, UnsignedInt version) throws IOException {
        NativeLib.INSTANCE.init();

        CType cType = CType.ARGON2_D;
        if (type.equals(Argon2Kdf.Type.ARGON2_ID))
            cType = CType.ARGON2_ID;

        return nTransformMasterKey(
                cType.cValue,
                password,
                salt,
                parallelism.toKotlinInt(),
                memory.toKotlinInt(),
                iterations.toKotlinInt(),
                secretKey,
                associatedData,
                version.toKotlinInt());
    }

    private static native byte[] nTransformMasterKey(int type, byte[] password, byte[] salt, int parallelism,
                                              int memory, int iterations, byte[] secretKey,
                                              byte[] associatedData, int version) throws IOException;
}
