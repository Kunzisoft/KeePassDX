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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VariantDictionary {
    private static final int VdVersion = 0x0100;
    private static final int VdmCritical = 0xFF00;
    private static final int VdmInfo = 0x00FF;

    private Map<String, Object> dict = new HashMap<String, Object>();

    private enum VdType {
        None(0x00),
        UInt32(0x04),
        UInt64(0x05),
        Bool(0x08),
        Int32(0x0C),
        Int64(0x0D),
        String(0x18),
        ByteArray(0x42);


        private final byte value;

        VdType(int value) {
            this.value = (byte) value;
        }

        boolean equals(byte type) {
            return type == value;
        }

        byte getValue() {
            return value;
        }
    }

    public void setUInt32(String name, long value) {
        dict.put(name, value);
    }
    public long getUInt32(String name) { return (long)dict.get(name); }

    public void setUInt64(String name, long value) {
        dict.put(name, value);
    }
    public long getUInt64(String name) { return (long)dict.get(name); }

    public void setBool(String name, boolean value) {
        dict.put(name, value);
    }

    public void setInt32(String name, int value) {
        dict.put(name, value);
    }

    public void setInt64(String name, long value) {
        dict.put(name, value);
    }

    public void setString(String name, String value) {
        dict.put(name, value);
    }

    public void setByteArray(String name, byte[] value) {
        dict.put(name, value);
    }
    public byte[] getByteArray(String name) { return (byte[])dict.get(name); }

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
            if (VdType.None.equals(bType)) {
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

            if (VdType.UInt32.equals(bType)) {
                if (valueLen == 4) {
                    d.setUInt32(name, LEDataInputStream.readUInt(valueBuf, 0));
                }
            }
            else if (VdType.UInt64.equals(bType)) {
                if (valueLen == 8) {
                    d.setUInt64(name, LEDataInputStream.readLong(valueBuf, 0));
                }
            }
            else if (VdType.Bool.equals(bType)) {
                if (valueLen == 1) {
                    d.setBool(name, valueBuf[0] != 0);
                }
            }
            else if (VdType.Int32.equals(bType)) {
                if (valueLen == 4) {
                    d.setInt32(name, LEDataInputStream.readInt(valueBuf, 0));
                }
            }
            else if (VdType.Int64.equals(bType)) {
                if (valueLen == 8) {
                    d.setInt64(name, LEDataInputStream.readLong(valueBuf, 0));
                }
            }
            else if (VdType.String.equals(bType)) {
                d.setString(name, new String(valueBuf, "UTF-8"));
            }
            else if (VdType.ByteArray.equals(bType)) {
                d.setByteArray(name, valueBuf);
            }
            else {
                assert(false);
            }
        }

        return d;
    }

    public void copyTo(VariantDictionary d) {
       for (Map.Entry<String, Object> entry : d.dict.entrySet()) {
           String key = entry.getKey();
           Object value = entry.getValue();

           dict.put(key, value);
       }
    }
}
