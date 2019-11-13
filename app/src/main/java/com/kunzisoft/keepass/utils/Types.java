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
 *
KeePass for J2ME

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This file was derived from 

Java clone of KeePass - A KeePass file viewer for Java
Copyright 2006 Bill Zwicky <billzwicky@users.sourceforge.net>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package com.kunzisoft.keepass.utils;

import com.kunzisoft.keepass.stream.LEDataOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;


/**
 * Tools for slicing and dicing Java and KeePass data types.
 *
 * @author Bill Zwicky <wrzwicky@pobox.com>
 */
public class Types {

    public static long ULONG_MAX_VALUE = -1;

    private static Charset defaultCharset = Charset.forName("UTF-8");

    private static final byte[] CRLFbuf = { 0x0D, 0x0A };
    private static final String CRLF = new String(CRLFbuf);
    private static final String SEP = System.getProperty("line.separator");
    private static final boolean REPLACE = !SEP.equals(CRLF);

    /** Read an unsigned byte */
    public static int readUByte( byte[] buf, int offset ) {
        return ((int)buf[offset] & 0xFF);
    }

    /**
     * Write an unsigned byte
     */
    public static void writeUByte(int val, byte[] buf, int offset) {
        buf[offset] = (byte)(val & 0xFF);
    }

    public static byte writeUByte(int val) {
        byte[] buf = new byte[1];

        writeUByte(val, buf, 0);

        return buf[0];
    }

    /**
     * Return len of null-terminated string (i.e. distance to null)
     * within a byte buffer.
     */
    private static int strlen( byte[] buf, int offset ) {
        int len = 0;
        while( buf[offset + len] != 0 )
            len++;
        return len;
    }

    public static String readCString(byte[] buf, int offset) {
        String jstring = new String(buf, offset, strlen(buf, offset), defaultCharset);

        if ( REPLACE ) {
            jstring = jstring.replace(CRLF, SEP);
        }

        return jstring;
    }

    public static int writeCString(String str, OutputStream os) throws IOException {
        if ( str == null ) {
            // Write out a null character
            os.write(LEDataOutputStream.writeIntBuf(1));
            os.write(0x00);
            return 0;
        }

        if ( REPLACE ) {
            str = str.replace(SEP, CRLF);
        }

        byte[] initial = str.getBytes(defaultCharset);

        int length = initial.length+1;
        os.write(LEDataOutputStream.writeIntBuf(length));
        os.write(initial);
        os.write(0x00);

        return length;
    }

    public static String readPassword(byte[] buf, int offset) {
        return new String(buf, offset, strlen(buf, offset), defaultCharset);
    }

    public static int writePassword(String str, OutputStream os) throws IOException {
        byte[] initial = str.getBytes(defaultCharset);
        int length = initial.length+1;
        os.write(LEDataOutputStream.writeIntBuf(length));
        os.write(initial);
        os.write(0x00);
        return length;
    }

    public static byte[] readBytes(byte[] buf, int offset, int len) {
        byte[] binaryData = new byte[len];
        System.arraycopy(buf, offset, binaryData, 0, len);
        return binaryData;
    }

    public static int writeBytes(byte[] data, int dataLen, OutputStream os ) throws IOException  {
        os.write(LEDataOutputStream.writeIntBuf(dataLen));
        if (data != null) {
            os.write(data);
        }
        return dataLen;
    }

    public static UUID bytestoUUID(byte[] buf) {
        return bytestoUUID(buf, 0);
    }

    public static UUID bytestoUUID(byte[] buf, int offset) {
        long lsb = 0;
        for (int i = 15; i >= 8; i--) {
            lsb = (lsb << 8) | (buf[i + offset] & 0xff);
        }

        long msb = 0;
        for (int i = 7; i >= 0; i--) {
            msb = (msb << 8) | (buf[i + offset] & 0xff);
        }

        return new UUID(msb, lsb);
    }

    public static byte[] UUIDtoBytes(UUID uuid) {
        byte[] buf = new byte[16];
        LEDataOutputStream.writeLong(uuid.getMostSignificantBits(), buf, 0);
        LEDataOutputStream.writeLong(uuid.getLeastSignificantBits(), buf, 8);
        return buf;
    }

}
