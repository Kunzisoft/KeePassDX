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

import java.io.IOException;

public class Argon2Native {

    public static byte[] transformKey(byte[] password, byte[] salt, int parallelism,
                                              long memory, long iterations, byte[] secretKey,
                                              byte[] associatedData, long version) throws IOException {
        NativeLib.INSTANCE.init();

        return nTransformMasterKey(password, salt, parallelism, memory, iterations, secretKey, associatedData, version);
    }

    private static native byte[] nTransformMasterKey(byte[] password, byte[] salt, int parallelism,
                                              long memory, long iterations, byte[] secretKey,
                                              byte[] associatedData, long version) throws IOException;
}
