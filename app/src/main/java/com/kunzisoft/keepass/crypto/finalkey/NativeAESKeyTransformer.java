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
package com.kunzisoft.keepass.crypto.finalkey;

import com.kunzisoft.keepass.crypto.NativeLib;

import java.io.IOException;


public class NativeAESKeyTransformer extends KeyTransformer {

    public static boolean available() {
        return NativeLib.INSTANCE.init();
    }

    @Override
    public byte[] transformMasterKey(byte[] seed, byte[] key, long rounds) throws IOException {
        NativeLib.INSTANCE.init();

        return nTransformMasterKey(seed, key, rounds);
    }

    private static native byte[] nTransformMasterKey(byte[] seed, byte[] key, long rounds);
}
