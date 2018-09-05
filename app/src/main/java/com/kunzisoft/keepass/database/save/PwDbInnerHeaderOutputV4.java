/*
 * Copyright 2017 Brian Pellin.
 *
 * This file is part of KeePass DX.
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
package com.kunzisoft.keepass.database.save;

import com.kunzisoft.keepass.database.PwDatabaseV4;
import com.kunzisoft.keepass.database.PwDbHeaderV4;
import com.kunzisoft.keepass.database.security.ProtectedBinary;
import com.kunzisoft.keepass.stream.LEDataOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
        los.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.InnerRandomStreamID);
        los.writeInt(4);
        los.writeInt(header.innerRandomStream.id);

        int streamKeySize = header.innerRandomStreamKey.length;
        los.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.InnerRandomstreamKey);
        los.writeInt(streamKeySize);
        los.write(header.innerRandomStreamKey);

        for (ProtectedBinary protectedBinary : db.getBinPool().binaries()) {
            byte flag = PwDbHeaderV4.KdbxBinaryFlags.None;
            if (protectedBinary.isProtected()) {
                flag |= PwDbHeaderV4.KdbxBinaryFlags.Protected;
            }

            los.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.Binary);
            los.writeInt((int) protectedBinary.length() + 1); // TODO verify
            los.write(flag);

            /*
            MemUtil.readBytes(protectedBinary.getData(),
                    (int) protectedBinary.length(),
                    buffer -> los.write(buffer));
            */

            byte[] buffer = new byte[3 * 256]; // TODO buffer generalize
            InputStream fileInputStream = protectedBinary.getData(); // TODO Nullable
            // To create the last buffer who is smaller
            long numberOfFullBuffer = protectedBinary.length() / buffer.length;
            long sizeOfFullBuffers = numberOfFullBuffer * buffer.length;
            int read = 0;
            //if (protectedBinary.length() > 0) {
            while (read < protectedBinary.length()) {
                // Create the last smaller buffer
                if (read >= sizeOfFullBuffers)
                    buffer = new byte[(int) (protectedBinary.length() % buffer.length)];
                read += fileInputStream.read(buffer, 0, buffer.length);
                los.write(buffer);
            }
        }

        los.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.EndOfHeader);
        los.writeInt(0);
    }

}
