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
package com.kunzisoft.keepass.utils;

import com.kunzisoft.keepass.stream.LittleEndianDataInputStream;
import com.kunzisoft.keepass.stream.LittleEndianDataOutputStream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static com.kunzisoft.keepass.stream.StreamBytesUtilsKt.bytes4ToUInt;
import static com.kunzisoft.keepass.stream.StreamBytesUtilsKt.bytes64ToLong;

public class VariantDictionary {
    private static final int VdVersion = 0x0100;
    private static final int VdmCritical = 0xFF00;
    private static final int VdmInfo = 0x00FF;
    private static Charset UTF8Charset = Charset.forName("UTF-8");

    private Map<String, VdType> dict = new HashMap<>();

    private class VdType {
        public static final byte None = 0x00;
        public static final byte UInt32 = 0x04;
        public static final byte UInt64 =0x05;
        public static final byte Bool =0x08;
        public static final byte Int32 =0x0C;
        public static final byte Int64 =0x0D;
        public static final byte String =0x18;
        public static final byte ByteArray =0x42;

        public final byte type;
        public final Object value;

        VdType(byte type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private Object getValue(String name) {
        VdType val = dict.get(name);
        if (val == null) {
            return null;
        }

        return val.value;
    }
    private void putType(byte type, String name, Object value) {
        dict.put(name, new VdType(type, value));
    }

    public void setUInt32(String name, UnsignedInt value) { putType(VdType.UInt32, name, value); }
    public UnsignedInt getUInt32(String name) { return (UnsignedInt)dict.get(name).value; }

    public void setUInt64(String name, long value) { putType(VdType.UInt64, name, value); }
    public long getUInt64(String name) { return (long)dict.get(name).value; }

    public void setBool(String name, boolean value) { putType(VdType.Bool, name, value); }
    public boolean getBool(String name) { return (boolean)dict.get(name).value; }

    public void setInt32(String name, int value) { putType(VdType.Int32 ,name, value); }
    public int getInt32(String name) { return (int)dict.get(name).value; }

    public void setInt64(String name, long value) { putType(VdType.Int64 ,name, value); }
    public long getInt64(String name) { return (long)dict.get(name).value; }

    public void setString(String name, String value) { putType(VdType.String ,name, value); }
    public String getString(String name) { return (String)getValue(name); }

    public void setByteArray(String name, byte[] value) { putType(VdType.ByteArray, name, value); }
    public byte[] getByteArray(String name) { return (byte[])getValue(name); }

    public static VariantDictionary deserialize(LittleEndianDataInputStream lis) throws IOException {
        VariantDictionary d = new VariantDictionary();

        int version = lis.readUShort();
        if ((version & VdmCritical) > (VdVersion & VdmCritical)) {
            throw new IOException("Invalid format");
        }

        while(true) {
            int type = lis.read();
            if (type < 0) {
                throw new IOException(("Invalid format"));
            }

            byte bType = (byte)type;
            if (bType == VdType.None) {
                break;
            }

            int nameLen = lis.readUInt().toInt();
            byte[] nameBuf = lis.readBytes(nameLen);
            if (nameLen != nameBuf.length) {
                throw new IOException("Invalid format");
            }
            String name = new String(nameBuf, UTF8Charset);

            int valueLen = lis.readUInt().toInt();
            byte[] valueBuf = lis.readBytes(valueLen);
            if (valueLen != valueBuf.length) {
                throw new IOException("Invalid format");
            }

            switch (bType) {
                case VdType.UInt32:
                    if (valueLen == 4) {
                        d.setUInt32(name, bytes4ToUInt(valueBuf));
                    }
                    break;
                case VdType.UInt64:
                    if (valueLen == 8) {
                        d.setUInt64(name, bytes64ToLong(valueBuf));
                    }
                    break;
                case VdType.Bool:
                    if (valueLen == 1) {
                        d.setBool(name, valueBuf[0] != 0);
                    }
                    break;
                case VdType.Int32:
                    if (valueLen == 4) {
                        d.setInt32(name, bytes4ToUInt(valueBuf).toInt());
                    }
                    break;
                case VdType.Int64:
                    if (valueLen == 8) {
                        d.setInt64(name, bytes64ToLong(valueBuf));
                    }
                    break;
                case VdType.String:
                    d.setString(name, new String(valueBuf, UTF8Charset));
                    break;
                case VdType.ByteArray:
                    d.setByteArray(name, valueBuf);
                    break;
                default:
                    break;
            }
        }

        return d;
    }

    public static void serialize(VariantDictionary d,
                                 LittleEndianDataOutputStream los) throws IOException{
        if (los == null) {
            return;
        }

        los.writeUShort(VdVersion);

        for (Map.Entry<String, VdType> entry: d.dict.entrySet()) {
            String name = entry.getKey();
            byte[] nameBuf = nameBuf = name.getBytes(UTF8Charset);

            VdType vd = entry.getValue();

            los.write(vd.type);
            los.writeInt(nameBuf.length);
            los.write(nameBuf);

            byte[] buf;
            switch (vd.type) {
                case VdType.UInt32:
                    los.writeInt(4);
                    los.writeUInt((UnsignedInt) vd.value);
                    break;
                case VdType.UInt64:
                    los.writeInt(8);
                    los.writeLong((long)vd.value);
                    break;
                case VdType.Bool:
                    los.writeInt(1);
                    byte bool = (boolean)vd.value ? (byte)1 : (byte)0;
                    los.write(bool);
                    break;
                case VdType.Int32:
                    los.writeInt(4);
                    los.writeInt((int)vd.value);
                    break;
                case VdType.Int64:
                    los.writeInt(8);
                    los.writeLong((long)vd.value);
                    break;
                case VdType.String:
                    String value = (String)vd.value;
                    buf = value.getBytes(UTF8Charset);
                    los.writeInt(buf.length);
                    los.write(buf);
                    break;
                case VdType.ByteArray:
                    buf = (byte[])vd.value;
                    los.writeInt(buf.length);
                    los.write(buf);
                    break;
                default:
                    break;
            }
        }

        los.write(VdType.None);
    }

    public void copyTo(VariantDictionary d) {
       for (Map.Entry<String, VdType> entry : d.dict.entrySet()) {
           String key = entry.getKey();
           VdType value = entry.getValue();
           dict.put(key, value);
       }
    }

    public int size() {
        return dict.size();
    }
}
