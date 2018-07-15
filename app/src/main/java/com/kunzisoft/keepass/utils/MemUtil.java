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
package com.kunzisoft.keepass.utils;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MemUtil {
	public static byte[] decompress(byte[] input) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(input);
		GZIPInputStream gzis = new GZIPInputStream(bais);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Util.copyStream(gzis, baos);
		
		return baos.toByteArray();
	}
	
	public static byte[] compress(byte[] input) throws IOException {		
		ByteArrayInputStream bais = new ByteArrayInputStream(input);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);
		Util.copyStream(bais, gzos);
		gzos.close();
		
		return baos.toByteArray();
	}

	// For writing to a Parcel
	public static <K extends Parcelable,V extends Parcelable> void writeParcelableMap(
			Parcel parcel, int flags, Map<K, V > map) {
		parcel.writeInt(map.size());
		for(Map.Entry<K, V> e : map.entrySet()){
			parcel.writeParcelable(e.getKey(), flags);
			parcel.writeParcelable(e.getValue(), flags);
		}
	}

	// For reading from a Parcel
	public static <K extends Parcelable,V extends Parcelable> Map<K,V> readParcelableMap(
			Parcel parcel, Class<K> kClass, Class<V> vClass) {
		int size = parcel.readInt();
		Map<K, V> map = new HashMap<K, V>(size);
		for(int i = 0; i < size; i++){
			map.put(kClass.cast(parcel.readParcelable(kClass.getClassLoader())),
					vClass.cast(parcel.readParcelable(vClass.getClassLoader())));
		}
		return map;
	}

    // For writing map with string key to a Parcel
    public static <V extends Parcelable> void writeStringParcelableMap(
            Parcel parcel, int flags, Map<String, V> map) {
        parcel.writeInt(map.size());
        for(Map.Entry<String, V> e : map.entrySet()){
            parcel.writeString(e.getKey());
            parcel.writeParcelable(e.getValue(), flags);
        }
    }

    // For reading map with string key from a Parcel
    public static <V extends Parcelable> HashMap<String,V> readStringParcelableMap(
            Parcel parcel, Class<V> vClass) {
        int size = parcel.readInt();
        HashMap<String, V> map = new HashMap<>(size);
        for(int i = 0; i < size; i++){
            map.put(parcel.readString(),
                    vClass.cast(parcel.readParcelable(vClass.getClassLoader())));
        }
        return map;
    }


	// For writing map with string key and string value to a Parcel
    public static void writeStringParcelableMap(Parcel dest, Map<String, String> map) {
        dest.writeInt(map.size());
        for(Map.Entry<String, String> e : map.entrySet()){
            dest.writeString(e.getKey());
            dest.writeString(e.getValue());
        }
    }

	// For reading map with string key and string value from a Parcel
	public static HashMap<String, String> readStringParcelableMap(Parcel in) {
        int size = in.readInt();
		HashMap<String, String> map = new HashMap<>(size);
        for(int i = 0; i < size; i++){
            map.put(in.readString(),
                    in.readString());
        }
        return map;
    }
}
