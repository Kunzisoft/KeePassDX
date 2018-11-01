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
import android.support.annotation.NonNull;
import android.util.Log;

import com.kunzisoft.keepass.stream.ActionReadBytes;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MemUtil {

    private static final String TAG = MemUtil.class.getName();
    public static final int BUFFER_SIZE_BYTES = 3 * 128;

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE_BYTES];
        int read;
        try {
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        } catch (OutOfMemoryError error) {
            throw new IOException(error);
        }
    }

    public static void readBytes(@NonNull InputStream inputStream, ActionReadBytes actionReadBytes)
            throws IOException {
        byte[] buffer = new byte[MemUtil.BUFFER_SIZE_BYTES];
        int read = 0;
        while (read != -1) {
            read = inputStream.read(buffer, 0, buffer.length);
            if (read != -1) {
                byte[] optimizedBuffer;
                if (buffer.length == read) {
                    optimizedBuffer = buffer;
                } else {
                    optimizedBuffer = Arrays.copyOf(buffer, read);
                }
                actionReadBytes.doAction(optimizedBuffer);
            }
        }

        /*
        byte[] buffer = new byte[BUFFER_SIZE_BYTES];
        // To create the last buffer who is smaller
        long numberOfFullBuffer = length / buffer.length;
        long sizeOfFullBuffers = numberOfFullBuffer * buffer.length;
        int read = 0;
        //if (protectedBinary.length() > 0) {
        while (read < length) {
            // Create the last smaller buffer
            if (read >= sizeOfFullBuffers)
                buffer = new byte[(int) (length % buffer.length)];
            read += inputStream.read(buffer, 0, buffer.length);
            actionReadBytes.doAction(buffer);
        }
        //*/
    }

	public static byte[] decompress(byte[] input) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(input);
		GZIPInputStream gzis = new GZIPInputStream(bais);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copyStream(gzis, baos);
		
		return baos.toByteArray();
	}

    public static byte[] compress(byte[] input) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(input);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(baos);
        copyStream(bais, gzos);
        gzos.close();

        return baos.toByteArray();
    }

    /**
     * Compresses the input data using GZip and outputs the compressed data.
     *
     * @param input
     *         An {@link InputStream} containing the input raw data.
     *
     * @return An {@link InputStream} to the compressed data.
     */
    public static InputStream compress(final InputStream input) {
        final PipedInputStream compressedDataStream = new PipedInputStream(3 * 128);
        Log.d(TAG, "About to compress input data using gzip asynchronously...");
        PipedOutputStream compressionOutput;
        GZIPOutputStream gzipCompressedDataStream = null;
        try {
            compressionOutput = new PipedOutputStream(compressedDataStream);
            gzipCompressedDataStream = new GZIPOutputStream(compressionOutput);
            IOUtils.copy(input, gzipCompressedDataStream);
            Log.e(TAG, "Successfully compressed input data using gzip.");
        } catch (IOException e) {
            Log.e(TAG, "Failed to compress input data.", e);
        } finally {
            if (gzipCompressedDataStream != null) {
                try {
                    gzipCompressedDataStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close gzip output stream.", e);
                }
            }
        }
        return compressedDataStream;
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
