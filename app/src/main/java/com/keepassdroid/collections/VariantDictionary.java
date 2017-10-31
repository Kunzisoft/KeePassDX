/*
 * Copyright 2017 Brian Pellin.
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
package com.keepassdroid.collections;

import com.keepassdroid.stream.LEDataInputStream;
import com.keepassdroid.stream.LEDataOutputStream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VariantDictionary {
    private static final int VdVersion = 0x0100;
    private static final int VdmCritical = 0xFF00;
    private static final int VdmInfo = 0x00FF;

    private Map<String, VdType> dict = new HashMap<String, VdType>();

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

    public void setUInt32(String name, long value) { putType(VdType.UInt32, name, value); }
    public long getUInt32(String name) { return (long)dict.get(name).value; }

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

    public static VariantDictionary deserialize(LEDataInputStream lis) throws IOException {
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

            int nameLen = lis.readInt();
            byte[] nameBuf = lis.readBytes(nameLen);
            if (nameLen != nameBuf.length) {
                throw new IOException("Invalid format");
            }
            String name = new String(nameBuf, "UTF-8");

            int valueLen = lis.readInt();
            byte[] valueBuf = lis.readBytes(valueLen);
            if (valueLen != valueBuf.length) {
                throw new IOException("Invalid format");
            }

            switch (bType) {
                case VdType.UInt32:
                    if (valueLen == 4) {
                        d.setUInt32(name, LEDataInputStream.readUInt(valueBuf, 0));
                    }
                    break;
                case VdType.UInt64:
                    if (valueLen == 8) {
                        d.setUInt64(name, LEDataInputStream.readLong(valueBuf, 0));
                    }
                    break;
                case VdType.Bool:
                    if (valueLen == 1) {
                        d.setBool(name, valueBuf[0] != 0);
                    }
                    break;
                case VdType.Int32:
                    if (valueLen == 4) {
                        d.setInt32(name, LEDataInputStream.readInt(valueBuf, 0));
                    }
                    break;
                case VdType.Int64:
                    if (valueLen == 8) {
                        d.setInt64(name, LEDataInputStream.readLong(valueBuf, 0));
                    }
                    break;
                case VdType.String:
                    d.setString(name, new String(valueBuf, "UTF-8"));
                    break;
                case VdType.ByteArray:
                    d.setByteArray(name, valueBuf);
                    break;
                default:
                    assert (false);
                    break;
            }
        }

        return d;
    }

    public static void serialize(VariantDictionary d, LEDataOutputStream los) throws IOException{
        if (los == null) {
            assert(false);
            return;
        }

        los.writeUShort(VdVersion);

        for (Map.Entry<String, VdType> entry: d.dict.entrySet()) {
            String name = entry.getKey();
            byte[] nameBuf = null;
            try {
                nameBuf = name.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                assert(false);
                throw new IOException("Couldn't encode parameter name.");
            }

            VdType vd = entry.getValue();

            los.write(vd.type);
            los.writeInt(nameBuf.length);
            los.write(nameBuf);

            byte[] buf;
            switch (vd.type) {
                case VdType.UInt32:
                    los.writeInt(4);
                    los.writeUInt((long)vd.value);
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
                    buf = value.getBytes("UTF-8");
                    los.writeInt(buf.length);
                    los.write(buf);
                    break;
                case VdType.ByteArray:
                    buf = (byte[])vd.value;
                    los.writeInt(buf.length);
                    los.write(buf);
                    break;
                default:
                    assert(false);
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
;
    public int size() {
        return dict.size();
    }
}
