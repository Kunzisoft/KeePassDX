/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.security;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ProtectedBinary implements Parcelable {

    private static final String TAG = ProtectedBinary.class.getName();
	public final static ProtectedBinary EMPTY = new ProtectedBinary();

	private boolean protect;
	private byte[] data;
	private File dataFile;
	private int size;

	public boolean isProtected() {
		return protect;
	}
	
	public long length() {
        if (data != null)
            return data.length;
        if (dataFile != null)
            return size;
        return 0;
	}
	
	private ProtectedBinary() {
        this.protect = false;
        this.data = null;
        this.dataFile = null;
        this.size = 0;
	}
	
	public ProtectedBinary(boolean enableProtection, byte[] data) {
		this.protect = enableProtection;
		this.data = data;
		this.dataFile = null;
		if (data != null)
		    this.size = data.length;
		else
		    this.size = 0;
	}

    public ProtectedBinary(boolean enableProtection, File dataFile, int size) {
        this.protect = enableProtection;
        this.data = null;
        this.dataFile = dataFile;
        this.size = size;
    }

	private ProtectedBinary(Parcel in) {
		protect = in.readByte() != 0;
		in.readByteArray(data);
        dataFile = new File(in.readString());
        size = in.readInt();
	}

    public InputStream getData() throws IOException {
        if (data != null)
        	return new ByteArrayInputStream(data);
	    else if (dataFile != null)
            return new FileInputStream(dataFile);
        else
        	return null;
    }

    public void clear() {
		data = null;
		if (dataFile != null && !dataFile.delete())
		    Log.e(TAG, "Unable to delete temp file " + dataFile.getAbsolutePath());
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProtectedBinary that = (ProtectedBinary) o;
        return protect == that.protect &&
                size == that.size &&
                Arrays.equals(data, that.data) &&
                dataFile != null &&
                dataFile.equals(that.dataFile);
    }

    @Override
    public int hashCode() {

        int result = 0;
        result = 31 * result + (protect ? 1 : 0);
        result = 31 * result + dataFile.hashCode();
        result = 31 * result + Integer.valueOf(size).hashCode();
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByte((byte) (protect ? 1 : 0));
		dest.writeByteArray(data);
		dest.writeString(dataFile.getAbsolutePath());
		dest.writeInt(size);
	}

	public static final Creator<ProtectedBinary> CREATOR = new Creator<ProtectedBinary>() {
		@Override
		public ProtectedBinary createFromParcel(Parcel in) {
			return new ProtectedBinary(in);
		}

		@Override
		public ProtectedBinary[] newArray(int size) {
			return new ProtectedBinary[size];
		}
	};

}
