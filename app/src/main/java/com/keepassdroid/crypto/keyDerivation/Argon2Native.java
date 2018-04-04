/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.crypto.keyDerivation;

import com.keepassdroid.crypto.NativeLib;

import java.io.IOException;

public class Argon2Native {

    public static byte[] transformKey(byte[] password, byte[] salt, int parallelism,
                                              long memory, long iterations, byte[] secretKey,
                                              byte[] associatedData, long version) throws IOException {
        NativeLib.init();

        return nTransformMasterKey(password, salt, parallelism, memory, iterations, secretKey, associatedData, version);
    }

    private static native byte[] nTransformMasterKey(byte[] password, byte[] salt, int parallelism,
                                              long memory, long iterations, byte[] secretKey,
                                              byte[] associatedData, long version) throws IOException;
}
