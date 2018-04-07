/*
 * Copyright 2017 Brian Pellin.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.keepassdroid.database.save;

import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.PwDbHeaderV4.PwDbInnerHeaderV4Fields;
import com.keepassdroid.database.PwDbHeaderV4.KdbxBinaryFlags;
import com.keepassdroid.database.security.ProtectedBinary;
import com.keepassdroid.stream.LEDataOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class PwDbInnerHeaderOutputV4 {
    private PwDatabaseV4 db;
    private PwDbHeaderV4 header;
    private LEDataOutputStream los;

    public PwDbInnerHeaderOutputV4(PwDatabaseV4 db, PwDbHeaderV4 header, OutputStream os) {
        this.db = db;
        this.header = header;

        this.los = new LEDataOutputStream(os);
    }

    public void output() throws IOException {
        los.write(PwDbInnerHeaderV4Fields.InnerRandomStreamID);
        los.writeInt(4);
        los.writeInt(header.innerRandomStream.id);

        int streamKeySize = header.innerRandomStreamKey.length;
        los.write(PwDbInnerHeaderV4Fields.InnerRandomstreamKey);
        los.writeInt(streamKeySize);
        los.write(header.innerRandomStreamKey);

        for (ProtectedBinary bin : db.getBinPool().binaries()) {
            byte flag = KdbxBinaryFlags.None;
            if (bin.isProtected()) {
                flag |= KdbxBinaryFlags.Protected;
            }

            byte[] binData = bin.getData();
            los.write(PwDbInnerHeaderV4Fields.Binary);
            los.writeInt(bin.length() + 1);
            los.write(flag);
            los.write(binData);

            Arrays.fill(binData, (byte)0);
        }

        los.write(PwDbInnerHeaderV4Fields.EndOfHeader);
        los.writeInt(0);
    }

}
