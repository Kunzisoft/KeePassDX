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
package com.kunzisoft.encrypt.argon2;

import java.io.IOException;

public class NativeArgon2KeyTransformer {

    enum CType {
        ARGON2_D(0),
        ARGON2_I(1),
        ARGON2_ID(2);

        int cValue = 0;

        CType(int i) {
            cValue = i;
        }
    }

    public static native byte[] nTransformKey(int type, byte[] password, byte[] salt, int parallelism,
                                              int memory, int iterations, byte[] secretKey,
                                              byte[] associatedData, int version) throws IOException;
}
