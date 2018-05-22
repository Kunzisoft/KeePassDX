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

import com.kunzisoft.keepass.collections.VariantDictionary;
import com.kunzisoft.keepass.stream.LEDataInputStream;
import com.kunzisoft.keepass.stream.LEDataOutputStream;
import com.kunzisoft.keepass.utils.Types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class KdfParameters extends VariantDictionary {

    private UUID kdfUUID;

    private static final String ParamUUID = "$UUID";

    KdfParameters(UUID uuid) {
        kdfUUID = uuid;
    }

    public UUID getUUID() {
        return kdfUUID;
    }

    protected void setParamUUID() {
        setByteArray(ParamUUID, Types.UUIDtoBytes(kdfUUID));
    }

    public static KdfParameters deserialize(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        LEDataInputStream lis = new LEDataInputStream(bis);

        VariantDictionary d = VariantDictionary.deserialize(lis);
        if (d == null) {
            return null;
        }

        UUID uuid = Types.bytestoUUID(d.getByteArray(ParamUUID));

        KdfParameters kdfP = new KdfParameters(uuid);
        kdfP.copyTo(d);
        return kdfP;
    }

    public static byte[] serialize(KdfParameters kdf) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        LEDataOutputStream los = new LEDataOutputStream(bos);

        KdfParameters.serialize(kdf, los);

        return bos.toByteArray();
    }

}
