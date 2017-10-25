/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.crypto.keyDerivation;

import com.keepassdroid.collections.VariantDictionary;
import com.keepassdroid.stream.LEDataInputStream;
import com.keepassdroid.utils.Types;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

public class KdfParameters extends VariantDictionary {

    public UUID kdfUUID;

    private static final String ParamUUID = "$UUID";

    public KdfParameters(UUID uuid) {
        kdfUUID = uuid;
    }

    public static KdfParameters deserialize(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        LEDataInputStream lis = new LEDataInputStream(bis);

        VariantDictionary d = VariantDictionary.deserialize(lis);
        if (d == null) {
            assert(false);
            return null;
        }

        UUID uuid = Types.bytestoUUID(d.getByteArray(ParamUUID));
        if (uuid == null) {
            assert(false);
            return null;
        }

        KdfParameters kdfP = new KdfParameters(uuid);
        kdfP.copyTo(d);
        return kdfP;

    }

}
